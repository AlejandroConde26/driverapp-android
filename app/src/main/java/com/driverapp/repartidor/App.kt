package com.driverapp.repartidor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.driverapp.repartidor.data.firebase.FirebaseConfig
import com.driverapp.repartidor.di.AppContainer
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class App : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        initFirebase()
        createNotificationChannel()
    }

    private fun initFirebase() {
        if (!FirebaseConfig.isConfigured()) return
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(
                this,
                FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .build()
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "orders", "Pedidos", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notificaciones de nuevos pedidos" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
