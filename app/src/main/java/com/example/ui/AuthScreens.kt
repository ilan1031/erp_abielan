package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.AuthState
import com.example.viewmodel.ErpViewModel
import com.example.ui.theme.MeshGradientBackground
import com.example.ui.theme.glassCardAdaptive
import com.example.ui.theme.SandalwoodGold
import com.example.ui.theme.SandalwoodGoldDark
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.CorporateSurfaceDark
import com.example.ui.theme.CorporateSurfaceLight

@Composable
fun AuthScreensModule(viewModel: ErpViewModel, currentScreen: String) {
    val scrollState = rememberScrollState()
    val state by viewModel.uiState.collectAsState()

    MeshGradientBackground(isDarkMode = state.isDarkMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large Header Abiel Logo block on Splash/Login
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.logo),
                        contentDescription = "Abielan Logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Abielan ERP",
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-1).sp
                    )
                }
                
                Text(
                    text = "Aesthetic Enterprise & Financial Solution",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 28.dp),
                    letterSpacing = 0.5.sp
                )

                // Dynamic view selector
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    when (currentScreen) {
                        "SPLASH" -> SplashView(viewModel)
                        "LOGIN" -> LoginView(viewModel)
                        "SIGNUP" -> SignupView(viewModel)
                        "PLANS" -> SubscriptionPlansView(viewModel)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Powered by Abielan Tech. clickable footer link
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Powered by ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Abielan Tech.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            try {
                                uriHandler.openUri("https://abielan.in")
                            } catch (e: Exception) {}
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Razorpay Loader overlay
            if (state.razorpaySimulating) {
                Dialog(onDismissRequest = {}) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.isDarkMode) Color(0xFF1C1C1E) else Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(12.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(0.9f)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "Contacting Razorpay Gateway...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Establishing transaction channel securely...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Success dialog overlay
            state.razorpaySuccessMessage?.let { msg ->
                Dialog(onDismissRequest = { viewModel.loginUser(state.auth.email, "123456") }) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.isDarkMode) Color(0xFF1C1C1E) else Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Success Status",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                "Transaction Complete",
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                msg,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.loginUser(state.auth.email, "123456") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Launch Abielan Workspace", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashView(viewModel: ErpViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to your Elite Center",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Highlight Features list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureRow(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Minimalist Cashflows",
                    desc = "Clear revenue, expenses, and invoices without noise."
                )
                FeatureRow(
                    icon = Icons.Filled.Timer,
                    title = "Smart Billing Timers",
                    desc = "Track billable minutes, trigger alarms, auto-generate PDF invoices."
                )
                FeatureRow(
                    icon = Icons.Filled.SupportAgent,
                    title = "Business Advisory Desk",
                    desc = "On-demand corporate guidance, tax advisory, and analytics summary."
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Button(
                onClick = { viewModel.setAuthScreen("LOGIN") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Get Started", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Proceed",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Create new company structure Instead",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setAuthScreen("SIGNUP") }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SandalwoodGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SandalwoodGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun LoginView(viewModel: ErpViewModel) {
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("admin@abielan.erp") }
    var password by remember { mutableStateOf("abielanadmin123") }
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
    ) {
        Column(modifier = Modifier.padding(26.dp)) {
            Text(
                text = "Sign In",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
                letterSpacing = (-0.5).sp
            )

            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Business Email") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Secret Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(image, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("Remember Device", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                }

                Text(
                    "Forgot?",
                    fontSize = 13.sp,
                    color = SandalwoodGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { /* Simulated click */ }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.loginUser(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Login to Enterprise", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    " SECURE BY ABIELAN ",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("New to Abielan? ", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                Text(
                    "Register Business",
                    color = SandalwoodGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable { viewModel.setAuthScreen("SIGNUP") }
                )
            }
        }
    }
}

@Composable
fun SignupView(viewModel: ErpViewModel) {
    val state by viewModel.uiState.collectAsState()
    var compName by remember { mutableStateOf("") }
    var ceoName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("") }
    var selectedBizType by remember { mutableStateOf("Consultancy & Technology services") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val bizTypes = listOf(
        "Consultancy & Technology services",
        "E-Commerce & Retail",
        "Manufacturing & Industry",
        "Medical & Healthcare",
        "Transport & Heavy Logistics",
        "Capital Finance & Accounting Office"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
    ) {
        Column(modifier = Modifier.padding(26.dp)) {
            Text(
                text = "Register Enterprise",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
                letterSpacing = (-0.5).sp
            )

            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )

            OutlinedTextField(
                value = compName,
                onValueChange = { compName = it },
                label = { Text("Company Name*") },
                leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = ceoName,
                onValueChange = { ceoName = it },
                label = { Text("Owner / CEO Name*") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = { Text("Mobile Number*") },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Corporate Email*") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Security Password*") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = gst,
                onValueChange = { gst = it },
                label = { Text("GST ID (Optional)") },
                leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                placeholder = { Text("e.g. 27AAAAA0000A1Z1") },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Dropdown Selector for Business Type
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedBizType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Business Category") },
                    leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                )
                // Transparent clickable overlay for robust dropdown interaction
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expandedDropdown = !expandedDropdown }
                )
                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(if (state.isDarkMode) Color(0xFF1C1C1E) else Color.White)
                ) {
                    bizTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp) },
                            onClick = {
                                selectedBizType = type
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (compName.isNotBlank() && email.isNotBlank()) {
                        viewModel.signupUser(compName, ceoName, mobile, email, gst, selectedBizType)
                    } else {
                        // Demo bypass with gorgeous default inputs
                        viewModel.signupUser("Abielan Corp Systems", ceoName.ifBlank { "Abielan Partner" }, mobile.ifBlank { "+91 9000000001" }, email.ifBlank { "partner@abielan.co" }, gst, selectedBizType)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Proceed to Subscriptions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Back to Corporate Login",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.logout() }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SubscriptionPlansView(viewModel: ErpViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Abielan ERP Premium Workspace Licenses",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            letterSpacing = (-0.5).sp
        )
        Text(
            "Acquire full analytical capability, accounting controls, and automated recurring billing timers.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 16.dp)
        )

        // Three dynamic pricing cards
        PricingCard(
            name = "Starter Licenses",
            price = "₹189 / 1st mo",
            originalPrice = "₹681/mo",
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            features = listOf(
                "User Limit: 2 active admins",
                "Invoice Limit: Up to 50 / month",
                "Storage Limit: 2 GB cloud capacity",
                "Recurring SO Limit: Parallel limits (Up to 2)",
                "Dashboard Insights: Basic grids only",
                "Zoho-equivalent Standard Plan (₹749 - ₹68)",
                "Special promo: ₹189 for the 1st Month"
            ),
            onSelect = { viewModel.selectSubscriptionPlan("Starter Plan") },
            isDarkMode = state.isDarkMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        PricingCard(
            name = "Business Licenses",
            price = "₹1431/mo",
            color = SandalwoodGold.copy(alpha = 0.15f),
            isPopular = true,
            features = listOf(
                "User Limit: 10 active administrators",
                "Invoice Limit: Unlimited Invoicing Flow",
                "Storage Limit: 15 GB corporate server",
                "Recurring SO Limit: Max (Up to 4 simultaneously)",
                "Dashboard: Full Bar/Pie/Line Analytics",
                "Export Reports: PDF, Excel, CSV formats",
                "Zoho-equivalent Professional Plan (₹1499 - ₹68)"
            ),
            onSelect = { viewModel.selectSubscriptionPlan("Business Plan") },
            isDarkMode = state.isDarkMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        PricingCard(
            name = "Enterprise Licenses",
            price = "₹2931/mo",
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            features = listOf(
                "User Limit: Unlimited Corporate Licences",
                "Invoice Limit: Unlimited processing volume",
                "Storage Limit: 100 GB structured space",
                "Recurring SO: Max (Unlimited concurrent run)",
                "Dashboard Analytics: full dynamic charts",
                "Export Reports: automated PDF/Excel/CSV sharing",
                "Priority Team access: SLA 2 Hrs response",
                "Zoho-equivalent Premium Plan (₹2999 - ₹68)"
            ),
            onSelect = { viewModel.selectSubscriptionPlan("Enterprise Plan") },
            isDarkMode = state.isDarkMode
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = { viewModel.loginUser("guest@abielan.in", "guest") },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SandalwoodGold),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(2.dp, SandalwoodGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
        ) {
            Text("Skip & Try Free Edition", fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
fun PricingCard(
    name: String,
    price: String,
    color: Color,
    isPopular: Boolean = false,
    features: List<String>,
    onSelect: () -> Unit,
    isDarkMode: Boolean = true,
    originalPrice: String? = null
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPopular) {
                if (isDarkMode) SandalwoodGold.copy(alpha = 0.16f) else Color(0xFFFDF8F0)
            } else {
                if (isDarkMode) CorporateSurfaceDark else CorporateSurfaceLight
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPopular) 2.dp else 1.dp,
                color = if (isPopular) SandalwoodGold else if (isDarkMode) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = if (isPopular) {
                                if (isDarkMode) SandalwoodGold else SandalwoodGoldDark
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (isPopular) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SandalwoodGold)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                             ) {
                                Text(
                                    "RECOMMENDED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = "Standard pricing parameters limit",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (originalPrice != null) {
                        Text(
                            text = originalPrice,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                    Text(
                        price,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = if (isPopular && !isDarkMode) SandalwoodGoldDark else MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = if (isPopular) {
                    if (isDarkMode) SandalwoodGold.copy(alpha = 0.2f) else SandalwoodGoldDark.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                features.forEach { feat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Included Feature",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feat,
                            fontSize = 11.sp,
                            color = if (isPopular && !isDarkMode) SandalwoodGoldDark.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPopular) {
                        if (isDarkMode) SandalwoodGold else SandalwoodGoldDark
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isPopular) Color.White else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Select & Subscribe Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
