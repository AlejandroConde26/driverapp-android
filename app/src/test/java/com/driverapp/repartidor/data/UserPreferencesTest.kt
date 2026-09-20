package com.driverapp.repartidor.data

import android.content.SharedPreferences
import com.driverapp.repartidor.data.local.UserPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferencesTest {

    private fun fakePrefs(initial: Map<String, Any?> = emptyMap()): SharedPreferences {
        val data = initial.toMutableMap()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putBoolean(any(), any()) } answers {
            data[firstArg<String>()] = secondArg<Boolean>()
            editor
        }
        every { editor.putString(any(), any()) } answers {
            data[firstArg<String>()] = secondArg<String>()
            editor
        }
        val prefs = mockk<SharedPreferences>()
        every { prefs.getBoolean(any(), any()) } answers {
            data[firstArg<String>()] as? Boolean ?: secondArg()
        }
        every { prefs.getString(any(), any()) } answers {
            data[firstArg<String>()] as? String ?: secondArg()
        }
        every { prefs.edit() } returns editor
        return prefs
    }

    @Test
    fun `valores por defecto`() {
        val prefs = UserPreferences(fakePrefs())

        assertFalse(prefs.darkMode)
        assertTrue(prefs.notificationsEnabled)
        assertTrue(prefs.locationEnabled)
        assertTrue(prefs.notificationSound)
        assertEquals(UserPreferences.LANG_ES, prefs.language)
    }

    @Test
    fun `las 5 preferencias se guardan y leen`() {
        val prefs = UserPreferences(fakePrefs())

        prefs.darkMode = true
        prefs.notificationsEnabled = false
        prefs.locationEnabled = false
        prefs.notificationSound = false
        prefs.language = UserPreferences.LANG_EN

        assertTrue(prefs.darkMode)
        assertFalse(prefs.notificationsEnabled)
        assertFalse(prefs.locationEnabled)
        assertFalse(prefs.notificationSound)
        assertEquals(UserPreferences.LANG_EN, prefs.language)
    }
}
