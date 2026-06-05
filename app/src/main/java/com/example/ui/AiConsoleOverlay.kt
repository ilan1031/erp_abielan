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
import androidx.compose.ui.platform.LocalDensity
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
    val state by viewModel.uiState.collectAsState()
    
    var showAiDashboard by remember { mutableStateOf(false) }
    
    // Draggable position coordinates for the compact Fab Button itself
    var buttonOffsetX by remember { mutableStateOf(0f) }
    var buttonOffsetY by remember { mutableStateOf(0f) }
    
    // Draggable position coordinates for the expanded Consultation Dialogue Card
    var dialogueOffsetX by remember { mutableStateOf(0f) }
    var dialogueOffsetY by remember { mutableStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        
        // Dynamic dimensions based on customizable user settings popup
        val cardWidthDp = when (state.aiOverlaySize) {
            "Small" -> 290.dp
            "Large" -> 375.dp
            else -> 335.dp // Medium default
        }.coerceAtMost(maxWidth - 12.dp)

        val cardHeightDp = when (state.aiOverlaySize) {
            "Small" -> 370.dp
            "Large" -> 535.dp
            else -> 465.dp // Medium default
        }.coerceAtMost(maxHeight - 90.dp)

        val cardWidthPx = with(density) { cardWidthDp.toPx() }
        val cardHeightPx = with(density) { cardHeightDp.toPx() }
        
        // --- BUTTON DRAG SETUP ---
        val buttonSizePx = with(density) { 56.dp.toPx() }
        val marginPx = with(density) { 16.dp.toPx() }
        val bottomMarginPx = with(density) { (16.dp + 60.dp).toPx() }

        val minBtnX = -(widthPx - 2 * marginPx - buttonSizePx)
        val maxBtnX = 0f
        val minBtnY = -(heightPx - marginPx - bottomMarginPx - buttonSizePx)
        val maxBtnY = 0f

        // --- DIALOGUE DRAG CLAMP CALCULATION ---
        val boundsSpace = state.aiOverlayMovingSpace
        
        val marginPaddingPx = with(density) { 8.dp.toPx() }
        val bottomBarPaddingPx = with(density) { 68.dp.toPx() }
        val topBarPaddingPx = with(density) { 48.dp.toPx() }

        // Within Margins: Clamped strictly on stage
        val minDlgX_clamped = -(widthPx - cardWidthPx) / 2f + marginPaddingPx
        val maxDlgX_clamped = (widthPx - cardWidthPx) / 2f - marginPaddingPx
        val minDlgY_clamped = -(heightPx - cardHeightPx) / 2f + topBarPaddingPx
        val maxDlgY_clamped = (heightPx - cardHeightPx) / 2f - bottomBarPaddingPx

        // Full Screen: Generous/freely move offstage boundary bleed
        val minDlgX_generous = -widthPx / 2f + with(density) { 80.dp.toPx() }     // at least 80dp remains on screen
        val maxDlgX_generous = widthPx / 2f - with(density) { 80.dp.toPx() }
        val minDlgY_generous = -heightPx / 2f + with(density) { 80.dp.toPx() }
        val maxDlgY_generous = heightPx / 2f - with(density) { 140.dp.toPx() }

        val clampedDlgX = if (boundsSpace == "Within Margins") {
            dialogueOffsetX.coerceIn(minDlgX_clamped, maxDlgX_clamped)
        } else {
            dialogueOffsetX.coerceIn(minDlgX_generous, maxDlgX_generous)
        }

        val clampedDlgY = if (boundsSpace == "Within Margins") {
            dialogueOffsetY.coerceIn(minDlgY_clamped, maxDlgY_clamped)
        } else {
            dialogueOffsetY.coerceIn(minDlgY_generous, maxDlgY_generous)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Render floating FAB if dialogue is not showing
            if (!showAiDashboard && state.isAiAssistantEnabled) {
                FloatingActionButton(
                    onClick = { showAiDashboard = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    modifier = Modifier
                        .offset { IntOffset(buttonOffsetX.roundToInt(), buttonOffsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newX = buttonOffsetX + dragAmount.x
                                val newY = buttonOffsetY + dragAmount.y
                                buttonOffsetX = newX.coerceIn(minBtnX, maxBtnX)
                                buttonOffsetY = newY.coerceIn(minBtnY, maxBtnY)
                            }
                        }
                        .padding(bottom = 60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SupportAgent,
                        contentDescription = "Workspace Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Render floating Consultation dialogue card when showing
        if (showAiDashboard && state.isAiAssistantEnabled) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AiAssistantDialogContent(
                    viewModel = viewModel,
                    onDismiss = { showAiDashboard = false },
                    modifier = Modifier
                        .size(cardWidthDp, cardHeightDp)
                        .offset { IntOffset(clampedDlgX.roundToInt(), clampedDlgY.roundToInt()) }
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    headerModifier = Modifier
                        .pointerInput(boundsSpace) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                dialogueOffsetX += dragAmount.x
                                dialogueOffsetY += dragAmount.y
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun AiAssistantDialogContent(
    viewModel: ErpViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (state.isDarkMode) CorporateSurfaceDark.copy(alpha = 0.95f) else CorporateSurfaceLight.copy(alpha = 0.98f)),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = modifier
            .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: styled to be a visually obvious draggable area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (state.isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                    .then(headerModifier)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = "Drag overlay",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Advisory Desk",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DRAG TO MOVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Real-time corporate ledger auditing, budget anomalies assessment, and GST consultations.",
                fontSize = 9.sp,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
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
                    .padding(8.dp)
            ) {
                if (state.aiLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Analyzing macro variables...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else if (state.aiResponse.isBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Forum, 
                            contentDescription = null, 
                            modifier = Modifier.size(28.dp), 
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Text(
                            "Consult Abielan Advisor:",
                            fontSize = 11.sp,
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
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                elevation = CardDefaults.cardElevation(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setAiPromptText(p)
                                        viewModel.askAdvisoryAgent()
                                    }
                                    .glassCardAdaptive(shape = RoundedCornerShape(8.dp), isDarkMode = state.isDarkMode)
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(p, fontSize = 10.sp, minLines = 1, maxLines = 2, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
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
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Query Input Form
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.aiPrompt,
                    onValueChange = { viewModel.setAiPromptText(it) },
                    placeholder = { Text("Consult advisor...", fontSize = 11.sp) },
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
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    maxLines = 1,
                    trailingIcon = {
                        if (state.aiPrompt.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setAiPromptText("") }) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { viewModel.askGenePrompt() },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .size(38.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// Bypassing compiler matching dynamically for viewModel queries
fun ErpViewModel.askGenePrompt() {
    this.askAdvisoryAgent()
}
