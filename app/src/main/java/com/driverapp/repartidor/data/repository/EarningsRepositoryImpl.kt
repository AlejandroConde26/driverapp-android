package com.driverapp.repartidor.data.repository

import com.driverapp.repartidor.data.local.db.LocalDataSource
import com.driverapp.repartidor.data.remote.PedidoApiService
import com.driverapp.repartidor.data.remote.toApiFailure
import com.driverapp.repartidor.domain.model.Ganancia
import com.driverapp.repartidor.domain.model.ResumenGanancias
import com.driverapp.repartidor.domain.repository.EarningsRepository

/**
 * Repositorio de ganancias cache-first: red primero para datos frescos,
 * caché local cuando la red falla (offline-first, Sprint 6).
 */
class EarningsRepositoryImpl(
    private val api: PedidoApiService,
    private val local: LocalDataSource
) : EarningsRepository {

    override suspend fun summary(period: String): ResumenGanancias {
        return try {
            val fresh = api.earningsSummary(period).toDomain()
            local.saveSummary(fresh)
            fresh
        } catch (e: Throwable) {
            val failure = e.toApiFailure()
            local.summary(period) ?: throw failure
        }
    }

    override suspend fun history(): List<Ganancia> {
        return try {
            val fresh = api.earningsHistory().map { it.toDomain() }
            local.saveHistory(fresh)
            fresh
        } catch (e: Throwable) {
            val failure = e.toApiFailure()
            local.history().ifEmpty { throw failure }
        }
    }
}
