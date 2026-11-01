package com.driverapp.repartidor.domain.repository

import com.driverapp.repartidor.domain.model.Ganancia
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.model.ResumenGanancias
import kotlinx.coroutines.flow.StateFlow

interface PedidoRepository {

    val available: StateFlow<List<Pedido>>
    val history: StateFlow<List<Pedido>>
    val activeOrder: StateFlow<Pedido?>

    suspend fun refreshAvailable()
    suspend fun refreshHistory()
    suspend fun refreshActive()

    suspend fun detail(id: Long): Pedido
    suspend fun accept(id: Long): Pedido
    suspend fun reject(id: Long): Pedido
    suspend fun markInTransit(id: Long): Pedido
    suspend fun complete(id: Long): Pedido
    fun markActive(pedido: Pedido?)

    suspend fun earningsSummary(period: String): ResumenGanancias
    suspend fun earningsHistory(): List<Ganancia>
}
