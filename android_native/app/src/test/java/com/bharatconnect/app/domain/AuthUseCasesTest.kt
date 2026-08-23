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
                dob = "15/08/1997",
                avatarUrl = "https://res.cloudinary.com/twiesyqj/image/upload/avatar.jpg"
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
        dob: String?,
        avatarUrl: String?
    ): Result<UserProfile> {
        val user = UserProfile(
            id = "user_new",
            email = email,
            username = username,
            fullName = fullName,
            phoneNumber = phoneNumber,
            dob = dob,
            avatarUrl = avatarUrl
        )
        currentUser = user
        return Result.success(user)
    }

    override suspend fun verifyEmailOtp(email: String, token: String): Result<UserProfile> {
        return if (email == "priya@bharatconnect.in" && token == "123456") {
            val user = UserProfile(
                id = "user_verified_123",
                email = email,
                username = "priya_p",
                fullName = "Priya Patel"
            )
            currentUser = user
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("Invalid verification code"))
        }
    }

    override suspend fun resendEmailOtp(email: String): Result<Unit> {
        return if (email.isNotBlank() && email.contains("@")) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Invalid email"))
        }
    }

    override suspend fun updateProfile(
        fullName: String,
        bio: String?,
        phoneNumber: String?,
        dob: String?,
        avatarUrl: String?
    ): Result<UserProfile> {
        val updated = (currentUser ?: UserProfile(id = "user_123", email = "test@bharatconnect.in", username = "testuser", fullName = fullName)).copy(
            fullName = fullName,
            bio = bio,
            phoneNumber = phoneNumber,
            dob = dob,
            avatarUrl = avatarUrl
        )
        currentUser = updated
        return Result.success(updated)
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
    private val verifyEmailOtpUseCase = VerifyEmailOtpUseCase(fakeAuthRepository)
    private val resendEmailOtpUseCase = ResendEmailOtpUseCase(fakeAuthRepository)
    private val updateProfileUseCase = UpdateProfileUseCase(fakeAuthRepository)
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
    fun `register creates new user profile with phone dob and avatar successfully`() = runBlocking {
        val result = registerUseCase(
            email = "priya@bharatconnect.in",
            password = "pass123",
            username = "priya_p",
            fullName = "Priya Patel",
            phoneNumber = "+919876543210",
            dob = "10/05/1998",
            avatarUrl = "https://res.cloudinary.com/twiesyqj/image/upload/v1/avatar.jpg"
        )
        assertTrue(result.isSuccess)
        assertEquals("priya_p", result.getOrNull()?.username)
        assertEquals("Priya Patel", result.getOrNull()?.fullName)
        assertEquals("+919876543210", result.getOrNull()?.phoneNumber)
        assertEquals("10/05/1998", result.getOrNull()?.dob)
        assertEquals("https://res.cloudinary.com/twiesyqj/image/upload/v1/avatar.jpg", result.getOrNull()?.avatarUrl)
    }

    @Test
    fun `verifyEmailOtp with valid code returns authenticated user profile`() = runBlocking {
        val result = verifyEmailOtpUseCase("priya@bharatconnect.in", "123456")
        assertTrue(result.isSuccess)
        assertEquals("user_verified_123", result.getOrNull()?.id)
        assertEquals("priya_p", result.getOrNull()?.username)
    }

    @Test
    fun `verifyEmailOtp with invalid code returns failure`() = runBlocking {
        val result = verifyEmailOtpUseCase("priya@bharatconnect.in", "000000")
        assertTrue(result.isFailure)
    }

    @Test
    fun `resendEmailOtp with valid email returns success`() = runBlocking {
        val result = resendEmailOtpUseCase("priya@bharatconnect.in")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `updateProfile modifies profile fields and avatar successfully`() = runBlocking {
        loginUseCase("test@bharatconnect.in", "secure123")
        val result = updateProfileUseCase(
            fullName = "Updated User",
            bio = "New bio",
            phoneNumber = "+919999999999",
            dob = "01/01/2000",
            avatarUrl = "https://res.cloudinary.com/twiesyqj/image/upload/v2/new_avatar.jpg"
        )
        assertTrue(result.isSuccess)
        assertEquals("Updated User", result.getOrNull()?.fullName)
        assertEquals("New bio", result.getOrNull()?.bio)
        assertEquals("https://res.cloudinary.com/twiesyqj/image/upload/v2/new_avatar.jpg", result.getOrNull()?.avatarUrl)
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
