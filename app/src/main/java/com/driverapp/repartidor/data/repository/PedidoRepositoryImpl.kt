package com.driverapp.repartidor.data.repository

import com.driverapp.repartidor.BuildConfig
import com.driverapp.repartidor.data.local.db.Buckets
import com.driverapp.repartidor.data.local.db.LocalDataSource
import com.driverapp.repartidor.data.local.queue.PendingType
import com.driverapp.repartidor.data.local.queue.SyncQueue
import com.driverapp.repartidor.data.remote.ApiFailure
import com.driverapp.repartidor.data.remote.PedidoApiService
import com.driverapp.repartidor.data.remote.dto.PedidoDto
import com.driverapp.repartidor.data.remote.toApiFailure
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.repository.PedidoRepository
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de pedidos **cache-first** (Sprint 6).
 *
 * Lectura: siempre desde Room (`orders_cache`), así la app funciona sin conexión.
 * Escritura: red primero; la respuesta se escribe en caché y la UI re-emite sola.
 * Transiciones sin red: se encolan en `pending_actions` (S09) y se aplican de
 * forma optimista en la caché para que el repartidor no se quede bloqueado.
 */
class PedidoRepositoryImpl(
    private val api: PedidoApiService,
    private val local: LocalDataSource,
    private val queue: SyncQueue,
    private val restaurantId: Int = BuildConfig.RESTAURANT_ID
) : PedidoRepository {

    override val available: Flow<List<Pedido>> = local.observeBucket(Buckets.AVAILABLE)
    override val history: Flow<List<Pedido>> = local.observeBucket(Buckets.HISTORY)
    override val activeOrder: Flow<Pedido?> = local.observeActive()
    override val pendingActions: Flow<Int> = local.observePendingCount()

    override suspend fun refreshAvailable() {
        val fresh = api.availableOrders(restaurantId).map { it.toDomain() }
        local.replaceBucket(Buckets.AVAILABLE, fresh)
        local.purgeHistoryOlderThan(HISTORY_TTL_DAYS)
    }

    override suspend fun refreshHistory() {
        val fresh = api.orderHistory(restaurantId).map { it.toDomain() }
        local.replaceBucket(Buckets.HISTORY, fresh)
        local.purgeHistoryOlderThan(HISTORY_TTL_DAYS)
    }

    override suspend fun refreshActive() {
        // Si el backend devuelve lista vacía se limpia la caché: antes quedaba
        // pegado un pedido ya cerrado en la pestaña Mapa.
        val fresh = api.myOrders(restaurantId).map { it.toDomain() }
        local.replaceBucket(Buckets.ACTIVE, fresh)
    }

    override suspend fun detail(id: Long): Pedido {
        return try {
            val fresh = api.orderDetail(id).toDomain()
            val bucket = if (fresh.estado == EstadoPedido.PENDIENTE) Buckets.AVAILABLE else Buckets.ACTIVE
            local.save(fresh, bucket)
            fresh
        } catch (e: Throwable) {
            val failure = e.toApiFailure()
            local.get(id.toInt()) ?: throw failure
        }
    }

    override suspend fun accept(id: Long): Pedido =
        transition(id, PendingType.ACCEPT, EstadoPedido.ACEPTADO, Buckets.ACTIVE) { api.acceptOrder(id) }

    override suspend fun reject(id: Long): Pedido =
        transition(id, PendingType.REJECT, EstadoPedido.RECHAZADO, Buckets.HISTORY) { api.rejectOrder(id) }

    override suspend fun markInTransit(id: Long): Pedido =
        transition(id, PendingType.IN_TRANSIT, EstadoPedido.EN_CAMINO, Buckets.ACTIVE) { api.inTransit(id) }

    override suspend fun complete(id: Long): Pedido =
        transition(id, PendingType.COMPLETE, EstadoPedido.ENTREGADO, Buckets.HISTORY) { api.completeOrder(id) }

    private suspend fun transition(
        id: Long,
        type: PendingType,
        estado: EstadoPedido,
        bucket: String,
        call: suspend () -> PedidoDto
    ): Pedido {
        val fresh = try {
            call().toDomain()
        } catch (e: Throwable) {
            val failure = e.toApiFailure()
            if (failure !is ApiFailure.NoConnection) throw failure

            // Sin red: se encola y se aplica el cambio de forma optimista.
            queue.enqueue(id.toInt(), type)
            val cached = local.get(id.toInt()) ?: throw failure
            val optimistic = cached.copy(estado = estado)
            local.save(optimistic, bucket)
            return optimistic
        }
        local.save(fresh, bucket)
        return fresh
    }

    companion object {
        /** Días que se conservan los pedidos del historial en caché. */
        const val HISTORY_TTL_DAYS = 15
    }
}
