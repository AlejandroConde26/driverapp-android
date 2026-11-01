package com.driverapp.repartidor.data

import com.driverapp.repartidor.data.local.UserPreferences
import com.driverapp.repartidor.testUserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Sprint 6 · UserPreferences sobre PreferencesDataStore (Flow + suspend). */
@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `valores por defecto`() = runTest {
        val prefs = testUserPreferences(tmp.root, backgroundScope)

        assertFalse(prefs.darkMode.first())
        assertTrue(prefs.notificationsEnabled.first())
        assertTrue(prefs.locationEnabled.first())
        assertTrue(prefs.notificationSound.first())
        assertTrue(prefs.showBalance.first())
        assertEquals(UserPreferences.LANG_ES, prefs.language.first())
    }

    @Test
    fun `las 5 preferencias se guardan y leen`() = runTest {
        val prefs = testUserPreferences(tmp.root, backgroundScope)

        prefs.setDarkMode(true)
        prefs.setNotificationsEnabled(false)
        prefs.setLocationEnabled(false)
        prefs.setNotificationSound(false)
        prefs.setShowBalance(false)
        prefs.setLanguage(UserPreferences.LANG_EN)

        assertTrue(prefs.darkMode.first())
        assertFalse(prefs.notificationsEnabled.first())
        assertFalse(prefs.locationEnabled.first())
        assertFalse(prefs.notificationSound.first())
        assertFalse(prefs.showBalance.first())
        assertEquals(UserPreferences.LANG_EN, prefs.language.first())
    }

    @Test
    fun `toggleShowBalance invierte y devuelve el nuevo valor`() = runTest {
        val prefs = testUserPreferences(tmp.root, backgroundScope)

        assertFalse(prefs.toggleShowBalance())
        assertFalse(prefs.showBalance.first())
        assertTrue(prefs.toggleShowBalance())
        assertTrue(prefs.showBalance.first())
    }

    @Test
    fun `snapshot devuelve todas las claves de una vez`() = runTest {
        val prefs = testUserPreferences(tmp.root, backgroundScope)
        prefs.setDarkMode(true)
        prefs.setNotificationSound(false)

        val snapshot = prefs.snapshot()

        assertTrue(snapshot.darkMode)
        assertFalse(snapshot.notificationSound)
        assertTrue(snapshot.notificationsEnabled)
        assertEquals(UserPreferences.LANG_ES, snapshot.language)
    }
}
