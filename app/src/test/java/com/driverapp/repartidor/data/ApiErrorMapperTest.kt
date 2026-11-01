package com.driverapp.repartidor.data

import com.driverapp.repartidor.data.remote.ApiErrorMapper
import com.driverapp.repartidor.data.remote.ApiFailure
import com.driverapp.repartidor.data.remote.toApiFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/** S09 · mapeo de errores HTTP a un vocabulario que la UI puede mostrar. */
class ApiErrorMapperTest {

    @Test
    fun `401 y 403 se mapean a Unauthorized`() {
        assertTrue(ApiErrorMapper.fromCode(401, null) is ApiFailure.Unauthorized)
        assertTrue(ApiErrorMapper.fromCode(403, "no permitido") is ApiFailure.Unauthorized)
    }

    @Test
    fun `404 se mapea a NotFound con el detalle del backend`() {
        val failure = ApiErrorMapper.fromCode(404, "{\"detail\":\"Order not found\"}")
        assertTrue(failure is ApiFailure.NotFound)
        assertEquals("Order not found", failure.message)
    }

    @Test
    fun `422 se mapea a Validation`() {
        val failure = ApiErrorMapper.fromCode(422, "{\"detail\":[{\"msg\":\"invalid vehicle\"}]}")
        assertTrue(failure is ApiFailure.Validation)
        assertEquals("invalid vehicle", failure.message)
    }

    @Test
    fun `5xx se mapea a Server`() {
        val failure = ApiErrorMapper.fromCode(502, null)
        assertTrue(failure is ApiFailure.Server)
        assertEquals(502, (failure as ApiFailure.Server).code)
    }

    @Test
    fun `timeout y errores de IO se mapean a NoConnection`() {
        assertTrue(SocketTimeoutException().toApiFailure() is ApiFailure.NoConnection)
        assertTrue(IOException().toApiFailure() is ApiFailure.NoConnection)
    }

    @Test
    fun `un ApiFailure no se vuelve a envolver`() {
        val original = ApiFailure.NoConnection()
        assertEquals(original, ApiErrorMapper.map(original))
    }
}
