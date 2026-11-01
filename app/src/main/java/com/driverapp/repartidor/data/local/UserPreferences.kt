package com.driverapp.repartidor.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Única instancia de DataStore de preferencias (Sprint 6).
 * Se migra automáticamente desde el SharedPreferences `driverapp_prefs` anterior,
 * para que el usuario no pierda sus ajustes al actualizar.
 */
val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "driverapp_prefs",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, UserPreferences.LEGACY_FILE))
    }
)

/**
 * Preferencias del repartidor sobre PreferencesDataStore.
 *
 * Lectura: `Flow` (los ViewModels hacen `stateIn`).
 * Escritura: funciones `suspend` (nunca bloquean el hilo principal).
 * Para lectores que no pueden ser suspendidos (arranque del tema, servicio FCM)
 * existe [snapshot], que se llama una sola vez y se documenta como bloqueante.
 */
class UserPreferences(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.userPrefsDataStore)

    val darkMode: Flow<Boolean> = dataStore.data.map { it[KEY_DARK] ?: false }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_NOTIF] ?: true }
    val locationEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_LOCATION] ?: true }
    val notificationSound: Flow<Boolean> = dataStore.data.map { it[KEY_SOUND] ?: true }
    val showBalance: Flow<Boolean> = dataStore.data.map { it[KEY_BALANCE] ?: true }
    val language: Flow<String> = dataStore.data.map { it[KEY_LANG] ?: LANG_ES }

    suspend fun setDarkMode(value: Boolean) = dataStore.edit { it[KEY_DARK] = value }

    suspend fun setNotificationsEnabled(value: Boolean) = dataStore.edit { it[KEY_NOTIF] = value }

    suspend fun setLocationEnabled(value: Boolean) = dataStore.edit { it[KEY_LOCATION] = value }

    suspend fun setNotificationSound(value: Boolean) = dataStore.edit { it[KEY_SOUND] = value }

    suspend fun setShowBalance(value: Boolean) = dataStore.edit { it[KEY_BALANCE] = value }

    suspend fun setLanguage(code: String) = dataStore.edit { it[KEY_LANG] = code }

    /** Invierte `show_balance` y devuelve el nuevo valor. */
    suspend fun toggleShowBalance(): Boolean {
        var next = true
        dataStore.edit {
            next = !(it[KEY_BALANCE] ?: true)
            it[KEY_BALANCE] = next
        }
        return next
    }

    /**
     * Lectura puntual de todas las preferencias.
     * Sólo para contextos no suspendibles (Application.onCreate aplicando el tema,
     * servicio FCM decidiendo si muestra la notificación). No usar en la UI.
     */
    suspend fun snapshot(): Snapshot {
        val p = dataStore.data.first()
        return Snapshot(
            darkMode = p[KEY_DARK] ?: false,
            notificationsEnabled = p[KEY_NOTIF] ?: true,
            locationEnabled = p[KEY_LOCATION] ?: true,
            notificationSound = p[KEY_SOUND] ?: true,
            showBalance = p[KEY_BALANCE] ?: true,
            language = p[KEY_LANG] ?: LANG_ES
        )
    }

    data class Snapshot(
        val darkMode: Boolean,
        val notificationsEnabled: Boolean,
        val locationEnabled: Boolean,
        val notificationSound: Boolean,
        val showBalance: Boolean,
        val language: String
    )

    companion object {
        /** Nombre del SharedPreferences legado que se migra a DataStore. */
        const val LEGACY_FILE = "driverapp_prefs"
        const val LANG_ES = "es"
        const val LANG_EN = "en"

        private val KEY_DARK = booleanPreferencesKey("dark")
        private val KEY_NOTIF = booleanPreferencesKey("notifications")
        private val KEY_LOCATION = booleanPreferencesKey("location")
        private val KEY_SOUND = booleanPreferencesKey("sound")
        private val KEY_BALANCE = booleanPreferencesKey("show_balance")
        private val KEY_LANG = stringPreferencesKey("language")
    }
}
