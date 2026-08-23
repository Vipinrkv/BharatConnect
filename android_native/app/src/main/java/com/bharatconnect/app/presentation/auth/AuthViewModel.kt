package com.bharatconnect.app.presentation.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharatconnect.app.core.network.NetworkErrorSanitizer
import com.bharatconnect.app.data.repository.AuthRepositoryImpl
import com.bharatconnect.app.domain.model.AuthState
import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.domain.repository.AuthRepository
import com.bharatconnect.app.domain.usecase.auth.GetCurrentUserUseCase
import com.bharatconnect.app.domain.usecase.auth.HandleAuthCallbackUseCase
import com.bharatconnect.app.domain.usecase.auth.LoginUseCase
import com.bharatconnect.app.domain.usecase.auth.LogoutUseCase
import com.bharatconnect.app.domain.usecase.auth.RegisterUseCase
import com.bharatconnect.app.domain.usecase.auth.ResendEmailOtpUseCase
import com.bharatconnect.app.domain.usecase.auth.ResetPasswordUseCase
import com.bharatconnect.app.domain.usecase.auth.UpdateProfileUseCase
import com.bharatconnect.app.domain.usecase.auth.VerifyEmailOtpUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    authRepository: AuthRepository = AuthRepositoryImpl(),
    private val loginUseCase: LoginUseCase = LoginUseCase(authRepository),
    private val registerUseCase: RegisterUseCase = RegisterUseCase(authRepository),
    private val handleAuthCallbackUseCase: HandleAuthCallbackUseCase = HandleAuthCallbackUseCase(authRepository),
    private val verifyEmailOtpUseCase: VerifyEmailOtpUseCase = VerifyEmailOtpUseCase(authRepository),
    private val resendEmailOtpUseCase: ResendEmailOtpUseCase = ResendEmailOtpUseCase(authRepository),
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

    private val _isVerifyingOtp = MutableStateFlow(false)
    val isVerifyingOtp: StateFlow<Boolean> = _isVerifyingOtp.asStateFlow()

    private val _resendCooldownSeconds = MutableStateFlow(0)
    val resendCooldownSeconds: StateFlow<Int> = _resendCooldownSeconds.asStateFlow()

    private var cooldownJob: Job? = null

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
                    if (user.id.isBlank()) {
                        // User created; awaiting email confirmation via deep link
                        _authState.value = AuthState.AwaitingOtp(
                            email = user.email ?: email.trim(),
                            username = username.trim(),
                            fullName = name.trim(),
                            phoneNumber = phoneNumber.trim(),
                            dob = dob.trim(),
                            avatarUrl = avatarUrl?.trim()
                        )
                        startResendCooldown(60)
                    } else {
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                    }
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(NetworkErrorSanitizer.sanitize(error))
                }
            )
        }
    }

    fun handleAuthCallback(uri: Uri) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = handleAuthCallbackUseCase(uri)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { error ->
                    val existingUser = getCurrentUserUseCase()
                    if (existingUser != null) {
                        _currentUser.value = existingUser
                        _authState.value = AuthState.Authenticated(existingUser)
                    } else {
                        _authState.value = AuthState.Error(NetworkErrorSanitizer.sanitize(error))
                    }
                }
            )
        }
    }

    fun verifyEmailOtp(email: String, token: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (token.isBlank() || token.trim().length < 6) {
            val errorMsg = "Please enter the complete 6-digit verification code"
            _authState.value = AuthState.Error(errorMsg)
            onResult(false, errorMsg)
            return
        }

        viewModelScope.launch {
            _isVerifyingOtp.value = true
            val result = verifyEmailOtpUseCase(email, token)
            _isVerifyingOtp.value = false
            result.fold(
                onSuccess = { verifiedUser ->
                    _currentUser.value = verifiedUser
                    _authState.value = AuthState.Authenticated(verifiedUser)
                    onResult(true, null)
                },
                onFailure = { error ->
                    val errorMsg = NetworkErrorSanitizer.sanitize(error)
                    _authState.value = AuthState.Error(errorMsg)
                    onResult(false, errorMsg)
                }
            )
        }
    }

    fun resendEmailOtp(email: String, onResult: (Boolean, String) -> Unit) {
        if (_resendCooldownSeconds.value > 0) {
            onResult(false, "Please wait ${_resendCooldownSeconds.value}s before requesting a new email")
            return
        }

        viewModelScope.launch {
            val result = resendEmailOtpUseCase(email)
            result.fold(
                onSuccess = {
                    startResendCooldown(60)
                    onResult(true, "A confirmation email has been sent! Please check your inbox.")
                },
                onFailure = { error ->
                    val msg = NetworkErrorSanitizer.sanitize(error)
                    onResult(false, msg)
                }
            )
        }
    }

    private fun startResendCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _resendCooldownSeconds.value = seconds
            while (_resendCooldownSeconds.value > 0) {
                delay(1000)
                _resendCooldownSeconds.value -= 1
            }
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

    fun setAwaitingOtp(email: String) {
        _authState.value = AuthState.AwaitingOtp(email = email)
        startResendCooldown(60)
    }
}
