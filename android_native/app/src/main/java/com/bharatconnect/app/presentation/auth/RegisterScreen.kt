package com.bharatconnect.app.presentation.auth

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bharatconnect.app.core.storage.CloudinaryManager
import com.bharatconnect.app.core.theme.ColorBackground080616
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.AuthState
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCountryCode by remember { mutableStateOf("+91") }
    var phoneNumber by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Profile Picture / Avatar State
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showCountryDropdown by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    // Gallery Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri
            localError = null
            coroutineScope.launch {
                isUploadingAvatar = true
                val uploadResult = CloudinaryManager.uploadProfilePicture(context, uri)
                isUploadingAvatar = false
                uploadResult.fold(
                    onSuccess = { secureUrl ->
                        avatarUrl = secureUrl
                    },
                    onFailure = { error ->
                        localError = com.bharatconnect.app.core.network.NetworkErrorSanitizer.sanitize(error)
                    }
                )
            }
        }
    }

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
            Spacer(modifier = Modifier.height(12.dp))

            // Profile Picture Picker Section
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFF9933), ColorPrimary6367FF, Color(0xFF138808)))
                    )
                    .padding(3.dp)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF16142E)),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Profile Picture Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Add Avatar",
                                tint = Color(0xFFFF9933),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF9933),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (avatarUrl != null) "✓ Profile Picture Uploaded" else "Tap to Add Profile Picture",
                fontSize = 12.sp,
                color = if (avatarUrl != null) Color(0xFF4EFEAA) else Color(0xFFFF9933),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { imagePickerLauncher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Join BharatConnect",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Fast, secure & encrypted network",
                fontSize = 12.sp,
                color = Color(0xFF9E9EB8),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Error Banner
            val errorMessage = localError ?: (authState as? AuthState.Error)?.message
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1520)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF6B6B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFFD1D1),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 1. Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    localError = null
                },
                label = { Text("Full Name") },
                placeholder = { Text("e.g. Rahul Sharma") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ColorPrimary6367FF) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Username
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }.lowercase()
                    localError = null
                },
                label = { Text("Username") },
                placeholder = { Text("e.g. rahul_sharma99") },
                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = ColorPrimary6367FF) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Email Address
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it.trim()
                    localError = null
                },
                label = { Text("Email Address") },
                placeholder = { Text("e.g. rahul@example.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ColorPrimary6367FF) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Phone Number with Country Code Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country Code Selector
                ExposedDropdownMenuBox(
                    expanded = showCountryDropdown,
                    onExpandedChange = { showCountryDropdown = !showCountryDropdown },
                    modifier = Modifier.width(110.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCountryCode,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCountryDropdown) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary6367FF,
                            unfocusedBorderColor = Color(0xFF262347),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = showCountryDropdown,
                        onDismissRequest = { showCountryDropdown = false },
                        modifier = Modifier.background(Color(0xFF16142E))
                    ) {
                        countryCodes.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.flag} ${item.code} (${item.name})", color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    selectedCountryCode = item.code
                                    showCountryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Phone Number field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it.filter { ch -> ch.isDigit() }
                        localError = null
                    },
                    label = { Text("Mobile Number") },
                    placeholder = { Text("9876543210") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ColorPrimary6367FF) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary6367FF,
                        unfocusedBorderColor = Color(0xFF262347),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = ColorPrimary6367FF,
                        unfocusedLabelColor = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Date of Birth (DOB) Picker
            OutlinedTextField(
                value = dob,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date of Birth (DOB)") },
                placeholder = { Text("Select your birth date") },
                leadingIcon = {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Pick Date",
                        tint = Color(0xFFFF9933),
                        modifier = Modifier.clickable { datePickerDialog.show() }
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.EditCalendar, contentDescription = "Open Calendar", tint = Color.LightGray)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Password
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                label = { Text("Password (min 6 chars)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ColorPrimary6367FF) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = Color.LightGray
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    localError = null
                },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null, tint = ColorPrimary6367FF) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle confirm password visibility",
                            tint = Color.LightGray
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Register Submit Button
            val isLoading = authState is AuthState.Loading || isUploadingAvatar
            Button(
                onClick = {
                    when {
                        fullName.isBlank() -> localError = "Please enter your full name"
                        username.isBlank() -> localError = "Please enter a valid username"
                        username.length < 3 -> localError = "Username must be at least 3 characters"
                        email.isBlank() || !email.contains("@") -> localError = "Please enter a valid email address"
                        phoneNumber.isBlank() -> localError = "Please enter your phone number"
                        dob.isBlank() -> localError = "Please select your date of birth"
                        password.length < 6 -> localError = "Password must be at least 6 characters"
                        password != confirmPassword -> localError = "Passwords do not match"
                        else -> {
                            val fullPhone = "$selectedCountryCode$phoneNumber"
                            authViewModel.register(
                                name = fullName,
                                username = username,
                                email = email,
                                phoneNumber = fullPhone,
                                dob = dob,
                                password = password,
                                avatarUrl = avatarUrl
                            )
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Create BharatConnect Account",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation to Login
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Text(
                    text = "Sign In",
                    color = Color(0xFFFF9933),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
