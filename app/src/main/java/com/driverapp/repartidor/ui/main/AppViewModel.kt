package com.driverapp.repartidor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.data.EarningOut
import com.driverapp.repartidor.data.EarningsSummary
import com.driverapp.repartidor.data.Order
import com.driverapp.repartidor.data.OrdersRepository
import com.driverapp.repartidor.data.Session
import com.driverapp.repartidor.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(private val repo: OrdersRepository = OrdersRepository()) : ViewModel() {

    val user: StateFlow<User?> = MutableStateFlow(Session.user())
    val activeOrder: StateFlow<Order?> = repo.activeOrder

    private val _available = MutableStateFlow<List<Order>>(emptyList())
    val available: StateFlow<List<Order>> = _available

    private val _history = MutableStateFlow<List<Order>>(emptyList())
    val history: StateFlow<List<Order>> = _history

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _earnings = MutableStateFlow<EarningsSummary?>(null)
    val earnings: StateFlow<EarningsSummary?> = _earnings

    private val _earningsHistory = MutableStateFlow<List<EarningOut>>(emptyList())
    val earningsHistory: StateFlow<List<EarningOut>> = _earningsHistory

    private val _online = MutableStateFlow(Session.user()?.online ?: false)
    val online: StateFlow<Boolean> = _online

    init {
        if (Session.token() != null) {
            loadAll()
        }
    }

    fun setUser(u: User) {
        (user as MutableStateFlow).value = u
        Session.saveUser(u)
        _online.value = u.online
    }

    fun toast(message: String) {
        _error.value = message
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(3000)
            _error.value = null
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { loadAvailable() }
            runCatching { loadHistory() }
            runCatching { loadEarnings(period = "day") }
            _loading.value = false
        }
    }

    suspend fun loadAvailable() {
        runCatching { _available.value = repo.available() }
    }

    suspend fun loadHistory() {
        runCatching { _history.value = repo.history() }
    }

    fun loadEarnings(period: String) {
        viewModelScope.launch {
            _earnings.value = runCatching { repo.earningsSummary(period) }.getOrNull()
        }
    }

    fun refreshActive() {
        viewModelScope.launch {
            val active = runCatching { repo.myActive() }.getOrNull()?.firstOrNull()
            if (active != null) repo.markActive(active)
        }
    }

    fun accept(order: Order, onDone: (Order) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val updated = repo.accept(order.id.toLong())
                _available.value = _available.value.filter { it.id != updated.id }
                loadHistory()
                onDone(updated)
            } catch (e: Exception) {
                toast("Error al aceptar: ${e.message}")
            }
        }
    }

    fun reject(order: Order) {
        viewModelScope.launch {
            try {
                repo.reject(order.id.toLong())
                _available.value = _available.value.filter { it.id != order.id }
                loadHistory()
                toast("Pedido rechazado")
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    fun complete(order: Order, onDone: (Order) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val updated = repo.complete(order.id.toLong())
                toast("¡Pedido completado! +$${"%.2f".format(order.deliveryFee)}")
                loadHistory()
                loadEarnings("day")
                refreshProfileStats()
                onDone(updated)
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    fun toggleStatus() {
        val newValue = !_online.value
        viewModelScope.launch {
            try {
                val updated = repo.updateStatus(newValue)
                setUser(updated)
                toast(if (newValue) "Conectado" else "Desconectado")
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    fun updateVehicle(vehicle: String) {
        viewModelScope.launch {
            try {
                val updated = repo.updateVehicle(vehicle)
                setUser(updated)
                toast("Vehículo actualizado: $vehicle")
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    fun updateProfile(name: String?, phone: String?) {
        viewModelScope.launch {
            try {
                val updated = repo.updateProfile(name, phone)
                setUser(updated)
                toast("Perfil actualizado")
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    fun updateFcmToken(token: String) {
        viewModelScope.launch {
            runCatching { repo.updateFcmToken(token) }
        }
    }

    fun reportLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            runCatching { repo.updateLocation(lat, lng) }
        }
    }

    fun refreshProfileStats() {
        viewModelScope.launch {
            runCatching {
                _earningsHistory.value = repo.earningsHistory()
                val s = repo.earningsSummary("year")
                updateProfileStats(s)
            }
        }
    }

    private var profileStats = MutableStateFlow<Triple<Int, Double, Double>>(Triple(0, 0.0, 0.0))
    val profileStatsFlow: StateFlow<Triple<Int, Double, Double>> = profileStats
    private fun updateProfileStats(s: EarningsSummary) {
        profileStats.value = Triple(s.ordersCount, 4.8, s.tipsTotal)
    }

    fun logout() {
        Session.clear()
        (user as MutableStateFlow).value = null
    }
}