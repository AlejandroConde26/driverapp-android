package com.driverapp.repartidor.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.domain.usecase.LoginUseCase
import com.driverapp.repartidor.ui.common.UiMessenger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface LoginState {
    object Idle : LoginState
    object Loading : LoginState
    data class Error(val message: String) : LoginState
    data class Success(val userName: String) : LoginState
}

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val messenger: UiMessenger
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(email: String, password: String) {
        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val user = loginUseCase(email, password)
                _state.value = LoginState.Success(user.name)
            } catch (e: Exception) {
                val msg = e.message ?: "Error de autenticación"
                _state.value = LoginState.Error(msg)
                messenger.show(msg)
            }
        }
    }
}
