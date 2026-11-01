package com.driverapp.repartidor.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.domain.repository.ProofRepository
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
    private val messenger: UiMessenger,
    private val proof: ProofRepository? = null
) : ViewModel() {

    val activeOrder: StateFlow<Pedido?> =
        repo.activeOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _error = MutableStateFlow<ErrorState>(ErrorState.None)
    val error: StateFlow<ErrorState> = _error

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    /** S10 · URL del comprobante subido a Storage. */
    private val _proofUrl = MutableStateFlow<String?>(null)
    val proofUrl: StateFlow<String?> = _proofUrl

    private val _uploadingProof = MutableStateFlow(false)
    val uploadingProof: StateFlow<Boolean> = _uploadingProof

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

    /**
     * S10 · sube la foto del comprobante a Firebase Storage y la registra en
     * Firestore. Sin Firebase configurado avisa y no rompe el flujo de entrega.
     */
    fun uploadProof(order: Pedido, file: java.io.File, onDone: (String) -> Unit = {}) {
        val repo = proof
        if (repo == null) {
            messenger.show(R.string.proof_failed)
            return
        }
        viewModelScope.launch {
            _uploadingProof.value = true
            runCatching { repo.upload(order.id, file) }
                .onSuccess {
                    _proofUrl.value = it
                    messenger.show(R.string.proof_uploaded)
                    onDone(it)
                }
                .onFailure { messenger.show(R.string.proof_failed) }
            _uploadingProof.value = false
        }
    }

    fun showMessage(resId: Int) = messenger.show(resId)
}
