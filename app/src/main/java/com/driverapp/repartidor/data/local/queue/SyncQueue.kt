package com.driverapp.repartidor.data.local.queue

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.driverapp.repartidor.data.local.db.LocalDataSource
import java.util.concurrent.TimeUnit

/** Tipos de transición que se pueden encolar cuando no hay red (S09). */
enum class PendingType { ACCEPT, REJECT, IN_TRANSIT, COMPLETE }

/**
 * Cola offline de transiciones de pedido.
 *
 * Cuando una transición falla por falta de red, se guarda en Room y se programa
 * un Worker con restricción de conectividad: al volver la red se reenvía en orden
 * de creación. La app aplica el cambio de estado de forma optimista en la caché,
 * así el repartidor puede seguir trabajando sin señal.
 */
class SyncQueue(
    private val context: Context,
    private val local: LocalDataSource
) {

    suspend fun enqueue(orderId: Int, type: PendingType) {
        local.enqueue(orderId, type.name)
        schedule()
    }

    fun schedule() {
        val request = OneTimeWorkRequestBuilder<OrderSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    companion object {
        const val UNIQUE_NAME = "order-sync-queue"
    }
}
