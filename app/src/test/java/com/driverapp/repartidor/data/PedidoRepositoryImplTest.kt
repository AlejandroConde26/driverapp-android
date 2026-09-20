package com.driverapp.repartidor.data

import com.driverapp.repartidor.data.remote.PedidoApiService
import com.driverapp.repartidor.data.repository.PedidoRepositoryImpl
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.samplePedidoDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PedidoRepositoryImplTest {

    private val api: PedidoApiService = mockk()
    private val repo = PedidoRepositoryImpl(api)

    @Test
    fun `refreshAvailable mapea DTO a dominio`() = runTest {
        coEvery { api.availableOrders() } returns listOf(samplePedidoDto())

        repo.refreshAvailable()

        val pedidos = repo.available.value
        assertEquals(1, pedidos.size)
        assertEquals(10, pedidos[0].id)
        assertEquals(EstadoPedido.PENDIENTE, pedidos[0].estado)
        assertEquals("Ana López", pedidos[0].clientName)
    }

    @Test
    fun `accept publica el pedido activo`() = runTest {
        coEvery { api.acceptOrder(10L) } returns samplePedidoDto().copy(status = "ACEPTADO")

        val result = repo.accept(10L)

        assertEquals(EstadoPedido.ACEPTADO, result.estado)
        assertEquals(10, repo.activeOrder.value?.id)
    }

    @Test
    fun `complete limpia el pedido activo`() = runTest {
        coEvery { api.acceptOrder(10L) } returns samplePedidoDto().copy(status = "ACEPTADO")
        coEvery { api.completeOrder(10L) } returns samplePedidoDto().copy(status = "ENTREGADO")
        repo.accept(10L)

        repo.complete(10L)

        assertNull(repo.activeOrder.value)
    }
}
