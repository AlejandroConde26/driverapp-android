package com.driverapp.repartidor.ui.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.ui.common.ErrorState
import com.driverapp.repartidor.ui.common.LoadingState
import com.driverapp.repartidor.ui.components.BusyOverlay
import com.driverapp.repartidor.ui.components.ConfirmDialog
import com.driverapp.repartidor.ui.components.EmptyStateBox
import com.driverapp.repartidor.ui.components.ErrorStateBox
import com.driverapp.repartidor.ui.components.LoadingStateBox
import com.driverapp.repartidor.ui.components.OrderCard
import com.driverapp.repartidor.ui.components.OrderDetailSheet
import com.driverapp.repartidor.ui.components.PeriodPills

/**
 * Pestaña Órdenes: lista Asignados / Historial + detalle inline + acciones.
 * Las listas vienen de Room, así que se ven aunque no haya red (Sprint 6).
 */
@Composable
fun OrdersScreen(
    vm: OrdersViewModel,
    onGoToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val available by vm.available.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(0) }
    var pendingAccept by remember { mutableStateOf<Pedido?>(null) }
    var pendingReject by remember { mutableStateOf<Pedido?>(null) }

    LaunchedEffect(Unit) { vm.loadAll() }

    Column(modifier.fillMaxSize()) {
        PeriodPills(
            labels = listOf(
                stringResource(R.string.tab_assigned) + " (${available.size})",
                stringResource(R.string.tab_history) + " (${history.size})"
            ),
            selected = tab,
            onSelect = { tab = it }
        )

        val list = if (tab == 0) available else history
        val emptyText = if (tab == 0) R.string.empty_orders else R.string.empty_history

        when {
            loading is LoadingState.Loading && list.isEmpty() -> LoadingStateBox()
            error !is ErrorState.None && list.isEmpty() -> ErrorStateBox(error, onRetry = { vm.retry() })
            list.isEmpty() -> EmptyStateBox(
                text = stringResource(emptyText),
                actionLabel = if (error !is ErrorState.None) stringResource(R.string.action_retry) else null,
                onAction = { vm.retry() }
            )
            else -> LazyColumn {
                items(list, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        isHistory = tab == 1,
                        onDetail = { vm.openDetail(order) },
                        onAccept = { pendingAccept = order },
                        onReject = { pendingReject = order },
                        onTrack = onGoToMap
                    )
                }
            }
        }
    }

    // --- Detalle inline (mismo patrón que view_order_detail) ---
    val current = detail
    if (current != null) {
        AlertDialog(
            onDismissRequest = { vm.closeDetail() },
            title = null,
            text = {
                OrderDetailSheet(
                    order = current,
                    canAccept = current.estado == EstadoPedido.PENDIENTE,
                    onAccept = { vm.closeDetail(); pendingAccept = current },
                    onReject = { vm.closeDetail(); pendingReject = current }
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.closeDetail() }) { Text(stringResource(R.string.detail_close)) }
            }
        )
    }

    // --- Confirmaciones ---
    pendingAccept?.let { order ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_accept_title),
            message = stringResource(R.string.confirm_accept_body, order.id.toString()),
            confirmText = stringResource(R.string.action_accept_short),
            onConfirm = {
                vm.accept(order) { onGoToMap() }
                pendingAccept = null
            },
            onDismiss = { pendingAccept = null }
        )
    }

    pendingReject?.let { order ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_reject_title),
            message = stringResource(R.string.confirm_reject_body, order.id.toString()),
            confirmText = stringResource(R.string.action_reject),
            onConfirm = {
                vm.reject(order)
                pendingReject = null
            },
            onDismiss = { pendingReject = null }
        )
    }

    BusyOverlay(busy)
}
