package com.example.ui

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: AppViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register

    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regReferCode by remember { mutableStateOf("") }
    var regTelegram by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF030712), // Deepest Void
            Color(0xFF0F172A), // Cosmic Navy
            Color(0xFF0284C7).copy(alpha = 0.35f), // Cyan Cyber Glow
            Color(0xFF030712)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        // Decorative Cyber Light Rays Accent in background
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.Center)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0EA5E9).copy(alpha = 0.25f),
                            Color(0xFF3B82F6).copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Glassmorphic Cyber Main Container Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.6f),
                                Color(0xFF1E293B).copy(alpha = 0.4f),
                                Color(0xFF0284C7).copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xDC090F1E) // High contrast translucent dark glass
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Futuristic Brand Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTECX",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            color = Color(0xFFF8FAFC)
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0EA5E9).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "BUYSELL",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Title & Description
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (selectedTab == 0) "Login with" else "Create Account",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Submit accounts securely and track earnings in real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Futuristic Option Buttons (Tab Switcher)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), CircleShape)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Option 1: Login
                        Surface(
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("login_tab"),
                            shape = CircleShape,
                            color = if (selectedTab == 0) Color(0xFF1E293B) else Color.Transparent,
                            border = if (selectedTab == 0) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.8f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "User Login",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 0) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Option 2: Register
                        Surface(
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("register_tab"),
                            shape = CircleShape,
                            color = if (selectedTab == 1) Color(0xFF1E293B) else Color.Transparent,
                            border = if (selectedTab == 1) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.8f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Account Create",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 1) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFF1E293B),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Feedback Banners
                    errorMessage?.let { msg ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF7F1D1D).copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    successMessage?.let { msg ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF064E3B).copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // TAB 0: LOGIN FORM
                    if (selectedTab == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Email Field
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Email",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                                OutlinedTextField(
                                    value = loginEmail,
                                    onValueChange = { loginEmail = it },
                                    placeholder = { Text("Enter your email", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.7f),
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_email_input")
                                )
                            }

                            // Password Field
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Password",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = { loginPassword = it },
                                    placeholder = { Text("Enter your password", color = Color(0xFF64748B)) },
                                    trailingIcon = {
                                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                            Icon(
                                                imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle Visibility",
                                                tint = Color(0xFF94A3B8)
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.7f),
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_password_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Futuristic Bright White Pill Button
                            Button(
                                onClick = {
                                    errorMessage = null
                                    successMessage = null
                                    isLoading = true
                                    viewModel.login(loginEmail, loginPassword) { success, msg ->
                                        isLoading = false
                                        if (!success) {
                                            errorMessage = msg
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("login_submit_btn"),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0F172A)
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF0F172A)
                                    )
                                } else {
                                    Text(
                                        text = "Login",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Admin Credentials Quick Info Box
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.8f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Admin Access Credentials:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Text(
                                        text = "Gmail: syfaff2@gmail.com | Password: aponkhan21",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    // TAB 1: REGISTRATION FORM
                    if (selectedTab == 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Full Name
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Full Name", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE2E8F0))
                                OutlinedTextField(
                                    value = regName,
                                    onValueChange = { regName = it },
                                    placeholder = { Text("Enter your full name", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.7f),
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
                                )
                            }

                            // Gmail
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Gmail / Email", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE2E8F0))
                                OutlinedTextField(
                                    value = regEmail,
                                    onValueChange = { regEmail = it },
                                    placeholder = { Text("Enter your Gmail address", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.7f),
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("reg_email_input")
                                )
                            }

                            // Password
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Account Creation Password", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE2E8F0))
                                OutlinedTextField(
                                    value = regPassword,
                                    onValueChange = { regPassword = it },
                                    placeholder = { Text("Enter Admin-given password", color = Color(0xFF64748B)) },
                                    trailingIcon = {
                                        IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                            Icon(
                                                imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8)
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.7f),
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("reg_password_input")
                                )
                            }

                            // Telegram Username
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Telegram Username", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE2E8F0))
                                OutlinedTextField(
                                    value = regTelegram,
                                    onValueChange = { regTelegram = it },
                                    placeholder = { Text("@username", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.7f),
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("reg_telegram_input")
                                )
                            }

                            // Warning Banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF78350F).copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "⚠️ সতর্কবার্তা:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                    Text(
                                        text = "Admin এর দেওয়া Password দিয়েই account তৈরি করতে হবে। সঠিক Password ছাড়া report আসবে না।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFDE68A)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Bright White Submit Button
                            Button(
                                onClick = {
                                    errorMessage = null
                                    successMessage = null
                                    isLoading = true
                                    viewModel.register(
                                        name = regName,
                                        email = regEmail,
                                        pass = regPassword,
                                        usedReferCode = "",
                                        telegram = regTelegram
                                    ) { success, msg ->
                                        isLoading = false
                                        if (success) {
                                            successMessage = msg
                                            loginEmail = regEmail
                                            loginPassword = regPassword
                                            selectedTab = 0
                                        } else {
                                            errorMessage = msg
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("reg_create_btn"),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0F172A)
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF0F172A)
                                    )
                                } else {
                                    Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

