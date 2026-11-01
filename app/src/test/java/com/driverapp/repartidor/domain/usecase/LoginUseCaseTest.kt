package com.driverapp.repartidor.domain.usecase

import com.driverapp.repartidor.domain.repository.AuthRepository
import com.driverapp.repartidor.sampleUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginUseCaseTest {

    private val auth: AuthRepository = mockk()
    private val useCase = LoginUseCase(auth)

    @Test
    fun `manda correo y contrasena al backend`() = runTest {
        coEvery { auth.login("repartidor1@driverapp.com", "secreta", null) } returns sampleUser()

        val user = useCase("repartidor1@driverapp.com", "secreta")

        assertEquals("Juan Delgado", user.name)
        coVerify { auth.login("repartidor1@driverapp.com", "secreta", null) }
    }

    @Test
    fun `recorta espacios del correo antes de enviarlo`() = runTest {
        coEvery { auth.login("repartidor1@driverapp.com", "secreta", null) } returns sampleUser()

        useCase("  repartidor1@driverapp.com  ", "secreta")

        coVerify { auth.login("repartidor1@driverapp.com", "secreta", null) }
    }

    @Test
    fun `reenvia el token FCM cuando se conoce`() = runTest {
        coEvery { auth.login("a@b.com", "pw", "FCM123") } returns sampleUser()

        useCase("a@b.com", "pw", "FCM123")

        coVerify { auth.login("a@b.com", "pw", "FCM123") }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `correo o clave vacios lanzan error`() = runTest {
        useCase("", "")
    }
}
