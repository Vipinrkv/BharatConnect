package com.bharatconnect.app.presentation.auth

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
    onNavigateToOtp: (String) -> Unit,
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
    val photoPickerLauncher = rememberLauncherForActivityResult(
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
        when (val state = authState) {
            is AuthState.Authenticated -> onRegisterSuccess()
            is AuthState.AwaitingOtp -> onNavigateToOtp(state.email)
            else -> {}
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

            Spacer(modifier = Modifier.height(4.dp))

            // Profile Picture Picker Section
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1838))
                    .clickable { photoPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null || avatarUri != null) {
                    AsyncImage(
                        model = avatarUrl ?: avatarUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload Avatar",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Photo",
                            color = Color(0xFFA0A3BD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (isUploadingAvatar) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF818CF8),
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (avatarUrl != null) "✓ Profile Picture Uploaded" else "Add Profile Photo (Optional)",
                fontSize = 12.sp,
                color = if (avatarUrl != null) Color(0xFF4ADE80) else Color(0xFF818CF8),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Join BharatConnect",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Fast, secure & encrypted network",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Local or Server Error Banner
            val errorMessage = localError ?: (authState as? AuthState.Error)?.message
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = Color(0xFF3B1219),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 1. Full Name Field
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    localError = null
                },
                label = { Text("Full Name") },
                placeholder = { Text("e.g. Vipin Vishwakarma") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF818CF8)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF140F26),
                    unfocusedContainerColor = Color(0xFF0F0B1E),
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Username Field
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.lowercase().filter { char -> char.isLetterOrDigit() || char == '_' }
                    localError = null
                },
                label = { Text("Username") },
                placeholder = { Text("e.g. vipinrkv25") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = null,
                        tint = Color(0xFF818CF8)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF140F26),
                    unfocusedContainerColor = Color(0xFF0F0B1E),
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                label = { Text("Email Address") },
                placeholder = { Text("e.g. virrkv25@gmail.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFF818CF8)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF140F26),
                    unfocusedContainerColor = Color(0xFF0F0B1E),
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Phone Number with Country Code Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country Code Selector Box
                Box(
                    modifier = Modifier
                        .width(105.dp)
                        .height(56.dp)
                        .background(Color(0xFF0F0B1E), shape = RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF262347), shape = RoundedCornerShape(14.dp))
                        .clickable { showCountryDropdown = true }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedCountryCode,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Country Code",
                            tint = Color(0xFF818CF8)
                        )
                    }

                    DropdownMenu(
                        expanded = showCountryDropdown,
                        onDismissRequest = { showCountryDropdown = false },
                        modifier = Modifier.background(Color(0xFF15102A))
                    ) {
                        countryCodes.forEach { country ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${country.flag} ${country.name} (${country.code})",
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

                // Phone Input Field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it.filter { char -> char.isDigit() }
                        localError = null
                    },
                    label = { Text("Mobile Number") },
                    placeholder = { Text("8261867326") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF818CF8)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF140F26),
                        unfocusedContainerColor = Color(0xFF0F0B1E),
                        focusedBorderColor = ColorPrimary6367FF,
                        unfocusedBorderColor = Color(0xFF262347),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = ColorPrimary6367FF,
                        unfocusedLabelColor = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Date of Birth (DOB) Field with Calendar Picker
            OutlinedTextField(
                value = dob,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date of Birth (DOB)") },
                placeholder = { Text("DD/MM/YYYY") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFFFFA500)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Pick Date",
                            tint = Color(0xFF818CF8)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF140F26),
                    unfocusedContainerColor = Color(0xFF0F0B1E),
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                label = { Text("Password (min 6 chars)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF818CF8)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.Gray
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF140F26),
                    unfocusedContainerColor = Color(0xFF0F0B1E),
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF262347),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ColorPrimary6367FF,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    localError = null
                },
                label = { Text("Confirm Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = null,
                        tint = Color(0xFF818CF8)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            tint = Color.Gray
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF140F26),
                    unfocusedContainerColor = Color(0xFF0F0B1E),
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
