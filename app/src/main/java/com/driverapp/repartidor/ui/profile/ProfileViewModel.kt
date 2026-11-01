package com.driverapp.repartidor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.data.local.UserPreferences
import com.driverapp.repartidor.domain.model.User
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.usecase.ActualizarPerfilUseCase
import com.driverapp.repartidor.domain.usecase.ActualizarVehiculoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerPerfilUseCase
import com.driverapp.repartidor.ui.common.UiMessenger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    val prefs: UserPreferences,
    private val auth: AuthRepository,
    private val obtenerPerfil: ObtenerPerfilUseCase,
    private val actualizarPerfil: ActualizarPerfilUseCase,
    private val actualizarVehiculo: ActualizarVehiculoUseCase,
    private val obtenerGanancias: ObtenerGananciasUseCase,
    private val historialGanancias: ObtenerHistorialGananciasUseCase,
    private val messenger: UiMessenger
) : ViewModel() {

    val user: StateFlow<User?> = auth.user

    private val _stats = MutableStateFlow(Triple(0, 0.0, 0.0))
    val profileStatsFlow: StateFlow<Triple<Int, Double, Double>> = _stats

    fun refreshAll() {
        viewModelScope.launch {
            runCatching { obtenerPerfil() }
            refreshProfileStats()
        }
    }

    fun refreshProfileStats() {
        viewModelScope.launch {
            runCatching {
                historialGanancias()
                val s = obtenerGanancias("year")
                _stats.value = Triple(s.ordersCount, 4.8, s.tipsTotal)
            }
        }
    }

    fun updateVehicle(vehicle: String) {
        viewModelScope.launch {
            try {
                actualizarVehiculo(vehicle)
                messenger.show("Vehículo actualizado: $vehicle")
            } catch (e: Exception) {
                messenger.show("Error: ${e.message}")
            }
        }
    }

    fun updateProfile(name: String?, phone: String?) {
        viewModelScope.launch {
            try {
                actualizarPerfil(name, phone)
                messenger.show("Perfil actualizado")
            } catch (e: Exception) {
                messenger.show("Error: ${e.message}")
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.darkMode = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.notificationsEnabled = enabled
        messenger.show(if (enabled) "Notificaciones activadas" else "Notificaciones desactivadas")
    }

    fun setNotificationSound(enabled: Boolean) {
        prefs.notificationSound = enabled
        messenger.show(if (enabled) "Sonido activado" else "Sonido desactivado")
    }

    fun setLanguage(code: String) {
        prefs.language = code
    }

    fun languageLabel(): String =
        if (prefs.language == UserPreferences.LANG_EN) "English" else "Español"

    fun toast(message: String) = messenger.show(message)

    fun logout() {
        auth.logout()
    }
}
