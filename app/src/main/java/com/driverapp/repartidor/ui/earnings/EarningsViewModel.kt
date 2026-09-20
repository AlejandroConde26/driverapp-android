package com.driverapp.repartidor.ui.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.domain.model.Ganancia
import com.driverapp.repartidor.domain.model.ResumenGanancias
import com.driverapp.repartidor.domain.usecase.ObtenerGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialGananciasUseCase
import com.driverapp.repartidor.ui.common.LoadingState
import com.driverapp.repartidor.ui.common.UiMessenger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EarningsViewModel(
    private val resumen: ObtenerGananciasUseCase,
    private val historial: ObtenerHistorialGananciasUseCase,
    private val messenger: UiMessenger
) : ViewModel() {

    private val _earnings = MutableStateFlow<ResumenGanancias?>(null)
    val earnings: StateFlow<ResumenGanancias?> = _earnings

    private val _earningsHistory = MutableStateFlow<List<Ganancia>>(emptyList())
    val earningsHistory: StateFlow<List<Ganancia>> = _earningsHistory

    private val _loading = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loading: StateFlow<LoadingState> = _loading

    fun loadEarnings(period: String) {
        viewModelScope.launch {
            _loading.value = LoadingState.Loading
            _earnings.value = runCatching { resumen(period) }
                .onFailure { messenger.show("Error al cargar ganancias: ${it.message}") }
                .getOrNull()
            _loading.value = LoadingState.Idle
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            _earningsHistory.value = runCatching { historial() }.getOrDefault(emptyList())
        }
    }
}
