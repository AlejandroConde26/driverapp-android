package com.driverapp.repartidor.domain.repository

import java.io.File

/** S10 · comprobante de entrega (Storage + registro en Firestore). */
interface ProofRepository {

    /** Sube la foto y devuelve la URL pública del comprobante. */
    suspend fun upload(orderId: Int, file: File): String
}
