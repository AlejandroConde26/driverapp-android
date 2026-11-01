package com.driverapp.repartidor.data.firebase

import com.driverapp.repartidor.BuildConfig

/**
 * Detección de configuración de Firebase (S10).
 *
 * Las credenciales viven en `local.properties` (no versionadas) y llegan aquí
 * como `BuildConfig`. Si están vacías la app sigue funcionando: el login usa el
 * modo dev del backend y las funciones de Firestore/Storage quedan apagadas.
 */
object FirebaseConfig {

    fun isConfigured(): Boolean =
        BuildConfig.FIREBASE_API_KEY.isNotBlank() && BuildConfig.FIREBASE_APP_ID.isNotBlank()

    /** Firestore sólo se usa si hay proyecto configurado. */
    fun isFirestoreEnabled(): Boolean =
        isConfigured() && BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()

    /** Storage necesita bucket además de proyecto. */
    fun isStorageEnabled(): Boolean =
        isConfigured() && BuildConfig.FIREBASE_STORAGE_BUCKET.isNotBlank()

    /**
     * El `mobilesdk_app_id` de Firebase está atado a un package_name concreto.
     * Si no coincide con el de esta app, Auth puede responder 403
     * (API_KEY_ANDROID_PACKAGE_BLOCKED) o auth/invalid-api-key.
     */
    fun packageHint(): String =
        "Verifica en la consola de Firebase que el package ${BuildConfig.APPLICATION_ID} " +
            "está registrado en el proyecto (el appId configurado puede pertenecer a otro package)."
}
