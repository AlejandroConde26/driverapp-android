package com.driverapp.repartidor.data.hardware.gps

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.driverapp.repartidor.App
import com.driverapp.repartidor.R
import com.driverapp.repartidor.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que reporta la ubicación del repartidor (Sprint 3).
 *
 * Antes el loop vivía en `MainActivity.lifecycleScope`: al cerrar la app el
 * reporte se detenía. Un `ForegroundService` con `foregroundServiceType="location"`
 * mantiene el acceso al GPS aunque la app esté en segundo plano, sin necesidad de
 * pedir `ACCESS_BACKGROUND_LOCATION`.
 *
 * Reporta cada [REPORT_INTERVAL_MS] (30 s) siempre que haya una lectura nueva.
 */
class LocationForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        // CONTRATO IMPORTANTE: si alguien llamó a startForegroundService(), hay que
        // llamar a startForeground() en menos de 5 s **aunque después vayamos a parar**.
        // Antes se hacía stopSelf() sin haber llamado a startForeground() → el sistema
        // mataba el proceso con "did not then call Service.startForeground()".
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            // p. ej. SecurityException en Android 14 si falta el permiso de ubicación
            android.util.Log.w("DriverApp", "No se pudo iniciar el servicio de ubicación: ${e.message}")
            stopSelf()
            return
        }

        val container = (application as App).container
        val repo = container.locationRepository

        if (!repo.hasPermission()) {
            stopTracking()
            stopSelf()
            return
        }

        repo.start()

        if (loop != null) return
        loop = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                if (!repo.isTracking()) continue
                val location = repo.location.value ?: continue
                if (!repo.shouldReport(REPORT_INTERVAL_MS)) continue
                // `report` lanza ApiFailure; sin red se descarta esta lectura y
                // se reintenta en el siguiente tick (la cola offline es para
                // transiciones de pedido, no para posiciones).
                runCatching { repo.report(location) }
                repo.markReported()
            }
        }
    }

    private fun stopTracking() {
        loop?.cancel()
        loop = null
        runCatching { (application as App).container.locationRepository.stop() }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, App.CHANNEL_LOCATION)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(getString(R.string.location_service_title))
            .setContentText(getString(R.string.location_service_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    override fun onDestroy() {
        stopTracking()
        scope.coroutineContext.cancelChildren()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.driverapp.repartidor.LOCATION_START"
        private const val ACTION_STOP = "com.driverapp.repartidor.LOCATION_STOP"
        private const val NOTIFICATION_ID = 4242
        private const val TICK_MS = 10_000L
        private const val REPORT_INTERVAL_MS = 30_000L

        fun start(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java)
                .setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java)
                .setAction(ACTION_STOP)
            runCatching { context.startService(intent) }
                .onFailure { context.stopService(Intent(context, LocationForegroundService::class.java)) }
        }
    }
}

private fun kotlin.coroutines.CoroutineContext.cancelChildren() {
    this[kotlinx.coroutines.Job]?.cancelChildren()
}
