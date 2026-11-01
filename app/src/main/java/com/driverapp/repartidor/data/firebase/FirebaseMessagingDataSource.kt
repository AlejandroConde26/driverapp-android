package com.driverapp.repartidor.data.firebase

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenHolder {
    @Volatile
    var token: String? = null
}

class FirebaseMessagingDataSource {

    suspend fun fetchToken(): String? {
        if (!FirebaseConfig.isConfigured()) return null
        return runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
    }
}
