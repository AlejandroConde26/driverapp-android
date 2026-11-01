package com.driverapp.repartidor.data

import com.driverapp.repartidor.data.local.db.Buckets
import com.driverapp.repartidor.data.local.db.LocalDataSource
import com.driverapp.repartidor.data.local.queue.PendingType
import com.driverapp.repartidor.data.local.queue.SyncQueue
import com.driverapp.repartidor.data.remote.ApiFailure
import com.driverapp.repartidor.data.remote.PedidoApiService
import com.driverapp.repartidor.data.repository.PedidoRepositoryImpl
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.samplePedido
import com.driverapp.repartidor.samplePedidoDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Sprint 6 · patrón Repository cache-first:
 *  - la red refresca, la lectura sale de la caché;
 *  - sin red, la transición se encola y se aplica de forma optimista.
 */
class PedidoRepositoryImplTest {

    private val api: PedidoApiService = mockk()
    private val local: LocalDataSource = mockk(relaxed = true)
    private val queue: SyncQueue = mockk(relaxed = true)

    private fun repo() = PedidoRepositoryImpl(api, local, queue, restaurantId = 5)

    @Test
    fun `refreshAvailable escribe el resultado en la cache AVAILABLE`() = runTest {
        coEvery { api.availableOrders(5) } returns listOf(samplePedidoDto())
        val bucket = slot<String>()
        val pedidos = slot<List<com.driverapp.repartidor.domain.model.Pedido>>()
        coEvery { local.replaceBucket(capture(bucket), capture(pedidos)) } returns Unit

        repo().refreshAvailable()

        assertEquals(Buckets.AVAILABLE, bucket.captured)
        assertEquals(1, pedidos.captured.size)
        assertEquals(10, pedidos.captured[0].id)
        assertEquals(EstadoPedido.PENDIENTE, pedidos.captured[0].estado)
        assertEquals("Ana López", pedidos.captured[0].clientName)
        assertEquals(1, pedidos.captured[0].items.size)
        assertEquals("Ceviche Clásico", pedidos.captured[0].items[0].productName)
    }

    @Test
    fun `refreshActive con lista vacia limpia la cache ACTIVE`() = runTest {
        coEvery { api.myOrders(5) } returns emptyList()
        val bucket = slot<String>()
        coEvery { local.replaceBucket(capture(bucket), any()) } returns Unit

        repo().refreshActive()

        assertEquals(Buckets.ACTIVE, bucket.captured)
        coVerify { local.replaceBucket(Buckets.ACTIVE, emptyList()) }
    }

    @Test
    fun `accept con red guarda el pedido en ACTIVE`() = runTest {
        coEvery { api.acceptOrder(10L) } returns samplePedidoDto().copy(status = "ACEPTADO")
        val bucket = slot<String>()
        coEvery { local.save(any(), capture(bucket)) } returns Unit

        val result = repo().accept(10L)

        assertEquals(EstadoPedido.ACEPTADO, result.estado)
        assertEquals(Buckets.ACTIVE, bucket.captured)
    }

    @Test
    fun `markInTransit llama a in-transit y deja el pedido EN_CAMINO`() = runTest {
        coEvery { api.inTransit(10L) } returns samplePedidoDto().copy(status = "EN_CAMINO")
        coEvery { local.save(any(), any()) } returns Unit

        val result = repo().markInTransit(10L)

        assertEquals(EstadoPedido.EN_CAMINO, result.estado)
        coVerify { api.inTransit(10L) }
    }

    @Test
    fun `complete mueve el pedido a HISTORY`() = runTest {
        coEvery { api.completeOrder(10L) } returns samplePedidoDto().copy(status = "ENTREGADO")
        val bucket = slot<String>()
        coEvery { local.save(any(), capture(bucket)) } returns Unit

        val result = repo().complete(10L)

        assertEquals(EstadoPedido.ENTREGADO, result.estado)
        assertEquals(Buckets.HISTORY, bucket.captured)
    }

    @Test
    fun `sin red la transicion se encola y se aplica de forma optimista`() = runTest {
        coEvery { api.inTransit(10L) } throws ApiFailure.NoConnection()
        coEvery { local.get(10) } returns samplePedido()
        coEvery { queue.enqueue(10, PendingType.IN_TRANSIT) } returns Unit
        coEvery { local.save(any(), any()) } returns Unit

        val result = repo().markInTransit(10L)

        assertEquals(EstadoPedido.EN_CAMINO, result.estado)
        coVerify { queue.enqueue(10, PendingType.IN_TRANSIT) }
        coVerify { local.save(any(), Buckets.ACTIVE) }
    }

    @Test(expected = ApiFailure.NotFound::class)
    fun `un 404 no se encola ni se aplica en local`() = runTest {
        coEvery { api.acceptOrder(10L) } throws ApiFailure.NotFound("no existe")

        repo().accept(10L)
    }

    @Test
    fun `detail sin red devuelve la copia cacheada`() = runTest {
        val cached = samplePedido()
        coEvery { api.orderDetail(10L) } throws ApiFailure.NoConnection()
        coEvery { local.get(10) } returns cached

        val result = repo().detail(10L)

        assertNotNull(result)
        assertEquals(cached.id, result.id)
    }
}
