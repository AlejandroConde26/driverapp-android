package com.driverapp.repartidor.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.repartidor.R
import com.driverapp.repartidor.data.local.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.driverapp.repartidor.ui.components.BusyOverlay
import com.driverapp.repartidor.ui.components.ConfirmDialog
import com.driverapp.repartidor.ui.components.ToggleRow
import java.util.Locale

/**
 * Pestaña Perfil (HU-02 + HU-06): datos, estadísticas reales y las 5 preferencias
 * sobre DataStore. El cambio de vehículo sólo ofrece los valores que acepta el
 * backend (`Bicicleta | Moto | Auto`).
 */
@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    onLogout: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    onLocationChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val user by vm.user.collectAsStateWithLifecycle()
    val prefs by vm.prefsState.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()

    var editDialog by remember { mutableStateOf(false) }
    var vehicleDialog by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }
    var logoutDialog by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Aviso de carga al cambiar una preferencia: el cambio de tema o de idioma era
    // instantáneo y se sentía brusco. Se muestra el overlay y se aplica después.
    var applying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(applying) {
        if (applying) {
            delay(450)
            applying = false
        }
    }

    LaunchedEffect(Unit) { vm.refreshAll() }

    Box(modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- Cabecera del perfil ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(56.dp)
            ) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (user?.name ?: "?").take(2).uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(Modifier.height(0.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    user?.name ?: stringResource(R.string.my_profile),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    user?.vehicle ?: stringResource(R.string.vehicle_bicycle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Estadísticas reales (salían de /earnings/summary?period=year) ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatColumn(
                    label = stringResource(R.string.stats_orders),
                    value = if (stats.loaded) "${stats.orders}" else stringResource(R.string.stats_empty),
                    modifier = Modifier.weight(1f)
                )
                StatColumn(
                    label = stringResource(R.string.stats_tips),
                    value = if (stats.loaded) {
                        "$" + String.format(Locale.ROOT, "%.2f", stats.tips)
                    } else {
                        stringResource(R.string.stats_empty)
                    },
                    modifier = Modifier.weight(1f)
                )
                StatColumn(
                    label = stringResource(R.string.stats_total),
                    value = if (stats.loaded) {
                        "$" + String.format(Locale.ROOT, "%.2f", stats.total)
                    } else {
                        stringResource(R.string.stats_empty)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.account_settings), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        ActionRow(stringResource(R.string.profile_edit)) { editDialog = true }
        ActionRow(stringResource(R.string.profile_vehicle)) { vehicleDialog = true }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.prefs_title), style = MaterialTheme.typography.titleMedium)

        ToggleRow(
            title = stringResource(R.string.pref_dark_mode),
            checked = prefs.darkMode,
            onCheckedChange = { applying = true; vm.setDarkMode(it) }
        )
        ToggleRow(
            title = stringResource(R.string.pref_notifications),
            checked = prefs.notifications,
            onCheckedChange = { applying = true; vm.setNotificationsEnabled(it) }
        )
        ToggleRow(
            title = stringResource(R.string.pref_location),
            checked = prefs.location,
            onCheckedChange = {
                applying = true
                vm.setLocationEnabled(it)
                onLocationChanged(it)
            }
        )
        ToggleRow(
            title = stringResource(R.string.pref_sound),
            checked = prefs.sound,
            onCheckedChange = { applying = true; vm.setNotificationSound(it) }
        )
        ActionRow(
            label = stringResource(R.string.pref_language),
            value = if (prefs.language == UserPreferences.LANG_EN) {
                stringResource(R.string.language_en)
            } else {
                stringResource(R.string.language_es)
            }
        ) { languageDialog = true }

        Spacer(Modifier.height(16.dp))
        val txtPayments = stringResource(R.string.profile_payments)
        val txtPaymentsBody = stringResource(R.string.profile_payments_body)
        val txtHelp = stringResource(R.string.profile_help)
        val txtHelpBody = stringResource(R.string.profile_help_body)
        val txtContact = stringResource(R.string.profile_contact)
        val txtContactBody = stringResource(R.string.profile_contact_body)
        val txtTerms = stringResource(R.string.profile_terms)
        val txtTermsBody = stringResource(R.string.profile_terms_body)

        ActionRow(txtPayments) { infoDialog = txtPayments to txtPaymentsBody }
        ActionRow(txtHelp) { infoDialog = txtHelp to txtHelpBody }
        ActionRow(txtContact) { infoDialog = txtContact to txtContactBody }
        ActionRow(txtTerms) { infoDialog = txtTerms to txtTermsBody }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { logoutDialog = true }) {
            Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(24.dp))
    }

        BusyOverlay(applying, label = stringResource(R.string.pref_applying))
    }

    // --- Diálogos ---
    if (editDialog) {
        EditProfileDialog(
            initialName = user?.name.orEmpty(),
            initialPhone = user?.phone.orEmpty(),
            onDismiss = { editDialog = false },
            onSave = { name, phone ->
                vm.updateProfile(name, phone)
                editDialog = false
            }
        )
    }

    if (vehicleDialog) {
        AlertDialog(
            onDismissRequest = { vehicleDialog = false },
            title = { Text(stringResource(R.string.profile_vehicle)) },
            text = {
                Column {
                    ProfileViewModel.VEHICLES.forEach { vehicle ->
                        ActionRow(
                            label = vehicle,
                            selected = vehicle.equals(user?.vehicle, true)
                        ) {
                            vm.updateVehicle(vehicle)
                            vehicleDialog = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vehicleDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (languageDialog) {
        AlertDialog(
            onDismissRequest = { languageDialog = false },
            title = { Text(stringResource(R.string.pref_language)) },
            text = {
                Column {
                    ActionRow(label = stringResource(R.string.language_es)) {
                        vm.setLanguage(UserPreferences.LANG_ES)
                        languageDialog = false
                        applying = true
                        // El idioma se aplica tras ver el aviso: antes se recreaba la
                        // pantalla de golpe y no había realimentación.
                        scope.launch {
                            delay(400)
                            onLanguageChanged(UserPreferences.LANG_ES)
                        }
                    }
                    ActionRow(label = stringResource(R.string.language_en)) {
                        vm.setLanguage(UserPreferences.LANG_EN)
                        languageDialog = false
                        applying = true
                        scope.launch {
                            delay(400)
                            onLanguageChanged(UserPreferences.LANG_EN)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { languageDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (logoutDialog) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_logout_title),
            message = stringResource(R.string.confirm_logout_body),
            confirmText = stringResource(R.string.action_logout),
            onConfirm = {
                vm.logout()
                logoutDialog = false
                onLogout()
            },
            onDismiss = { logoutDialog = false }
        )
    }

    infoDialog?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { infoDialog = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { infoDialog = null }) {
                    Text(stringResource(R.string.detail_close))
                }
            }
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionRow(
    label: String,
    value: String = "",
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                // Icono, no carácter: la app solo usa iconos.
                Icon(
                    painter = painterResource(R.drawable.ic_fa_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_fa_chevron_right),
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialPhone: String,
    onDismiss: () -> Unit,
    onSave: (String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    isError = error == R.string.error_name_short,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; error = null },
                    label = { Text(stringResource(R.string.field_phone)) },
                    singleLine = true,
                    isError = error == R.string.error_phone_invalid,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val nameError = if (name.trim().length < 2) R.string.error_name_short else null
                val digits = phone.filter { it.isDigit() }
                val phoneError = if (digits.length !in 7..15) R.string.error_phone_invalid else null
                error = nameError ?: phoneError
                if (error == null) onSave(name.trim(), digits)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
