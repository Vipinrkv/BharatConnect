package com.bharatconnect.app.domain.usecase.auth

import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(identifier: String, password: String): Result<UserProfile> {
        return authRepository.login(identifier, password)
    }
}

class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        username: String,
        fullName: String,
        phoneNumber: String? = null,
        dob: String? = null
    ): Result<UserProfile> {
        return authRepository.register(email, password, username, fullName, phoneNumber, dob)
    }
}

class ResetPasswordUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(emailOrIdentifier: String): Result<Unit> {
        return authRepository.resetPassword(emailOrIdentifier)
    }
}

class LogoutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.logout()
    }
}

class GetCurrentUserUseCase(private val authRepository: AuthRepository) {
    val currentUserFlow: Flow<UserProfile?> = authRepository.currentUserFlow

    suspend operator fun invoke(): UserProfile? {
        return authRepository.getCurrentUser()
    }

    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }
}
