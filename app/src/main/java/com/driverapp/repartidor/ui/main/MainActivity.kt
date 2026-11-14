package com.driverapp.repartidor.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.driverapp.repartidor.App
import com.driverapp.repartidor.data.hardware.gps.LocationForegroundService
import com.driverapp.repartidor.ui.AppNav
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Única Activity de la app (Sprint 5).
 *
 * Toda la navegación vive en el `NavHost` de Compose ([AppNav]). El reporte de
 * ubicación ya NO se detiene aquí: lo gestiona `LocationForegroundService`,
 * que sobrevive a la Activity (Sprint 3).
 */
class MainActivity : AppCompatActivity() {

    private val container get() = (application as App).container

    /** Pedido que abrió la app desde una notificación; se consume una sola vez. */
    private val pendingOrderId = MutableStateFlow<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readOrderFromIntent(intent)
        setContent {
            AppNav(
                container = container,
                pendingOrderId = pendingOrderId,
                onOrderConsumed = { pendingOrderId.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readOrderFromIntent(intent)
    }

    /** La notificación de pedido nuevo viaja con `extra_order_id`. */
    private fun readOrderFromIntent(intent: Intent?) {
        val id = intent?.getIntExtra(EXTRA_ORDER_ID, 0) ?: 0
        if (id > 0) {
            pendingOrderId.value = id
            intent?.removeExtra(EXTRA_ORDER_ID)
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

    companion object {
        /** Extra de la notificación para abrir directamente un pedido. */
        const val EXTRA_ORDER_ID = "extra_order_id"
    }
}
