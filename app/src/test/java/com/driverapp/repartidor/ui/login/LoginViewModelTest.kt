package com.driverapp.repartidor.ui.login

import app.cash.turbine.test
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.usecase.LoginUseCase
import com.driverapp.repartidor.sampleUser
import com.driverapp.repartidor.ui.common.UiMessage
import com.driverapp.repartidor.ui.common.UiMessenger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val auth: AuthRepository = mockk()
    private val messenger = UiMessenger()

    private fun viewModel() = LoginViewModel(LoginUseCase(auth), auth, messenger)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { auth.currentToken() } returns null
        every { auth.isFirebaseConfigured() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login exitoso emite Loading y Success`() = runTest {
        coEvery { auth.login("repartidor1", null) } returns sampleUser()
        val vm = viewModel()

        vm.state.test {
            assertEquals(LoginState.Idle, awaitItem())
            vm.login("repartidor1@driverapp.com", "secreta")
            assertEquals(LoginState.Loading, awaitItem())
            val result = awaitItem()
            assertTrue(result is LoginState.Success)
            assertEquals("Juan Delgado", (result as LoginState.Success).userName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `credenciales vacias emiten Error sin cerrar la app`() = runTest {
        val vm = viewModel()

        vm.state.test {
            assertEquals(LoginState.Idle, awaitItem())
            vm.login("", "")
            assertEquals(LoginState.Loading, awaitItem())
            val result = awaitItem()
            assertTrue(result is LoginState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `valida el formato del correo`() {
        val vm = viewModel()

        assertEquals(R.string.error_email_required, vm.validateEmail(""))
        assertEquals(R.string.error_email_invalid, vm.validateEmail("no-es-correo@"))
        assertNull(vm.validateEmail("repartidor1@driverapp.com"))
        // En modo dev se acepta usuario sin dominio
        assertNull(vm.validateEmail("repartidor1"))
    }

    @Test
    fun `valida la longitud minima de la contrasena`() {
        val vm = viewModel()

        assertEquals(R.string.error_required, vm.validatePassword(""))
        assertEquals(R.string.error_password_short, vm.validatePassword("12345"))
        assertNull(vm.validatePassword("123456"))
    }

    @Test
    fun `isLoggedIn refleja el token guardado`() {
        every { auth.currentToken() } returns "token"
        assertTrue(viewModel().isLoggedIn())

        every { auth.currentToken() } returns null
        assertTrue(!viewModel().isLoggedIn())
    }

    @Test
    fun `un error de red se muestra con mensaje propio`() = runTest {
        coEvery { auth.login(any(), any()) } throws java.net.UnknownHostException()
        val vm = viewModel()

        vm.login("repartidor1@driverapp.com", "secreta")

        val message = messenger.message.value
        assertNotNull(message)
        assertTrue(message is UiMessage.Raw)
        assertTrue((message as UiMessage.Raw).text.contains("Sin conexión"))
    }
}
