package com.driverapp.repartidor.data.hardware.gps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationTracker(context: Context) {

    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _location.value = location
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
    }

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    @Volatile
    private var tracking = false

    private var lastReportAt = 0L

    fun isTracking(): Boolean = tracking

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission()) return
        if (tracking) return
        tracking = true

        manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { _location.value = it }
        manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { _location.value = it }

        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000L, 10f, listener, Looper.getMainLooper())
        } catch (_: Exception) {
        }
        try {
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10_000L, 10f, listener, Looper.getMainLooper())
        } catch (_: Exception) {
        }
    }

    fun stop() {
        if (!tracking) return
        tracking = false
        try {
            manager.removeUpdates(listener)
        } catch (_: Exception) {
        }
    }

    fun markReported() {
        lastReportAt = System.currentTimeMillis()
    }

    fun shouldReport(intervalMs: Long): Boolean =
        lastReportAt == 0L || System.currentTimeMillis() - lastReportAt >= intervalMs
}
