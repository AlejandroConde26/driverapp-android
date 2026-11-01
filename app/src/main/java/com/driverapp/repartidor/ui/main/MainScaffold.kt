package com.driverapp.repartidor.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driverapp.repartidor.R
import com.driverapp.repartidor.ui.common.UiMessenger
import com.driverapp.repartidor.ui.components.OfflineBanner
import com.driverapp.repartidor.ui.components.StatusPill
import com.driverapp.repartidor.ui.components.ToastHost

object Routes {
    const val LOGIN = "login"
    const val ORDERS = "orders"
    const val MAP = "map"
    const val EARNINGS = "earnings"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
}

/**
 * Esqueleto compartido de las 4 pestañas: header (saludo, pill online/offline,
 * acceso a novedades), banner de conectividad, contenido y NavigationBar.
 */
@Composable
fun MainScaffold(
    title: String,
    subtitle: String,
    online: Boolean,
    connectivity: Boolean,
    pendingSync: Int,
    currentRoute: String,
    messenger: UiMessenger,
    onToggleStatus: () -> Unit,
    onTab: (String) -> Unit,
    onNotifications: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavItem(
                        route = Routes.ORDERS,
                        icon = R.drawable.ic_fa_list_check,
                        label = stringResource(R.string.tab_orders),
                        currentRoute = currentRoute,
                        onTab = onTab
                    )
                    NavItem(
                        route = Routes.MAP,
                        icon = R.drawable.ic_fa_map,
                        label = stringResource(R.string.tab_map),
                        currentRoute = currentRoute,
                        onTab = onTab
                    )
                    NavItem(
                        route = Routes.EARNINGS,
                        icon = R.drawable.ic_fa_wallet,
                        label = stringResource(R.string.tab_earnings),
                        currentRoute = currentRoute,
                        onTab = onTab
                    )
                    NavItem(
                        route = Routes.PROFILE,
                        icon = R.drawable.ic_fa_user,
                        label = stringResource(R.string.tab_profile),
                        currentRoute = currentRoute,
                        onTab = onTab
                    )
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNotifications) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fa_bell),
                            contentDescription = stringResource(R.string.tab_notifications),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    StatusPill(online = online, onClick = onToggleStatus)
                }

                OfflineBanner(online = connectivity, pending = pendingSync)

                Box(Modifier.fillMaxSize()) { content() }
            }
        }

        ToastHost(
            messenger = messenger,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun RowScope.NavItem(
    route: String,
    icon: Int,
    label: String,
    currentRoute: String,
    onTab: (String) -> Unit
) {
    val selected = currentRoute == route
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onTab(route) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 16.dp, vertical = 3.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
