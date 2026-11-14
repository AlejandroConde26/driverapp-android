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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

/** Relleno del encuadre: los marcadores no deben quedar pegados al borde. */
private const val MAP_FIT_PADDING_PX = 140

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
    val order by vm.displayedOrder.collectAsStateWithLifecycle()
    val disponibles by vm.availableOrders.collectAsStateWithLifecycle()
    val location by locationRepo.location.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    var confirmStart by remember { mutableStateOf(false) }
    var confirmComplete by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val googleMap = remember { mutableStateOf<GoogleMap?>(null) }
    var centeredOnce by remember { mutableStateOf(false) }
    var recenter by remember { mutableStateOf(0) }
    var fitKey by remember { mutableStateOf("") }

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
                        // `googleMap`, `order`, `location` y `recenter` se leen aquí:
                        // cuando cambian, esta lambda se vuelve a ejecutar.
                        val map = googleMap.value ?: return@AndroidView
                        // Con el permiso concedido con la pantalla abierta, se activa aquí.
                        if (hasLocationPermission && !map.isMyLocationEnabled) {
                            runCatching { map.isMyLocationEnabled = true }
                        }
                        map.clear()

                        val currentOrder = order
                        val currentLocation = location
                        val puntos = mutableListOf<LatLng>()

                        // Restaurante en azul para no confundirlo con el destino.
                        currentOrder?.restaurant?.let { restaurant ->
                            val punto = LatLng(restaurant.lat, restaurant.lng)
                            puntos += punto
                            map.addMarker(
                                MarkerOptions()
                                    .position(punto)
                                    .icon(
                                        BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_AZURE
                                        )
                                    )
                                    .title(
                                        context.getString(R.string.map_marker_restaurant, restaurant.name)
                                    )
                            )
                        }

                        // Destino (cliente) en rojo, con la dirección como subtítulo.
                        currentOrder?.let { pedido ->
                            val punto = LatLng(pedido.destLat, pedido.destLng)
                            puntos += punto
                            map.addMarker(
                                MarkerOptions()
                                    .position(punto)
                                    .icon(
                                        BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_RED
                                        )
                                    )
                                    .title(
                                        context.getString(R.string.map_marker_client, pedido.clientName)
                                    )
                                    .snippet(pedido.clientAddress)
                            )
                        }

                        // Pedidos disponibles en naranja: permiten elegir por distancia
                        // sin tener que aceptarlos a ciegas.
                        val idsEnCurso = setOfNotNull(currentOrder?.id)
                        disponibles.filterNot { it.id in idsEnCurso }.forEach { pedido ->
                            val punto = LatLng(pedido.destLat, pedido.destLng)
                            map.addMarker(
                                MarkerOptions()
                                    .position(punto)
                                    .icon(
                                        BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_ORANGE
                                        )
                                    )
                                    .title(
                                        context.getString(R.string.map_marker_available, pedido.id)
                                    )
                                    .snippet(pedido.clientAddress)
                            )
                            // Solo se encuadran si no hay reparto en curso: si lo hay, la
                            // ruta manda y no conviene alejar la cámara.
                            if (currentOrder == null) puntos += punto
                        }

                        if (currentLocation != null) {
                            val punto = LatLng(currentLocation.lat, currentLocation.lng)
                            // Con punto azul nativo no se duplica el marcador.
                            if (!hasLocationPermission) {
                                map.addMarker(
                                    MarkerOptions()
                                        .position(punto)
                                        .icon(
                                            BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_GREEN
                                            )
                                        )
                                        .title(context.getString(R.string.map_my_location))
                                )
                            }
                            puntos += punto
                        }

                        // Encuadre: se reajusta cuando cambia el pedido o al pulsar
                        // "Centrar". Antes se centraba solo en el restaurante y el destino
                        // podía quedar 11 km fuera de pantalla.
                        val clave = "${currentOrder?.id ?: -1}#$recenter#${currentLocation != null}"
                        if (clave != fitKey && puntos.isNotEmpty()) {
                            fitKey = clave
                            if (puntos.size == 1) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(puntos.first(), 16f))
                            } else {
                                val bounds = LatLngBounds.builder()
                                    .also { b -> puntos.forEach { b.include(it) } }
                                    .build()
                                runCatching {
                                    map.moveCamera(
                                        CameraUpdateFactory.newLatLngBounds(bounds, MAP_FIT_PADDING_PX)
                                    )
                                }.onFailure {
                                    // Si el mapa aún no tiene tamaño, se centra en el destino.
                                    map.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(puntos.first(), 13f)
                                    )
                                }
                            }
                        }
                    }
                )

                // Aviso cuando no hay permiso: sin él no hay punto azul ni posición propia.
                if (!hasLocationPermission) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.map_activate_location),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Volver a centrar en la posición del repartidor tras mover el mapa.
                if (hasLocationPermission) {
                    FilledTonalButton(
                        onClick = {
                            centeredOnce = false
                            recenter++
                        },
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
                // Un pedido disponible (PENDIENTE) no se puede marcar en camino ni
                // completar: primero hay que aceptarlo desde la lista.
                if (current.estado != EstadoPedido.PENDIENTE) {
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
                }
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
