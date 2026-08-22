package com.bharatconnect.app.domain

import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.domain.repository.AuthRepository
import com.bharatconnect.app.domain.usecase.auth.GetCurrentUserUseCase
import com.bharatconnect.app.domain.usecase.auth.LoginUseCase
import com.bharatconnect.app.domain.usecase.auth.LogoutUseCase
import com.bharatconnect.app.domain.usecase.auth.RegisterUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthRepository : AuthRepository {
    private var currentUser: UserProfile? = null

    override val currentUserFlow: Flow<UserProfile?> = flowOf(currentUser)

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        return if (email == "test@bharatconnect.in" && password == "secure123") {
            val user = UserProfile(id = "user_123", email = email, username = "testuser", fullName = "Test User")
            currentUser = user
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("Invalid credentials"))
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        username: String,
        fullName: String
    ): Result<UserProfile> {
        val user = UserProfile(id = "user_new", email = email, username = username, fullName = fullName)
        currentUser = user
        return Result.success(user)
    }

    override suspend fun logout(): Result<Unit> {
        currentUser = null
        return Result.success(Unit)
    }

    override suspend fun getCurrentUser(): UserProfile? = currentUser

    override fun isUserLoggedIn(): Boolean = currentUser != null
}

class AuthUseCasesTest {

    private val fakeAuthRepository = FakeAuthRepository()
    private val loginUseCase = LoginUseCase(fakeAuthRepository)
    private val registerUseCase = RegisterUseCase(fakeAuthRepository)
    private val logoutUseCase = LogoutUseCase(fakeAuthRepository)
    private val getCurrentUserUseCase = GetCurrentUserUseCase(fakeAuthRepository)

    @Test
    fun `login with valid credentials returns success`() = runBlocking {
        val result = loginUseCase("test@bharatconnect.in", "secure123")
        assertTrue(result.isSuccess)
        assertEquals("user_123", result.getOrNull()?.id)
        assertEquals("Test User", result.getOrNull()?.fullName)
    }

    @Test
    fun `login with invalid credentials returns failure`() = runBlocking {
        val result = loginUseCase("wrong@email.com", "badpass")
        assertTrue(result.isFailure)
    }

    @Test
    fun `register creates new user profile successfully`() = runBlocking {
        val result = registerUseCase("priya@bharatconnect.in", "pass123", "priya_p", "Priya Patel")
        assertTrue(result.isSuccess)
        assertEquals("priya_p", result.getOrNull()?.username)
        assertEquals("Priya Patel", result.getOrNull()?.fullName)
    }

    @Test
    fun `logout clears session state`() = runBlocking {
        loginUseCase("test@bharatconnect.in", "secure123")
        logoutUseCase()
        assertEquals(null, getCurrentUserUseCase())
    }
}
