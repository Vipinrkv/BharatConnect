package com.bharatconnect.app.domain.usecase.auth

import android.net.Uri
import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(identifier: String, password: String): Result<UserProfile> {
        if (identifier.isBlank()) return Result.failure(IllegalArgumentException("Please enter your username, email, or mobile number"))
        if (password.isBlank()) return Result.failure(IllegalArgumentException("Password cannot be blank"))
        return repository.login(identifier, password)
    }
}

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        username: String,
        fullName: String,
        phoneNumber: String? = null,
        dob: String? = null,
        avatarUrl: String? = null
    ): Result<UserProfile> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email cannot be blank"))
        if (!email.contains("@") || !email.contains(".")) return Result.failure(IllegalArgumentException("Invalid email format"))
        if (password.length < 6) return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        if (username.isBlank()) return Result.failure(IllegalArgumentException("Username cannot be blank"))
        if (fullName.isBlank()) return Result.failure(IllegalArgumentException("Full name cannot be blank"))
        return repository.register(email, password, username, fullName, phoneNumber, dob, avatarUrl)
    }
}

class HandleAuthCallbackUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(uri: Uri): Result<UserProfile> {
        return repository.handleAuthCallback(uri)
    }
}

class VerifyEmailOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, token: String): Result<UserProfile> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email cannot be blank"))
        if (token.isBlank()) return Result.failure(IllegalArgumentException("Please enter the verification code"))
        if (token.trim().length < 6) return Result.failure(IllegalArgumentException("Verification code must be 6 digits"))
        return repository.verifyEmailOtp(email, token)
    }
}

class ResendEmailOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email cannot be blank"))
        return repository.resendEmailOtp(email)
    }
}

class UpdateProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        fullName: String,
        bio: String? = null,
        phoneNumber: String? = null,
        dob: String? = null,
        avatarUrl: String? = null
    ): Result<UserProfile> {
        if (fullName.isBlank()) return Result.failure(IllegalArgumentException("Full name cannot be blank"))
        return repository.updateProfile(fullName, bio, phoneNumber, dob, avatarUrl)
    }
}

class ResetPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(emailOrIdentifier: String): Result<Unit> {
        if (emailOrIdentifier.isBlank()) return Result.failure(IllegalArgumentException("Please enter your registered email, username, or mobile number"))
        return repository.resetPassword(emailOrIdentifier)
    }
}

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.logout()
}

class GetCurrentUserUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): UserProfile? = repository.getCurrentUser()
}

class ObserveAuthStateUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<UserProfile?> = repository.currentUserFlow
}

data class AuthUseCases(
    val login: LoginUseCase,
    val register: RegisterUseCase,
    val handleAuthCallback: HandleAuthCallbackUseCase,
    val verifyEmailOtp: VerifyEmailOtpUseCase,
    val resendEmailOtp: ResendEmailOtpUseCase,
    val updateProfile: UpdateProfileUseCase,
    val resetPassword: ResetPasswordUseCase,
    val logout: LogoutUseCase,
    val getCurrentUser: GetCurrentUserUseCase,
    val observeAuthState: ObserveAuthStateUseCase
)
