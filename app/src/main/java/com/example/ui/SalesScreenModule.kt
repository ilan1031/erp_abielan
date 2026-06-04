package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BusinessDocument
import com.example.data.RecurringSO
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.AccentGold
import com.example.ui.theme.glassCardAdaptive
import com.example.viewmodel.ErpViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SalesScreenModule(viewModel: ErpViewModel) {
    val state by viewModel.uiState.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val recurringSOs by viewModel.recurringSOs.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isCompactMobile = screenWidth < 380

    val scrollState = rememberScrollState()
    val rawSOs = documents.filter { it.type == "SO" }

    // State for creating new recurring SO template
    var showCreateRecurringModal by remember { mutableStateOf(false) }
    var editingRecurringSO by remember { mutableStateOf<RecurringSO?>(null) }

    val mainPadding = if (isCompactMobile) 10.dp else 16.dp

    var activeSalesTab by remember { mutableStateOf("orders") } // "orders" or "inventory"

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Broad Header row with the beautiful pill sub-tab selector pinned at the top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = mainPadding, vertical = 12.dp)
                .background(
                    if (state.isDarkMode) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                    RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (activeSalesTab == "orders") MaterialTheme.colorScheme.primary 
                        else Color.Transparent
                    )
                    .clickable { activeSalesTab = "orders" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Work,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (activeSalesTab == "orders") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sales Orders (SO)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeSalesTab == "orders") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (activeSalesTab == "inventory") MaterialTheme.colorScheme.primary 
                        else Color.Transparent
                    )
                    .clickable { activeSalesTab = "inventory" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (activeSalesTab == "inventory") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Inventory & Tax Hub",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeSalesTab == "inventory") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // AnimatedContent rendering each view
        AnimatedContent(
            targetState = activeSalesTab,
            label = "SalesTabAnimation",
            modifier = Modifier.weight(1f)
        ) { tabState ->
            when (tabState) {
                "orders" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(start = mainPadding, end = mainPadding, bottom = mainPadding)
                    ) {
        // --- SCREEN TITLE ---
        Text(
            text = "Autonomous Work Sales Orders (SO)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Track incoming client jobs, trigger hourly-rate timers, and auto-issue invoices.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // --- SECTION 1: ACTIVE SALES ORDERS ---
        Text(
            "Active Standard Sales Orders",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (rawSOs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .glassCardAdaptive(isDarkMode = state.isDarkMode)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.WorkOutline, 
                        contentDescription = null, 
                        modifier = Modifier.size(32.dp), 
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No standard Sales Orders found. Add an SO in the Finance Ledger to begin.", fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            rawSOs.forEach { so ->
                val isTimerActive = so.timerState == "RUNNING"
                val isTimerPaused = so.timerState == "PAUSED"
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(so.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text("${so.docNumber} • ${so.clientName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            // Timer Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (so.timerState) {
                                            "RUNNING" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                            "PAUSED" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            "COMPLETED" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                            else -> MaterialTheme.colorScheme.background
                                        }, RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = so.timerState,
                                    color = when (so.timerState) {
                                        "RUNNING" -> Color(0xFF10B981)
                                        "PAUSED" -> Color(0xFFF59E0B)
                                        "COMPLETED" -> Color(0xFF3B82F6)
                                        else -> MaterialTheme.colorScheme.secondary
                                    },
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Dynamic accrued financials row
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)).padding(if (isCompactMobile) 6.dp else 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (isCompactMobile) 4.dp else 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hourly Rate", fontSize = if (isCompactMobile) 9.sp else 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                                Text("₹${so.hourlyRate}/Hr", fontSize = if (isCompactMobile) 11.sp else 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Accrued Amount", fontSize = if (isCompactMobile) 9.sp else 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                                Text("₹${String.format(java.util.Locale.US, "%,.2f", so.totalAmount)}", fontSize = if (isCompactMobile) 11.sp else 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen, maxLines = 1)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Duration", fontSize = if (isCompactMobile) 9.sp else 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                                Text("${so.timerDurationMinutes} Mins", fontSize = if (isCompactMobile) 11.sp else 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Timer clocks indicator
                        val seconds = so.elapsedSeconds % 60
                        val minutes = (so.elapsedSeconds / 60) % 60
                        val hours = so.elapsedSeconds / 3600
                        val timeStr = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Timelapse, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(18.dp), 
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = timeStr,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isTimerActive) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Work controls
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isTimerActive) {
                                    IconButton(
                                        onClick = { viewModel.pauseSoTimer(so.id) },
                                        modifier = Modifier.background(Color(0xFFF59E0B), RoundedCornerShape(8.dp)).size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Pause, 
                                            contentDescription = "Pause", 
                                            tint = Color.White, 
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { viewModel.selectTimerDetailsToConfigure(so) },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)).size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow, 
                                            contentDescription = "Play", 
                                            tint = Color.White, 
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.resetSoTimer(so.id) },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh, 
                                        contentDescription = "Reset", 
                                        tint = MaterialTheme.colorScheme.onSurface, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (so.status != "COMPLETED") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.markPaymentAsCompleteAndMoveToInvoice(so.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Payment Complete (Move to Invoice)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 2: CONCURRENT RECURRING SO CHANNELS (UP TO 4 SIMULTANEOUS) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recurring Services (Max 4 Parallel Channels)",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp
            )
            
            // Add template trigger
            IconButton(
                onClick = { showCreateRecurringModal = true },
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)).size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (recurringSOs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .glassCardAdaptive(isDarkMode = state.isDarkMode)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Timer, 
                        contentDescription = null, 
                        modifier = Modifier.size(32.dp), 
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No recurring templates defined. Standard tier supports up to 4 parallel channels.", fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            // Clean robust display of active streams utilizing standard Columns and Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chunksCount = if (isCompactMobile) 1 else 2
                val chunks = recurringSOs.chunked(chunksCount)
                chunks.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { r ->
                            val isRunning = r.timerState == "RUNNING"
                            val isTimedOut = r.timerState == "TIMED_OUT"
                            val remSecs = (r.durationMinutes * 60) - r.elapsedSeconds
                            val displayRem = if (remSecs > 0) {
                                String.format(java.util.Locale.US, "%02d:%02d", remSecs / 60, remSecs % 60)
                            } else "00:00"

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(20.dp),
                                modifier = (if (chunksCount > 1) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .heightIn(min = 195.dp)
                                    .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = state.isDarkMode)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                r.serviceName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { editingRecurringSO = r },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Edit, 
                                                        contentDescription = "Edit Template", 
                                                        modifier = Modifier.size(12.dp), 
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { viewModel.deleteRecurringTemplate(r.id) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete, 
                                                        contentDescription = "Delete Template", 
                                                        modifier = Modifier.size(12.dp), 
                                                        tint = OverdueRed
                                                    )
                                                }
                                            }
                                        }
                                        Text("Rate: ₹${r.hourlyRate}/Hr  •  ${r.numPersons} Crew", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                                    }

                                    // Dynamic billing simulation values
                                    val accumulatedFee = if (isTimedOut) {
                                        (r.durationMinutes * 60 / 3600.0) * r.hourlyRate * r.numPersons
                                    } else {
                                        (r.elapsedSeconds / 3600.0) * r.hourlyRate * r.numPersons
                                    }
                                    Text(
                                        text = if (isTimedOut) "Completed Billing: ₹${String.format(java.util.Locale.US, "%.2f", accumulatedFee)}" else "Live billing: ₹${String.format(java.util.Locale.US, "%.2f", accumulatedFee)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTimedOut) OverdueRed else SuccessGreen,
                                        maxLines = 1
                                    )

                                    // Countdown clock UI circle layout representation
                                    if (isTimedOut) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { viewModel.continueRecurringTimer(r.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Continue", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Button(
                                                onClick = { viewModel.completeRecurringTimerAndMoveToSO(r.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Complete", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp)).padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Est Remaining", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                                                Text(displayRem, fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (isRunning) SuccessGreen else MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (isRunning) {
                                                    IconButton(
                                                        onClick = { viewModel.pauseRecurringTimer(r.id) },
                                                        modifier = Modifier.background(Color(0xFFF59E0B), RoundedCornerShape(4.dp)).size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Pause, 
                                                            contentDescription = null, 
                                                            tint = Color.White, 
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                     }
                                                 } else {
                                                     IconButton(
                                                         onClick = { viewModel.startRecurringTimer(r.id) },
                                                         modifier = Modifier.background(SuccessGreen, RoundedCornerShape(4.dp)).size(28.dp)
                                                     ) {
                                                         Icon(
                                                             imageVector = Icons.Filled.PlayArrow, 
                                                             contentDescription = null, 
                                                             tint = Color.White, 
                                                             modifier = Modifier.size(12.dp)
                                                         )
                                                     }
                                                 }
                                                 IconButton(
                                                     onClick = { viewModel.resetRecurringTimer(r.id) },
                                                     modifier = Modifier.background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)).size(28.dp)
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Filled.Refresh, 
                                                         contentDescription = null, 
                                                         tint = MaterialTheme.colorScheme.onSurface, 
                                                         modifier = Modifier.size(12.dp)
                                                     )
                                                 }
                                             }
                                         }
                                     }
                                 }
                             }
                        }
                        if (chunksCount > 1 && pair.size < chunksCount) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS & POPUPS MODULES ---

    // 1. Timer Setup Modal Configuration
    state.activeSoTimer?.let { so ->
        var inputRate by remember { mutableStateOf(so.hourlyRate.toInt().toString()) }
        var inputDuration by remember { mutableStateOf(so.timerDurationMinutes.toString()) }

        Dialog(onDismissRequest = { viewModel.clearActiveSoSelection() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Configure SO Work Timer", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Set custom billing parameters before starting work telemetry.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 16.dp))

                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    OutlinedTextField(
                        value = inputRate,
                        onValueChange = { inputRate = it },
                        label = { Text("Hourly Billing Rate (₹)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(25.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
 
                    OutlinedTextField(
                        value = inputDuration,
                        onValueChange = { inputDuration = it },
                        label = { Text("Timer Limit (minutes)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(25.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.clearActiveSoSelection() }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val rate = inputRate.toDoubleOrNull() ?: 500.0
                                val mins = inputDuration.toIntOrNull() ?: 30
                                viewModel.startSoTimer(so.id, mins, rate)
                                viewModel.clearActiveSoSelection()
                            }
                        ) {
                            Text("Start Telemetry Clock")
                        }
                    }
                }
            }
        }
    }

    // 2. Add New Recurring Template modal
    if (showCreateRecurringModal) {
        var templateName by remember { mutableStateOf("") }
        var numStaff by remember { mutableStateOf("1") }
        var rateText by remember { mutableStateOf("1000") }
        var minsText by remember { mutableStateOf("60") }

        Dialog(onDismissRequest = { showCreateRecurringModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Create Recurring Service Template", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
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
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Service/Contract Name", fontSize = 11.sp) },
                        colors = textFieldColors,
                        shape = RoundedCornerShape(25.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = numStaff,
                            onValueChange = { numStaff = it },
                            label = { Text("Staff Crew", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(25.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { rateText = it },
                            label = { Text("Rate (₹/hr)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(25.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = minsText,
                        onValueChange = { minsText = it },
                        label = { Text("Service Duration (mins)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(25.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateRecurringModal = false }) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (templateName.isNotBlank()) {
                                    viewModel.addNewRecurringTemplate(
                                        templateName,
                                        numStaff.toIntOrNull() ?: 1,
                                        rateText.toDoubleOrNull() ?: 1000.0,
                                        minsText.toIntOrNull() ?: 60
                                    )
                                    showCreateRecurringModal = false
                                }
                            }
                        ) {
                            Text("Enlist Channel")
                        }
                    }
                }
            }
        }
    }

    // Edit Recurring SO template modal
    if (editingRecurringSO != null) {
        val original = editingRecurringSO!!
        var templateName by remember(original) { mutableStateOf(original.serviceName) }
        var numStaff by remember(original) { mutableStateOf(original.numPersons.toString()) }
        var rateText by remember(original) { mutableStateOf(original.hourlyRate.toString()) }
        var minsText by remember(original) { mutableStateOf(original.durationMinutes.toString()) }

        Dialog(onDismissRequest = { editingRecurringSO = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Edit Recurring Service Template", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
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
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Service/Contract Name", fontSize = 11.sp) },
                        colors = textFieldColors,
                        shape = RoundedCornerShape(25.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = numStaff,
                            onValueChange = { numStaff = it },
                            label = { Text("Staff Crew", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(25.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { rateText = it },
                            label = { Text("Rate (₹/hr)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(25.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = minsText,
                        onValueChange = { minsText = it },
                        label = { Text("Service Duration (mins)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(25.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingRecurringSO = null }) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (templateName.isNotBlank()) {
                                    viewModel.updateRecurringTemplate(
                                        original.id,
                                        templateName,
                                        numStaff.toIntOrNull() ?: 1,
                                        rateText.toDoubleOrNull() ?: 1000.0,
                                        minsText.toIntOrNull() ?: 60
                                    )
                                    editingRecurringSO = null
                                }
                            }
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }

    // 3. Alarm notification Overdue dialogpopup popup
    if (state.showTimerCompletionDialog) {
        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "Alert",
                        tint = OverdueRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Billing Timer Ended!",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        state.timerCompletionMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.timerActionCompleteToInvoice() },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.FilePresent, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Invoice Automatically")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.timerActionContinue(30) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Continue Work? (Add 30m Timer)")
                            }
                        }
                    }
                }
            }
        }
                    }
                }
                "inventory" -> {
                    InventoryScreenModule(viewModel)
                }
            }
        }
    }
}
