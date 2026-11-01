package com.driverapp.repartidor.domain.usecase

import com.driverapp.repartidor.domain.model.Ganancia
import com.driverapp.repartidor.domain.model.ResumenGanancias
import com.driverapp.repartidor.domain.repository.EarningsRepository

class ObtenerGananciasUseCase(private val repo: EarningsRepository) {
    suspend operator fun invoke(period: String): ResumenGanancias = repo.summary(period)
}

class ObtenerHistorialGananciasUseCase(private val repo: EarningsRepository) {
    suspend operator fun invoke(): List<Ganancia> = repo.history()
}
