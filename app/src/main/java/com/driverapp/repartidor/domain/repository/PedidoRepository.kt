package com.driverapp.repartidor.domain.repository

import com.driverapp.repartidor.domain.model.Pedido
import kotlinx.coroutines.flow.Flow

/**
 * Pedidos del repartidor con caché local (Sprint 6).
 *
 * Las tres listas se leen de Room (offline-first); los `refresh*` son los que
 * tocan la red y pueden lanzar [com.driverapp.repartidor.data.remote.ApiFailure].
 */
interface PedidoRepository {

    val available: Flow<List<Pedido>>
    val history: Flow<List<Pedido>>
    val activeOrder: Flow<Pedido?>

    /** Nº de transiciones pendientes de enviar (cola offline, S09). */
    val pendingActions: Flow<Int>

    suspend fun refreshAvailable()
    suspend fun refreshHistory()
    suspend fun refreshActive()

    suspend fun detail(id: Long): Pedido

    suspend fun accept(id: Long): Pedido
    suspend fun reject(id: Long): Pedido
    suspend fun markInTransit(id: Long): Pedido
    suspend fun complete(id: Long): Pedido
}
