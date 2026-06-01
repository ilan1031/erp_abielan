package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.example.ui.theme.glassCardAdaptive
import com.example.viewmodel.AuthState
import com.example.viewmodel.ErpViewModel

@Composable
fun SettingsScreenModule(viewModel: ErpViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var editingCompany by remember { mutableStateOf(state.auth.companyName) }
    var editingOwner by remember { mutableStateOf(state.auth.ownerName) }
    var editingGst by remember { mutableStateOf(state.auth.gstNumber) }

    var selectedCurrency by remember { mutableStateOf("₹ INR") }
    var gstRatePct by remember { mutableStateOf("18%") }
    var notificationsEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(text = "Enterprise Configurations", fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Text(text = "Fine-tune tax structures, visual parameters, user access lists", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 16.dp))

        // --- SECTION 1: VISUAL THEME PARAMETERS ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Theme & Branding Style", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (state.isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Active Theme: ${if (state.isDarkMode) "Dark Corporate" else "Light Professional"}", fontSize = 13.sp)
                    }
                    
                    Switch(
                        checked = state.isDarkMode,
                        onCheckedChange = { viewModel.setThemeMode(it) },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }

        // --- SECTION 2: COMPANY PROFILE PARAMS ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Corporate Demographics", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )

                OutlinedTextField(
                    value = editingCompany,
                    onValueChange = { 
                        editingCompany = it
                        viewModel.updateCompanyDetails(it, editingOwner, editingGst)
                    },
                    label = { Text("Firm Name") },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editingOwner,
                    onValueChange = { 
                        editingOwner = it
                        viewModel.updateCompanyDetails(editingCompany, it, editingGst)
                    },
                    label = { Text("CEO / Board Representative") },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editingGst,
                    onValueChange = { 
                        editingGst = it
                        viewModel.updateCompanyDetails(editingCompany, editingOwner, it)
                    },
                    label = { Text("Tax Registration GST Identification") },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- SECTION 3: ACCOUNTING PARAMETERS & TAXES ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Taxation & Currency Units", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                // Currency switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Display Capital Currency", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("₹ INR", "$ USD").forEach { curr ->
                            val isSel = selectedCurrency == curr
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedCurrency = curr }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(curr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GST Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Standard GST bracket mapping", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("5%", "12%", "18%", "28%").forEach { gst ->
                            val isSel = gstRatePct == gst
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { gstRatePct = gst }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(gst, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 4: NOTIFICATION TRIGGERS ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Alarm & Telemetry Alerts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Real-time Billing completion notifications", fontSize = 13.sp)
                        Text("Show alarm popup when Sales Order timer runs out", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }

        // --- SECTION 4.5: CONTINUATION TIMER OPTIONS ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Alarm Continuation Extension Hours", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Configure how many hours/minutes are allocated automatically when you select the 'Continue' action on a completed timer alarm.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Choice options list
                val options = listOf(
                    Pair(-1, "Original"),
                    Pair(15, "15 min"),
                    Pair(30, "30 min"),
                    Pair(60, "1 hr"),
                    Pair(120, "2 hr"),
                    Pair(240, "4 hr")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    options.forEach { (optionMinutes, label) ->
                        val isSelected = state.continuationDurationMinutes == optionMinutes
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                )
                                .clickable {
                                    viewModel.setContinuationDurationMinutes(optionMinutes)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    "Or set a custom dynamic minutes duration:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val standardPresets = listOf(-1, 15, 30, 60, 120, 240)
                val isCustomSelected = state.continuationDurationMinutes > 0 && state.continuationDurationMinutes !in standardPresets
                var customMinutesText by remember(state.continuationDurationMinutes) {
                    mutableStateOf(if (isCustomSelected) state.continuationDurationMinutes.toString() else "")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = { newVal ->
                            val filtered = newVal.filter { it.isDigit() }
                            customMinutesText = filtered
                            val parsed = filtered.toIntOrNull()
                            if (parsed != null && parsed > 0) {
                                viewModel.setContinuationDurationMinutes(parsed)
                            }
                        },
                        label = { Text("Custom Minutes", fontSize = 11.sp) },
                        placeholder = { Text("e.g., 45, 90") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f),
                        suffix = { Text("min", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                    
                    if (isCustomSelected) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = "ACTIVE CUSTOM",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION 5: TEAM PARAMETERS ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Active Admin Team Members", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                val team = listOf(
                    Pair("Deepak Kumar", "Consultation Lead Super"),
                    Pair("Ananya Sengupta", "Finance General Comptroller"),
                    Pair("Arjun Ramaswamy", "Technical Operations Auditor")
                )

                team.forEach { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(member.first, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(member.second, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Log out button
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ExitToApp, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp), 
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Close Enterprise Session", fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Powered by Abielan Tech. clickable footer link
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
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
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
