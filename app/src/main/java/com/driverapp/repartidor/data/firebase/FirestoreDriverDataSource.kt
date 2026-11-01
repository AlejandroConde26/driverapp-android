package com.driverapp.repartidor.data.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Puente `Task` de Play Services → corrutina, sin añadir dependencias extra. */
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}

/**
 * S10 · presencia del repartidor en Cloud Firestore.
 *
 * Documento `drivers/{key}` con `online`, `lat`, `lng`, `updatedAt`.
 * Es un espejo del estado que ya manda la app por REST (`PUT users/me/location`):
 * sirve para que el dashboard pueda escuchar cambios en tiempo real sin polling.
 *
 * Si Firebase no está configurado, todos los métodos son no-op: la app sigue
 * funcionando solo con el backend REST.
 */
class FirestoreDriverDataSource {

    private val collection = "drivers"

    private fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun syncPresence(
        driverKey: String,
        online: Boolean,
        lat: Double?,
        lng: Double?
    ): Result<Unit> {
        if (!FirebaseConfig.isFirestoreEnabled() || driverKey.isBlank()) {
            return Result.success(Unit)
        }
        return runCatching {
            compensationRoomRedo(key = driverKey, online = online, lat = lat, lng = lng)
        }
    }

    private suspend fun compensationRoomRedo(
        key: String,
        online: Boolean,
        lat: Double?,
        lng: Double?
    ) {
        db().collection(collection).document(key).set(
            mapOf(
                "online" to online,
                "lat" to lat,
                "lng" to lng,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).awaitResult()
    }

    /** Registra la URL del comprobante de entrega en el pedido. */
    suspend fun attachProofUrl(driverKey: String, orderId: Int, url: String): Result<Unit> {
        if (!FirebaseConfig.isFirestoreEnabled() || driverKey.isBlank()) {
            return Result.success(Unit)
        }
        return runCatching {
            db().collection(collection).document(driverKey)
                .collection("deliveries").document(orderId.toString())
                .set(
                    mapOf("proofUrl" to url, "updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                )
                .awaitResult()
        }
    }
}
