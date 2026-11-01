package com.driverapp.repartidor.domain.repository

import com.driverapp.repartidor.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    val user: StateFlow<User?>

    fun currentToken(): String?

    /** Login contra el backend: devuelve el usuario y guarda el JWT de sesión. */
    suspend fun login(email: String, password: String, fcmToken: String? = null): User

    suspend fun fetchMe(): User

    suspend fun updateProfile(name: String?, phone: String?): User

    suspend fun updateVehicle(vehicle: String): User

    suspend fun setOnline(online: Boolean): User

    suspend fun updateFcmToken(token: String)

    fun logout()
}
