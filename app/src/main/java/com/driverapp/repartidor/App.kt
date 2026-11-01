package com.driverapp.repartidor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.driverapp.repartidor.data.firebase.FirebaseConfig
import com.driverapp.repartidor.data.hardware.gps.LocationForegroundService
import com.driverapp.repartidor.di.AppContainer
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.runBlocking

class App : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()

        // Lectura puntual y bloqueante: el tema y el idioma tienen que estar puestos
        // antes de que se infle la primera pantalla, si no la app parpadea.
        val prefs = runBlocking { container.userPreferences.snapshot() }

        applyLanguage(prefs.language)
        applyNightMode(prefs.darkMode)
        initFirebase()
        createNotificationChannels()

        // Sprint 3: si el repartidor dejó la ubicación activada, el servicio arranca
        // con la app (antes había que entrar a Perfil y tocar el switch cada vez).
        if (prefs.locationEnabled) {
            LocationForegroundService.start(this)
        }
    }

    companion object {
        private const val TAG = "DriverApp"

        const val CHANNEL_ORDERS = "orders"
        const val CHANNEL_LOCATION = "location"

        fun applyLanguage(code: String) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
        }

        fun applyNightMode(enabled: Boolean) {
            AppCompatDelegate.setDefaultNightMode(
                if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun initFirebase() {
        if (!FirebaseConfig.isConfigured()) {
            Log.i(TAG, "Firebase no configurado: la app funciona en modo dev (auth local + REST).")
            return
        }
        if (FirebaseApp.getApps(this).isEmpty()) {
            runCatching {
                FirebaseApp.initializeApp(
                    this,
                    FirebaseOptions.Builder()
                        .setApiKey(BuildConfig.FIREBASE_API_KEY)
                        .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                        .apply {
                            if (BuildConfig.FIREBASE_STORAGE_BUCKET.isNotBlank()) {
                                setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                            }
                        }
                        .build()
                )
            }.onFailure { Log.e(TAG, "No se pudo inicializar Firebase: ${it.message}") }
        }
    }

    private fun createNotificationChannels() {
        // NotificationChannel existe desde API 26; minSdk es 24, así que en Android 7
        // no hay canales y hay que salir sin tocar la API nueva (antes crasheaba).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)

        val orders = NotificationChannel(
            CHANNEL_ORDERS, "Pedidos", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notificaciones de nuevos pedidos" }

        val location = NotificationChannel(
            CHANNEL_LOCATION, "Ubicación en tiempo real", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Mantiene el reporte de ubicación mientras estás en línea" }

        manager.createNotificationChannels(listOf(orders, location))
    }
}
