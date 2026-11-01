package com.driverapp.repartidor.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.domain.usecase.AceptarPedidoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerDetallePedidoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerPedidosDisponiblesUseCase
import com.driverapp.repartidor.domain.usecase.RechazarPedidoUseCase
import com.driverapp.repartidor.ui.common.ErrorState
import com.driverapp.repartidor.ui.common.LoadingState
import com.driverapp.repartidor.ui.common.UiMessenger
import com.driverapp.repartidor.ui.common.toErrorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OrdersViewModel(
    repo: PedidoRepository,
    private val disponibles: ObtenerPedidosDisponiblesUseCase,
    private val historial: ObtenerHistorialUseCase,
    private val aceptar: AceptarPedidoUseCase,
    private val rechazar: RechazarPedidoUseCase,
    private val detalle: ObtenerDetallePedidoUseCase,
    private val messenger: UiMessenger
) : ViewModel() {

    // Las listas vienen de Room: se actualizan solas al refrescar la red.
    val available: StateFlow<List<Pedido>> =
        repo.available.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<Pedido>> =
        repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Transiciones encoladas por falta de red (S09). */
    val pendingSync: StateFlow<Int> =
        repo.pendingActions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _loading = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loading: StateFlow<LoadingState> = _loading

    private val _error = MutableStateFlow<ErrorState>(ErrorState.None)
    val error: StateFlow<ErrorState> = _error

    private val _detail = MutableStateFlow<Pedido?>(null)
    val detail: StateFlow<Pedido?> = _detail

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

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

    fun openDetail(order: Pedido) {
        _detail.value = order
        viewModelScope.launch {
            // Refresca items desde la API; si falla, se queda el detalle cacheado.
            runCatching { detalle(order.id.toLong()) }.onSuccess { _detail.value = it }
        }
    }

    fun closeDetail() {
        _detail.value = null
    }

    fun accept(order: Pedido, onDone: (Pedido) -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val updated = aceptar(order.id.toLong())
                loadAll()
                messenger.show(R.string.msg_order_accepted)
                onDone(updated)
            } catch (e: Throwable) {
                _error.value = e.toErrorState()
                messenger.showRaw(e.message ?: "Error")
            } finally {
                _busy.value = false
            }
        }
    }

    fun reject(order: Pedido) {
        viewModelScope.launch {
            _busy.value = true
            try {
                rechazar(order.id.toLong())
                loadAll()
                messenger.show(R.string.msg_order_rejected)
            } catch (e: Throwable) {
                _error.value = e.toErrorState()
                messenger.showRaw(e.message ?: "Error")
            } finally {
                _busy.value = false
            }
        }
    }
}
