package com.driverapp.repartidor.domain.usecase

import com.driverapp.repartidor.domain.model.Ubicacion
import com.driverapp.repartidor.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

class ObtenerUbicacionUseCase(private val repo: LocationRepository) {
    operator fun invoke(): Flow<Ubicacion?> = repo.location
}

class ReportarUbicacionUseCase(private val repo: LocationRepository) {
    suspend operator fun invoke(ubicacion: Ubicacion) = repo.report(ubicacion)
}
