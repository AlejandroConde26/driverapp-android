package com.driverapp.repartidor.fcm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.driverapp.repartidor.data.Auth
import com.driverapp.repartidor.data.FirebaseTokenProvider
import com.driverapp.repartidor.ui.main.AppViewModel
import com.google.firebase.messaging.FirebaseMessaging

object FirebaseRegistration {

    fun ensureToken(vm: AppViewModel) {
        if (!Auth.isFirebaseConfigured()) return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                FirebaseTokenProvider.fcmToken = token
                vm.updateFcmToken(token)
            }
            .addOnFailureListener { }
    }

    fun ensureNotificationPermission(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= 33) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(permission)
            }
        }
    }
}