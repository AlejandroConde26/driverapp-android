package com.driverapp.repartidor.ui.tracking

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Geo
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.repository.LocationRepository
import com.driverapp.repartidor.ui.components.BusyOverlay
import com.driverapp.repartidor.ui.components.ConfirmDialog
import com.driverapp.repartidor.ui.components.etaLabel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
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
    val mapState = remember { mutableStateOf<MapView?>(null) }

    // S10 · comprobante de entrega: cámara -> Firebase Storage
    val proofFile = remember { mutableStateOf<java.io.File?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val currentOrder = vm.activeOrder.value
        val file = proofFile.value
        if (success && currentOrder != null && file != null) {
            vm.uploadProof(currentOrder, file)
        }
    }

    LaunchedEffect(Unit) { vm.refreshActive() }

    Column(modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    onResume()
                    mapState.value = this
                }
            },
            update = { map ->
                map.overlays.removeAll { it is Marker }
                val currentLocation = location
                val currentOrder = order

                if (currentOrder != null) {
                    currentOrder.restaurant?.let { restaurant ->
                        map.overlays.add(
                            Marker(map).apply {
                                position = GeoPoint(restaurant.lat, restaurant.lng)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = restaurant.name
                            }
                        )
                    }
                    map.overlays.add(
                        Marker(map).apply {
                            position = GeoPoint(currentOrder.destLat, currentOrder.destLng)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = currentOrder.clientName
                        }
                    )
                }
                if (currentLocation != null) {
                    map.overlays.add(
                        Marker(map).apply {
                            position = GeoPoint(currentLocation.lat, currentLocation.lng)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "Mi ubicación"
                        }
                    )
                }
                map.invalidate()
            }
        )

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
                OutlinedButton(
                    onClick = {
                        val dir = java.io.File(context.cacheDir, "proofs").apply { mkdirs() }
                        val file = java.io.File(dir, "order_${current.id}.jpg")
                        proofFile.value = file
                        runCatching {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            takePicture.launch(uri)
                        }.onFailure { vm.showMessage(R.string.proof_failed) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.proof_attach))
                }
                Spacer(Modifier.height(8.dp))
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
                        val phone = current.restaurant?.phone
                        if (phone.isNullOrBlank()) {
                            vm.showMessage(R.string.map_no_phone)
                        } else {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }.onFailure { vm.showMessage(R.string.map_dial_error) }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.map_call_restaurant))
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
