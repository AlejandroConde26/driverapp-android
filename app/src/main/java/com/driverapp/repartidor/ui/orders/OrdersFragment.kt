package com.driverapp.repartidor.ui.orders

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.driverapp.repartidor.R
import com.driverapp.repartidor.databinding.FragmentOrdersBinding
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.ui.common.ConfirmDialog
import com.driverapp.repartidor.ui.common.ErrorState
import com.driverapp.repartidor.ui.common.LoadingState
import com.driverapp.repartidor.ui.main.MainActivity
import com.driverapp.repartidor.ui.main.mainVm
import com.driverapp.repartidor.ui.main.ordersVm
import kotlinx.coroutines.launch
import java.util.Locale

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private val vm: OrdersViewModel by lazy { ordersVm() }
    private val mainViewModel by lazy { mainVm() }

    private val adapter = OrdersAdapter(
        onDetail = { showDetail(it) },
        onAccept = { confirmAccept(it) },
        onReject = { confirmReject(it) },
        onTrack = { (activity as? MainActivity)?.goToMap() },
        currentUserId = { mainViewModel.user.value?.id }
    )
    private var currentTab = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.tabAssigned.setOnClickListener { selectTab(0) }
        binding.tabHistory.setOnClickListener { selectTab(1) }
        binding.backButton.setOnClickListener { backToList() }
        binding.detailContent.detailAccept.setOnClickListener { currentDetail?.let { confirmAccept(it) } }
        binding.detailContent.detailReject.setOnClickListener { currentDetail?.let { confirmReject(it) } }
        binding.emptyView.setOnClickListener {
            if (vm.error.value != ErrorState.None) vm.retry()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.available.collect { onData() } }
                launch { vm.history.collect { onData() } }
                launch { vm.loading.collect { binding.loading.isVisible = it is LoadingState.Loading } }
                launch { vm.error.collect { onData() } }
            }
        }

        selectTab(0)
    }

    private fun onData() {
        updateTabLabels()
        val err = vm.error.value
        if (currentTab == 0) {
            adapter.submitList(vm.available.value)
            val empty = vm.available.value.isEmpty()
            binding.emptyView.isVisible = empty
            binding.emptyView.text = errorOr("Sin pedidos por ahora", err, empty)
        } else {
            adapter.submitList(vm.history.value)
            val empty = vm.history.value.isEmpty()
            binding.emptyView.isVisible = empty
            binding.emptyView.text = errorOr("Sin historial todavía", err, empty)
        }
    }

    private fun errorOr(default: String, err: ErrorState, empty: Boolean): String =
        if (empty && err is ErrorState.Message) err.text
        else if (empty && err is ErrorState.SinConexion) "${err.text} · toca para reintentar"
        else default

    private fun updateTabLabels() {
        binding.tabAssigned.text = "Asignados (${vm.available.value.size})"
        binding.tabHistory.text = "Historial (${vm.history.value.size})"
    }

    private fun selectTab(index: Int) {
        currentTab = index
        val assigned = index == 0
        binding.tabAssigned.setBackgroundResource(if (assigned) R.drawable.bg_pill_active else android.R.color.transparent)
        binding.tabHistory.setBackgroundResource(if (assigned) android.R.color.transparent else R.drawable.bg_pill_active)
        binding.tabAssigned.setTextColor(ContextCompat.getColor(requireContext(), if (assigned) R.color.white else R.color.text_muted))
        binding.tabHistory.setTextColor(ContextCompat.getColor(requireContext(), if (assigned) R.color.text_muted else R.color.white))
        binding.recycler.scrollToPosition(0)
        updateTabLabels()
        if (index == 0) {
            adapter.isHistory = false
            adapter.submitList(vm.available.value)
            binding.emptyView.isVisible = vm.available.value.isEmpty()
            binding.emptyView.text = "Sin pedidos por ahora"
            vm.loadAll()
        } else {
            adapter.isHistory = true
            adapter.submitList(vm.history.value)
            binding.emptyView.isVisible = vm.history.value.isEmpty()
            binding.emptyView.text = "Sin historial todavía"
            vm.loadAll()
        }
    }

    private var currentDetail: Pedido? = null

    private fun showDetail(order: Pedido) {
        currentDetail = order
        val d = binding.detailContent
        d.detailOrderId.text = "Orden #${order.id}"
        d.detailPrice.text = "$" + String.format(Locale.ROOT, "%.2f", order.total)
        d.detailAvatar.text = order.clientName.take(2).uppercase(Locale.ROOT)
        d.detailClient.text = order.clientName
        d.detailAddress.text = order.clientAddress
        d.detailRestaurant.text = order.restaurant?.name ?: "Restaurante"
        d.detailRestaurantAddress.text = order.restaurant?.address ?: "-"
        d.detailTime.text = formatMinutes(order.estTimeMin)
        d.detailDistance.text = "${order.distanceKm} km"
        d.detailPayment.text = order.paymentMethod
        d.detailFee.text = "$" + String.format(Locale.ROOT, "%.2f", order.deliveryFee)
        d.detailItemCount.text = "${order.items.size} ${if (order.items.size == 1) "item" else "items"}"
        renderItems(d.detailItems, order)

        val mine = order.driverId == mainViewModel.user.value?.id
        if (mine && order.estado == EstadoPedido.ACEPTADO) {
            d.detailStatus.isVisible = true
            d.detailAccept.isVisible = false
            d.detailReject.isVisible = false
        } else if (order.isPast) {
            d.detailStatus.isVisible = true
            d.detailStatus.text = if (order.estado == EstadoPedido.ENTREGADO) "Entregado" else "Rechazado"
            d.detailStatus.setBackgroundResource(if (order.estado == EstadoPedido.ENTREGADO) R.drawable.bg_badge_green else R.drawable.bg_badge_red)
            d.detailStatus.setTextColor(if (order.estado == EstadoPedido.ENTREGADO) Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68))
            d.detailAccept.isVisible = false
            d.detailReject.isVisible = false
        } else {
            d.detailStatus.isVisible = false
            d.detailAccept.isVisible = true
            d.detailReject.isVisible = true
            d.detailAccept.text = "Aceptar Pedido"
        }

        binding.listContainer.isVisible = false
        binding.detailContainer.isVisible = true
    }

    private fun renderItems(container: android.widget.LinearLayout, order: Pedido) {
        container.removeAllViews()
        order.items.forEach { item ->
            val row = LinearLayoutCompat(requireContext())
            row.orientation = android.widget.LinearLayout.HORIZONTAL
            row.gravity = android.view.Gravity.CENTER_VERTICAL
            row.setPadding(0, 6, 0, 6)
            val name = TextView(requireContext()).apply {
                text = "${item.quantity}× ${item.productName}"
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            }
            val price = TextView(requireContext()).apply {
                text = "$" + String.format(Locale.ROOT, "%.2f", item.quantity * item.unitPrice)
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            }
            row.addView(name)
            row.addView(price)
            container.addView(row)
        }
    }

    private fun backToList(): Boolean {
        if (binding.detailContainer.isVisible) {
            currentDetail = null
            binding.detailContainer.isVisible = false
            binding.listContainer.isVisible = true
            return true
        }
        return false
    }

    fun onBack(): Boolean = backToList()

    private fun formatMinutes(min: Int): String {
        val h = min / 60
        val m = min % 60
        return if (h > 0) "$h:${"%02d".format(m)} hrs" else "0:${"%02d".format(m)} hrs"
    }

    private fun confirmAccept(order: Pedido) {
        if (order.driverId == mainViewModel.user.value?.id && order.estado == EstadoPedido.ACEPTADO) {
            (activity as? MainActivity)?.goToMap()
            return
        }
        ConfirmDialog(
            requireContext(),
            "Aceptar Pedido",
            "¿Quieres aceptar la orden #${order.id} y dirigirte al restaurante?",
            confirmText = "Aceptar",
            icon = R.drawable.ic_fa_circle_check,
            iconBg = R.drawable.bg_icon_green,
            iconTint = Color.rgb(16, 185, 129),
            confirmStyle = false,
        ) {
            vm.accept(order) {
                currentDetail = null
                binding.detailContainer.isVisible = false
                binding.listContainer.isVisible = true
                (activity as? MainActivity)?.goToMap()
            }
        }.show()
    }

    private fun confirmReject(order: Pedido) {
        ConfirmDialog(
            requireContext(),
            "Rechazar Pedido",
            "¿Seguro que quieres rechazar la orden #${order.id}?",
            confirmText = "Rechazar",
            icon = R.drawable.ic_fa_circle_exclamation,
            iconBg = R.drawable.bg_icon_red,
            iconTint = Color.rgb(229, 57, 53),
        ) {
            vm.reject(order)
            currentDetail = null
            binding.detailContainer.isVisible = false
            binding.listContainer.isVisible = true
        }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun LinearLayoutCompat(context: android.content.Context) =
    androidx.appcompat.widget.LinearLayoutCompat(context)
