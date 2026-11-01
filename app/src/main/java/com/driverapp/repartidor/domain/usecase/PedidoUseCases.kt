package com.driverapp.repartidor.domain.usecase

import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.repository.PedidoRepository

class ObtenerPedidosDisponiblesUseCase(private val repo: PedidoRepository) {
    suspend operator fun invoke() = repo.refreshAvailable()
}

class ObtenerMisPedidosUseCase(private val repo: PedidoRepository) {
    suspend operator fun invoke() = repo.refreshActive()
}

class ObtenerHistorialUseCase(private val repo: PedidoRepository) {
    suspend operator fun invoke() = repo.refreshHistory()
}

class AceptarPedidoUseCase(private val repo: PedidoRepository) {
    suspend operator fun invoke(id: Long): Pedido = repo.accept(id)
}

class RechazarPedidoUseCase(private val repo: PedidoRepository) {
    suspend operator fun invoke(id: Long): Pedido = repo.reject(id)
}

class MarcarEnCaminoUseCase(private val repo: PedidoRepository) {
    suspend operator fun invoke(id: Long): Pedido = repo.markInTransit(id)
}

class MarcarPedidoEntregadoUseCase(private val repo: PedidoRepository) {
    suspend operator fun invoke(id: Long): Pedido = repo.complete(id)
}

class ObtenerDetallePedidoUseCase(private val repo: PedidoRepository) {
    suspend operator fun invoke(id: Long): Pedido = repo.detail(id)
}
