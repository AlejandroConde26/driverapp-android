package com.driverapp.repartidor.data.remote

import com.driverapp.repartidor.data.remote.dto.FcmTokenUpdateDto
import com.driverapp.repartidor.data.remote.dto.LocationUpdateDto
import com.driverapp.repartidor.data.remote.dto.LoginRequestDto
import com.driverapp.repartidor.data.remote.dto.LoginResponseDto
import com.driverapp.repartidor.data.remote.dto.StatusUpdateDto
import com.driverapp.repartidor.data.remote.dto.UserDto
import com.driverapp.repartidor.data.remote.dto.UserUpdateDto
import com.driverapp.repartidor.data.remote.dto.VehicleUpdateDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @GET("api/v1/users/me")
    suspend fun me(): UserDto

    @PUT("api/v1/users/me")
    suspend fun updateMe(@Body body: UserUpdateDto): UserDto

    @PUT("api/v1/users/me/vehicle")
    suspend fun updateVehicle(@Body body: VehicleUpdateDto): UserDto

    @PUT("api/v1/users/me/status")
    suspend fun updateStatus(@Body body: StatusUpdateDto): UserDto

    @PUT("api/v1/users/me/location")
    suspend fun updateLocation(@Body body: LocationUpdateDto): UserDto

    @PUT("api/v1/users/me/fcm-token")
    suspend fun updateFcmToken(@Body body: FcmTokenUpdateDto): UserDto
}
