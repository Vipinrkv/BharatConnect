package com.bharatconnect.app.presentation.auth

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.theme.ColorBackground080616
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.AuthState
import java.util.Calendar

data class CountryCode(val code: String, val name: String, val flag: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCountryCode by remember { mutableStateOf("+91") }
    var phoneNumber by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showCountryDropdown by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val countryCodes = listOf(
        CountryCode("+91", "India", "🇮🇳"),
        CountryCode("+1", "USA / Canada", "🇺🇸"),
        CountryCode("+44", "United Kingdom", "🇬🇧"),
        CountryCode("+971", "UAE", "🇦🇪"),
        CountryCode("+65", "Singapore", "🇸🇬"),
        CountryCode("+61", "Australia", "🇦🇺"),
        CountryCode("+49", "Germany", "🇩🇪"),
        CountryCode("+33", "France", "🇫🇷"),
        CountryCode("+81", "Japan", "🇯🇵"),
        CountryCode("+966", "Saudi Arabia", "🇸🇦"),
        CountryCode("+977", "Nepal", "🇳🇵"),
        CountryCode("+880", "Bangladesh", "🇧🇩"),
        CountryCode("+94", "Sri Lanka", "🇱🇰")
    )

    // DatePicker setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedDay = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
            val formattedMonth = if (month + 1 < 10) "0${month + 1}" else "${month + 1}"
            dob = "$formattedDay/$formattedMonth/$year"
            localError = null
        },
        calendar.get(Calendar.YEAR) - 20,
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onRegisterSuccess()
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
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Branding Header
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFF9933), ColorPrimary6367FF, Color(0xFF138808))),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Register",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Join BharatConnect",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Secure, offline-first communications network",
                fontSize = 13.sp,
                color = Color(0xFF9E9EB8),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Error Banner
            val errorMessage = localError ?: (authState as? AuthState.Error)?.message
            if (errorMessage != null) {
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
                            text = errorMessage,
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 1. Full Name Input
            OutlinedTextField(
                value = fullName,
                onValueChange = { 
                    fullName = it
                    localError = null
                    authViewModel.clearError()
                },
                label = { Text("Full Name *") },
                placeholder = { Text("e.g. Aarav Sharma", color = Color.Gray) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Full Name", tint = Color.LightGray)
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

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Username Input
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it.lowercase().filter { char -> char.isLetterOrDigit() || char == '_' }
                    localError = null
                    authViewModel.clearError()
                },
                label = { Text("Username *") },
                placeholder = { Text("e.g. aarav_99", color = Color.Gray) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = "Username", tint = Color.LightGray)
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

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    localError = null
                    authViewModel.clearError()
                },
                label = { Text("Email Address *") },
                placeholder = { Text("e.g. aarav@example.com", color = Color.Gray) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = Color.LightGray)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Phone Number with Country Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Country Code Picker Box
                Box(modifier = Modifier.width(110.dp)) {
                    OutlinedTextField(
                        value = selectedCountryCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Code") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Country Code",
                                tint = Color.LightGray,
                                modifier = Modifier.clickable { showCountryDropdown = true }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary6367FF,
                            unfocusedBorderColor = Color(0xFF2C2A4A),
                            focusedLabelColor = ColorPrimary6367FF,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCountryDropdown = true }
                    )

                    DropdownMenu(
                        expanded = showCountryDropdown,
                        onDismissRequest = { showCountryDropdown = false },
                        modifier = Modifier.background(Color(0xFF16142E))
                    ) {
                        countryCodes.forEach { country ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${country.flag} ${country.name} (${country.code})",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    selectedCountryCode = country.code
                                    showCountryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Phone Number Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { 
                        phoneNumber = it.filter { char -> char.isDigit() }
                        localError = null
                        authViewModel.clearError()
                    },
                    label = { Text("Mobile Number *") },
                    placeholder = { Text("9876543210", color = Color.Gray) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone", tint = Color.LightGray)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Date of Birth (DOB)
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Date of Birth (DOB) *") },
                placeholder = { Text("DD/MM/YYYY", color = Color.Gray) },
                readOnly = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "DOB", tint = Color.LightGray)
                },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = ColorPrimary6367FF)
                    }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    localError = null
                    authViewModel.clearError()
                },
                label = { Text("Password (min 6 chars) *") },
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

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Confirm Password Input
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    localError = null
                    authViewModel.clearError()
                },
                label = { Text("Confirm Password *") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.LockClock, contentDescription = "Confirm Password", tint = Color.LightGray)
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Confirm Password Visibility",
                            tint = Color.LightGray
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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

            Spacer(modifier = Modifier.height(24.dp))

            // Register Button
            Button(
                onClick = {
                    val fullPhone = "$selectedCountryCode$phoneNumber"
                    when {
                        fullName.isBlank() -> localError = "Please enter your Full Name"
                        username.isBlank() -> localError = "Please choose a Username"
                        username.length < 3 -> localError = "Username must be at least 3 characters"
                        email.isBlank() || !email.contains("@") -> localError = "Please enter a valid Email Address"
                        phoneNumber.isBlank() || phoneNumber.length < 7 -> localError = "Please enter a valid Mobile Number"
                        dob.isBlank() -> localError = "Please select your Date of Birth (DOB)"
                        password.length < 6 -> localError = "Password must be at least 6 characters"
                        password != confirmPassword -> localError = "Passwords do not match"
                        else -> {
                            authViewModel.register(
                                name = fullName,
                                username = username,
                                email = email,
                                phoneNumber = fullPhone,
                                dob = dob,
                                password = password
                            )
                        }
                    }
                },
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
                    Text("Create Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account?", color = Color.Gray, fontSize = 14.sp)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Sign In", color = ColorPrimary6367FF, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            TextButton(onClick = onNavigateBack) {
                Text("← Back to Splash", color = Color.LightGray, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
