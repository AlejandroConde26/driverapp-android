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

class AceptarPedidoUseCaseTest {

    private val repo: PedidoRepository = mockk()
    private val useCase = AceptarPedidoUseCase(repo)

    @Test
    fun `aceptar delega al repositorio y devuelve el pedido`() = runTest {
        val aceptado = samplePedido().copy(estado = EstadoPedido.ACEPTADO)
        coEvery { repo.accept(10L) } returns aceptado

        val result = useCase(10L)

        assertEquals(EstadoPedido.ACEPTADO, result.estado)
        coVerify(exactly = 1) { repo.accept(10L) }
    }
}
