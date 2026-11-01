package com.driverapp.repartidor.data.local.queue

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.driverapp.repartidor.App
import com.driverapp.repartidor.data.remote.ApiFailure
import com.driverapp.repartidor.data.remote.toApiFailure

/**
 * Reenvía la cola offline en orden FIFO.
 *
 * - Sin red → Result.retry() (WorkManager reintenta con backoff exponencial).
 * - Error definitivo (404/422) → se descarta la acción para no bloquear el resto
 *   de la cola; el pedido se resincroniza en el siguiente refresh.
 */
class OrderSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as? App)?.container ?: return Result.success()
        val pending = container.local.pendingActions()
        if (pending.isEmpty()) return Result.success()

        for (action in pending) {
            val type = runCatching { PendingType.valueOf(action.type) }.getOrNull()
            if (type == null) {
                container.local.deleteAction(action.id)
                continue
            }
            try {
                when (type) {
                    PendingType.ACCEPT -> container.pedidoApi.acceptOrder(action.orderId.toLong())
                    PendingType.REJECT -> container.pedidoApi.rejectOrder(action.orderId.toLong())
                    PendingType.IN_TRANSIT -> container.pedidoApi.inTransit(action.orderId.toLong())
                    PendingType.COMPLETE -> container.pedidoApi.completeOrder(action.orderId.toLong())
                }
                container.local.deleteAction(action.id)
            } catch (e: Throwable) {
                val failure = e.toApiFailure()
                if (failure is ApiFailure.NoConnection) {
                    container.local.markActionFailed(action.id, failure.message)
                    return Result.retry()
                }
                // 4xx: la transición ya no es válida; se descarta y se sigue.
                container.local.markActionFailed(action.id, failure.message)
                container.local.deleteAction(action.id)
            }
        }
        return Result.success()
    }
}
