package com.driverapp.repartidor.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.model.Notificacion
import com.driverapp.repartidor.domain.repository.NotificationRepository
import com.driverapp.repartidor.ui.common.ErrorState
import com.driverapp.repartidor.ui.common.LoadingState
import com.driverapp.repartidor.ui.common.UiMessenger
import com.driverapp.repartidor.ui.common.toErrorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * S09 · CRUD sobre el recurso `notifications`:
 *  - Read:   GET  /api/v1/notifications/my
 *  - Update: PUT  /api/v1/notifications/my/read  (marcar todas como leídas)
 *  - Delete: descarte local en memoria (el backend no expone DELETE de avisos)
 *  - Create: lo hace el backend al cambiar el estado de un pedido
 */
class NotificationsViewModel(
    private val repo: NotificationRepository,
    private val messenger: UiMessenger
) : ViewModel() {

    private val _items = MutableStateFlow<List<Notificacion>>(emptyList())
    val items: StateFlow<List<Notificacion>> = _items

    private val _loading = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loading: StateFlow<LoadingState> = _loading

    private val _error = MutableStateFlow<ErrorState>(ErrorState.None)
    val error: StateFlow<ErrorState> = _error

    private val _dismissed = MutableStateFlow<Set<Int>>(emptySet())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = LoadingState.Loading
            _error.value = ErrorState.None
            runCatching { repo.list() }
                .onSuccess { list -> _items.value = list.filterNot { it.id in _dismissed.value } }
                .onFailure { _error.value = it.toErrorState() }
            _loading.value = LoadingState.Idle
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            runCatching { repo.markAllRead() }
                .onSuccess {
                    _items.value = _items.value.map { it.copy(read = true) }
                    messenger.show(R.string.notifications_all_read)
                }
                .onFailure { _error.value = it.toErrorState() }
        }
    }

    fun dismiss(item: Notificacion) {
        _dismissed.value = _dismissed.value + item.id
        _items.value = _items.value.filterNot { it.id == item.id }
    }

    fun retry() = load()
}
