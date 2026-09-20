package com.driverapp.repartidor.data.repository

import com.driverapp.repartidor.data.remote.PedidoApiService
import com.driverapp.repartidor.domain.model.Ganancia
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.model.ResumenGanancias
import com.driverapp.repartidor.domain.repository.PedidoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PedidoRepositoryImpl(private val api: PedidoApiService) : PedidoRepository {

    private val _available = MutableStateFlow<List<Pedido>>(emptyList())
    override val available: StateFlow<List<Pedido>> = _available

    private val _history = MutableStateFlow<List<Pedido>>(emptyList())
    override val history: StateFlow<List<Pedido>> = _history

    private val _activeOrder = MutableStateFlow<Pedido?>(null)
    override val activeOrder: StateFlow<Pedido?> = _activeOrder

    override suspend fun refreshAvailable() {
        _available.value = api.availableOrders().map { it.toDomain() }
    }

    override suspend fun refreshHistory() {
        _history.value = api.orderHistory().map { it.toDomain() }
    }

    override suspend fun refreshActive() {
        api.myOrders().map { it.toDomain() }.firstOrNull()?.let { _activeOrder.value = it }
    }

    override suspend fun detail(id: Long): Pedido = api.orderDetail(id).toDomain()

    override suspend fun accept(id: Long): Pedido =
        api.acceptOrder(id).toDomain().also { _activeOrder.value = it }

    override suspend fun reject(id: Long): Pedido = api.rejectOrder(id).toDomain()

    override suspend fun markInTransit(id: Long): Pedido =
        api.inTransit(id).toDomain().also { _activeOrder.value = it }

    override suspend fun complete(id: Long): Pedido =
        api.completeOrder(id).toDomain().also { _activeOrder.value = null }

    override fun markActive(pedido: Pedido?) {
        _activeOrder.value = pedido
    }

    override suspend fun earningsSummary(period: String): ResumenGanancias =
        api.earningsSummary(period).toDomain()

    override suspend fun earningsHistory(): List<Ganancia> =
        api.earningsHistory().map { it.toDomain() }
}
