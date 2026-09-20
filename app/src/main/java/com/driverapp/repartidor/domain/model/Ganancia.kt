package com.driverapp.repartidor.domain.model

data class Ganancia(
    val id: Int,
    val driverId: Int,
    val orderId: Int?,
    val amount: Double,
    val type: String,
    val clientName: String?,
    val createdAt: String? = null
)

data class ResumenGanancias(
    val period: String,
    val total: Double,
    val ordersCount: Int,
    val deliveriesTotal: Double,
    val tipsTotal: Double,
    val bonusesTotal: Double,
    val otherTotal: Double,
    val chartLabels: List<String>,
    val chartValues: List<Double>,
    val history: List<Ganancia>
)
