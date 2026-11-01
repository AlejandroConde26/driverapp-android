package com.driverapp.repartidor.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.domain.usecase.MarcarEnCaminoUseCase
import com.driverapp.repartidor.domain.usecase.MarcarPedidoEntregadoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerMisPedidosUseCase
import com.driverapp.repartidor.ui.common.ErrorState
import com.driverapp.repartidor.ui.common.UiMessenger
import com.driverapp.repartidor.ui.common.toErrorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackingViewModel(
    repo: PedidoRepository,
    private val misPedidos: ObtenerMisPedidosUseCase,
    private val historial: ObtenerHistorialUseCase,
    private val enCamino: MarcarEnCaminoUseCase,
    private val entregar: MarcarPedidoEntregadoUseCase,
    private val messenger: UiMessenger
) : ViewModel() {

    val activeOrder: StateFlow<Pedido?> =
        repo.activeOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _error = MutableStateFlow<ErrorState>(ErrorState.None)
    val error: StateFlow<ErrorState> = _error

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun refreshActive() {
        viewModelScope.launch {
            runCatching { misPedidos() }.onFailure { _error.value = it.toErrorState() }
        }
    }

    /**
     * Sprint 2/3: cierra el hueco que dejaba el flujo en ACEPTADO → ENTREGADO.
     * A partir de aquí `TrackingScreen` calcula el ETA hacia el cliente.
     */
    fun marcarEnCamino(order: Pedido, onDone: (Pedido) -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val updated = enCamino(order.id.toLong())
                messenger.show(R.string.msg_order_on_the_way)
                onDone(updated)
            } catch (e: Throwable) {
                _error.value = e.toErrorState()
                messenger.showRaw(e.message ?: "Error")
            } finally {
                _busy.value = false
            }
        }
    }

    fun complete(order: Pedido, onDone: (Pedido) -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val updated = entregar(order.id.toLong())
                runCatching { historial() }
                messenger.show(R.string.msg_order_completed)
                onDone(updated)
            } catch (e: Throwable) {
                _error.value = e.toErrorState()
                messenger.showRaw(e.message ?: "Error")
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearError() { _error.value = ErrorState.None }

    fun showMessage(resId: Int) = messenger.show(resId)
}
