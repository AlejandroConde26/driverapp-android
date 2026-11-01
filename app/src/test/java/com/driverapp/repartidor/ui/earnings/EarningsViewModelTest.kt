package com.driverapp.repartidor.ui.earnings

import android.content.SharedPreferences
import com.driverapp.repartidor.data.local.UserPreferences
import com.driverapp.repartidor.domain.usecase.ObtenerGananciasUseCase
import com.driverapp.repartidor.domain.usecase.ObtenerHistorialGananciasUseCase
import com.driverapp.repartidor.ui.common.UiMessenger
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EarningsViewModelTest {

    private fun fakePrefs(): SharedPreferences {
        val data = mutableMapOf<String, Any?>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putBoolean(any(), any()) } answers {
            data[firstArg<String>()] = secondArg<Boolean>()
            editor
        }
        val prefs = mockk<SharedPreferences>()
        every { prefs.getBoolean(any(), any()) } answers {
            data[firstArg<String>()] as? Boolean ?: secondArg()
        }
        every { prefs.edit() } returns editor
        return prefs
    }

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleBalance alterna mostrar saldo`() {
        val vm = EarningsViewModel(
            mockk<ObtenerGananciasUseCase>(),
            mockk<ObtenerHistorialGananciasUseCase>(),
            UiMessenger(),
            UserPreferences(fakePrefs())
        )

        assertTrue(vm.prefs.showBalance)
        vm.toggleBalance()
        assertFalse(vm.prefs.showBalance)
        vm.toggleBalance()
        assertTrue(vm.prefs.showBalance)
    }
}
