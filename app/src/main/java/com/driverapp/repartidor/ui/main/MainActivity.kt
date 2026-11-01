package com.driverapp.repartidor.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.driverapp.repartidor.App
import com.driverapp.repartidor.data.hardware.gps.LocationForegroundService
import com.driverapp.repartidor.ui.AppNav
import kotlinx.coroutines.launch

/**
 * Única Activity de la app (Sprint 5).
 *
 * Toda la navegación vive en el `NavHost` de Compose ([AppNav]). El reporte de
 * ubicación ya NO se detiene aquí: lo gestiona `LocationForegroundService`,
 * que sobrevive a la Activity (Sprint 3).
 */
class MainActivity : ComponentActivity() {

    private val container get() = (application as App).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNav(container)
        }
    }

    override fun onResume() {
        super.onResume()
        // Sprint 3: reanuda el reporte de ubicación desde primer plano (Android 12+
        // prohíbe arrancarlo desde Application.onCreate) y sólo con permiso concedido.
        lifecycleScope.launch {
            val prefs = runCatching { container.userPreferences.snapshot() }.getOrNull()
                ?: return@launch
            if (prefs.locationEnabled && container.locationRepository.hasPermission()) {
                runCatching { LocationForegroundService.start(this@MainActivity) }
            }
        }
    }
}
