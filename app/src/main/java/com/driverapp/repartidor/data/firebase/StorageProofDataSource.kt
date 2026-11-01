package com.driverapp.repartidor.data.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.io.File

/**
 * S10 · comprobante de entrega en Cloud Storage.
 *
 * Sube la foto a `deliveries/{orderId}.jpg` y devuelve su URL de descarga.
 * Si Storage no está configurado lanza una excepción explícita: la UI muestra
 * "no se pudo subir el comprobante" en vez de fallar en silencio.
 */
class StorageProofDataSource {

    suspend fun uploadProof(orderId: Int, file: File): String {
        check(FirebaseConfig.isStorageEnabled()) { "Firebase Storage no está configurado" }
        val ref = FirebaseStorage.getInstance().reference.child("deliveries/$orderId.jpg")
        ref.putFile(Uri.fromFile(file)).awaitResult()
        return ref.downloadUrl.awaitResult().toString()
    }
}
