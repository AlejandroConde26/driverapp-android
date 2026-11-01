package com.driverapp.repartidor.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.model.Notificacion
import com.driverapp.repartidor.ui.common.ErrorState
import com.driverapp.repartidor.ui.common.LoadingState
import com.driverapp.repartidor.ui.components.EmptyStateBox
import com.driverapp.repartidor.ui.components.ErrorStateBox
import com.driverapp.repartidor.ui.components.LoadingStateBox

/** S09 · pantalla del recurso notificaciones (listar, marcar leídas, descartar). */
@Composable
fun NotificationsScreen(
    vm: NotificationsViewModel,
    modifier: Modifier = Modifier
) {
    val items by vm.items.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.notifications_title),
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = { vm.markAllRead() }) {
                Text(stringResource(R.string.notifications_mark_read))
            }
        }

        when {
            loading is LoadingState.Loading && items.isEmpty() -> LoadingStateBox()
            error !is ErrorState.None && items.isEmpty() ->
                ErrorStateBox(error, onRetry = { vm.retry() })
            items.isEmpty() -> EmptyStateBox(stringResource(R.string.notifications_empty))
            else -> LazyColumn {
                items(items, key = { it.id }) { item ->
                    NotificationRow(item, onDismiss = { vm.dismiss(item) })
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: Notificacion, onDismiss: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.read) FontWeight.Normal else FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(0.dp))
                    if (!item.read) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                stringResource(R.string.notifications_unread_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.createdAt?.take(16)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notifications_dismiss))
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline)
    }
}
