package com.driverapp.repartidor.ui.profile

import com.driverapp.repartidor.R
import com.driverapp.repartidor.data.local.UserPreferences
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.usecase.ActualizarPerfilUseCase
import com.driverapp.repartidor.domain.usecase.ActualizarVehiculoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerPerfilUseCase
import com.driverapp.repartidor.domain.model.ResumenGanancias
import com.driverapp.repartidor.sampleUser
import com.driverapp.repartidor.testUserPreferences
import com.driverapp.repartidor.ui.common.UiMessage
import com.driverapp.repartidor.ui.common.UiMessenger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val auth: AuthRepository = mockk()
    private val messenger = UiMessenger()
    private val actualizarVehiculo: ActualizarVehiculoUseCase = mockk()

    private lateinit var prefs: UserPreferences

    private fun viewModel() = ProfileViewModel(
        prefs,
        auth,
        mockk<ObtenerPerfilUseCase>(),
        mockk<ActualizarPerfilUseCase>(),
        actualizarVehiculo,
        mockk<ObtenerGananciasUseCase>(),
        messenger
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { auth.user } returns MutableStateFlow(sampleUser())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateVehicle actualiza y avisa`() = runTest {
        prefs = testUserPreferences(tmp.root, backgroundScope)
        coEvery { actualizarVehiculo("Moto") } returns sampleUser().copy(vehicle = "Moto")
        val vm = viewModel()

        vm.updateVehicle("Moto")

        coVerify { actualizarVehiculo("Moto") }
        assertEquals(UiMessage.Res(R.string.msg_vehicle_updated, listOf("Moto")), messenger.message.value)
    }

    @Test
    fun `el backend solo acepta Bicicleta Moto Auto`() {
        assertFalse(ProfileViewModel.VEHICLES.contains("Carro"))
        assertEquals(listOf("Bicicleta", "Moto", "Auto"), ProfileViewModel.VEHICLES)
    }

    @Test
    fun `setNotificationsEnabled guarda en DataStore y avisa`() = runTest {
        prefs = testUserPreferences(tmp.root, backgroundScope)
        val vm = viewModel()

        vm.setNotificationsEnabled(false)

        assertFalse(prefs.notificationsEnabled.first())
        assertEquals(UiMessage.Res(R.string.msg_notifications_off), messenger.message.value)
    }

    @Test
    fun `setLanguage guarda el codigo en DataStore`() = runTest {
        prefs = testUserPreferences(tmp.root, backgroundScope)
        val vm = viewModel()

        vm.setLanguage(UserPreferences.LANG_EN)

        assertEquals(UserPreferences.LANG_EN, prefs.language.first())
    }

    @Test
    fun `refreshStats usa las ganancias reales y no un rating fijo`() = runTest {
        prefs = testUserPreferences(tmp.root, backgroundScope)
        val obtenerGanancias: ObtenerGananciasUseCase = mockk()
        coEvery { obtenerGanancias("year") } returns ResumenGanancias(
            period = "year",
            total = 1200.0,
            ordersCount = 48,
            deliveriesTotal = 1000.0,
            tipsTotal = 150.0,
            bonusesTotal = 50.0,
            otherTotal = 0.0,
            chartLabels = emptyList(),
            chartValues = emptyList(),
            history = emptyList()
        )
        val vm = ProfileViewModel(
            prefs, auth, mockk(), mockk(), actualizarVehiculo, obtenerGanancias, messenger
        )

        vm.refreshStats()

        val stats = vm.stats.value
        assertTrue(stats.loaded)
        assertEquals(48, stats.orders)
        assertEquals(150.0, stats.tips, 0.001)
        assertEquals(1200.0, stats.total, 0.001)
    }
}
