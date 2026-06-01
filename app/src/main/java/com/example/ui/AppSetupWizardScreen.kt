package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MeshGradientBackground
import com.example.ui.theme.SandalwoodGold
import com.example.ui.theme.glassCardAdaptive
import com.example.viewmodel.ErpViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppSetupWizardScreen(viewModel: ErpViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Setup permission states
    var isLocationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isNotificationGranted by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Launchers
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.updatePermissionStatus(isLocationGranted, isNotificationGranted)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationGranted = isGranted
        viewModel.updatePermissionStatus(isLocationGranted, isNotificationGranted)
    }

    // Active screen step: 1 = View Mode, 2 = Notifications/Alarm, 3 = Location
    var currentStep by remember { mutableStateOf(1) }

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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header block
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.logo),
                        contentDescription = "Abielan Setup Logo",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Abielan ERP Setup",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Customize your workplace configuration inside the startup engine",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Step Indicator Stepper
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        StepIndicator(stepNumber = 1, activeStep = currentStep, label = "Theme", icon = Icons.Filled.Palette)
                        HorizontalDivider(modifier = Modifier.width(20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StepIndicator(stepNumber = 2, activeStep = currentStep, label = "Notification", icon = Icons.Filled.NotificationsActive)
                        HorizontalDivider(modifier = Modifier.width(20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        StepIndicator(stepNumber = 3, activeStep = currentStep, label = "Location", icon = Icons.Filled.MyLocation)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content area switching with animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() with
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() with
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }.using(SizeTransform(clip = false))
                        },
                        label = "WizardStepTransition"
                    ) { step ->
                        when (step) {
                            1 -> StepThemeContent(state.isDarkMode) { isDark ->
                                viewModel.setThemeMode(isDark)
                            }
                            2 -> StepNotificationContent(
                                isDarkMode = state.isDarkMode,
                                isNotificationGranted = isNotificationGranted,
                                onRequestPermission = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        isNotificationGranted = true
                                    }
                                }
                            )
                            3 -> StepLocationContent(
                                isDarkMode = state.isDarkMode,
                                isLocationGranted = isLocationGranted,
                                onRequestPermission = {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation controls footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(50.dp).weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Go Back")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (currentStep < 3) {
                        Button(
                            onClick = { currentStep++ },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(50.dp).weight(1f).padding(start = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Continue", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Continue")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.completeSetup(state.isDarkMode, isLocationGranted, isNotificationGranted)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SandalwoodGold),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(50.dp).weight(1.3f).padding(start = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, contentDescription = "Done")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Complete Setup", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicator(stepNumber: Int, activeStep: Int, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val isCompleted = activeStep > stepNumber
    val isActive = activeStep == stepNumber
    val isPending = activeStep < stepNumber

    val containerColor = when {
        isCompleted -> Color(0xFF10B981)
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isCompleted -> Color.White
        isActive -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Filled.Check else icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive || isCompleted) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun StepThemeContent(isDarkMode: Boolean, onThemeChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAdaptive(isDarkMode = isDarkMode),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Step 1: Workplace Theme Mode",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Configure your primary lighting interface. Toggle modes dynamically to test visual readability.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Light Mode preview card
                ThemePreviewCard(
                    isDarkLabel = false,
                    isSelected = !isDarkMode,
                    onClick = { onThemeChange(false) },
                    modifier = Modifier.weight(1f)
                )

                // Dark Mode preview card
                ThemePreviewCard(
                    isDarkLabel = true,
                    isSelected = isDarkMode,
                    onClick = { onThemeChange(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ThemePreviewCard(isDarkLabel: Boolean, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val cardBackground = if (isDarkLabel) Color(0xFF0C2B1D) else Color(0xFFFFFFFF)
    val cardText = if (isDarkLabel) Color.White else Color(0xFF111827)
    val borderBrush = if (isSelected) {
        SolidColorBrush(SandalwoodGold)
    } else {
        SolidColorBrush(Color.Transparent)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                brush = if (isSelected) Brush.linearGradient(listOf(SandalwoodGold, Color(0xFFC59B27))) else Brush.linearGradient(listOf(Color.LightGray.copy(alpha = 0.3f), Color.LightGray.copy(alpha = 0.1f))),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isDarkLabel) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = null,
                tint = if (isSelected) SandalwoodGold else cardText.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isDarkLabel) "Deep Emerald" else "Sandalwood Warm",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = cardText
            )
            Text(
                text = if (isDarkLabel) "Dark Mode" else "Light Mode",
                fontSize = 10.sp,
                color = cardText.copy(alpha = 0.6f)
            )
        }
    }
}

// Solid Color utility helper for borders
fun SolidColorBrush(color: Color) = Brush.linearGradient(listOf(color, color))

@Composable
fun StepNotificationContent(isDarkMode: Boolean, isNotificationGranted: Boolean, onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAdaptive(isDarkMode = isDarkMode),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Step 2: Alarms & Timers Permits",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Abielan ERP utilizes active system alarms to notify you the instant a billable hourly Sales Order or custom Recurring Job completes its countdown, avoiding lost work units.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Status Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isNotificationGranted) Color(0xFF10B981).copy(alpha = 0.12f)
                        else Color(0xFFF59E0B).copy(alpha = 0.12f)
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isNotificationGranted) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (isNotificationGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isNotificationGranted) "System Notifications: ENABLED" else "System Notifications: DISABLED",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isNotificationGranted) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNotificationGranted) Color.Gray.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary
                ),
                enabled = !isNotificationGranted,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = if (isNotificationGranted) "Permission Already Approved ✓" else "Request Notification Permission",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isNotificationGranted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun StepLocationContent(isDarkMode: Boolean, isLocationGranted: Boolean, onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAdaptive(isDarkMode = isDarkMode),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SandalwoodGold.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = SandalwoodGold,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Step 3: Verification & Logistics Location",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Location configuration is required for automatic regional GST taxation limits computation, company billing-address compliance, and regional heavy logistics dispatch distance estimates.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Status Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isLocationGranted) Color(0xFF10B981).copy(alpha = 0.12f)
                        else Color(0xFFF59E0B).copy(alpha = 0.12f)
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLocationGranted) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (isLocationGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLocationGranted) "Location Access: PERMITTED" else "Location Access: PENDING/RESTRICTED",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isLocationGranted) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLocationGranted) Color.Gray.copy(alpha = 0.2f) else SandalwoodGold
                ),
                enabled = !isLocationGranted,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = if (isLocationGranted) "Location Approved ✓" else "Request Location Permission",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isLocationGranted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color.White
                )
            }
        }
    }
}
