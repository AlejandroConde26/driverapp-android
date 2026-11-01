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
    fun `modo dev usa la parte local del correo como idToken`() = runTest {
        coEvery { auth.isFirebaseConfigured() } returns false
        coEvery { auth.login("repartidor1", null) } returns sampleUser()

        val user = useCase("repartidor1@driverapp.com", "cualquiera")

        assertEquals("Juan Delgado", user.name)
        coVerify { auth.login("repartidor1", null) }
    }

    @Test
    fun `con firebase usa el idToken de Firebase`() = runTest {
        coEvery { auth.isFirebaseConfigured() } returns true
        coEvery { auth.firebaseSignIn("a@b.com", "pw") } returns "FB_TOKEN"
        coEvery { auth.login("FB_TOKEN", null) } returns sampleUser()

        useCase("a@b.com", "pw")

        coVerify { auth.firebaseSignIn("a@b.com", "pw") }
        coVerify { auth.login("FB_TOKEN", null) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `correo o clave vacios lanzan error`() = runTest {
        useCase("", "")
    }
}
