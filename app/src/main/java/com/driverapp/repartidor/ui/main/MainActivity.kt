package com.driverapp.repartidor.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.driverapp.repartidor.App
import com.driverapp.repartidor.ui.AppNav

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
}
