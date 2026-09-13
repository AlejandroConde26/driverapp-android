package com.driverapp.repartidor.data

import com.google.gson.annotations.SerializedName

data class User(
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
)

data class LoginRequest(@SerializedName("id_token") val idToken: String, @SerializedName("fcm_token") val fcmToken: String? = null)

data class LoginResponse(val user: User, val token: String)

data class UserUpdate(val name: String? = null, val phone: String? = null, val email: String? = null)
data class VehicleUpdate(val vehicle: String)
data class StatusUpdate(val online: Boolean)
data class LocationUpdate(val latitude: Double, val longitude: Double)
data class FcmTokenUpdate(@SerializedName("fcm_token") val fcmToken: String)

data class Restaurant(
    val id: Int,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val phone: String? = null,
    @SerializedName("opening_hours") val openingHours: String? = null
)

data class RestaurantIn(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val phone: String? = null,
    @SerializedName("opening_hours") val openingHours: String? = null
)

data class OrderItem(
    val id: Int = 0,
    @SerializedName("product_name") val productName: String,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double
)

data class DriverBrief(val id: Int, val name: String)

data class Order(
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
    val restaurant: Restaurant? = null,
    val driver: DriverBrief? = null,
    val items: List<OrderItem> = emptyList()
)

data class EarningOut(
    val id: Int,
    @SerializedName("driver_id") val driverId: Int,
    @SerializedName("order_id") val orderId: Int?,
    val amount: Double,
    val type: String,
    @SerializedName("client_name") val clientName: String?,
    @SerializedName("created_at") val createdAt: String? = null
)

data class EarningsSummary(
    val period: String,
    val total: Double,
    @SerializedName("orders_count") val ordersCount: Int,
    @SerializedName("deliveries_total") val deliveriesTotal: Double,
    @SerializedName("tips_total") val tipsTotal: Double,
    @SerializedName("bonuses_total") val bonusesTotal: Double,
    @SerializedName("other_total") val otherTotal: Double,
    @SerializedName("chart_labels") val chartLabels: List<String>,
    @SerializedName("chart_values") val chartValues: List<Double>,
    val history: List<EarningOut>
)

data class NotificationOut(
    val id: Int,
    val title: String,
    val message: String,
    val read: Boolean
)

data class Dashboard(
    val kpis: Kpis,
    @SerializedName("orders_by_status") val ordersByStatus: Map<String, Int>,
    @SerializedName("recent_orders") val recentOrders: List<Order>
) {
    data class Kpis(
        @SerializedName("orders_today") val ordersToday: Int,
        @SerializedName("earnings_today") val earningsToday: Double,
        @SerializedName("active_drivers") val activeDrivers: Int,
        @SerializedName("pending_orders") val pendingOrders: Int,
        val drivers: Int,
        val restaurants: Int
    )
}

data class AdminDriver(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val vehicle: String? = null,
    val online: Boolean,
    val active: Boolean,
    @SerializedName("completed_orders") val completedOrders: Int,
    @SerializedName("earnings_total") val earningsTotal: Double
)