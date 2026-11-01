package com.driverapp.repartidor.domain.repository

import com.driverapp.repartidor.domain.model.Ganancia
import com.driverapp.repartidor.domain.model.ResumenGanancias

/** Ganancias con caché local por periodo (Sprint 6). */
interface EarningsRepository {

    suspend fun summary(period: String): ResumenGanancias
    suspend fun history(): List<Ganancia>
}
