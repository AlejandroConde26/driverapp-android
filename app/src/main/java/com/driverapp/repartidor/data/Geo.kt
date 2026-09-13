package com.driverapp.repartidor.data

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {

    private const val EARTH_RADIUS_M = 6371000.0

    fun distanceMeters(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val dLat = Math.toRadians(bLat - aLat)
        val dLng = Math.toRadians(bLng - aLng)
        val la1 = Math.toRadians(aLat)
        val la2 = Math.toRadians(bLat)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(la1) * cos(la2) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
    }

    fun vehicleSpeedKmh(vehicle: String?): Double = when (vehicle?.lowercase()) {
        "moto" -> 35.0
        "auto", "carro" -> 28.0
        else -> 16.0
    }

    fun estimateMinutes(meters: Double, speedKmh: Double): Int =
        ((meters / 1000.0) / speedKmh * 60.0).roundToInt().coerceAtLeast(1)

    fun formatDistance(meters: Double): String =
        if (meters < 1000) "${meters.roundToInt()} m" else "${"%.2f".format(meters / 1000.0)} km"

    fun formatEta(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}