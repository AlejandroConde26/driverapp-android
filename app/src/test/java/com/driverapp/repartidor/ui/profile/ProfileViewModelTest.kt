package com.driverapp.repartidor.ui.profile

import android.content.SharedPreferences
import com.driverapp.repartidor.data.local.UserPreferences
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.usecase.ActualizarPerfilUseCase
import com.driverapp.repartidor.domain.usecase.ActualizarVehiculoUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerPerfilUseCase
import com.driverapp.repartidor.sampleUser
import com.driverapp.repartidor.ui.common.UiMessenger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val auth: AuthRepository = mockk()
    private val messenger = UiMessenger()

    private fun fakePrefs(): SharedPreferences {
        val data = mutableMapOf<String, Any?>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putBoolean(any(), any()) } answers {
            data[firstArg<String>()] = secondArg<Boolean>()
            editor
        }
        every { editor.putString(any(), any()) } answers {
            data[firstArg<String>()] = secondArg<String>()
            editor
        }
        val prefs = mockk<SharedPreferences>()
        every { prefs.getBoolean(any(), any()) } answers {
            data[firstArg<String>()] as? Boolean ?: secondArg()
        }
        every { prefs.getString(any(), any()) } answers {
            data[firstArg<String>()] as? String ?: secondArg()
        }
        every { prefs.edit() } returns editor
        return prefs
    }

    private val actualizarVehiculo: ActualizarVehiculoUseCase = mockk()

    private fun viewModel() = ProfileViewModel(
        UserPreferences(fakePrefs()),
        auth,
        mockk<ObtenerPerfilUseCase>(),
        mockk<ActualizarPerfilUseCase>(),
        actualizarVehiculo,
        mockk<ObtenerGananciasUseCase>(),
        mockk<ObtenerHistorialGananciasUseCase>(),
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
        coEvery { actualizarVehiculo("Moto") } returns sampleUser().copy(vehicle = "Moto")
        val vm = viewModel()

        vm.updateVehicle("Moto")

        coVerify { actualizarVehiculo("Moto") }
        assertEquals("Vehículo actualizado: Moto", messenger.message.value)
    }

    @Test
    fun `setNotificationsEnabled guarda y avisa`() = runTest {
        val vm = viewModel()

        vm.setNotificationsEnabled(false)

        assertEquals(false, vm.prefs.notificationsEnabled)
        assertEquals("Notificaciones desactivadas", messenger.message.value)
    }

    @Test
    fun `setLanguage guarda el codigo`() = runTest {
        val vm = viewModel()

        vm.setLanguage(UserPreferences.LANG_EN)

        assertEquals(UserPreferences.LANG_EN, vm.prefs.language)
        assertEquals("English", vm.languageLabel())
    }
}
