package com.driverapp.repartidor.di

import android.content.Context
import com.driverapp.repartidor.BuildConfig
import com.driverapp.repartidor.data.firebase.FirebaseAuthDataSource
import com.driverapp.repartidor.data.firebase.FirebaseMessagingDataSource
import com.driverapp.repartidor.data.firebase.FirestoreDriverDataSource
import com.driverapp.repartidor.data.firebase.StorageProofDataSource
import com.driverapp.repartidor.data.hardware.gps.LocationRepositoryImpl
import com.driverapp.repartidor.data.local.TokenDataStore
import com.driverapp.repartidor.data.local.UserPreferences
import com.driverapp.repartidor.data.local.db.AppDatabase
import com.driverapp.repartidor.data.local.db.LocalDataSource
import com.driverapp.repartidor.data.local.queue.SyncQueue
import com.driverapp.repartidor.data.network.NetworkMonitor
import com.driverapp.repartidor.data.remote.AuthApiService
import com.driverapp.repartidor.data.remote.PedidoApiService
import com.driverapp.repartidor.data.remote.RetrofitClient
import com.driverapp.repartidor.data.repository.AuthRepositoryImpl
import com.driverapp.repartidor.data.repository.EarningsRepositoryImpl
import com.driverapp.repartidor.data.repository.NotificationRepositoryImpl
import com.driverapp.repartidor.data.repository.PedidoRepositoryImpl
import com.driverapp.repartidor.data.repository.ProofRepositoryImpl
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.repository.EarningsRepository
import com.driverapp.repartidor.domain.repository.LocationRepository
import com.driverapp.repartidor.domain.repository.NotificationRepository
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.domain.repository.ProofRepository
import com.driverapp.repartidor.domain.usecase.AceptarPedidoUseCase
import com.driverapp.repartidor.domain.usecase.ActualizarFcmTokenUseCase
import com.driverapp.repartidor.domain.usecase.ActualizarPerfilUseCase
import com.driverapp.repartidor.domain.usecase.ActualizarVehiculoUseCase
import com.driverapp.repartidor.domain.usecase.CambiarEstadoEnLineaUseCase
import com.driverapp.repartidor.domain.usecase.LoginUseCase
import com.driverapp.repartidor.domain.usecase.MarcarEnCaminoUseCase
import com.driverapp.repartidor.domain.usecase.MarcarPedidoEntregadoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerDetallePedidoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerMisPedidosUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerPedidosDisponiblesUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerPerfilUseCase
import com.driverapp.repartidor.domain.usecase.RechazarPedidoUseCase
import com.driverapp.repartidor.domain.usecase.ReportarUbicacionUseCase
import com.driverapp.repartidor.ui.common.UiMessenger
import com.driverapp.repartidor.ui.earnings.EarningsViewModel
import com.driverapp.repartidor.ui.login.LoginViewModel
import com.driverapp.repartidor.ui.main.MainViewModel
import com.driverapp.repartidor.ui.notifications.NotificationsViewModel
import com.driverapp.repartidor.ui.orders.OrdersViewModel
import com.driverapp.repartidor.ui.profile.ProfileViewModel
import com.driverapp.repartidor.ui.tracking.TrackingViewModel

/**
 * Inyección de dependencias manual (sin Hilt). Único punto donde se construye
 * el grafo: base de datos, red, repositorios, casos de uso y ViewModels.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val messenger = UiMessenger()

    // ---------- almacenamiento local (Sprint 6) ----------
    val db: AppDatabase by lazy { AppDatabase.get(appContext) }
    val local: LocalDataSource by lazy { LocalDataSource(db) }
    val tokenStore: TokenDataStore by lazy { TokenDataStore(appContext) }
    val userPreferences: UserPreferences by lazy { UserPreferences(appContext) }
    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(appContext) }
    val syncQueue: SyncQueue by lazy { SyncQueue(appContext, local) }

    // ---------- red (S08) ----------
    private val retrofit by lazy {
        RetrofitClient.build(
            baseUrl = BuildConfig.API_BASE_URL,
            tokenProvider = { tokenStore.token() }
        )
    }

    val authApi: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
    val pedidoApi: PedidoApiService by lazy { retrofit.create(PedidoApiService::class.java) }

    val firebaseAuth: FirebaseAuthDataSource by lazy { FirebaseAuthDataSource() }
    val firebaseMessaging: FirebaseMessagingDataSource by lazy { FirebaseMessagingDataSource() }
    val firestore: FirestoreDriverDataSource by lazy { FirestoreDriverDataSource() }
    val storage: StorageProofDataSource by lazy { StorageProofDataSource() }

    // ---------- repositorios ----------
    val locationRepository: LocationRepository by lazy { LocationRepositoryImpl(appContext, authApi) }
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(authApi, firebaseAuth, tokenStore) }
    val pedidoRepository: PedidoRepository by lazy { PedidoRepositoryImpl(pedidoApi, local, syncQueue) }
    val earningsRepository: EarningsRepository by lazy { EarningsRepositoryImpl(pedidoApi, local) }
    val notificationRepository: NotificationRepository by lazy { NotificationRepositoryImpl(pedidoApi) }
    val proofRepository: ProofRepository by lazy {
        ProofRepositoryImpl(storage, firestore) { tokenStore.user()?.email }
    }

    // ---------- casos de uso: sesión y perfil ----------
    val loginUseCase by lazy { LoginUseCase(authRepository) }
    val obtenerPerfil by lazy { ObtenerPerfilUseCase(authRepository) }
    val actualizarPerfil by lazy { ActualizarPerfilUseCase(authRepository) }
    val actualizarVehiculo by lazy { ActualizarVehiculoUseCase(authRepository) }
    val cambiarEstado by lazy { CambiarEstadoEnLineaUseCase(authRepository) }
    val actualizarFcmToken by lazy { ActualizarFcmTokenUseCase(authRepository) }

    // ---------- casos de uso: pedidos ----------
    val pedidosDisponibles by lazy { ObtenerPedidosDisponiblesUseCase(pedidoRepository) }
    val misPedidos by lazy { ObtenerMisPedidosUseCase(pedidoRepository) }
    val historialPedidos by lazy { ObtenerHistorialUseCase(pedidoRepository) }
    val detallePedido by lazy { ObtenerDetallePedidoUseCase(pedidoRepository) }
    val aceptarPedido by lazy { AceptarPedidoUseCase(pedidoRepository) }
    val rechazarPedido by lazy { RechazarPedidoUseCase(pedidoRepository) }
    val marcarEnCamino by lazy { MarcarEnCaminoUseCase(pedidoRepository) }
    val marcarEntregado by lazy { MarcarPedidoEntregadoUseCase(pedidoRepository) }

    // ---------- casos de uso: ganancias y ubicación ----------
    val obtenerGanancias by lazy { ObtenerGananciasUseCase(earningsRepository) }
    val historialGanancias by lazy { ObtenerHistorialGananciasUseCase(earningsRepository) }
    val reportarUbicacion by lazy { ReportarUbicacionUseCase(locationRepository) }

    // ---------- ViewModel factories ----------
    fun loginViewModelFactory() = ViewModelFactory {
        LoginViewModel(loginUseCase, authRepository, messenger)
    }

    fun mainViewModelFactory() = ViewModelFactory {
        MainViewModel(authRepository, cambiarEstado, actualizarFcmToken, reportarUbicacion, networkMonitor, messenger)
    }

    fun ordersViewModelFactory() = ViewModelFactory {
        OrdersViewModel(
            pedidoRepository, pedidosDisponibles, historialPedidos,
            aceptarPedido, rechazarPedido, detallePedido, messenger
        )
    }

    fun trackingViewModelFactory() = ViewModelFactory {
        TrackingViewModel(
            pedidoRepository, misPedidos, historialPedidos,
            marcarEnCamino, marcarEntregado, messenger, proofRepository
        )
    }

    fun earningsViewModelFactory() = ViewModelFactory {
        EarningsViewModel(obtenerGanancias, historialGanancias, messenger, userPreferences)
    }

    fun profileViewModelFactory() = ViewModelFactory {
        ProfileViewModel(
            userPreferences, authRepository, obtenerPerfil, actualizarPerfil, actualizarVehiculo,
            obtenerGanancias, messenger
        )
    }

    fun notificationsViewModelFactory() = ViewModelFactory {
        NotificationsViewModel(notificationRepository, messenger)
    }
}
