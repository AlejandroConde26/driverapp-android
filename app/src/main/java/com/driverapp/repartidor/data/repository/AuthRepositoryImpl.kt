package com.driverapp.repartidor.data.repository

import com.driverapp.repartidor.data.firebase.FirebaseAuthDataSource
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

class AuthRepositoryImpl(
    private val api: AuthApiService,
    private val firebaseAuth: FirebaseAuthDataSource,
    private val tokenStore: TokenDataStore
) : AuthRepository {

    private val _user = MutableStateFlow(tokenStore.user())
    override val user: StateFlow<User?> = _user

    override fun currentToken(): String? = tokenStore.token()

    override fun isFirebaseConfigured(): Boolean = firebaseAuth.isConfigured()

    override suspend fun login(idToken: String, fcmToken: String?): User {
        val resp = api.login(LoginRequestDto(idToken, fcmToken))
        tokenStore.saveToken(resp.token)
        val user = resp.user.toDomain()
        tokenStore.saveUser(user)
        _user.value = user
        return user
    }

    override suspend fun firebaseSignIn(email: String, password: String): String =
        firebaseAuth.signIn(email, password)

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
