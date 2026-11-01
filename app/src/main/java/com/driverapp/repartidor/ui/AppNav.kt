package com.driverapp.repartidor.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.driverapp.repartidor.App
import com.driverapp.repartidor.R
import com.driverapp.repartidor.data.hardware.gps.LocationForegroundService
import com.driverapp.repartidor.di.AppContainer
import com.driverapp.repartidor.ui.login.LoginScreen
import com.driverapp.repartidor.ui.login.LoginViewModel
import com.driverapp.repartidor.ui.main.MainScaffold
import com.driverapp.repartidor.ui.main.MainViewModel
import com.driverapp.repartidor.ui.main.Routes
import com.driverapp.repartidor.ui.notifications.NotificationsScreen
import com.driverapp.repartidor.ui.notifications.NotificationsViewModel
import com.driverapp.repartidor.ui.orders.OrdersScreen
import com.driverapp.repartidor.ui.orders.OrdersViewModel
import com.driverapp.repartidor.ui.profile.ProfileScreen
import com.driverapp.repartidor.ui.profile.ProfileViewModel
import com.driverapp.repartidor.ui.theme.DriverAppTheme
import com.driverapp.repartidor.ui.tracking.TrackingScreen
import com.driverapp.repartidor.ui.tracking.TrackingViewModel
import kotlinx.coroutines.launch

/**
 * Grafo de navegación declarativo (Sprint 5): `login` → `main`, con las 4
 * pestañas como destinos y `notifications` como destino auxiliar del header.
 */
@Composable
fun AppNav(container: AppContainer) {
    val context = LocalContext.current
    val nav = rememberNavController()

    val mainVm: MainViewModel = viewModel(factory = container.mainViewModelFactory())
    val loginVm: LoginViewModel = viewModel(factory = container.loginViewModelFactory())

    val online by mainVm.online.collectAsStateWithLifecycle()
    val connectivity by mainVm.connectivity.collectAsStateWithLifecycle()
    val user by mainVm.user.collectAsStateWithLifecycle()

    val pending by container.pedidoRepository.pendingActions
        .collectAsStateWithLifecycle(initialValue = 0)

    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main) }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            LocationForegroundService.start(context)
            container.messenger.show(R.string.msg_location_on)
        } else {
            container.messenger.show(R.string.msg_location_denied)
            scope.launch { container.userPreferences.setLocationEnabled(false) }
        }
    }

    val start = remember { if (loginVm.isLoggedIn()) Routes.ORDERS else Routes.LOGIN }

    // El tema sigue la PREFERENCIA del usuario, no el modo del sistema. Con
    // ComponentActivity, `AppCompatDelegate.setDefaultNightMode` no se aplica, así
    // que desactivar el modo oscuro no cambiaba nada: la UI seguía en oscuro porque
    // leía `isSystemInDarkTheme()`. Hasta la primera emisión se usa el sistema.
    val darkPref by container.userPreferences.darkMode
        .collectAsStateWithLifecycle(initialValue = isSystemInDarkTheme())

    DriverAppTheme(darkTheme = darkPref) {
        NavHost(navController = nav, startDestination = start) {

            composable(Routes.LOGIN) {
                LoginScreen(
                    vm = loginVm,
                    onLoggedIn = {
                        nav.navigate(Routes.ORDERS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ORDERS) {
                val vm: OrdersViewModel = viewModel(factory = container.ordersViewModelFactory())
                val pendingSync by vm.pendingSync.collectAsStateWithLifecycle()
                MainScaffold(
                    title = context.getString(R.string.hello_user, firstName(user?.name)),
                    subtitle = context.getString(R.string.subtitle_orders),
                    online = online,
                    connectivity = connectivity,
                    pendingSync = pendingSync,
                    currentRoute = Routes.ORDERS,
                    messenger = container.messenger,
                    onToggleStatus = { mainVm.toggleStatus() },
                    onTab = { nav.navigateTo(it) },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) }
                ) {
                    OrdersScreen(vm = vm, onGoToMap = { nav.navigateTo(Routes.MAP) })
                }
            }

            composable(Routes.MAP) {
                val vm: TrackingViewModel = viewModel(factory = container.trackingViewModelFactory())
                MainScaffold(
                    title = context.getString(R.string.tab_map),
                    subtitle = context.getString(R.string.subtitle_map),
                    online = online,
                    connectivity = connectivity,
                    pendingSync = 0,
                    currentRoute = Routes.MAP,
                    messenger = container.messenger,
                    onToggleStatus = { mainVm.toggleStatus() },
                    onTab = { nav.navigateTo(it) },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) }
                ) {
                    TrackingScreen(
                        vm = vm,
                        locationRepo = container.locationRepository,
                        vehicle = user?.vehicle
                    )
                }
            }

            composable(Routes.EARNINGS) {
                val vm: com.driverapp.repartidor.ui.earnings.EarningsViewModel =
                    viewModel(factory = container.earningsViewModelFactory())
                MainScaffold(
                    title = context.getString(R.string.my_earnings),
                    subtitle = context.getString(R.string.subtitle_earnings),
                    online = online,
                    connectivity = connectivity,
                    pendingSync = 0,
                    currentRoute = Routes.EARNINGS,
                    messenger = container.messenger,
                    onToggleStatus = { mainVm.toggleStatus() },
                    onTab = { nav.navigateTo(it) },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) }
                ) {
                    com.driverapp.repartidor.ui.earnings.EarningsScreen(vm = vm)
                }
            }

            composable(Routes.PROFILE) {
                val vm: ProfileViewModel = viewModel(factory = container.profileViewModelFactory())
                MainScaffold(
                    title = context.getString(R.string.my_profile),
                    subtitle = context.getString(R.string.subtitle_profile),
                    online = online,
                    connectivity = connectivity,
                    pendingSync = pending,
                    currentRoute = Routes.PROFILE,
                    messenger = container.messenger,
                    onToggleStatus = { mainVm.toggleStatus() },
                    onTab = { nav.navigateTo(it) },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) }
                ) {
                    ProfileScreen(
                        vm = vm,
                        onLogout = {
                            LocationForegroundService.stop(context)
                            mainVm.logout()
                            // `popUpTo(0)` desmontaba el grafo entero mientras el
                            // NavHost seguía componiendo (crash al cerrar sesión).
                            nav.navigate(Routes.LOGIN) {
                                popUpTo(nav.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onLanguageChanged = {
                            (context as? android.app.Activity)?.recreate()
                        },
                        onLocationChanged = { enabled ->
                            if (enabled) {
                                if (container.locationRepository.hasPermission()) {
                                    LocationForegroundService.start(context)
                                } else {
                                    locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            } else {
                                LocationForegroundService.stop(context)
                            }
                        }
                    )
                }
            }

            composable(Routes.NOTIFICATIONS) {
                val vm: NotificationsViewModel =
                    viewModel(factory = container.notificationsViewModelFactory())
                MainScaffold(
                    title = context.getString(R.string.notifications_title),
                    subtitle = context.getString(R.string.subtitle_notifications),
                    online = online,
                    connectivity = connectivity,
                    pendingSync = pending,
                    currentRoute = Routes.NOTIFICATIONS,
                    messenger = container.messenger,
                    onToggleStatus = { mainVm.toggleStatus() },
                    onTab = { nav.navigateTo(it) },
                    onNotifications = { }
                ) {
                    NotificationsScreen(vm = vm)
                }
            }
        }
    }

    // Token FCM y permisos se piden una sola vez al entrar a la app.
    LaunchedEffect(Unit) {
        ensureFcmToken(context, container)
        requestNotificationPermission(context)
    }
}

private fun firstName(name: String?): String =
    name?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Repartidor"

/** Navegación entre pestañas sin apilar: una sola entrada de cada tab. */
private fun androidx.navigation.NavHostController.navigateTo(route: String) {
    navigate(route) {
        popUpTo(Routes.ORDERS) { inclusive = false }
        launchSingleTop = true
    }
}

private fun ensureFcmToken(context: Context, container: AppContainer) {
    if (!com.driverapp.repartidor.data.firebase.FirebaseConfig.isConfigured()) return
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    scope.launch {
        val token = container.firebaseMessaging.fetchToken() ?: return@launch
        com.driverapp.repartidor.data.firebase.FcmTokenHolder.token = token
        runCatching { container.authRepository.updateFcmToken(token) }
    }
}

private fun requestNotificationPermission(context: Context) {
    if (android.os.Build.VERSION.SDK_INT < 33) return
    val activity = context as? android.app.Activity ?: return
    if (androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1001
        )
    }
}
