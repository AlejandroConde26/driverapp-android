package com.driverapp.repartidor.data.remote

import com.driverapp.repartidor.data.remote.dto.GananciaDto
import com.driverapp.repartidor.data.remote.dto.NotificacionDto
import com.driverapp.repartidor.data.remote.dto.PedidoDto
import com.driverapp.repartidor.data.remote.dto.RestauranteDto
import com.driverapp.repartidor.data.remote.dto.ResumenGananciasDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PedidoApiService {

    @GET("api/v1/orders/available")
    suspend fun availableOrders(): List<PedidoDto>

    @GET("api/v1/orders/my")
    suspend fun myOrders(): List<PedidoDto>

    @GET("api/v1/orders/history")
    suspend fun orderHistory(): List<PedidoDto>

    @GET("api/v1/orders/{id}")
    suspend fun orderDetail(@Path("id") id: Long): PedidoDto

    @POST("api/v1/orders/{id}/accept")
    suspend fun acceptOrder(@Path("id") id: Long): PedidoDto

    @POST("api/v1/orders/{id}/reject")
    suspend fun rejectOrder(@Path("id") id: Long): PedidoDto

    @POST("api/v1/orders/{id}/in-transit")
    suspend fun inTransit(@Path("id") id: Long): PedidoDto

    @POST("api/v1/orders/{id}/complete")
    suspend fun completeOrder(@Path("id") id: Long): PedidoDto

    @GET("api/v1/earnings/summary")
    suspend fun earningsSummary(@Query("period") period: String): ResumenGananciasDto

    @GET("api/v1/earnings/history")
    suspend fun earningsHistory(): List<GananciaDto>

    @GET("api/v1/restaurants")
    suspend fun restaurants(): List<RestauranteDto>

    @GET("api/v1/notifications/my")
    suspend fun myNotifications(): List<NotificacionDto>
}
