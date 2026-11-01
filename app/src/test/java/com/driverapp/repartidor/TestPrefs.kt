package com.driverapp.repartidor

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.driverapp.repartidor.data.local.UserPreferences
import kotlinx.coroutines.CoroutineScope
import java.io.File

/**
 * DataStore real sobre un fichero temporal (no un mock): así el test ejercita
 * el mismo camino que la app, incluido el `Flow` y la escritura atómica.
 * Pasar `backgroundScope` de `runTest` para que se cancele al terminar.
 */
fun testPrefsStore(dir: File, scope: CoroutineScope): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(dir, "prefs_${System.nanoTime()}.preferences_pb") }
    )

fun testUserPreferences(dir: File, scope: CoroutineScope): UserPreferences =
    UserPreferences(testPrefsStore(dir, scope))
