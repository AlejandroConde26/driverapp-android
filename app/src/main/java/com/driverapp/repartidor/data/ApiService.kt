package com.driverapp.repartidor.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/v1/users/me")
    suspend fun me(): User

    @PUT("api/v1/users/me")
    suspend fun updateMe(@Body body: UserUpdate): User

    @PUT("api/v1/users/me/vehicle")
    suspend fun updateVehicle(@Body body: VehicleUpdate): User

    @PUT("api/v1/users/me/status")
    suspend fun updateStatus(@Body body: StatusUpdate): User

    @PUT("api/v1/users/me/location")
    suspend fun updateLocation(@Body body: LocationUpdate): User

    @PUT("api/v1/users/me/fcm-token")
    suspend fun updateFcmToken(@Body body: FcmTokenUpdate): User

    @GET("api/v1/orders/available")
    suspend fun availableOrders(): List<Order>

    @GET("api/v1/orders/my")
    suspend fun myOrders(): List<Order>

    @GET("api/v1/orders/history")
    suspend fun orderHistory(): List<Order>

    @GET("api/v1/orders/{id}")
    suspend fun orderDetail(@Path("id") id: Long): Order

    @POST("api/v1/orders/{id}/accept")
    suspend fun acceptOrder(@Path("id") id: Long): Order

    @POST("api/v1/orders/{id}/reject")
    suspend fun rejectOrder(@Path("id") id: Long): Order

    @POST("api/v1/orders/{id}/in-transit")
    suspend fun inTransit(@Path("id") id: Long): Order

    @POST("api/v1/orders/{id}/complete")
    suspend fun completeOrder(@Path("id") id: Long): Order

    @GET("api/v1/earnings/summary")
    suspend fun earningsSummary(@Query("period") period: String): EarningsSummary

    @GET("api/v1/earnings/history")
    suspend fun earningsHistory(): List<EarningOut>

    @GET("api/v1/restaurants")
    suspend fun restaurants(): List<Restaurant>

    @GET("api/v1/notifications/my")
    suspend fun myNotifications(): List<NotificationOut>
}