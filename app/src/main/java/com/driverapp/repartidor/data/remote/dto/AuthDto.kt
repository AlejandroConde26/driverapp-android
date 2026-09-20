package com.driverapp.repartidor.data.remote.dto

import com.driverapp.repartidor.domain.model.User
import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("fcm_token") val fcmToken: String? = null
)

data class LoginResponseDto(
    val user: UserDto,
    val token: String
)

data class UserDto(
    val id: Int,
    val email: String,
    val name: String,
    val phone: String? = null,
    val role: String = "REPARTIDOR",
    val vehicle: String? = null,
    val online: Boolean = false,
    val active: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun toDomain(): User = User(
        id = id,
        email = email,
        name = name,
        phone = phone,
        role = role,
        vehicle = vehicle,
        online = online,
        active = active,
        latitude = latitude,
        longitude = longitude
    )
}

data class UserUpdateDto(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null
)

data class VehicleUpdateDto(val vehicle: String)

data class StatusUpdateDto(val online: Boolean)

data class LocationUpdateDto(
    val latitude: Double,
    val longitude: Double
)

data class FcmTokenUpdateDto(@SerializedName("fcm_token") val fcmToken: String)
