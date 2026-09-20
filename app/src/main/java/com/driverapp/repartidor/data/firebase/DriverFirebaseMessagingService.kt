package com.driverapp.repartidor.data.firebase

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.driverapp.repartidor.App
import com.driverapp.repartidor.R
import com.driverapp.repartidor.ui.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DriverFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenHolder.token = token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "DriverApp"
        val body = message.notification?.body ?: message.data["message"] ?: ""
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val token = (application as? App)?.container?.tokenStore?.token()
        if (token.isNullOrBlank()) return
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            NotificationManagerCompat.from(this).areNotificationsEnabled().not()
        ) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "orders")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification) }
    }
}
