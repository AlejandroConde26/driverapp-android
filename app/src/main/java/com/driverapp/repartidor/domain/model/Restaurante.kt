package com.driverapp.repartidor.domain.model

data class Restaurante(
    val id: Int,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val phone: String? = null,
    val openingHours: String? = null
)

data class PedidoItem(
    val id: Int = 0,
    val menuItemId: Int? = null,
    val productName: String,
    val description: String? = null,
    val quantity: Int,
    val unitPrice: Double,
    val notes: String? = null
)

data class ConductorBrief(
    val id: Int,
    val name: String
)
