package com.driverapp.repartidor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.R
import com.driverapp.repartidor.data.local.UserPreferences
import com.driverapp.repartidor.domain.model.User
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.usecase.ActualizarPerfilUseCase
import com.driverapp.repartidor.domain.usecase.ActualizarVehiculoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerPerfilUseCase
import com.driverapp.repartidor.ui.common.UiMessenger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de las 5 preferencias tal como se muestran en pantalla. */
data class PrefsUiState(
    val darkMode: Boolean = false,
    val notifications: Boolean = true,
    val location: Boolean = true,
    val sound: Boolean = true,
    val language: String = UserPreferences.LANG_ES
)

/**
 * Estadísticas reales del perfil: salen de `/earnings/summary?period=year`.
 * Antes se pintaba un rating fijo de 4.8 sin ninguna fuente detrás.
 */
data class ProfileStats(
    val orders: Int = 0,
    val tips: Double = 0.0,
    val total: Double = 0.0,
    val loaded: Boolean = false
)

class ProfileViewModel(
    private val prefs: UserPreferences,
    private val auth: AuthRepository,
    private val obtenerPerfil: ObtenerPerfilUseCase,
    private val actualizarPerfil: ActualizarPerfilUseCase,
    private val actualizarVehiculo: ActualizarVehiculoUseCase,
    private val obtenerGanancias: ObtenerGananciasUseCase,
    private val messenger: UiMessenger
) : ViewModel() {

    val user: StateFlow<User?> = auth.user

    val prefsState: StateFlow<PrefsUiState> = combine(
        prefs.darkMode, prefs.notificationsEnabled, prefs.locationEnabled,
        prefs.notificationSound, prefs.language
    ) { dark, notif, location, sound, language ->
        PrefsUiState(dark, notif, location, sound, language)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrefsUiState())

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats

    fun refreshAll() {
        viewModelScope.launch {
            runCatching { obtenerPerfil() }
            refreshStats()
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            runCatching {
                val s = obtenerGanancias("year")
                _stats.value = ProfileStats(
                    orders = s.ordersCount,
                    tips = s.tipsTotal,
                    total = s.total,
                    loaded = true
                )
            }
        }
    }

    fun updateVehicle(vehicle: String) {
        viewModelScope.launch {
            try {
                actualizarVehiculo(vehicle)
                messenger.show(R.string.msg_vehicle_updated, vehicle)
            } catch (e: Throwable) {
                messenger.showRaw(e.message ?: "Error")
            }
        }
    }

    fun updateProfile(name: String?, phone: String?) {
        viewModelScope.launch {
            try {
                actualizarPerfil(name, phone)
                messenger.show(R.string.msg_profile_updated)
            } catch (e: Throwable) {
                messenger.showRaw(e.message ?: "Error")
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDarkMode(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setNotificationsEnabled(enabled)
            messenger.show(if (enabled) R.string.msg_notifications_on else R.string.msg_notifications_off)
        }
    }

    fun setNotificationSound(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setNotificationSound(enabled)
            messenger.show(if (enabled) R.string.msg_sound_on else R.string.msg_sound_off)
        }
    }

    fun setLocationEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setLocationEnabled(enabled) }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { prefs.setLanguage(code) }
    }

    fun logout() = auth.logout()

    companion object {
        /** Valores que acepta el backend (enum VehicleType). */
        val VEHICLES = listOf("Bicicleta", "Moto", "Auto")
    }
}
