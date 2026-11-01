package com.driverapp.repartidor.data.hardware.gps

import android.content.Context
import com.driverapp.repartidor.data.remote.AuthApiService
import com.driverapp.repartidor.data.remote.dto.LocationUpdateDto
import com.driverapp.repartidor.domain.model.Ubicacion
import com.driverapp.repartidor.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LocationRepositoryImpl(
    context: Context,
    private val authApi: AuthApiService
) : LocationRepository {

    private val engine = LocationTracker(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectJob: Job? = null

    private val _location = MutableStateFlow<Ubicacion?>(null)
    override val location: StateFlow<Ubicacion?> = _location

    override fun hasPermission(): Boolean = engine.hasPermission()

    override fun isTracking(): Boolean = engine.isTracking()

    override fun start() {
        engine.start()
        if (collectJob == null) {
            collectJob = scope.launch {
                engine.location.collect { loc ->
                    _location.value = loc?.let {
                        Ubicacion(lat = it.latitude, lng = it.longitude, speedMps = it.speed.takeIf { s -> s > 0f })
                    }
                }
            }
        }
    }

    override fun stop() {
        engine.stop()
        collectJob?.cancel()
        collectJob = null
    }

    override fun shouldReport(intervalMs: Long): Boolean = engine.shouldReport(intervalMs)

    override fun markReported() = engine.markReported()

    override suspend fun report(ubicacion: Ubicacion) {
        authApi.updateLocation(LocationUpdateDto(ubicacion.lat, ubicacion.lng))
    }
}
