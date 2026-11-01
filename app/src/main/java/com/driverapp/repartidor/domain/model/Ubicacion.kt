package com.driverapp.repartidor.domain.model

data class Ubicacion(
    val lat: Double,
    val lng: Double,
    val speedMps: Float? = null
)
