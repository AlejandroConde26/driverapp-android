package com.driverapp.repartidor.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverapp.repartidor.BuildConfig
import com.driverapp.repartidor.data.OrdersRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface LoginState {
    object Idle : LoginState
    object Loading : LoginState
    data class Error(val message: String) : LoginState
    data class Success(val userName: String) : LoginState
}

class LoginViewModel(private val repo: OrdersRepository = OrdersRepository()) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    private val firebaseReady: Boolean =
        BuildConfig.FIREBASE_API_KEY.isNotBlank() && BuildConfig.FIREBASE_APP_ID.isNotBlank()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Ingresa correo y contraseña")
            return
        }
        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val idToken = if (firebaseReady) {
                    val auth = FirebaseAuth.getInstance()
                    auth.signInWithEmailAndPassword(email.trim(), password).await()
                    auth.currentUser?.getIdToken(false)?.await()?.token
                        ?: throw Exception("No se pudo obtener el token de Firebase")
                } else {
                    // DEV_MODE: el backend usa la parte local del correo como UID
                    email.trim().substringBefore("@")
                }
                val user = repo.login(idToken, null)
                _state.value = LoginState.Success(user.name)
            } catch (e: Exception) {
                _state.value = LoginState.Error(e.message ?: "Error de autenticación")
            }
        }
    }
}