package com.driverapp.repartidor.di

import android.content.Context
import com.driverapp.repartidor.BuildConfig
import com.driverapp.repartidor.data.firebase.FirebaseAuthDataSource
import com.driverapp.repartidor.data.firebase.FirebaseMessagingDataSource
import com.driverapp.repartidor.data.hardware.gps.LocationRepositoryImpl
import com.driverapp.repartidor.data.local.TokenDataStore
import com.driverapp.repartidor.data.local.UserPreferences
import com.driverapp.repartidor.data.remote.AuthApiService
import com.driverapp.repartidor.data.remote.PedidoApiService
import com.driverapp.repartidor.data.remote.RetrofitClient
import com.driverapp.repartidor.data.repository.AuthRepositoryImpl
import com.driverapp.repartidor.data.repository.PedidoRepositoryImpl
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.repository.LocationRepository
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.domain.usecase.ActualizarFcmTokenUseCase
import com.driverapp.repartidor.domain.usecase.ActualizarPerfilUseCase
import com.driverapp.repartidor.domain.usecase.ActualizarVehiculoUseCase
import com.driverapp.repartidor.domain.usecase.AceptarPedidoUseCase
import com.driverapp.repartidor.domain.usecase.CambiarEstadoEnLineaUseCase
import com.driverapp.repartidor.domain.usecase.LoginUseCase
import com.driverapp.repartidor.domain.usecase.MarcarPedidoEntregadoUseCase
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
import com.driverapp.repartidor.ui.orders.OrdersViewModel
import com.driverapp.repartidor.ui.profile.ProfileViewModel
import com.driverapp.repartidor.ui.tracking.TrackingViewModel

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val messenger = UiMessenger()

    val tokenStore: TokenDataStore by lazy { TokenDataStore(appContext) }
    val userPreferences: UserPreferences by lazy { UserPreferences(appContext) }

    private val retrofit by lazy {
        RetrofitClient.build(
            baseUrl = BuildConfig.API_BASE_URL,
            tokenProvider = { tokenStore.token() }
        )
    }

    val authApi: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
    val pedidoApi: PedidoApiService by lazy { retrofit.create(PedidoApiService::class.java) }

    val firebaseAuth = FirebaseAuthDataSource()
    val firebaseMessaging = FirebaseMessagingDataSource()

    val locationRepository: LocationRepository by lazy { LocationRepositoryImpl(appContext, authApi) }
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(authApi, firebaseAuth, tokenStore) }
    val pedidoRepository: PedidoRepository by lazy { PedidoRepositoryImpl(pedidoApi) }

    // Use cases
    val loginUseCase by lazy { LoginUseCase(authRepository) }
    val obtenerPerfil by lazy { ObtenerPerfilUseCase(authRepository) }
    val actualizarPerfil by lazy { ActualizarPerfilUseCase(authRepository) }
    val actualizarVehiculo by lazy { ActualizarVehiculoUseCase(authRepository) }
    val cambiarEstado by lazy { CambiarEstadoEnLineaUseCase(authRepository) }
    val actualizarFcmToken by lazy { ActualizarFcmTokenUseCase(authRepository) }

    val pedidosDisponibles by lazy { ObtenerPedidosDisponiblesUseCase(pedidoRepository) }
    val misPedidos by lazy { ObtenerMisPedidosUseCase(pedidoRepository) }
    val historialPedidos by lazy { ObtenerHistorialUseCase(pedidoRepository) }
    val aceptarPedido by lazy { AceptarPedidoUseCase(pedidoRepository) }
    val rechazarPedido by lazy { RechazarPedidoUseCase(pedidoRepository) }
    val marcarEntregado by lazy { MarcarPedidoEntregadoUseCase(pedidoRepository) }
    val obtenerGanancias by lazy { ObtenerGananciasUseCase(pedidoRepository) }
    val historialGanancias by lazy { ObtenerHistorialGananciasUseCase(pedidoRepository) }

    val reportarUbicacion by lazy { ReportarUbicacionUseCase(locationRepository) }

    // ViewModel factories
    fun loginViewModelFactory() = ViewModelFactory {
        LoginViewModel(loginUseCase, messenger)
    }

    fun mainViewModelFactory() = ViewModelFactory {
        MainViewModel(authRepository, pedidoRepository, cambiarEstado, actualizarFcmToken, reportarUbicacion, messenger)
    }

    fun ordersViewModelFactory() = ViewModelFactory {
        OrdersViewModel(pedidoRepository, pedidosDisponibles, historialPedidos, aceptarPedido, rechazarPedido, messenger)
    }

    fun trackingViewModelFactory() = ViewModelFactory {
        TrackingViewModel(pedidoRepository, misPedidos, historialPedidos, marcarEntregado, messenger)
    }

    fun earningsViewModelFactory() = ViewModelFactory {
        EarningsViewModel(obtenerGanancias, historialGanancias, messenger)
    }

    fun profileViewModelFactory() = ViewModelFactory {
        ProfileViewModel(
            userPreferences, authRepository, obtenerPerfil, actualizarPerfil, actualizarVehiculo,
            obtenerGanancias, historialGanancias, messenger
        )
    }
}
