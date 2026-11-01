package com.driverapp.repartidor.ui.login

import app.cash.turbine.test
import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.domain.usecase.LoginUseCase
import com.driverapp.repartidor.sampleUser
import com.driverapp.repartidor.ui.common.UiMessenger
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val auth: AuthRepository = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login exitoso emite Loading y Success`() = runTest {
        coEvery { auth.isFirebaseConfigured() } returns false
        coEvery { auth.login("repartidor1", null) } returns sampleUser()
        val vm = LoginViewModel(LoginUseCase(auth), UiMessenger())

        vm.state.test {
            assertEquals(LoginState.Idle, awaitItem())
            vm.login("repartidor1@driverapp.com", "x")
            assertEquals(LoginState.Loading, awaitItem())
            val result = awaitItem()
            assertTrue(result is LoginState.Success)
            assertEquals("Juan Delgado", (result as LoginState.Success).userName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `credenciales vacias emiten Error`() = runTest {
        val vm = LoginViewModel(LoginUseCase(auth), UiMessenger())

        vm.state.test {
            assertEquals(LoginState.Idle, awaitItem())
            vm.login("", "")
            assertEquals(LoginState.Loading, awaitItem())
            val result = awaitItem()
            assertTrue(result is LoginState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
