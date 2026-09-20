package com.driverapp.repartidor.ui.tracking

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.driverapp.repartidor.App
import com.driverapp.repartidor.R
import com.driverapp.repartidor.databinding.FragmentTrackingBinding
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Geo
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.repository.LocationRepository
import com.driverapp.repartidor.ui.common.ConfirmDialog
import com.driverapp.repartidor.ui.main.mainVm
import com.driverapp.repartidor.ui.main.trackingVm
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackingFragment : Fragment() {

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private val vm: TrackingViewModel by lazy { trackingVm() }
    private val mainViewModel by lazy { mainVm() }
    private val locationRepo: LocationRepository by lazy {
        (requireActivity().application as App).container.locationRepository
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Configuration.getInstance().userAgentValue = requireContext().packageName
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(15.0)

        binding.btnComplete.setOnClickListener { confirmComplete() }
        binding.btnCall.setOnClickListener { callRestaurant() }
        binding.btnItems.setOnClickListener { showItems() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.activeOrder.collect { render(it) } }
                launch { mainViewModel.user.collect { renderUser(it?.vehicle) } }
                launch {
                    locationRepo.location.collect {
                        renderLive()
                        updateCurrentMarker()
                    }
                }
                launch {
                    while (true) {
                        updateClock()
                        delay(15_000)
                    }
                }
            }
        }
        renderUser(mainViewModel.user.value?.vehicle)
        render(vm.activeOrder.value)
        renderLive()
    }

    private fun renderUser(vehicle: String?) {
        val v = vehicle ?: "Bicicleta"
        binding.curVehicle.text = v
        val icon = when (v.lowercase()) {
            "moto" -> R.drawable.ic_fa_motorcycle
            "auto", "carro" -> R.drawable.ic_fa_car
            else -> R.drawable.ic_fa_bicycle
        }
        binding.curVehicleIcon.setImageResource(icon)
    }

    private fun updateClock() {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.curTime.text = fmt.format(Date())
    }

    private fun render(order: Pedido?) {
        if (order == null) {
            binding.availableCard.isVisible = true
            binding.deliveryCard.isVisible = false
            binding.curStatus.text = "Disponible"
            setMapMarkers(null)
            return
        }
        binding.deliveryCard.isVisible = true
        binding.availableCard.isVisible = false
        binding.curStatus.text = "En Entrega"
        binding.trackOrderId.text = "Orden #${order.id}"
        binding.trackPrice.text = "$" + String.format(Locale.ROOT, "%.2f", order.total)
        binding.trackRestaurant.text = order.restaurant?.name ?: "Restaurante"
        binding.trackRestaurantAddress.text = order.restaurant?.address ?: "-"
        binding.trackClient.text = order.clientName
        binding.trackClientAddress.text = order.clientAddress
        setMapMarkers(order)
        renderLive()
    }

    private fun isOnTheWay(order: Pedido): Boolean = order.estado == EstadoPedido.EN_CAMINO

    private fun originPoint(order: Pedido) =
        order.restaurant?.let { GeoPoint(it.lat, it.lng) }

    private fun renderLive() {
        val order = vm.activeOrder.value
        val loc = locationRepo.location.value
        if (order == null) {
            if (loc == null) {
                binding.curLocation.text = "Localizando…"
            } else {
                binding.curLocation.text = "${"%.5f".format(loc.lat)}, ${"%.5f".format(loc.lng)}"
            }
            return
        }
        if (loc == null) {
            binding.liveTime.text = "--"
            binding.liveDistance.text = "--"
            binding.liveHint.text = if (locationRepo.isTracking()) {
                "Activando GPS…"
            } else {
                "Activa la ubicación en tiempo real desde Perfil"
            }
            binding.curLocation.text = "Localizando…"
            return
        }

        binding.curLocation.text = "${"%.5f".format(loc.lat)}, ${"%.5f".format(loc.lng)}"

        val target: org.osmdroid.util.GeoPoint? =
            if (isOnTheWay(order)) GeoPoint(order.destLat, order.destLng) else originPoint(order)
        if (target == null) {
            binding.liveTime.text = "--"
            binding.liveDistance.text = "--"
            binding.liveHint.text = "Destino sin coordenadas"
            return
        }

        val meters = Geo.distanceMeters(loc.lat, loc.lng, target.latitude, target.longitude)
        val gpsKmh: Double? = loc.speedMps?.takeIf { it in 0.6f..60f }?.let { (it * 3.6).toDouble() }
        val speed = gpsKmh ?: Geo.vehicleSpeedKmh(mainViewModel.user.value?.vehicle)
        val minutes = Geo.estimateMinutes(meters, speed)
        binding.liveTime.text = etaLabel(minutes)
        binding.liveDistance.text = Geo.formatDistance(meters)
        binding.liveHint.text = if (gpsKmh != null) {
            "Velocidad actual ${"%.0f".format(gpsKmh)} km/h · en tiempo real"
        } else {
            "ETA estimado a velocidad promedio de ${mainViewModel.user.value?.vehicle ?: "Bicicleta"}".replace("de Bicicleta", "de ${mainViewModel.user.value?.vehicle ?: "Bicicleta"}")
        }
    }

    private fun etaLabel(min: Int): String {
        val h = min / 60
        val m = min % 60
        return if (h > 0) "$h:${"%02d".format(m)} hrs" else "0:${"%02d".format(m)} hrs"
    }

    private fun tintedIcon(resId: Int, color: Int): android.graphics.drawable.Drawable? {
        val d = requireContext().getDrawable(resId)?.mutate() ?: return null
        androidx.core.graphics.drawable.DrawableCompat.setTint(d, color)
        return d
    }

    private var currentMarker: Marker? = null

    private fun setMapMarkers(order: Pedido?) {
        binding.mapView.overlays.removeAll { it is Marker }
        currentMarker = null
        if (order == null) {
            updateCurrentMarker()
            return
        }
        val origin = originPoint(order)
        val dest = GeoPoint(order.destLat, order.destLng)
        origin?.let { geo ->
            binding.mapView.overlays.add(Marker(binding.mapView).apply {
                position = geo
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = order.restaurant?.name
                icon = tintedIcon(R.drawable.ic_fa_shop, android.graphics.Color.rgb(229, 57, 53))
            })
        }
        binding.mapView.overlays.add(Marker(binding.mapView).apply {
            position = dest
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = order.clientName
            icon = tintedIcon(R.drawable.ic_fa_location_dot, android.graphics.Color.rgb(59, 130, 246))
        })
        val center = if (origin != null) {
            GeoPoint((origin.latitude + dest.latitude) / 2.0, (origin.longitude + dest.longitude) / 2.0)
        } else dest
        binding.mapView.controller.setZoom(15.0)
        binding.mapView.controller.setCenter(center)
        binding.mapView.invalidate()
    }

    private fun updateCurrentMarker() {
        val loc = locationRepo.location.value ?: return
        val map = binding.mapView
        if (currentMarker == null) {
            currentMarker = Marker(map).apply {
                position = GeoPoint(loc.lat, loc.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "Mi ubicación"
                icon = tintedIcon(R.drawable.ic_fa_map_location_dot, android.graphics.Color.rgb(59, 130, 246))
            }
            map.overlays.add(currentMarker!!)
        } else {
            currentMarker!!.position = GeoPoint(loc.lat, loc.lng)
        }
        map.invalidate()
    }

    private fun callRestaurant() {
        val phone = vm.activeOrder.value?.restaurant?.phone
        if (phone.isNullOrBlank()) {
            vm.showMessage("El restaurante no tiene teléfono registrado")
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        } catch (_: Exception) {
            vm.showMessage("No se pudo abrir el marcador")
        }
    }

    private fun showItems() {
        val order = vm.activeOrder.value ?: return
        if (order.items.isEmpty()) {
            vm.showMessage("La orden no tiene items (pedido directo)")
            return
        }
        val lines = order.items.map { "${it.quantity}× ${it.productName}" }
        AlertDialog.Builder(requireContext())
            .setTitle("Items de la orden #${order.id}")
            .setItems(lines.toTypedArray(), null)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun confirmComplete() {
        val order = vm.activeOrder.value ?: return
        ConfirmDialog(
            requireContext(),
            "Completar Entrega",
            "¿Confirmas que entregaste la orden #${order.id} correctamente?",
            confirmText = "Sí, entregado",
            icon = R.drawable.ic_fa_circle_check,
            iconBg = R.drawable.bg_icon_green,
            iconTint = android.graphics.Color.rgb(16, 185, 129),
            confirmStyle = false,
        ) {
            vm.complete(order) {
                vm.showMessage("¡Pedido completado! 🎉")
            }
        }.show()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDetach()
        _binding = null
    }
}
