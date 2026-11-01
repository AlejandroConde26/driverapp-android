package com.driverapp.repartidor.domain.usecase

import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.samplePedido
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MarcarPedidoEntregadoUseCaseTest {

    private val repo: PedidoRepository = mockk()
    private val useCase = MarcarPedidoEntregadoUseCase(repo)

    @Test
    fun `entregar delega al repositorio y devuelve el pedido`() = runTest {
        val entregado = samplePedido().copy(estado = EstadoPedido.ENTREGADO)
        coEvery { repo.complete(10L) } returns entregado

        val result = useCase(10L)

        assertEquals(EstadoPedido.ENTREGADO, result.estado)
        coVerify(exactly = 1) { repo.complete(10L) }
    }
}
