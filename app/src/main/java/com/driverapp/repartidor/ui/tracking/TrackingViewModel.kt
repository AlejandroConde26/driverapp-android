package com.driverapp.repartidor.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.domain.usecase.MarcarPedidoEntregadoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerMisPedidosUseCase
import com.driverapp.repartidor.ui.common.UiMessenger
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrackingViewModel(
    private val repo: PedidoRepository,
    private val misPedidos: ObtenerMisPedidosUseCase,
    private val historial: ObtenerHistorialUseCase,
    private val entregar: MarcarPedidoEntregadoUseCase,
    private val messenger: UiMessenger
) : ViewModel() {

    val activeOrder: StateFlow<Pedido?> = repo.activeOrder

    fun refreshActive() {
        viewModelScope.launch {
            runCatching { misPedidos() }
        }
    }

    fun complete(order: Pedido, onDone: (Pedido) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val updated = entregar(order.id.toLong())
                runCatching { historial() }
                onDone(updated)
            } catch (e: Exception) {
                messenger.show("Error: ${e.message}")
            }
        }
    }

    fun showMessage(text: String) = messenger.show(text)
}
