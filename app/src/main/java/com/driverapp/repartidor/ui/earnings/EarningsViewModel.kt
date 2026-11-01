package com.driverapp.repartidor.ui.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.data.local.UserPreferences
import com.driverapp.repartidor.domain.model.Ganancia
import com.driverapp.repartidor.domain.model.ResumenGanancias
import com.driverapp.repartidor.domain.usecase.ObtenerGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialGananciasUseCase
import com.driverapp.repartidor.ui.common.ErrorState
import com.driverapp.repartidor.ui.common.LoadingState
import com.driverapp.repartidor.ui.common.UiMessenger
import com.driverapp.repartidor.ui.common.toErrorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EarningsViewModel(
    private val resumen: ObtenerGananciasUseCase,
    private val historial: ObtenerHistorialGananciasUseCase,
    private val messenger: UiMessenger,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _earnings = MutableStateFlow<ResumenGanancias?>(null)
    val earnings: StateFlow<ResumenGanancias?> = _earnings

    private val _earningsHistory = MutableStateFlow<List<Ganancia>>(emptyList())
    val earningsHistory: StateFlow<List<Ganancia>> = _earningsHistory

    private val _loading = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loading: StateFlow<LoadingState> = _loading

    private val _error = MutableStateFlow<ErrorState>(ErrorState.None)
    val error: StateFlow<ErrorState> = _error

    /** El botón ojo lee la preferencia desde DataStore (Sprint 6). */
    val showBalance: StateFlow<Boolean> =
        prefs.showBalance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _period = MutableStateFlow("day")
    val period: StateFlow<String> = _period

    fun load(period: String) {
        _period.value = period
        viewModelScope.launch {
            _loading.value = LoadingState.Loading
            _error.value = ErrorState.None
            val result = runCatching { resumen(period) }
            _earnings.value = result.getOrNull()
            result.exceptionOrNull()?.let {
                _error.value = it.toErrorState()
                messenger.showRaw(it.message ?: "Error")
            }
            _loading.value = LoadingState.Idle
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            runCatching { historial() }
                .onSuccess { _earningsHistory.value = it }
                .onFailure { _error.value = it.toErrorState() }
        }
    }

    fun toggleBalance() {
        viewModelScope.launch { prefs.toggleShowBalance() }
    }

    fun retry() = load(_period.value)
}
