package com.driverapp.repartidor.ui.common

sealed interface LoadingState {
    data object Idle : LoadingState
    data object Loading : LoadingState
}

sealed interface ErrorState {
    data object None : ErrorState
    data class Message(val text: String) : ErrorState
    data class SinConexion(val text: String = "Sin conexión. Reintenta") : ErrorState
}

fun Throwable.toErrorState(): ErrorState {
    val offline = this is java.io.IOException ||
        this is java.net.UnknownHostException ||
        this is java.net.SocketTimeoutException ||
        this is java.net.ConnectException
    return if (offline) ErrorState.SinConexion() else ErrorState.Message(message ?: "Error inesperado")
}
