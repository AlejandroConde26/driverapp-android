package com.driverapp.repartidor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.model.PedidoItem
import java.util.Locale

/** Tarjeta de pedido para las listas Asignados / Historial. */
@Composable
fun OrderCard(
    order: Pedido,
    isHistory: Boolean,
    onDetail: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.order_label, order.id.toString()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(order.estado)
            }
            Spacer(Modifier.height(6.dp))
            Text(order.clientName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                order.clientAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "$" + String.format(Locale.ROOT, "%.2f", order.total),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.order_fee) + ": $" +
                        String.format(Locale.ROOT, "%.2f", order.deliveryFee),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.order_items_count, order.items.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDetail) { Text(stringResource(R.string.show_detail)) }
                if (isHistory) {
                    if (order.estado == EstadoPedido.ACEPTADO || order.estado == EstadoPedido.EN_CAMINO) {
                        TextButton(onClick = onTrack) { Text(stringResource(R.string.view_tracking)) }
                    }
                } else {
                    TextButton(onClick = onAccept) { Text(stringResource(R.string.action_accept_short)) }
                    TextButton(onClick = onReject) { Text(stringResource(R.string.reject)) }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(estado: EstadoPedido) {
    val (labelRes, bg) = when (estado) {
        EstadoPedido.PENDIENTE -> R.string.status_pending to MaterialTheme.colorScheme.surfaceVariant
        EstadoPedido.ACEPTADO -> R.string.status_accepted to MaterialTheme.colorScheme.surfaceVariant
        EstadoPedido.EN_CAMINO -> R.string.status_on_the_way to MaterialTheme.colorScheme.surfaceVariant
        EstadoPedido.ENTREGADO -> R.string.status_delivered to MaterialTheme.colorScheme.surfaceVariant
        EstadoPedido.RECHAZADO, EstadoPedido.CANCELADO -> R.string.status_rejected to MaterialTheme.colorScheme.surfaceVariant
        EstadoPedido.DESCONOCIDO -> R.string.status_pending to MaterialTheme.colorScheme.surfaceVariant
    }
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Detalle del pedido: cabecera, datos de entrega, items y acciones. */
@Composable
fun OrderDetailSheet(
    order: Pedido,
    canAccept: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.order_label, order.id.toString()),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            StatusBadge(order.estado)
        }
        Spacer(Modifier.height(12.dp))
        Text(order.clientName, style = MaterialTheme.typography.titleMedium)
        Text(
            order.clientAddress,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))

        DetailRow(stringResource(R.string.order_status), stringResource(statusRes(order.estado)))
        DetailRow(stringResource(R.string.order_payment), order.paymentMethod)
        DetailRow(
            stringResource(R.string.order_distance),
            String.format(Locale.ROOT, "%.1f km", order.distanceKm)
        )
        DetailRow(stringResource(R.string.order_eta), etaLabel(order.estTimeMin))
        DetailRow(
            stringResource(R.string.order_fee),
            "$" + String.format(Locale.ROOT, "%.2f", order.deliveryFee)
        )

        Spacer(Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))

        if (order.items.isEmpty()) {
            Text(
                stringResource(R.string.order_items_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            order.items.forEach { item -> ItemRow(item) }
        }

        if (canAccept) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAccept) { Text(stringResource(R.string.accept)) }
                TextButton(onClick = onReject) { Text(stringResource(R.string.reject)) }
            }
        }
    }
}

@Composable
private fun ItemRow(item: PedidoItem) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "${item.quantity}× ${item.productName}",
                style = MaterialTheme.typography.bodyLarge
            )
            item.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item.notes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    stringResource(R.string.label_note, it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$" + String.format(Locale.ROOT, "%.2f", item.quantity * item.unitPrice),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
    }
}

fun statusRes(estado: EstadoPedido): Int = when (estado) {
    EstadoPedido.PENDIENTE -> R.string.status_pending
    EstadoPedido.ACEPTADO -> R.string.status_accepted
    EstadoPedido.EN_CAMINO -> R.string.status_on_the_way
    EstadoPedido.ENTREGADO -> R.string.status_delivered
    EstadoPedido.RECHAZADO, EstadoPedido.CANCELADO -> R.string.status_rejected
    EstadoPedido.DESCONOCIDO -> R.string.status_pending
}

fun etaLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "$h:${"%02d".format(m)} hrs" else "0:${"%02d".format(m)} hrs"
}
