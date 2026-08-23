package com.bharatconnect.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharatconnect.app.core.network.NetworkErrorSanitizer
import com.bharatconnect.app.data.repository.AuthRepositoryImpl
import com.bharatconnect.app.domain.model.AuthState
import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.domain.repository.AuthRepository
import com.bharatconnect.app.domain.usecase.auth.GetCurrentUserUseCase
import com.bharatconnect.app.domain.usecase.auth.LoginUseCase
import com.bharatconnect.app.domain.usecase.auth.LogoutUseCase
import com.bharatconnect.app.domain.usecase.auth.RegisterUseCase
import com.bharatconnect.app.domain.usecase.auth.ResetPasswordUseCase
import com.bharatconnect.app.domain.usecase.auth.UpdateProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    authRepository: AuthRepository = AuthRepositoryImpl(),
    private val loginUseCase: LoginUseCase = LoginUseCase(authRepository),
    private val registerUseCase: RegisterUseCase = RegisterUseCase(authRepository),
    private val updateProfileUseCase: UpdateProfileUseCase = UpdateProfileUseCase(authRepository),
    private val resetPasswordUseCase: ResetPasswordUseCase = ResetPasswordUseCase(authRepository),
    private val logoutUseCase: LogoutUseCase = LogoutUseCase(authRepository),
    private val getCurrentUserUseCase: GetCurrentUserUseCase = GetCurrentUserUseCase(authRepository)
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _resetPasswordMessage = MutableStateFlow<String?>(null)
    val resetPasswordMessage: StateFlow<String?> = _resetPasswordMessage.asStateFlow()

    private val _isResettingPassword = MutableStateFlow(false)
    val isResettingPassword: StateFlow<Boolean> = _isResettingPassword.asStateFlow()

    private val _isUpdatingProfile = MutableStateFlow(false)
    val isUpdatingProfile: StateFlow<Boolean> = _isUpdatingProfile.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            if (user != null) {
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please enter your username, email, or mobile number and password")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = loginUseCase(identifier, password)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(NetworkErrorSanitizer.sanitize(error))
                }
            )
        }
    }

    fun register(
        name: String,
        username: String,
        email: String,
        phoneNumber: String,
        dob: String,
        password: String,
        avatarUrl: String? = null
    ) {
        if (name.isBlank() || username.isBlank() || email.isBlank() || phoneNumber.isBlank() || dob.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("All fields are required for registration")
            return
        }

        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = registerUseCase(
                email = email,
                password = password,
                username = username,
                fullName = name,
                phoneNumber = phoneNumber,
                dob = dob,
                avatarUrl = avatarUrl
            )
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(NetworkErrorSanitizer.sanitize(error))
                }
            )
        }
    }

    fun updateProfile(
        fullName: String,
        bio: String? = null,
        phoneNumber: String? = null,
        dob: String? = null,
        avatarUrl: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isUpdatingProfile.value = true
            val result = updateProfileUseCase(fullName, bio, phoneNumber, dob, avatarUrl)
            _isUpdatingProfile.value = false
            result.fold(
                onSuccess = { updatedUser ->
                    _currentUser.value = updatedUser
                    _authState.value = AuthState.Authenticated(updatedUser)
                    onResult(true, null)
                },
                onFailure = { error ->
                    onResult(false, NetworkErrorSanitizer.sanitize(error))
                }
            )
        }
    }

    fun forgotPassword(emailOrIdentifier: String, onResult: (Boolean, String) -> Unit) {
        if (emailOrIdentifier.isBlank()) {
            onResult(false, "Please enter your email, username, or mobile number")
            return
        }

        viewModelScope.launch {
            _isResettingPassword.value = true
            val result = resetPasswordUseCase(emailOrIdentifier)
            _isResettingPassword.value = false
            result.fold(
                onSuccess = {
                    val msg = "Password reset instructions sent! Please check your email inbox."
                    _resetPasswordMessage.value = msg
                    onResult(true, msg)
                },
                onFailure = { error ->
                    val msg = NetworkErrorSanitizer.sanitize(error)
                    _resetPasswordMessage.value = msg
                    onResult(false, msg)
                }
            )
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            _currentUser.value = null
            _authState.value = AuthState.Idle
            onComplete()
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
        _resetPasswordMessage.value = null
    }
}
