package com.driverapp.repartidor.data.remote

import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Errores de red tipados (S08 / S09).
 * Traducen HTTP + excepciones de plataforma a un vocabulario que la UI entiende,
 * en lugar de mostrar el `e.message` crudo de Retrofit.
 */
sealed class ApiFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** Sin red, DNS caído o timeout. Único caso que se encola para reintento. */
    class NoConnection(cause: Throwable? = null) :
        ApiFailure("Sin conexión. Reintenta cuando vuelvas a estar en línea.", cause)

    /** 401 / 403 — token inválido o sin permiso. */
    class Unauthorized(val code: Int, detail: String?) :
        ApiFailure(detail ?: "Sesión no válida. Vuelve a iniciar sesión.")

    /** 404 — recurso inexistente. */
    class NotFound(detail: String?) :
        ApiFailure(detail ?: "El pedido ya no está disponible.")

    /** 422 — validación del backend. */
    class Validation(val detail: String?) :
        ApiFailure(detail ?: "Datos rechazados por el servidor.")

    /** 5xx — el servidor falló. */
    class Server(val code: Int) :
        ApiFailure("El servidor falló ($code). Reintenta en unos minutos.")

    class Unknown(cause: Throwable?) :
        ApiFailure(cause?.message ?: "Error inesperado.", cause)
}

object ApiErrorMapper {

    fun map(throwable: Throwable): ApiFailure = when (throwable) {
        is ApiFailure -> throwable
        is HttpException -> fromCode(throwable.code(), throwable.response()?.errorBody()?.string())
        is UnknownHostException, is ConnectException, is SocketTimeoutException, is IOException ->
            ApiFailure.NoConnection(throwable)
        else -> ApiFailure.Unknown(throwable)
    }

    fun fromCode(code: Int, body: String?): ApiFailure {
        val detail = body?.takeIf { it.isNotBlank() }?.let { extractDetail(it) }
        return when {
            code == 401 || code == 403 -> ApiFailure.Unauthorized(code, detail)
            code == 404 -> ApiFailure.NotFound(detail)
            code == 422 -> ApiFailure.Validation(detail)
            code in 500..599 -> ApiFailure.Server(code)
            else -> ApiFailure.Unknown(RuntimeException("HTTP $code: ${detail ?: ""}"))
        }
    }

    /** FastAPI devuelve {"detail": "..."} o {"detail": [{"msg": ...}]}. */
    private fun extractDetail(body: String): String? {
        val quoted = Regex("\"(?:detail|msg)\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
        return quoted ?: body.take(160)
    }
}

fun Throwable.toApiFailure(): ApiFailure = ApiErrorMapper.map(this)
