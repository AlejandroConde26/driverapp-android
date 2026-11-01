package com.driverapp.repartidor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.R
import com.driverapp.repartidor.data.network.NetworkMonitor
import com.driverapp.repartidor.domain.model.Ubicacion
import com.driverapp.repartidor.domain.model.User
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.usecase.ActualizarFcmTokenUseCase
import com.driverapp.repartidor.domain.usecase.CambiarEstadoEnLineaUseCase
import com.driverapp.repartidor.domain.usecase.ReportarUbicacionUseCase
import com.driverapp.repartidor.ui.common.UiMessenger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val auth: AuthRepository,
    private val cambiarEstado: CambiarEstadoEnLineaUseCase,
    private val actualizarFcmToken: ActualizarFcmTokenUseCase,
    private val reportarUbicacion: ReportarUbicacionUseCase,
    networkMonitor: NetworkMonitor,
    private val messenger: UiMessenger
) : ViewModel() {

    val user: StateFlow<User?> = auth.user

    private val _online = MutableStateFlow(auth.user.value?.online ?: false)
    val online: StateFlow<Boolean> = _online

    /** Conectividad observada (S09): alimenta el banner de "sin conexión". */
    val connectivity: StateFlow<Boolean> = networkMonitor.online
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), networkMonitor.isOnline())

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
                messenger.show(if (newValue) R.string.msg_connected else R.string.msg_disconnected)
            } catch (e: Throwable) {
                messenger.showRaw(e.message ?: "Error")
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

    fun toast(resId: Int) = messenger.show(resId)

    fun logout() = auth.logout()
}
