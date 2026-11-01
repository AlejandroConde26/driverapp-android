package com.driverapp.repartidor.domain.repository

import com.driverapp.repartidor.domain.model.Ubicacion
import kotlinx.coroutines.flow.StateFlow

interface LocationRepository {

    val location: StateFlow<Ubicacion?>

    fun hasPermission(): Boolean
    fun isTracking(): Boolean
    fun start()
    fun stop()

    fun shouldReport(intervalMs: Long): Boolean
    fun markReported()

    suspend fun report(ubicacion: Ubicacion)
}
