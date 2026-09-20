package com.driverapp.repartidor.data.remote.dto

import com.driverapp.repartidor.domain.model.ConductorBrief
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Ganancia
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.model.PedidoItem
import com.driverapp.repartidor.domain.model.Restaurante
import com.driverapp.repartidor.domain.model.ResumenGanancias
import com.google.gson.annotations.SerializedName

data class RestauranteDto(
    val id: Int,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val phone: String? = null,
    @SerializedName("opening_hours") val openingHours: String? = null
) {
    fun toDomain(): Restaurante = Restaurante(
        id = id, name = name, address = address, lat = lat, lng = lng,
        phone = phone, openingHours = openingHours
    )
}

data class PedidoItemDto(
    val id: Int = 0,
    @SerializedName("product_name") val productName: String,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double
) {
    fun toDomain(): PedidoItem = PedidoItem(
        id = id, productName = productName, quantity = quantity, unitPrice = unitPrice
    )
}

data class ConductorBriefDto(
    val id: Int,
    val name: String
) {
    fun toDomain(): ConductorBrief = ConductorBrief(id = id, name = name)
}

data class PedidoDto(
    val id: Int,
    @SerializedName("restaurant_id") val restaurantId: Int,
    @SerializedName("driver_id") val driverId: Int? = null,
    @SerializedName("client_name") val clientName: String,
    @SerializedName("client_address") val clientAddress: String,
    @SerializedName("dest_lat") val destLat: Double,
    @SerializedName("dest_lng") val destLng: Double,
    val status: String,
    val total: Double,
    @SerializedName("delivery_fee") val deliveryFee: Double = 0.0,
    @SerializedName("payment_method") val paymentMethod: String = "Efectivo",
    @SerializedName("distance_km") val distanceKm: Double = 0.0,
    @SerializedName("est_time_min") val estTimeMin: Int = 20,
    val restaurant: RestauranteDto? = null,
    val driver: ConductorBriefDto? = null,
    val items: List<PedidoItemDto> = emptyList()
) {
    fun toDomain(): Pedido = Pedido(
        id = id,
        restaurantId = restaurantId,
        driverId = driverId,
        clientName = clientName,
        clientAddress = clientAddress,
        destLat = destLat,
        destLng = destLng,
        estado = EstadoPedido.from(status),
        total = total,
        deliveryFee = deliveryFee,
        paymentMethod = paymentMethod,
        distanceKm = distanceKm,
        estTimeMin = estTimeMin,
        restaurant = restaurant?.toDomain(),
        driver = driver?.toDomain(),
        items = items.map { it.toDomain() }
    )
}

data class GananciaDto(
    val id: Int,
    @SerializedName("driver_id") val driverId: Int,
    @SerializedName("order_id") val orderId: Int?,
    val amount: Double,
    val type: String,
    @SerializedName("client_name") val clientName: String?,
    @SerializedName("created_at") val createdAt: String? = null
) {
    fun toDomain(): Ganancia = Ganancia(
        id = id, driverId = driverId, orderId = orderId, amount = amount,
        type = type, clientName = clientName, createdAt = createdAt
    )
}

data class ResumenGananciasDto(
    val period: String,
    val total: Double,
    @SerializedName("orders_count") val ordersCount: Int,
    @SerializedName("deliveries_total") val deliveriesTotal: Double,
    @SerializedName("tips_total") val tipsTotal: Double,
    @SerializedName("bonuses_total") val bonusesTotal: Double,
    @SerializedName("other_total") val otherTotal: Double,
    @SerializedName("chart_labels") val chartLabels: List<String>,
    @SerializedName("chart_values") val chartValues: List<Double>,
    val history: List<GananciaDto>
) {
    fun toDomain(): ResumenGanancias = ResumenGanancias(
        period = period,
        total = total,
        ordersCount = ordersCount,
        deliveriesTotal = deliveriesTotal,
        tipsTotal = tipsTotal,
        bonusesTotal = bonusesTotal,
        otherTotal = otherTotal,
        chartLabels = chartLabels,
        chartValues = chartValues,
        history = history.map { it.toDomain() }
    )
}

data class NotificacionDto(
    val id: Int,
    val title: String,
    val message: String,
    val read: Boolean
)
