package com.driverapp.repartidor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.model.User
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.domain.usecase.ActualizarFcmTokenUseCase
import com.driverapp.repartidor.domain.usecase.CambiarEstadoEnLineaUseCase
import com.driverapp.repartidor.domain.usecase.ReportarUbicacionUseCase
import com.driverapp.repartidor.domain.model.Ubicacion
import com.driverapp.repartidor.ui.common.UiMessenger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val auth: AuthRepository,
    private val pedidos: PedidoRepository,
    private val cambiarEstado: CambiarEstadoEnLineaUseCase,
    private val actualizarFcmToken: ActualizarFcmTokenUseCase,
    private val reportarUbicacion: ReportarUbicacionUseCase,
    private val messenger: UiMessenger
) : ViewModel() {

    val user: StateFlow<User?> = auth.user
    val activeOrder: StateFlow<Pedido?> = pedidos.activeOrder

    private val _online = MutableStateFlow(auth.user.value?.online ?: false)
    val online: StateFlow<Boolean> = _online

    init {
        if (auth.currentToken() != null) {
            viewModelScope.launch {
                runCatching { auth.fetchMe() }.onSuccess { _online.value = it.online }
            }
        }
    }

    fun toggleStatus() {
        val newValue = !_online.value
        viewModelScope.launch {
            try {
                val updated = cambiarEstado(newValue)
                _online.value = updated.online
                messenger.show(if (newValue) "Conectado" else "Desconectado")
            } catch (e: Exception) {
                messenger.show("Error: ${e.message}")
            }
        }
    }

    fun updateFcmToken(token: String) {
        viewModelScope.launch {
            runCatching { actualizarFcmToken(token) }
        }
    }

    fun reportLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            runCatching { reportarUbicacion(Ubicacion(lat, lng)) }
        }
    }

    fun toast(message: String) = messenger.show(message)

    fun logout() {
        auth.logout()
    }
}
