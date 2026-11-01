package com.driverapp.repartidor.ui.earnings

import app.cash.turbine.test
import com.driverapp.repartidor.domain.usecase.ObtenerGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialGananciasUseCase
import com.driverapp.repartidor.testUserPreferences
import com.driverapp.repartidor.ui.common.UiMessenger
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class EarningsViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleBalance alterna mostrar saldo y persiste en DataStore`() = runTest {
        val prefs = testUserPreferences(tmp.root, backgroundScope)
        val vm = EarningsViewModel(
            mockk<ObtenerGananciasUseCase>(),
            mockk<ObtenerHistorialGananciasUseCase>(),
            UiMessenger(),
            prefs
        )

        vm.showBalance.test {
            assertTrue(awaitItem())
            vm.toggleBalance()
            assertFalse(awaitItem())
            vm.toggleBalance()
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
