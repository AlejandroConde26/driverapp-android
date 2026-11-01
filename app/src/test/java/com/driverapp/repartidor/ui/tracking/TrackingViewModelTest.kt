package com.driverapp.repartidor.ui.tracking

import app.cash.turbine.test
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.domain.usecase.MarcarEnCaminoUseCase
import com.driverapp.repartidor.domain.usecase.MarcarPedidoEntregadoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerMisPedidosUseCase
import com.driverapp.repartidor.samplePedido
import com.driverapp.repartidor.ui.common.UiMessenger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * El hueco que dejaba el flujo (ACEPTADO → ENTREGADO sin pasar por EN_CAMINO)
 * queda cubierto aquí: si `marcarEnCamino` deja de llamarse, este test falla.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackingViewModelTest {

    private val repo: PedidoRepository = mockk(relaxed = true)
    private val misPedidos: ObtenerMisPedidosUseCase = mockk()
    private val historial: ObtenerHistorialUseCase = mockk()
    private val enCamino: MarcarEnCaminoUseCase = mockk()
    private val entregar: MarcarPedidoEntregadoUseCase = mockk()
    private val messenger = UiMessenger()

    private fun viewModel() =
        TrackingViewModel(repo, misPedidos, historial, enCamino, entregar, messenger)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.activeOrder } returns flowOf(samplePedido().copy(estado = EstadoPedido.ACEPTADO))
        coEvery { misPedidos() } returns Unit
        coEvery { historial() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `el pedido activo llega a la UI desde el repositorio`() = runTest {
        val vm = viewModel()

        vm.activeOrder.test {
            assertEquals(EstadoPedido.ACEPTADO, awaitItem()?.estado)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `marcarEnCamino llama a in-transit y deja el pedido EN_CAMINO`() = runTest {
        val onTheWay = samplePedido().copy(estado = EstadoPedido.EN_CAMINO)
        coEvery { enCamino(10L) } returns onTheWay
        val vm = viewModel()
        var result: com.driverapp.repartidor.domain.model.Pedido? = null

        vm.marcarEnCamino(samplePedido()) { result = it }

        coVerify(exactly = 1) { enCamino(10L) }
        assertEquals(EstadoPedido.EN_CAMINO, result?.estado)
        assertEquals(false, vm.busy.value)
    }

    @Test
    fun `complete delega en el caso de uso y refresca el historial`() = runTest {
        coEvery { entregar(10L) } returns samplePedido().copy(estado = EstadoPedido.ENTREGADO)
        val vm = viewModel()
        var result: com.driverapp.repartidor.domain.model.Pedido? = null

        vm.complete(samplePedido()) { result = it }

        coVerify { entregar(10L) }
        coVerify { historial() }
        assertEquals(EstadoPedido.ENTREGADO, result?.estado)
    }

    @Test
    fun `refreshActive tolera fallos de red sin romper la pantalla`() = runTest {
        coEvery { misPedidos() } throws java.net.UnknownHostException()
        val vm = viewModel()

        vm.refreshActive()

        assertEquals(
            com.driverapp.repartidor.ui.common.ErrorState.SinConexion::class,
            vm.error.value::class
        )
    }
}
