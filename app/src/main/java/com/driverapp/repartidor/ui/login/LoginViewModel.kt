package com.driverapp.repartidor.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.R
import com.driverapp.repartidor.data.remote.ApiFailure
import com.driverapp.repartidor.data.remote.toApiFailure
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.usecase.LoginUseCase
import com.driverapp.repartidor.ui.common.UiMessenger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data class Error(val message: String) : LoginState
    data class Success(val userName: String) : LoginState
}

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val auth: AuthRepository,
    private val messenger: UiMessenger
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    /** Si ya hay token guardado, el NavHost arranca en `main` en vez de `login`. */
    fun isLoggedIn(): Boolean = auth.currentToken() != null

    fun login(email: String, password: String) {
        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val user = loginUseCase(email, password)
                _state.value = LoginState.Success(user.name)
            } catch (e: Throwable) {
                val failure = e.toApiFailure()
                val msg = when (failure) {
                    is ApiFailure.NoConnection -> "Sin conexión. Revisa tu red e intenta de nuevo."
                    is ApiFailure.Unauthorized -> "Credenciales inválidas."
                    else -> failure.message ?: "Error de autenticación"
                }
                _state.value = LoginState.Error(msg)
                messenger.showRaw(msg)
            }
        }
    }

    fun resetError() {
        if (_state.value is LoginState.Error) _state.value = LoginState.Idle
    }

    fun validateEmail(value: String): Int? = when {
        value.isBlank() -> R.string.error_email_required
        !value.contains("@") && value.length < 3 -> R.string.error_email_required
        value.contains("@") && !EMAIL_REGEX.matches(value.trim()) -> R.string.error_email_invalid
        else -> null
    }

    fun validatePassword(value: String): Int? = when {
        value.isBlank() -> R.string.error_required
        value.length < MIN_PASSWORD -> R.string.error_password_short
        else -> null
    }

    companion object {
        const val MIN_PASSWORD = 6
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
