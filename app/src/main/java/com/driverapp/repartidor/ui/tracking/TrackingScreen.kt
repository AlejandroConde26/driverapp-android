package com.driverapp.repartidor.ui.tracking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Geo
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.repository.LocationRepository
import com.driverapp.repartidor.ui.components.BusyOverlay
import com.driverapp.repartidor.ui.components.ConfirmDialog
import com.driverapp.repartidor.ui.components.etaLabel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

/**
 * Pestaña Mapa (Sprint 3): restaurante, cliente y posición GPS en osmdroid,
 * con distancia y ETA en vivo. El destino del ETA depende del estado:
 *  - ACEPTADO  → restaurante (hay que recoger el pedido)
 *  - EN_CAMINO → cliente (se marca con el botón "Salí del restaurante")
 */
@Composable
fun TrackingScreen(
    vm: TrackingViewModel,
    locationRepo: LocationRepository,
    vehicle: String?,
    modifier: Modifier = Modifier
) {
    val order by vm.activeOrder.collectAsStateWithLifecycle()
    val location by locationRepo.location.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    var confirmStart by remember { mutableStateOf(false) }
    var confirmComplete by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val googleMap = remember { mutableStateOf<GoogleMap?>(null) }
    var centeredOnce by remember { mutableStateOf(false) }
    var recenter by remember { mutableStateOf(0) }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Google Maps exige Play Services; sin él no hay mapa que mostrar.
    val hasPlayServices = remember {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
            ConnectionResult.SUCCESS
    }

    // Ciclo de vida del MapView: Google Maps necesita onCreate/onResume/onPause/onDestroy
    // o la vista se queda en blanco al volver a la pestaña.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewState.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewState.value?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewState.value?.onPause()
            mapViewState.value?.onDestroy()
            mapViewState.value = null
            googleMap.value = null
        }
    }

    LaunchedEffect(Unit) { vm.refreshActive() }

    Column(modifier.fillMaxSize()) {
        if (!hasPlayServices) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.map_play_services_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            onCreate(null)
                            onResume()
                            mapViewState.value = this
                            getMapAsync { map ->
                                map.uiSettings.isZoomControlsEnabled = false
                                map.uiSettings.isMyLocationButtonEnabled = false
                                // Punto azul nativo: se mueve en tiempo real sin que la
                                // app tenga que repintar marcadores.
                                if (hasLocationPermission) {
                                    runCatching { map.isMyLocationEnabled = true }
                                }
                                googleMap.value = map
                            }
                        }
                    },
                    update = { view ->
                        // `googleMap` y `recenter` se leen aquí: cuando cambian, esta
                        // lambda se vuelve a ejecutar.
                        val map = googleMap.value ?: return@AndroidView
                        recenter.let { }
                        // Si el permiso se concedió con la pantalla abierta, se activa aquí.
                        if (hasLocationPermission && !map.isMyLocationEnabled) {
                            runCatching { map.isMyLocationEnabled = true }
                        }
                        map.clear()

                        val currentOrder = order
                        val currentLocation = location

                        currentOrder?.restaurant?.let { restaurant ->
                            map.addMarker(
                                MarkerOptions()
                                    .position(LatLng(restaurant.lat, restaurant.lng))
                                    .title(restaurant.name)
                            )
                        }
                        currentOrder?.let {
                            map.addMarker(
                                MarkerOptions()
                                    .position(LatLng(it.destLat, it.destLng))
                                    .title(it.clientName)
                            )
                        }
                        // Sin punto azul nativo (permiso pendiente) se dibuja el marcador.
                        if (!hasLocationPermission) {
                            currentLocation?.let {
                                map.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(it.lat, it.lng))
                                        .title(context.getString(R.string.map_my_location))
                                )
                            }
                        }

                        // Centrar: restaurante > mi ubicación > destino. Sin pedido activo
                        // cae en la posición del repartidor, que es lo que se espera.
                        if (!centeredOnce) {
                            val target = currentOrder?.restaurant?.let { LatLng(it.lat, it.lng) }
                                ?: currentLocation?.let { LatLng(it.lat, it.lng) }
                                ?: currentOrder?.let { LatLng(it.destLat, it.destLng) }
                            if (target != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 16f))
                                centeredOnce = true
                            }
                        }
                    }
                )

                // Volver a centrar en la posición del repartidor tras mover el mapa.
                if (hasLocationPermission) {
                    FilledTonalButton(
                        onClick = {
                            centeredOnce = false
                            recenter++
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fa_location_crosshairs),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.map_center_me),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            val current = order
            if (current == null) {
                Text(
                    stringResource(R.string.map_no_active_order),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.map_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ActiveOrderCard(
                    order = current,
                    vehicle = vehicle,
                    location = location,
                    isTracking = locationRepo.isTracking()
                )

                Spacer(Modifier.height(14.dp))

                val onTheWay = current.estado == EstadoPedido.EN_CAMINO
                if (!onTheWay) {
                    Button(
                        onClick = { confirmStart = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.map_start_delivery))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = { confirmComplete = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.map_complete_delivery))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        openRoute(context, current.destLat, current.destLng, current.clientName, vm)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.map_open_route))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val phone = current.clientPhone
                        if (phone.isNullOrBlank()) {
                            vm.showMessage(R.string.map_no_client_phone)
                        } else {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }.onFailure { vm.showMessage(R.string.map_dial_error) }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.map_call_client))
                }
            }
        }
    }

    if (confirmStart) {
        order?.let { current ->
            ConfirmDialog(
                title = stringResource(R.string.confirm_start_title),
                message = stringResource(R.string.confirm_start_body, current.id.toString()),
                confirmText = stringResource(R.string.action_start),
                onConfirm = {
                    vm.marcarEnCamino(current)
                    confirmStart = false
                },
                onDismiss = { confirmStart = false }
            )
        }
    }

    if (confirmComplete) {
        order?.let { current ->
            ConfirmDialog(
                title = stringResource(R.string.confirm_complete_title),
                message = stringResource(R.string.confirm_complete_body, current.id.toString()),
                confirmText = stringResource(R.string.action_delivered),
                onConfirm = {
                    vm.complete(current)
                    confirmComplete = false
                },
                onDismiss = { confirmComplete = false }
            )
        }
    }

    BusyOverlay(busy)
}

@Composable
private fun ActiveOrderCard(
    order: Pedido,
    vehicle: String?,
    location: com.driverapp.repartidor.domain.model.Ubicacion?,
    isTracking: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.order_label, order.id.toString()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.map_on_delivery),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(order.clientName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                order.clientAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                (order.restaurant?.name ?: "") + " · " + (order.restaurant?.address ?: "-"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            val onTheWay = order.estado == EstadoPedido.EN_CAMINO
            if (location == null) {
                Text(
                    text = if (isTracking) {
                        stringResource(R.string.map_activating_gps)
                    } else {
                        stringResource(R.string.map_activate_location)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val targetLat = if (onTheWay) order.destLat else order.restaurant?.lat
                val targetLng = if (onTheWay) order.destLng else order.restaurant?.lng
                if (targetLat == null || targetLng == null) {
                    Text(
                        stringResource(R.string.map_no_coordinates),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val meters = Geo.distanceMeters(location.lat, location.lng, targetLat, targetLng)
                    val gpsKmh = location.speedMps
                        ?.takeIf { it in 0.6f..60f }
                        ?.let { (it * 3.6).toDouble() }
                    val speed = gpsKmh ?: Geo.vehicleSpeedKmh(vehicle)
                    val minutes = Geo.estimateMinutes(meters, speed)

                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text(
                                stringResource(R.string.order_eta),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(etaLabel(minutes), style = MaterialTheme.typography.titleMedium)
                        }
                        Column {
                            Text(
                                stringResource(R.string.order_distance),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                Geo.formatDistance(meters),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (gpsKmh != null) {
                            stringResource(R.string.map_live_speed, String.format(Locale.ROOT, "%.0f", gpsKmh))
                        } else {
                            stringResource(R.string.map_eta_avg) + " · " + (vehicle ?: "Bicicleta")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Abre la ruta al destino, de la opción más específica a la más genérica:
 * app de Google Maps en modo conducción → intents `geo:` → Google Maps web.
 * Si el teléfono no tiene Maps instalado, el primer intent lanza
 * ActivityNotFoundException y se prueba el siguiente.
 */
private fun openRoute(
    context: android.content.Context,
    lat: Double,
    lng: Double,
    label: String?,
    vm: TrackingViewModel
) {
    val candidates = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng&mode=d"))
            .setPackage("com.google.android.apps.maps"),
        Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label.orEmpty())})")),
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
        )
    )
    for (intent in candidates) {
        if (runCatching { context.startActivity(intent) }.isSuccess) return
    }
    vm.showMessage(R.string.map_route_error)
}
