package com.example.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BusinessDocument
import com.example.data.Partner
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PendingYellow
import com.example.ui.theme.glassCardAdaptive
import com.example.viewmodel.ErpViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PartnersScreenModule(viewModel: ErpViewModel, mainPadding: androidx.compose.ui.unit.Dp) {
    val state by viewModel.uiState.collectAsState()
    val partners by viewModel.partners.collectAsState()
    val documents by viewModel.documents.collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isThinScreen = screenWidth < 360

    val selectedPartner = remember(state.selectedPartnerId, partners) {
        partners.find { it.id == state.selectedPartnerId }
    }
    var showCreateForm by remember { mutableStateOf(false) }

    // List Filters State
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // "ALL", "CLIENT", "VENDOR"
    var filterStatus by remember { mutableStateOf("ALL") } // "ALL", "Active", "Inactive"

    AnimatedContent(
        targetState = selectedPartner,
        label = "PartnerScreenTransition"
    ) { partner ->
        if (partner != null) {
            // Zoho-Style Active Portfolio Space
            PartnerPortfolioView(
                partner = partner,
                viewModel = viewModel,
                documents = documents,
                onBack = { viewModel.selectPartner(null) },
                mainPadding = mainPadding,
                isThinScreen = isThinScreen,
                isDarkMode = state.isDarkMode
            )
        } else {
            // List and Master Controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = mainPadding, vertical = 8.dp)
            ) {
                // Intro Header info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Partners Profile Workspace",
                            fontSize = if (isThinScreen) 16.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Centralized ledger directories auditing clients & vendors like Zoho CRM.",
                            fontSize = if (isThinScreen) 10.sp else 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Button(
                        onClick = { showCreateForm = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            if (!isThinScreen) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Partner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- SEARCH & FILTER CONTROLS ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCardAdaptive(shape = RoundedCornerShape(16.dp), isDarkMode = state.isDarkMode)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Text Search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Filter by name, company, email...", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Type Filters Row
                        Column {
                            Text("Partner Type:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("ALL" to "All Contacts", "CLIENT" to "Clients", "VENDOR" to "Vendors").forEach { (key, label) ->
                                    val isSelected = filterType == key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else (if (state.isDarkMode) Color(0xFF1E293B) else Color.White)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                        else (if (state.isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { filterType = key }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = if (isThinScreen) 9.sp else 10.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Status filter chips
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Status:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            listOf("ALL" to "All", "Active" to "Active Only", "Inactive" to "Inactive").forEach { (key, label) ->
                                val isSelected = filterStatus == key
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else (if (state.isDarkMode) Color(0xFF1E293B) else Color.White)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else (if (state.isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable { filterStatus = key }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- PARTNERS LIST VIEW ---
                val filteredPartners = partners.filter { partnerItem ->
                    val matchQuery = searchQuery.isBlank() || 
                                     partnerItem.name.contains(searchQuery, ignoreCase = true) ||
                                     partnerItem.company.contains(searchQuery, ignoreCase = true) ||
                                     partnerItem.email.contains(searchQuery, ignoreCase = true) ||
                                     partnerItem.phone.contains(searchQuery, ignoreCase = true)
                    
                    val matchType = filterType == "ALL" || partnerItem.type == filterType
                    val matchStatus = filterStatus == "ALL" || partnerItem.status == filterStatus

                    matchQuery && matchType && matchStatus
                }

                if (filteredPartners.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCardAdaptive(shape = RoundedCornerShape(16.dp), isDarkMode = state.isDarkMode)
                            .padding(24.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.PeopleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "No Linked Partners Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "No clients or vendors met your filter query. Add a new partner record or clear search keywords to review presets.",
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredPartners.forEach { partnerItem ->
                            PartnerRowItem(
                                partnerItem = partnerItem,
                                onClick = { viewModel.selectPartner(partnerItem.id) },
                                documents = documents,
                                isDarkMode = state.isDarkMode,
                                isThinScreen = isThinScreen
                            )
                        }
                    }
                }
            }
        }
    }

    // --- CREATE/EDIT PARTNER MODAL FORM ---
    if (showCreateForm) {
        PartnerFormDialog(
            onDismiss = { showCreateForm = false },
            onSave = { name, type, email, phone, company, gstin, address, notes, status ->
                viewModel.createNewPartner(name, type, email, phone, company, gstin, address, notes, status)
                showCreateForm = false
            },
            isDarkMode = state.isDarkMode,
            isThinScreen = isThinScreen
        )
    }
}

@Composable
fun PartnerRowItem(
    partnerItem: Partner,
    onClick: () -> Unit,
    documents: List<BusinessDocument>,
    isDarkMode: Boolean,
    isThinScreen: Boolean
) {
    // Dynamic financial aggregate calculations for list cards
    val linkedDocs = documents.filter { it.clientName.equals(partnerItem.name, ignoreCase = true) }
    val isClient = partnerItem.type == "CLIENT"

    val pendingDocs = linkedDocs.filter { 
        it.status.equals("PENDING", ignoreCase = true) || 
        it.status.equals("OVERDUE", ignoreCase = true) ||
        it.status.equals("Approved", ignoreCase = true) ||
        it.status.equals("Active", ignoreCase = true)
    }
    val outstandingBalance = pendingDocs.sumOf { it.totalAmount }
    val txCount = linkedDocs.size

    val avatarInitials = if (partnerItem.name.isNotBlank()) {
        partnerItem.name.trim().split(" ").take(2).joinToString("") { it.take(1) }.uppercase()
    } else "CP"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAdaptive(shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stylized Badge Avatar
            Box(
                modifier = Modifier
                    .size(if (isThinScreen) 38.dp else 44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isClient) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitials,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (isThinScreen) 11.sp else 13.sp,
                    color = if (isClient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Body Context
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = partnerItem.name,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isThinScreen) 11.sp else 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Compact Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isClient) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = partnerItem.type,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isClient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (partnerItem.company.isNotBlank()) {
                    Text(
                        text = partnerItem.company,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${if (isClient) "Receivable" else "Payable"}: ₹${String.format(Locale.US, "%,.2f", outstandingBalance)} • ${txCount} Tx",
                    fontSize = if (isThinScreen) 9.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (outstandingBalance > 0) {
                        if (isClient) AccentGold else OverdueRed
                    } else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right arrow caret for tactile feel and alignment safety
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// --- PORTFOLIO VIEW (ZOHO ERP STYLE) ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PartnerPortfolioView(
    partner: Partner,
    viewModel: ErpViewModel,
    documents: List<BusinessDocument>,
    onBack: () -> Unit,
    mainPadding: androidx.compose.ui.unit.Dp,
    isThinScreen: Boolean,
    isDarkMode: Boolean
) {
    val isClient = partner.type == "CLIENT"
    val scrollState = rememberScrollState()

    // 1. Scan ledger reports dynamically
    val matchedDocuments = documents.filter { 
        it.clientName.equals(partner.name, ignoreCase = true) 
    }

    val totalBusinessVolume = matchedDocuments
        .filter { it.status.equals("PAID", ignoreCase = true) || it.status.equals("COMPLETED", ignoreCase = true) }
        .sumOf { it.totalAmount }

    val outstandingReceivablesPayables = matchedDocuments
        .filter { 
            it.status.equals("PENDING", ignoreCase = true) || 
            it.status.equals("OVERDUE", ignoreCase = true) ||
            it.status.equals("Approved", ignoreCase = true) ||
            it.status.equals("Active", ignoreCase = true)
        }
        .sumOf { it.totalAmount }

    // State for interactive instant document creation inside the profile
    var showQuickTransactionSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = mainPadding, vertical = 8.dp)
    ) {
        // Back Navigation header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Return", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Partner Portfolio Desk",
                fontSize = if (isThinScreen) 14.sp else 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // --- SECTION 1: MASTER IDENTITY CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .glassCardAdaptive(shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode)
                .padding(12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Initials glow icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (isClient) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = partner.name.split(" ").take(2).joinToString("") { it.take(1) }.uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isClient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                partner.name,
                                fontWeight = FontWeight.Black,
                                fontSize = if (isThinScreen) 13.sp else 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isClient) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.secondaryContainer
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    partner.type,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isClient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        if (partner.company.isNotBlank()) {
                            Text(
                                partner.company,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (partner.status == "Active") SuccessGreen else OverdueRed)
                            )
                            Text(
                                partner.status,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (partner.status == "Active") SuccessGreen else OverdueRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))

                // Detail Contact Field Items
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (partner.email.isNotBlank()) {
                        ContactInfoField(icon = Icons.Filled.Email, label = "Email Profile", value = partner.email, isThin = isThinScreen)
                    }
                    if (partner.phone.isNotBlank()) {
                        ContactInfoField(icon = Icons.Filled.Phone, label = "Phone Channel", value = partner.phone, isThin = isThinScreen)
                    }
                    if (partner.gstin.isNotBlank()) {
                        ContactInfoField(icon = Icons.Filled.Receipt, label = "GSTIN / TAX NO", value = partner.gstin, isThin = isThinScreen)
                    }
                    if (partner.address.isNotBlank()) {
                        ContactInfoField(icon = Icons.Filled.LocationOn, label = "Street Address", value = partner.address, isThin = isThinScreen)
                    }
                    if (partner.notes.isNotBlank()) {
                        ContactInfoField(icon = Icons.Filled.Comment, label = "Internal Workspace Notes", value = partner.notes, isThin = isThinScreen)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- SECTION 2: ZOHO LEDGER METRICS BOARD ---
        Text(
            "Executive Ledger Balance",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Outstanding receivables/payables
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .weight(1f)
                    .glassCardAdaptive(shape = RoundedCornerShape(12.dp), isDarkMode = isDarkMode)
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        if (isClient) "Total Receivable dues" else "Total Payable deficits",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "₹${String.format(Locale.US, "%,.2f", outstandingReceivablesPayables)}",
                        fontSize = if (isThinScreen) 13.sp else 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (outstandingReceivablesPayables > 0) {
                            if (isClient) AccentGold else OverdueRed
                        } else SuccessGreen,
                        maxLines = 1
                    )
                }
            }

            // Completed volume
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .weight(1f)
                    .glassCardAdaptive(shape = RoundedCornerShape(12.dp), isDarkMode = isDarkMode)
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        if (isClient) "Revenue compiled" else "Procurements paid",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "₹${String.format(Locale.US, "%,.2f", totalBusinessVolume)}",
                        fontSize = if (isThinScreen) 13.sp else 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- SECTION 3: RECENT TRANSACTIONS CHRONOLOGY ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Document History Ledger (${matchedDocuments.size} Tx)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Direct interactive link to issue invoice or bill
            Text(
                "+ Create Transaction",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showQuickTransactionSheet = true }
            )
        }

        if (matchedDocuments.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCardAdaptive(shape = RoundedCornerShape(12.dp), isDarkMode = isDarkMode)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "No Linked Invoices, Bills or Orders",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Use the Create Transaction link above to launch a quick sales order, quote, invoice, or procurement bill for this partner.",
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                matchedDocuments.forEach { doc ->
                    LedgerRowItem(doc = doc, isDarkMode = isDarkMode, isThinScreen = isThinScreen)
                }
            }
        }
    }

    // Modal quick document compiler directly within partners
    if (showQuickTransactionSheet) {
        QuickDocCreateDialog(
            partnerName = partner.name,
            partnerType = partner.type,
            onDismiss = { showQuickTransactionSheet = false },
            onSave = { docType, title, amount, notes, hourly, limits ->
                viewModel.addNewDocument(
                    type = docType,
                    title = title,
                    client = partner.name,
                    amount = amount,
                    status = if (docType == "SO") "Active" else "PENDING",
                    notes = notes,
                    hourlyRate = hourly,
                    timerDurationMinutes = limits
                )
                showQuickTransactionSheet = false
            },
            isDarkMode = isDarkMode
        )
    }
}

@Composable
fun LedgerRowItem(doc: BusinessDocument, isDarkMode: Boolean, isThinScreen: Boolean) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(doc.issueDate))
    
    val badgeColor = when (doc.status.uppercase()) {
        "PAID", "COMPLETED" -> SuccessGreen
        "PENDING" -> PendingYellow
        "OVERDUE" -> OverdueRed
        "DRAFT" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAdaptive(shape = RoundedCornerShape(12.dp), borderWidth = 0.5.dp, isDarkMode = isDarkMode)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Type Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doc.type,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isThinScreen) 10.sp else 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${doc.docNumber} • $dateStr",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.US, "%,.2f", doc.totalAmount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = if (isThinScreen) 11.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = doc.status,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeColor
                    )
                }
            }
        }
    }
}

@Composable
fun ContactInfoField(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, isThin: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp).padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Text(value, fontSize = if (isThin) 10.sp else 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// Dialog helper to create a new Partner profile
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PartnerFormDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, email: String, phone: String, company: String, gstin: String, address: String, notes: String, status: String) -> Unit,
    isDarkMode: Boolean,
    isThinScreen: Boolean
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("CLIENT") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }

    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Launch Contact Profile",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Create a high-integrity profile for corporate ledger synchronization.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Type Toggle Field
                Column {
                    Text("Partner Account Type", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CLIENT" to "Client Portfolio", "VENDOR" to "Vendor Supplier").forEach { (key, label) ->
                            val isSelected = type == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else (if (isDarkMode) Color(0xFF1E293B) else Color.White)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else (if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { type = key }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input fields
                OutlinedTextFieldStyled(label = "Contact Full Name *", value = name, onValueChange = { name = it; showError = false }, isError = showError)
                if (showError) {
                    Text("Contact name is required.", color = OverdueRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextFieldStyled(label = "Organization / Company Name", value = company, onValueChange = { company = it })

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextFieldStyled(label = "Email Address", value = email, onValueChange = { email = it }, keyboardType = KeyboardType.Email)

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextFieldStyled(label = "Phone Contact", value = phone, onValueChange = { phone = it }, keyboardType = KeyboardType.Phone)

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextFieldStyled(label = "GSTIN / Corporate Tax Identification", value = gstin, onValueChange = { gstin = it })

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextFieldStyled(label = "Street Registered Address", value = address, onValueChange = { address = it })

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextFieldStyled(label = "Internal Reference Notes", value = notes, onValueChange = { notes = it })

                Spacer(modifier = Modifier.height(10.dp))

                // Status Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Status Tracker", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Active", "Inactive").forEach { s ->
                            val isSelected = status == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) {
                                            if (s == "Active") SuccessGreen else OverdueRed
                                        } else (if (isDarkMode) Color(0xFF1E293B) else Color.White)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) {
                                            if (s == "Active") SuccessGreen else OverdueRed
                                        } else (if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { status = s }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    s,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                showError = true
                            } else {
                                onSave(name, type, email, phone, company, gstin, address, notes, status)
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Quick Dialog to compiled transaction directly within partner portfolio
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickDocCreateDialog(
    partnerName: String,
    partnerType: String,
    onDismiss: () -> Unit,
    onSave: (docType: String, title: String, amount: Double, notes: String, hourly: Double, limitMin: Int) -> Unit,
    isDarkMode: Boolean
) {
    var docType by remember { mutableStateOf(if (partnerType == "VENDOR") "BILL" else "INVOICE") } // "QUOTE", "INVOICE", "BILL", "PO", "SO"
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Sales Order parameters
    var hourlyRateText by remember { mutableStateOf("1200") }
    var limitMinutesText by remember { mutableStateOf("30") }

    var titleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Compile Transaction Ledger",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Issue a direct accounting ledger document for $partnerName.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Doc Type pills
                Column {
                    Text("Document Category", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val availableTypes = if (partnerType == "VENDOR") {
                            listOf("BILL" to "Purchase Bill", "PO" to "Purchase Order (PO)")
                        } else {
                            listOf("INVOICE" to "Invoice", "QUOTE" to "Quote Proposal", "SO" to "Sales Order (SO)")
                        }

                        availableTypes.forEach { (typeKey, typeLabel) ->
                            val isSelected = docType == typeKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else (if (isDarkMode) Color(0xFF1E293B) else Color.White)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else (if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { docType = typeKey }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    typeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextFieldStyled(
                    label = "Transaction Title *",
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    isError = titleError
                )

                if (docType != "SO") {
                    OutlinedTextFieldStyled(
                        label = "Total Transaction Amount (₹)",
                        value = amountText,
                        onValueChange = { amountText = it },
                        keyboardType = KeyboardType.Number
                    )
                } else {
                    // Show Sales Order Timer custom configurations
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("SO Timer Configuration Setup", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextFieldStyled(label = "Hourly Professional Billing Rate (₹/hr)", value = hourlyRateText, onValueChange = { hourlyRateText = it }, keyboardType = KeyboardType.Number)
                            OutlinedTextFieldStyled(label = "Workflow Clock Timer Limit (Minutes)", value = limitMinutesText, onValueChange = { limitMinutesText = it }, keyboardType = KeyboardType.Number)
                        }
                    }
                }

                OutlinedTextFieldStyled(
                    label = "Transaction Memo / Notes",
                    value = notes,
                    onValueChange = { notes = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                            } else {
                                val finalAmt = amountText.toDoubleOrNull() ?: 0.0
                                val hourlyVal = hourlyRateText.toDoubleOrNull() ?: 0.0
                                val limitMins = limitMinutesText.toIntOrNull() ?: 30
                                onSave(docType, title, finalAmt, notes, hourlyVal, limitMins)
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Emit Doc", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OutlinedTextFieldStyled(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        maxLines = 1,
        isError = isError,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
