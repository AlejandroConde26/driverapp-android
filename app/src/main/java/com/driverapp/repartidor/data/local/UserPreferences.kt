package com.driverapp.repartidor.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    )

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF, value).apply()

    var locationEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCATION, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCATION, value).apply()

    var notificationSound: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANG, LANG_ES) ?: LANG_ES
        set(value) = prefs.edit().putString(KEY_LANG, value).apply()

    companion object {
        const val FILE = "driverapp_prefs"
        const val LANG_ES = "es"
        const val LANG_EN = "en"
        private const val KEY_DARK = "dark"
        private const val KEY_NOTIF = "notifications"
        private const val KEY_LOCATION = "location"
        private const val KEY_SOUND = "sound"
        private const val KEY_LANG = "language"
    }
}
