package com.bharatconnect.app.domain

import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.domain.repository.AuthRepository
import com.bharatconnect.app.domain.usecase.auth.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthRepository : AuthRepository {
    private var currentUser: UserProfile? = null

    override val currentUserFlow: Flow<UserProfile?> = flowOf(currentUser)

    override suspend fun login(identifier: String, password: String): Result<UserProfile> {
        return if ((identifier == "test@bharatconnect.in" || identifier == "testuser" || identifier == "+919876543210") && password == "secure123") {
            val user = UserProfile(
                id = "user_123",
                email = "test@bharatconnect.in",
                username = "testuser",
                fullName = "Test User",
                phoneNumber = "+919876543210",
                dob = "15/08/1997"
            )
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
        fullName: String,
        phoneNumber: String?,
        dob: String?
    ): Result<UserProfile> {
        val user = UserProfile(
            id = "user_new",
            email = email,
            username = username,
            fullName = fullName,
            phoneNumber = phoneNumber,
            dob = dob
        )
        currentUser = user
        return Result.success(user)
    }

    override suspend fun resetPassword(emailOrIdentifier: String): Result<Unit> {
        return if (emailOrIdentifier.isNotBlank()) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Identifier cannot be empty"))
        }
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
    private val resetPasswordUseCase = ResetPasswordUseCase(fakeAuthRepository)
    private val logoutUseCase = LogoutUseCase(fakeAuthRepository)
    private val getCurrentUserUseCase = GetCurrentUserUseCase(fakeAuthRepository)

    @Test
    fun `login with email identifier returns success`() = runBlocking {
        val result = loginUseCase("test@bharatconnect.in", "secure123")
        assertTrue(result.isSuccess)
        assertEquals("user_123", result.getOrNull()?.id)
        assertEquals("Test User", result.getOrNull()?.fullName)
    }

    @Test
    fun `login with username identifier returns success`() = runBlocking {
        val result = loginUseCase("testuser", "secure123")
        assertTrue(result.isSuccess)
        assertEquals("user_123", result.getOrNull()?.id)
    }

    @Test
    fun `login with mobile number identifier returns success`() = runBlocking {
        val result = loginUseCase("+919876543210", "secure123")
        assertTrue(result.isSuccess)
        assertEquals("user_123", result.getOrNull()?.id)
    }

    @Test
    fun `login with invalid credentials returns failure`() = runBlocking {
        val result = loginUseCase("wrong@email.com", "badpass")
        assertTrue(result.isFailure)
    }

    @Test
    fun `register creates new user profile with phone and dob successfully`() = runBlocking {
        val result = registerUseCase(
            email = "priya@bharatconnect.in",
            password = "pass123",
            username = "priya_p",
            fullName = "Priya Patel",
            phoneNumber = "+919876543210",
            dob = "10/05/1998"
        )
        assertTrue(result.isSuccess)
        assertEquals("priya_p", result.getOrNull()?.username)
        assertEquals("Priya Patel", result.getOrNull()?.fullName)
        assertEquals("+919876543210", result.getOrNull()?.phoneNumber)
        assertEquals("10/05/1998", result.getOrNull()?.dob)
    }

    @Test
    fun `resetPassword with valid identifier succeeds`() = runBlocking {
        val result = resetPasswordUseCase("priya@bharatconnect.in")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `logout clears session state`() = runBlocking {
        loginUseCase("test@bharatconnect.in", "secure123")
        logoutUseCase()
        assertEquals(null, getCurrentUserUseCase())
    }
}
