package com.bharatconnect.app.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.theme.ColorBackground080616
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    email: String,
    authViewModel: AuthViewModel,
    onVerificationSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var resendFeedback by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsState()
    val isVerifying by authViewModel.isVerifyingOtp.collectAsState()
    val resendCooldown by authViewModel.resendCooldownSeconds.collectAsState()

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onVerificationSuccess()
        }
    }

    // Email mask helper (e.g. rahul@example.com -> r***l@example.com)
    val maskedEmail = remember(email) {
        if (email.contains("@")) {
            val parts = email.split("@")
            val user = parts[0]
            val domain = parts[1]
            if (user.length > 2) {
                "${user.first()}***${user.last()}@$domain"
            } else {
                "${user.first()}***@$domain"
            }
        } else {
            email
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground080616),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        authViewModel.clearError()
                        onNavigateBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email Confirmation Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF1E1B4B), shape = RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Check Your Email",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We have sent a confirmation email to",
                fontSize = 14.sp,
                color = Color(0xFFA0A3BD),
                textAlign = TextAlign.Center
            )

            Text(
                text = maskedEmail,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF60A5FA),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deep link direct action card
            Surface(
                color = Color(0xFF15102A),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262347)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF818CF8),
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Waiting for email confirmation...",
                            color = Color(0xFFC7D2FE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Open your email app and tap 'Confirm your email' to instantly activate your BharatConnect account.",
                        color = Color(0xFFA0A3BD),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider or Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF262347))
                Text(
                    text = "  OR ENTER 6-DIGIT CODE  ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF262347))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6-Digit Interactive OTP Box UI
            BasicTextField(
                value = otpCode,
                onValueChange = { newValue ->
                    if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                        otpCode = newValue
                        localError = null
                        resendFeedback = null
                        if (newValue.length == 6) {
                            focusManager.clearFocus()
                            authViewModel.verifyEmailOtp(email, newValue)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (otpCode.length == 6) {
                            authViewModel.verifyEmailOtp(email, otpCode)
                        }
                    }
                ),
                modifier = Modifier.focusRequester(focusRequester),
                decorationBox = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 6) {
                            val char = otpCode.getOrNull(i)?.toString() ?: ""
                            val isFocused = otpCode.length == i || (i == 5 && otpCode.length == 6)
                            val hasChar = char.isNotEmpty()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .background(
                                        color = if (hasChar) Color(0xFF1E1838) else Color(0xFF140F26),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) ColorPrimary6367FF else if (hasChar) Color(0xFF4F46E5) else Color(0xFF2E264E),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error Display Banner
            val errorMessage = localError ?: (authState as? AuthState.Error)?.message
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = Color(0xFF3B1219),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Resend Feedback Banner
            if (!resendFeedback.isNullOrBlank()) {
                Surface(
                    color = Color(0xFF0F3822),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resendFeedback!!,
                        color = Color(0xFF4ADE80),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Verify Submit Button
            if (otpCode.isNotEmpty()) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (otpCode.length == 6) {
                            authViewModel.verifyEmailOtp(email, otpCode)
                        } else {
                            localError = "Please enter all 6 digits of the verification code"
                        }
                    },
                    enabled = !isVerifying,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Verify Code",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Resend Email Section
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn't receive email? ",
                    color = Color(0xFFA0A3BD),
                    fontSize = 13.sp
                )
                if (resendCooldown > 0) {
                    Text(
                        text = "Resend in ${resendCooldown}s",
                        color = Color(0xFF818CF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Resend Email",
                        color = Color(0xFFFF9933),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            localError = null
                            authViewModel.resendEmailOtp(email) { _, message ->
                                resendFeedback = message
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Wrong email address? Change Email",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.clickable {
                    authViewModel.clearError()
                    onNavigateBack()
                }
            )
        }
    }
}
