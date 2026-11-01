package com.driverapp.repartidor.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.driverapp.repartidor.domain.model.User
import com.google.gson.Gson

class TokenDataStore(context: Context) {

    private val gson = Gson()

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "driverapp_secret",
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()

    fun token(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUser(user: User) = prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()

    fun user(): User? =
        prefs.getString(KEY_USER, null)?.let { runCatching { gson.fromJson(it, User::class.java) }.getOrNull() }

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user"
    }
}
