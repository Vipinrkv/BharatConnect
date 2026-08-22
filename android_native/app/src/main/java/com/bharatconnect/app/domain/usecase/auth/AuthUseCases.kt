package com.bharatconnect.app.domain.usecase.auth

import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<UserProfile> {
        return authRepository.login(email, password)
    }
}

class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        username: String,
        fullName: String
    ): Result<UserProfile> {
        return authRepository.register(email, password, username, fullName)
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
