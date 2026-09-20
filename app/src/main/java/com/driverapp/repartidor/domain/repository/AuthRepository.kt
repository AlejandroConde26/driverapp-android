package com.driverapp.repartidor.domain.repository

import com.driverapp.repartidor.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    val user: StateFlow<User?>

    fun currentToken(): String?

    fun isFirebaseConfigured(): Boolean

    suspend fun login(idToken: String, fcmToken: String?): User

    suspend fun firebaseSignIn(email: String, password: String): String

    suspend fun fetchMe(): User

    suspend fun updateProfile(name: String?, phone: String?): User

    suspend fun updateVehicle(vehicle: String): User

    suspend fun setOnline(online: Boolean): User

    suspend fun updateFcmToken(token: String)

    fun logout()
}
