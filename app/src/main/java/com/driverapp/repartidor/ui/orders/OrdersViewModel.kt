package com.driverapp.repartidor.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.domain.usecase.AceptarPedidoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerPedidosDisponiblesUseCase
import com.driverapp.repartidor.domain.usecase.RechazarPedidoUseCase
import com.driverapp.repartidor.ui.common.ErrorState
import com.driverapp.repartidor.ui.common.LoadingState
import com.driverapp.repartidor.ui.common.UiMessenger
import com.driverapp.repartidor.ui.common.toErrorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class OrdersViewModel(
    private val repo: PedidoRepository,
    private val disponibles: ObtenerPedidosDisponiblesUseCase,
    private val historial: ObtenerHistorialUseCase,
    private val aceptar: AceptarPedidoUseCase,
    private val rechazar: RechazarPedidoUseCase,
    private val messenger: UiMessenger
) : ViewModel() {

    val available: StateFlow<List<Pedido>> = repo.available
    val history: StateFlow<List<Pedido>> = repo.history

    private val _loading = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loading: StateFlow<LoadingState> = _loading

    private val _error = MutableStateFlow<ErrorState>(ErrorState.None)
    val error: StateFlow<ErrorState> = _error

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _loading.value = LoadingState.Loading
            _error.value = ErrorState.None
            val r1 = runCatching { disponibles() }
            val r2 = runCatching { historial() }
            val failure = r1.exceptionOrNull() ?: r2.exceptionOrNull()
            if (failure != null) _error.value = failure.toErrorState()
            _loading.value = LoadingState.Idle
        }
    }

    fun retry() = loadAll()

    fun accept(order: Pedido, onDone: (Pedido) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val updated = aceptar(order.id.toLong())
                loadAll()
                onDone(updated)
            } catch (e: Exception) {
                messenger.show("Error al aceptar: ${e.message}")
            }
        }
    }

    fun reject(order: Pedido) {
        viewModelScope.launch {
            try {
                rechazar(order.id.toLong())
                loadAll()
                messenger.show("Pedido rechazado")
            } catch (e: Exception) {
                messenger.show("Error: ${e.message}")
            }
        }
    }

    fun feeLabel(order: Pedido): String =
        "¡Pedido completado! +$${"%.2f".format(Locale.ROOT, order.deliveryFee)}"
}
