package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.MeshGradientBackground
import com.example.ui.theme.glassCardAdaptive
import com.example.viewmodel.ErpViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ErpAppMain(viewModel: ErpViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    MyApplicationTheme(darkTheme = state.isDarkMode) {
        if (!state.isSetupCompleted) {
            AppSetupWizardScreen(viewModel = viewModel)
        } else if (!state.auth.isLoggedIn) {
            // Unauthenticated Flow
            AuthScreensModule(viewModel = viewModel, currentScreen = state.showAuthScreen)
        } else {
            // Authenticated ERP Workspace
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isWideScreen = configuration.screenWidthDp >= 600

            MeshGradientBackground(isDarkMode = state.isDarkMode) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isWideScreen) {
                        NavigationRail(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = if (state.isDarkMode) Color(0xFF030712).copy(alpha = 0.4f) else Color(0xFFFFFFFF).copy(alpha = 0.6f),
                            header = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.logo),
                                        contentDescription = "Abielan Logo",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Abielan",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = if (state.isDarkMode) {
                                            listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                                        } else {
                                            listOf(Color.Black.copy(alpha = 0.05f), Color.Transparent)
                                        }
                                    ),
                                    shape = androidx.compose.ui.graphics.RectangleShape
                                )
                        ) {
                            val navItems = listOf(
                                Triple("Dashboard", Icons.Filled.Dashboard, "Dashboard"),
                                Triple("Sales", Icons.Filled.Work, "Sales"),
                                Triple("Finance", Icons.Filled.AccountBalance, "Finance"),
                                Triple("Reports", Icons.Filled.Assessment, "Reports"),
                                Triple("Settings", Icons.Filled.Settings, "Settings")
                            )

                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 12.dp)
                                ) {
                                    navItems.forEach { item ->
                                        val isSelected = state.activeTab == item.first
                                        NavigationRailItem(
                                            selected = isSelected,
                                            onClick = { viewModel.setActiveTab(item.first) },
                                            icon = {
                                                Icon(
                                                    imageVector = item.second,
                                                    contentDescription = item.third
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = item.first,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.sp
                                                )
                                            },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }

                                // Sidebar bottom actions - Theme toggle
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.setThemeMode(!state.isDarkMode) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (state.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                            contentDescription = "Toggle Theme",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!isWideScreen) {
                                            androidx.compose.foundation.Image(
                                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.logo),
                                                contentDescription = "Abielan Logo",
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            "Abielan ERP Hub",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp
                                        )
                                    }
                                },
                                actions = {
                                    // Active status pill
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .glassCardAdaptive(shape = RoundedCornerShape(6.dp), isDarkMode = state.isDarkMode)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        )
                                    }
                                    
                                    if (!isWideScreen) {
                                        // Simple theme toggle icon
                                        IconButton(onClick = { viewModel.setThemeMode(!state.isDarkMode) }) {
                                            Icon(
                                                imageVector = if (state.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                                contentDescription = "Toggle Theme"
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = if (state.isDarkMode) Color(0xFF030712).copy(alpha = 0.35f) else Color(0xFFFFFFFF).copy(alpha = 0.45f)
                                )
                            )
                        },
                        bottomBar = {
                            if (!isWideScreen) {
                                NavigationBar(
                                    windowInsets = WindowInsets.navigationBars,
                                    containerColor = if (state.isDarkMode) Color(0xFF030712).copy(alpha = 0.4f) else Color(0xFFFFFFFF).copy(alpha = 0.6f),
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.verticalGradient(
                                                colors = if (state.isDarkMode) {
                                                    listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                                                } else {
                                                    listOf(Color.Black.copy(alpha = 0.05f), Color.Transparent)
                                                }
                                            ),
                                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                        )
                                ) {
                                    val navItems = listOf(
                                        Triple("Dashboard", Icons.Filled.Dashboard, "Dashboard"),
                                        Triple("Sales", Icons.Filled.Work, "Sales"),
                                        Triple("Finance", Icons.Filled.AccountBalance, "Finance"),
                                        Triple("Reports", Icons.Filled.Assessment, "Reports"),
                                        Triple("Settings", Icons.Filled.Settings, "Settings")
                                    )

                                    navItems.forEach { item ->
                                        val isSelected = state.activeTab == item.first
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { viewModel.setActiveTab(item.first) },
                                            icon = {
                                                Icon(
                                                    imageVector = item.second,
                                                    contentDescription = item.third
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = item.first,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 10.sp
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                                unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                                indicatorColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { paddingValues ->
                        Surface(
                            color = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Staggered transitions
                                AnimatedContent(
                                    targetState = state.activeTab,
                                    label = "ScreenTransition"
                                ) { targetTab ->
                                    when (targetTab) {
                                        "Dashboard" -> DashboardScreenModule(viewModel)
                                        "Sales" -> SalesScreenModule(viewModel)
                                        "Finance" -> FinanceScreenModule(viewModel)
                                        "Reports" -> ReportsScreenModule(viewModel)
                                        "Settings" -> SettingsScreenModule(viewModel)
                                        else -> DashboardScreenModule(viewModel)
                                    }
                                }

                                 // Floating Workspace Assistant overlay
                                if (state.isAiAssistantEnabled) {
                                    AiFloatingConsoleButton(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
    }
}
}
