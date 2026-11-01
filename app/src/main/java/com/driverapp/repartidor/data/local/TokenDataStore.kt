package com.driverapp.repartidor.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.driverapp.repartidor.domain.model.User
import com.google.gson.Gson

/**
 * Sesión cifrada (`EncryptedSharedPreferences`, AES256-SIV/GCM).
 *
 * Endurecido para arranque: `MasterKeys.getOrCreate` / `EncryptedSharedPreferences.create`
 * pueden lanzar en dispositivos con el Keystore roto o tras una restauración
 * (`KeyStoreException`, `InvalidProtocolBufferException`). Si eso pasa, la sesión
 * pasa a memoria (se pierde al cerrar la app) en vez de tumbar el proceso, y
 * **nunca** se escribe en claro en disco.
 */
class TokenDataStore(context: Context) {

    private val gson = Gson()
    private val appContext = context.applicationContext

    /** Fallback en memoria: sólo se usa si el Keystore no está disponible. */
    private val memory = mutableMapOf<String, String>()

    private val prefs: SharedPreferences? by lazy {
        runCatching {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                FILE,
                masterKeyAlias,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.onFailure {
            Log.e(TAG, "Keystore no disponible; la sesión no se persistirá: ${it.message}", it)
        }.getOrNull()
    }

    fun saveToken(token: String) = put(KEY_TOKEN, token)

    fun token(): String? = read(KEY_TOKEN)

    fun saveUser(user: User) = put(KEY_USER, gson.toJson(user))

    fun user(): User? =
        read(KEY_USER)?.let { runCatching { gson.fromJson(it, User::class.java) }.getOrNull() }

    fun clear() {
        memory.clear()
        prefs?.edit()?.clear()?.apply()
    }

    private fun put(key: String, value: String) {
        val encrypted = prefs
        if (encrypted != null) {
            encrypted.edit().putString(key, value).apply()
        } else {
            memory[key] = value
        }
    }

    private fun read(key: String): String? = prefs?.getString(key, null) ?: memory[key]

    companion object {
        private const val TAG = "DriverApp"
        private const val FILE = "driverapp_secret"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user"
    }
}
