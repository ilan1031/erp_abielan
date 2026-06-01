package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.glassCardAdaptive
import com.example.ui.theme.CorporateSurfaceDark
import com.example.ui.theme.CorporateSurfaceLight
import com.example.viewmodel.ErpViewModel
import kotlin.math.roundToInt

@Composable
fun AiFloatingConsoleButton(viewModel: ErpViewModel) {
    var showAiDashboard by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { showAiDashboard = true },
            containerColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .padding(bottom = 60.dp) // Offset above the bottom navigation bar safely
        ) {
            Icon(
                imageVector = Icons.Filled.SupportAgent,
                contentDescription = "Workspace Assistant",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showAiDashboard) {
        Dialog(onDismissRequest = { showAiDashboard = false }) {
            AiAssistantDialogContent(
                viewModel = viewModel,
                onDismiss = { showAiDashboard = false }
            )
        }
    }
}

@Composable
fun AiAssistantDialogContent(
    viewModel: ErpViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (state.isDarkMode) CorporateSurfaceDark.copy(alpha = 0.90f) else CorporateSurfaceLight.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(8.dp)
            .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.SupportAgent,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Abielan Consultation Desk",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }

            Text(
                "Real-time corporate ledger auditing, budget anomalies analysis, and tax consultations.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            // Dynamic Scroll Dialogue Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(if (state.isDarkMode) Color(0xFF030712).copy(alpha = 0.4f) else Color(0xFFF3F4F6).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (state.isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                if (state.aiLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Analyzing macro enterprise variables...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else if (state.aiResponse.isBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Icon(
                            imageVector = Icons.Filled.Forum, 
                            contentDescription = null, 
                            modifier = Modifier.size(48.dp), 
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Text(
                            "Ask Abielan Advisor anything about your enterprise:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        // Quick prompt chips
                        val prompts = listOf(
                            "Draft advice on saving corporate tax using GST.",
                            "Explain cost depreciation strategies.",
                            "Recommend ways to improve payment dues."
                        )

                        prompts.forEach { p ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                elevation = CardDefaults.cardElevation(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setAiPromptText(p)
                                        viewModel.askAdvisoryAgent()
                                    }
                                    .glassCardAdaptive(shape = RoundedCornerShape(10.dp), isDarkMode = state.isDarkMode)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(p, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                } else {
                    // Show scrollable response markdown
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = state.aiResponse,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Query Input Form
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.aiPrompt,
                    onValueChange = { viewModel.setAiPromptText(it) },
                    placeholder = { Text("Consult workspace advisor...", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 2,
                    trailingIcon = {
                        if (state.aiPrompt.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setAiPromptText("") }) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.askGenePrompt() },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .size(48.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// Bypassing compiler matching dynamically for viewModel queries
fun ErpViewModel.askGenePrompt() {
    this.askAdvisoryAgent()
}
