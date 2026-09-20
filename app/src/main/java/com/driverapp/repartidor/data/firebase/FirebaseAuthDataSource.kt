package com.driverapp.repartidor.data.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSource {

    fun isConfigured(): Boolean = FirebaseConfig.isConfigured()

    suspend fun signIn(email: String, password: String): String {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        return auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("No se pudo obtener el token de Firebase")
    }
}
