package com.driverapp.repartidor.ui.common

import com.driverapp.repartidor.data.remote.ApiFailure
import com.driverapp.repartidor.data.remote.toApiFailure

sealed interface LoadingState {
    data object Idle : LoadingState
    data object Loading : LoadingState
}

/**
 * Estado de error que consume la UI (S09).
 * Cada caso HTTP tiene su propio texto y su propia acción sugerida; ya no se
 * muestra el `message` crudo de Retrofit.
 */
sealed interface ErrorState {

    data object None : ErrorState

    data class SinConexion(val text: String = "Sin conexión. Se muestran los datos guardados.") : ErrorState

    data class NoAutorizado(val text: String = "Tu sesión expiró. Vuelve a iniciar sesión.") : ErrorState

    data class NoEncontrado(val text: String = "El pedido ya no está disponible.") : ErrorState

    data class Validacion(val text: String = "El servidor rechazó los datos.") : ErrorState

    data class Servidor(val text: String = "El servidor falló. Reintenta en unos minutos.") : ErrorState

    data class Message(val text: String) : ErrorState

    val message: String
        get() = when (this) {
            None -> ""
            is SinConexion -> text
            is NoAutorizado -> text
            is NoEncontrado -> text
            is Validacion -> text
            is Servidor -> text
            is Message -> text
        }
}

fun Throwable.toErrorState(): ErrorState = when (val failure = toApiFailure()) {
    is ApiFailure.NoConnection -> ErrorState.SinConexion()
    is ApiFailure.Unauthorized -> ErrorState.NoAutorizado(failure.message ?: "Sesión no válida")
    is ApiFailure.NotFound -> ErrorState.NoEncontrado(failure.message ?: "No encontrado")
    is ApiFailure.Validation -> ErrorState.Validacion(failure.message ?: "Datos rechazados")
    is ApiFailure.Server -> ErrorState.Servidor(failure.message ?: "Error del servidor")
    is ApiFailure.Unknown -> ErrorState.Message(failure.message ?: "Error inesperado")
}
