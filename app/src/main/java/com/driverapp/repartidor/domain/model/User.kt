package com.driverapp.repartidor.domain.model

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
