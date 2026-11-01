package com.driverapp.repartidor.ui.orders

import app.cash.turbine.test
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.repository.PedidoRepository
import com.driverapp.repartidor.domain.usecase.AceptarPedidoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerDetallePedidoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerPedidosDisponiblesUseCase
import com.driverapp.repartidor.domain.usecase.RechazarPedidoUseCase
import com.driverapp.repartidor.samplePedido
import com.driverapp.repartidor.ui.common.ErrorState
import com.driverapp.repartidor.ui.common.LoadingState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    private val repo: PedidoRepository = mockk(relaxed = true)
    private val disponibles: ObtenerPedidosDisponiblesUseCase = mockk()
    private val historial: ObtenerHistorialUseCase = mockk()
    private val aceptar: AceptarPedidoUseCase = mockk()
    private val rechazar: RechazarPedidoUseCase = mockk()
    private val detalle: ObtenerDetallePedidoUseCase = mockk()
    private val messenger = UiMessenger()

    private fun viewModel() =
        OrdersViewModel(repo, disponibles, historial, aceptar, rechazar, detalle, messenger)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.available } returns flowOf(listOf(samplePedido()))
        every { repo.history } returns flowOf(emptyList())
        every { repo.pendingActions } returns flowOf(0)
        coEvery { disponibles() } returns Unit
        coEvery { historial() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `las listas vienen del repositorio y llegan a la UI`() = runTest {
        val vm = viewModel()

        vm.available.test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
        vm.history.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadAll termina en Idle cuando todo va bien`() = runTest {
        val vm = viewModel()
        assertEquals(LoadingState.Idle, vm.loading.value)
        assertEquals(ErrorState.None, vm.error.value)
    }

    @Test
    fun `sin red loadAll deja ErrorState de conexion`() = runTest {
        coEvery { disponibles() } throws UnknownHostException()
        val vm = viewModel()

        vm.loadAll()

        assertTrue(vm.error.value is ErrorState.SinConexion)
    }

    @Test
    fun `accept delega en el caso de uso y avisa`() = runTest {
        val accepted = samplePedido().copy(estado = EstadoPedido.ACEPTADO)
        coEvery { aceptar(10L) } returns accepted
        val vm = viewModel()
        var callback: com.driverapp.repartidor.domain.model.Pedido? = null

        vm.accept(samplePedido()) { callback = it }

        coVerify { aceptar(10L) }
        assertEquals(EstadoPedido.ACEPTADO, callback?.estado)
        assertEquals(false, vm.busy.value)
    }

    @Test
    fun `reject delega en el caso de uso`() = runTest {
        coEvery { rechazar(10L) } returns samplePedido().copy(estado = EstadoPedido.RECHAZADO)
        val vm = viewModel()

        vm.reject(samplePedido())

        coVerify { rechazar(10L) }
    }

    @Test
    fun `openDetail refresca el detalle y closeDetail lo limpia`() = runTest {
        coEvery { detalle(10L) } returns samplePedido()
        val vm = viewModel()

        vm.openDetail(samplePedido())
        assertEquals(10, vm.detail.value?.id)

        vm.closeDetail()
        assertEquals(null, vm.detail.value)
    }
}
