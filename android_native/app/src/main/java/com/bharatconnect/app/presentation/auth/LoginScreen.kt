package com.bharatconnect.app.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.theme.ColorBackground080616
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordInput by remember { mutableStateOf("") }
    var forgotPasswordFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val authState by authViewModel.authState.collectAsState()
    val isResettingPassword by authViewModel.isResettingPassword.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
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
                    .padding(top = 4.dp),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Branding Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFF9933), ColorPrimary6367FF, Color(0xFF138808))),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "Sign In",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to BharatConnect",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Sign in using your Username, Email, or Mobile Number",
                fontSize = 13.sp,
                color = Color(0xFF9E9EB8),
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Error Banner
            if (authState is AuthState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1218)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF6B6B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Identifier Input (Username / Email / Phone)
            OutlinedTextField(
                value = identifier,
                onValueChange = { 
                    identifier = it
                    authViewModel.clearError()
                },
                label = { Text("Username, Email, or Mobile Number") },
                placeholder = { Text("e.g. rahul_99, rahul@mail.com, or 9876543210", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = "User Identity", tint = Color.LightGray)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF2C2A4A),
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    authViewModel.clearError()
                },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Password", tint = Color.LightGray)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Password Visibility",
                            tint = Color.LightGray
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF2C2A4A),
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Forgot Password Button (Right aligned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        forgotPasswordInput = identifier
                        forgotPasswordFeedback = null
                        showForgotPasswordDialog = true
                    }
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = ColorPrimary6367FF,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign In Button
            Button(
                onClick = { authViewModel.login(identifier, password) },
                enabled = authState !is AuthState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign In", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have an account?", color = Color.Gray, fontSize = 14.sp)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Register Now", color = ColorPrimary6367FF, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            TextButton(onClick = onNavigateBack) {
                Text("← Back to Splash", color = Color.LightGray, fontSize = 13.sp)
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text(
                    text = "Reset Password",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your registered Email, Username, or Mobile Number. We will send password reset instructions to your associated email.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (forgotPasswordFeedback != null) {
                        val (isSuccess, msg) = forgotPasswordFeedback!!
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSuccess) Color(0xFF0F3818) else Color(0xFF3B1218)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = msg,
                                color = if (isSuccess) Color(0xFF4EFEAA) else Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = forgotPasswordInput,
                        onValueChange = { 
                            forgotPasswordInput = it
                            forgotPasswordFeedback = null
                        },
                        label = { Text("Email, Username, or Mobile Number") },
                        placeholder = { Text("e.g. user@example.com", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.LightGray)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary6367FF,
                            unfocusedBorderColor = Color(0xFF2C2A4A),
                            focusedLabelColor = ColorPrimary6367FF,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.forgotPassword(forgotPasswordInput) { isSuccess, message ->
                            forgotPasswordFeedback = Pair(isSuccess, message)
                        }
                    },
                    enabled = !isResettingPassword && forgotPasswordInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isResettingPassword) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Reset Link", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Close", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF16142E),
            shape = RoundedCornerShape(18.dp)
        )
    }
}
