package com.driverapp.repartidor.data.repository

import com.driverapp.repartidor.data.local.TokenDataStore
import com.driverapp.repartidor.data.remote.AuthApiService
import com.driverapp.repartidor.data.remote.dto.FcmTokenUpdateDto
import com.driverapp.repartidor.data.remote.dto.LocationUpdateDto
import com.driverapp.repartidor.data.remote.dto.LoginRequestDto
import com.driverapp.repartidor.data.remote.dto.StatusUpdateDto
import com.driverapp.repartidor.data.remote.dto.UserUpdateDto
import com.driverapp.repartidor.data.remote.dto.VehicleUpdateDto
import com.driverapp.repartidor.domain.model.User
import com.driverapp.repartidor.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Autenticación contra el backend propio: la app manda correo + contraseña,
 * el backend verifica el hash y devuelve su JWT. Firebase ya no participa en
 * el login (sólo en las notificaciones push).
 */
class AuthRepositoryImpl(
    private val api: AuthApiService,
    private val tokenStore: TokenDataStore
) : AuthRepository {

    private val _user = MutableStateFlow(tokenStore.user())
    override val user: StateFlow<User?> = _user

    override fun currentToken(): String? = tokenStore.token()

    override suspend fun login(email: String, password: String, fcmToken: String?): User {
        val resp = api.login(LoginRequestDto(email, password, fcmToken))
        tokenStore.saveToken(resp.token)
        val user = resp.user.toDomain()
        tokenStore.saveUser(user)
        _user.value = user
        return user
    }

    override suspend fun fetchMe(): User = api.me().toDomain().also {
        tokenStore.saveUser(it)
        _user.value = it
    }

    override suspend fun updateProfile(name: String?, phone: String?): User =
        api.updateMe(UserUpdateDto(name, phone)).toDomain().also {
            tokenStore.saveUser(it)
            _user.value = it
        }

    override suspend fun updateVehicle(vehicle: String): User =
        api.updateVehicle(VehicleUpdateDto(vehicle)).toDomain().also {
            tokenStore.saveUser(it)
            _user.value = it
        }

    override suspend fun setOnline(online: Boolean): User =
        api.updateStatus(StatusUpdateDto(online)).toDomain().also {
            tokenStore.saveUser(it)
            _user.value = it
        }

    override suspend fun updateFcmToken(token: String) {
        api.updateFcmToken(FcmTokenUpdateDto(token))
    }

    override fun logout() {
        tokenStore.clear()
        _user.value = null
    }

    internal suspend fun reportLocation(lat: Double, lng: Double) {
        api.updateLocation(LocationUpdateDto(lat, lng))
    }
}
