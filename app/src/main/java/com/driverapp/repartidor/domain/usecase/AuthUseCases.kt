package com.driverapp.repartidor.domain.usecase

import com.driverapp.repartidor.domain.model.User
import com.driverapp.repartidor.domain.repository.AuthRepository

class LoginUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, fcmToken: String? = null): User {
        if (email.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("Ingresa correo y contraseña")
        }
        val idToken = if (auth.isFirebaseConfigured()) {
            auth.firebaseSignIn(email.trim(), password)
        } else {
            // DEV_MODE: el backend usa la parte local del correo como UID
            email.trim().substringBefore("@")
        }
        return auth.login(idToken, fcmToken)
    }
}

class ObtenerPerfilUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(): User = auth.fetchMe()
}

class ActualizarPerfilUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(name: String?, phone: String?): User =
        auth.updateProfile(name, phone)
}

class ActualizarVehiculoUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(vehicle: String): User = auth.updateVehicle(vehicle)
}

class CambiarEstadoEnLineaUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(online: Boolean): User = auth.setOnline(online)
}

class ActualizarFcmTokenUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(token: String) = auth.updateFcmToken(token)
}
