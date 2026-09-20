package com.driverapp.repartidor.data.firebase

import com.driverapp.repartidor.BuildConfig

object FirebaseConfig {
    fun isConfigured(): Boolean =
        BuildConfig.FIREBASE_API_KEY.isNotBlank() && BuildConfig.FIREBASE_APP_ID.isNotBlank()
}
