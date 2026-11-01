package com.driverapp.repartidor.data.repository

import com.driverapp.repartidor.data.firebase.FirestoreDriverDataSource
import com.driverapp.repartidor.data.firebase.StorageProofDataSource
import com.driverapp.repartidor.domain.repository.ProofRepository
import java.io.File

class ProofRepositoryImpl(
    private val storage: StorageProofDataSource,
    private val firestore: FirestoreDriverDataSource,
    private val driverKeyProvider: () -> String?
) : ProofRepository {

    override suspend fun upload(orderId: Int, file: File): String {
        val url = storage.uploadProof(orderId, file)
        runCatching { firestore.attachProofUrl(driverKeyProvider().orEmpty(), orderId, url) }
        return url
    }
}
