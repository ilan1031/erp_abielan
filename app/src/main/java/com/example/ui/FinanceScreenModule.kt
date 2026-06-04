package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BusinessDocument
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CorporateSurfaceDark
import com.example.ui.theme.CorporateSurfaceLight
import com.example.ui.theme.glassCardAdaptive
import com.example.viewmodel.ErpViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.Color as AndroidColor
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinanceScreenModule(viewModel: ErpViewModel) {
    val state by viewModel.uiState.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current

    val horizontalFilterScroll = rememberScrollState()

    // Screen local parameters
    var showAddDocModal by remember { mutableStateOf(false) }
    var docToEdit by remember { mutableStateOf<BusinessDocument?>(null) }
    var selectedPdfDoc by remember { mutableStateOf<BusinessDocument?>(null) }

    var currentPage by remember { mutableStateOf(1) }
    var itemsPerPage by remember { mutableStateOf(10) }

    var appliedStartDate by remember { mutableStateOf<Long?>(null) }
    var appliedEndDate by remember { mutableStateOf<Long?>(null) }
    var appliedStartHour by remember { mutableStateOf<Int?>(null) }
    var appliedStartMin by remember { mutableStateOf<Int?>(null) }
    var appliedEndHour by remember { mutableStateOf<Int?>(null) }
    var appliedEndMin by remember { mutableStateOf<Int?>(null) }

    var showAdvancedFilterDialog by remember { mutableStateOf(false) }

    val filtersActive = appliedStartDate != null || appliedEndDate != null || appliedStartHour != null || appliedEndHour != null

    // --- Search & Filtering Pipeline ---
    val searchedDocs = documents.filter { doc ->
        val query = state.searchQuery.lowercase()
        doc.title.lowercase().contains(query) ||
        doc.clientName.lowercase().contains(query) ||
        doc.docNumber.lowercase().contains(query)
    }

    val typedDocs = if (state.activeFilterTab == "ALL") {
        searchedDocs
    } else {
        searchedDocs.filter { it.type == state.activeFilterTab }
    }

    val filteredDocs = typedDocs.filter { doc ->
        // 1. Date range filter
        var matchesDate = true
        if (appliedStartDate != null) {
            val startCal = Calendar.getInstance().apply {
                timeInMillis = appliedStartDate!!
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (doc.createdAt < startCal.timeInMillis) matchesDate = false
        }
        if (appliedEndDate != null) {
            val endCal = Calendar.getInstance().apply {
                timeInMillis = appliedEndDate!!
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            if (doc.createdAt > endCal.timeInMillis) matchesDate = false
        }

        // 2. Time range within day filter
        var matchesTime = true
        if (appliedStartHour != null || appliedEndHour != null) {
            val docCal = Calendar.getInstance().apply { timeInMillis = doc.createdAt }
            val docHour = docCal.get(Calendar.HOUR_OF_DAY)
            val docMin = docCal.get(Calendar.MINUTE)
            val docMinutesSinceMidnight = docHour * 60 + docMin

            val startMinutesLimit = (appliedStartHour ?: 0) * 60 + (appliedStartMin ?: 0)
            val endMinutesLimit = (appliedEndHour ?: 23) * 60 + (appliedEndMin ?: 59)

            if (docMinutesSinceMidnight < startMinutesLimit || docMinutesSinceMidnight > endMinutesLimit) {
                matchesTime = false
            }
        }

        matchesDate && matchesTime
    }

    val sortedDocs = when (state.activeSortMode) {
        "DATE_DESC" -> filteredDocs.sortedByDescending { it.createdAt }
        "DATE_ASC" -> filteredDocs.sortedBy { it.createdAt }
        "AMOUNT_DESC" -> filteredDocs.sortedByDescending { it.totalAmount }
        "AMOUNT_ASC" -> filteredDocs.sortedBy { it.totalAmount }
        else -> filteredDocs
    }

    val totalPages = if (sortedDocs.isEmpty()) 1 else java.lang.Math.ceil(sortedDocs.size.toDouble() / itemsPerPage).toInt()
    val safeCurrentPage = currentPage.coerceIn(1, totalPages)
    val startIndex = (safeCurrentPage - 1) * itemsPerPage
    val endIndex = (startIndex + itemsPerPage).coerceAtMost(sortedDocs.size)
    val paginatedDocs = if (sortedDocs.isEmpty()) emptyList() else sortedDocs.subList(startIndex, endIndex)

    // Reset page to 1 on filter, tab, or search query transitions
    LaunchedEffect(sortedDocs.size, state.activeFilterTab, state.searchQuery) {
        currentPage = 1
    }

    val density = LocalDensity.current
    val titleHeightDp = 70.dp
    val titleHeightPx = with(density) { titleHeightDp.toPx() }
    var titleOffsetPx by remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                val newOffset = (titleOffsetPx + delta).coerceIn(-titleHeightPx, 0f)
                if (delta < 0) {
                    val consumed = newOffset - titleOffsetPx
                    titleOffsetPx = newOffset
                    return androidx.compose.ui.geometry.Offset(0f, consumed)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                if (delta > 0) {
                    val oldOffset = titleOffsetPx
                    val newOffset = (titleOffsetPx + delta).coerceIn(-titleHeightPx, 0f)
                    titleOffsetPx = newOffset
                    val consumedDelta = newOffset - oldOffset
                    return androidx.compose.ui.geometry.Offset(0f, consumedDelta)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    val pageScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(pageScrollState)
            .nestedScroll(nestedScrollConnection)
    ) {
        // --- LEDGER HEADCONTROLLERS ---
        Column(modifier = Modifier.padding(16.dp)) {
            val animatedHeight = with(density) { (titleHeightPx + titleOffsetPx).coerceAtLeast(0f).toDp() }
            val alpha = ((titleHeightPx + titleOffsetPx) / titleHeightPx).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp)
                    .height(animatedHeight)
                    .graphicsLayer {
                        this.alpha = alpha
                        this.translationY = titleOffsetPx * 0.4f
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Enterprise Finance Ledger", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(text = "Manage dual-ledger documents, generate statements, view previews", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // --- SEARCH BAR & TOGGLE & ADD BUTTON ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .border(1.dp, if (state.isDarkMode) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .background(if (state.isDarkMode) MaterialTheme.colorScheme.surface else Color.White, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (state.isDarkMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.searchQuery.isEmpty()) {
                            Text(
                                text = "Search Client or Title...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.changeSearchQuery(it) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.changeSearchQuery("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Advanced Filter Button
                OutlinedButton(
                    onClick = { showAdvancedFilterDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (filtersActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else (if (state.isDarkMode) MaterialTheme.colorScheme.surface else Color.White),
                        contentColor = if (filtersActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (filtersActive) MaterialTheme.colorScheme.primary else (if (state.isDarkMode) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "Filters",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Grid List Toggle button
                OutlinedButton(
                    onClick = { viewModel.toggleListGrid() },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (state.isDarkMode) MaterialTheme.colorScheme.surface else Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (state.isDarkMode) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (state.isListGridToggle) Icons.Filled.GridView else Icons.Filled.List,
                        contentDescription = "Toggle Layout",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Add Document button
                Button(
                    onClick = { showAddDocModal = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add, 
                        contentDescription = "Add Document", 
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- TYPE CATEGORY FILTER TABS ---
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .horizontalScroll(horizontalFilterScroll),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("ALL", "QUOTE", "INVOICE", "BILL", "PO", "SO").forEach { tab ->
                        val isSelected = state.activeFilterTab == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setActiveFilterTab(tab) },
                            label = { Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (state.isDarkMode) MaterialTheme.colorScheme.surface else Color.White,
                                labelColor = if (state.isDarkMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (state.isDarkMode) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Left fade gradient & scroll chevron indicator
                if (horizontalFilterScroll.value > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(end = 6.dp)
                            .height(42.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Scroll Left Indicator",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Right fade gradient & scroll chevron indicator
                if (horizontalFilterScroll.value < horizontalFilterScroll.maxValue) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                            .padding(start = 6.dp)
                            .height(42.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Scroll Right Indicator",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Showing ${sortedDocs.size} records",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (filtersActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.FilterAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    "Filtered",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Inline sort controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sort: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    val sortLabel = when (state.activeSortMode) {
                        "DATE_DESC" -> "Newest"
                        "DATE_ASC" -> "Oldest"
                        "AMOUNT_DESC" -> "Highest Sum"
                        "AMOUNT_ASC" -> "Lowest Sum"
                        else -> "Default"
                    }
                    Text(
                        text = "$sortLabel ▼",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                val nextSort = when (state.activeSortMode) {
                                    "DATE_DESC" -> "AMOUNT_DESC"
                                    "AMOUNT_DESC" -> "AMOUNT_ASC"
                                    "AMOUNT_ASC" -> "DATE_ASC"
                                    else -> "DATE_DESC"
                                }
                                viewModel.changeSortMode(nextSort)
                            }
                            .padding(4.dp)
                    )
                }
            }
        }

        // --- RECORDS GRAPH VIEW ---
        if (sortedDocs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Inbox, 
                        contentDescription = null, 
                        modifier = Modifier.size(48.dp), 
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No documents matches filters.", color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else {
            if (state.isListGridToggle) {
                // Corporate Table View with Scroll and Pagination
                val scrollState = rememberScrollState()
                val containerBg = if (state.isDarkMode) Color(0xFF131926).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f)
                val borderColor = if (state.isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = containerBg)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            Column(
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .horizontalScroll(scrollState)
                            ) {
                                TableHeaderRow(isDark = state.isDarkMode)
                                
                                Column(
                                    modifier = Modifier
                                        .width(1050.dp)
                                        .padding(bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    paginatedDocs.forEach { doc ->
                                        TableRowItem(
                                            doc = doc,
                                            onCardClick = { selectedPdfDoc = doc },
                                            onDeleteClick = { viewModel.deleteDocument(doc) },
                                            onEditClick = { docToEdit = doc },
                                            onStatusUpdate = { newStatus -> viewModel.updateDocumentStatus(doc.id, newStatus) },
                                            onConvertToInvoice = { viewModel.convertQuoteToInvoice(doc.id) },
                                            isDarkMode = state.isDarkMode
                                        )
                                        HorizontalDivider(
                                            color = borderColor,
                                            thickness = 1.dp
                                        )
                                    }
                                }
                            }
                        }

                        // Compact horizontal scrollbar, attached right between the table and footer
                        TableHorizontalScrollIndicator(
                            scrollState = scrollState,
                            isDark = state.isDarkMode,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        // Table Pagination Control integrated inside the same card
                        TablePaginationControl(
                            currentPage = safeCurrentPage,
                            totalPages = totalPages,
                            totalItems = sortedDocs.size,
                            itemsPerPage = itemsPerPage,
                            startIndex = startIndex,
                            endIndex = endIndex,
                            onPageChange = { currentPage = it },
                            onItemsPerPageChange = { 
                                itemsPerPage = it 
                                currentPage = 1
                            },
                            isDark = state.isDarkMode,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            } else {
                // Grid View that scales dynamically based on width
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val isWideScreen = screenWidth >= 600
                val isCompactMobile = screenWidth < 400
                val containerBg = if (state.isDarkMode) Color(0xFF131926).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f)
                val borderColor = if (state.isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = containerBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(12.dp)
                    ) {
                        val numColumns = if (isWideScreen) 3 else if (isCompactMobile) 1 else 2
                        val docChunks = paginatedDocs.chunked(numColumns)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            docChunks.forEach { rowDocs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowDocs.forEach { doc ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            DocumentItemCard(
                                                doc = doc,
                                                onCardClick = { selectedPdfDoc = doc },
                                                onDeleteClick = { viewModel.deleteDocument(doc) },
                                                onEditClick = { docToEdit = doc },
                                                onStatusUpdate = { newStatus -> viewModel.updateDocumentStatus(doc.id, newStatus) },
                                                onConvertToInvoice = { viewModel.convertQuoteToInvoice(doc.id) },
                                                isDarkMode = state.isDarkMode
                                            )
                                        }
                                    }
                                    val dummyCount = numColumns - rowDocs.size
                                    if (dummyCount > 0) {
                                        repeat(dummyCount) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = borderColor, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Table Pagination Control integrated inside the same card
                        TablePaginationControl(
                            currentPage = safeCurrentPage,
                            totalPages = totalPages,
                            totalItems = sortedDocs.size,
                            itemsPerPage = itemsPerPage,
                            startIndex = startIndex,
                            endIndex = endIndex,
                            onPageChange = { currentPage = it },
                            onItemsPerPageChange = { 
                                itemsPerPage = it 
                                currentPage = 1
                            },
                            isDark = state.isDarkMode,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }

    // Add or Edit Document dialog modal
    if (showAddDocModal || docToEdit != null) {
        AddDocumentDialogForm(
            docToEdit = docToEdit,
            viewModel = viewModel,
            onSubmit = { type, title, client, amt, notes, hrs, docNo, timerMins, status, issueStr, dueStr, gstPct, baseAmt ->
                val draftStatus = if (status.isNotBlank()) status else {
                    with(type) {
                        when (this) {
                            "INVOICE" -> "PENDING"
                            "QUOTE" -> "OPEN"
                            "BILL" -> "PENDING"
                            "PO" -> "Approved"
                            "SO" -> "Active"
                            else -> "Pending"
                        }
                    }
                }
                var issueTime: Long? = null
                var dueTime: Long? = null
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                if (issueStr.isNotBlank()) {
                    try {
                        issueTime = sdf.parse(issueStr.trim())?.time
                    } catch (e: java.lang.Exception) {
                        // ignore
                    }
                }
                if (dueStr.isNotBlank()) {
                    try {
                        dueTime = sdf.parse(dueStr.trim())?.time
                    } catch (e: java.lang.Exception) {
                        // ignore
                    }
                }
                val currentDoc = docToEdit
                if (currentDoc != null) {
                    viewModel.updateFullDocument(
                        docId = currentDoc.id,
                        type = type,
                        title = title,
                        client = client,
                        amount = amt,
                        status = draftStatus,
                        notes = notes,
                        hourlyRate = hrs,
                        customDocNumber = docNo,
                        timerDurationMinutes = timerMins,
                        customIssueDate = issueTime,
                        customDueDate = dueTime,
                        gstRatePct = gstPct,
                        baseAmount = baseAmt
                    )
                } else {
                    viewModel.addNewDocument(
                        type = type,
                        title = title,
                        client = client,
                        amount = amt,
                        status = draftStatus,
                        notes = notes,
                        hourlyRate = hrs,
                        customDocNumber = docNo,
                        timerDurationMinutes = timerMins,
                        customIssueDate = issueTime,
                        customDueDate = dueTime,
                        gstRatePct = gstPct,
                        baseAmount = baseAmt
                    )
                }
                showAddDocModal = false
                docToEdit = null
            },
            onDismiss = {
                showAddDocModal = false
                docToEdit = null
            },
            isDark = state.isDarkMode
        )
    }

    // Advanced custom range Date and Time filter dialog
    if (showAdvancedFilterDialog) {
        var tempStartDate by remember { mutableStateOf(appliedStartDate) }
        var tempEndDate by remember { mutableStateOf(appliedEndDate) }
        var tempStartHour by remember { mutableStateOf(appliedStartHour) }
        var tempStartMin by remember { mutableStateOf(appliedStartMin) }
        var tempEndHour by remember { mutableStateOf(appliedEndHour) }
        var tempEndMin by remember { mutableStateOf(appliedEndMin) }

        Dialog(onDismissRequest = { showAdvancedFilterDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Filter Ledger Docs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                            )
                        }
                        IconButton(onClick = { showAdvancedFilterDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // DATE RANGE SECTION
                    Text(
                        text = "CUSTOM DATE RANGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Date Button
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                if (tempStartDate != null) cal.timeInMillis = tempStartDate!!
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        tempStartDate = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (tempStartDate != null) {
                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(tempStartDate)
                                } else {
                                    "Start Date"
                                },
                                maxLines = 1,
                                fontSize = 12.sp
                            )
                        }

                        // End Date Button
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                if (tempEndDate != null) cal.timeInMillis = tempEndDate!!
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        tempEndDate = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (tempEndDate != null) {
                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(tempEndDate)
                                } else {
                                    "End Date"
                                },
                                maxLines = 1,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Reset Dates inline if any selected
                    if (tempStartDate != null || tempEndDate != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Reset Dates",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable {
                                        tempStartDate = null
                                        tempEndDate = null
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // TIME RANGE SECTION
                    Text(
                        text = "CUSTOM TIME RANGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Time Button
                        OutlinedButton(
                            onClick = {
                                val currentHour = tempStartHour ?: 0
                                val currentMin = tempStartMin ?: 0
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        tempStartHour = hourOfDay
                                        tempStartMin = minute
                                    },
                                    currentHour,
                                    currentMin,
                                    false
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (tempStartHour != null) {
                                    String.format(Locale.getDefault(), "%02d:%02d %s", 
                                        if (tempStartHour!! == 0 || tempStartHour!! == 12) 12 else tempStartHour!! % 12,
                                        tempStartMin ?: 0,
                                        if (tempStartHour!! >= 12) "PM" else "AM"
                                    )
                                } else {
                                    "Start Time"
                                },
                                maxLines = 1,
                                fontSize = 12.sp
                            )
                        }

                        // End Time Button
                        OutlinedButton(
                            onClick = {
                                val currentHour = tempEndHour ?: 23
                                val currentMin = tempEndMin ?: 59
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        tempEndHour = hourOfDay
                                        tempEndMin = minute
                                    },
                                    currentHour,
                                    currentMin,
                                    false
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (tempEndHour != null) {
                                    String.format(Locale.getDefault(), "%02d:%02d %s", 
                                        if (tempEndHour!! == 0 || tempEndHour!! == 12) 12 else tempEndHour!! % 12,
                                        tempEndMin ?: 59,
                                        if (tempEndHour!! >= 12) "PM" else "AM"
                                    )
                                } else {
                                    "End Time"
                                },
                                maxLines = 1,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Reset Times slot inline if selected
                    if (tempStartHour != null || tempEndHour != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Reset Times",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable {
                                        tempStartHour = null
                                        tempStartMin = null
                                        tempEndHour = null
                                        tempEndMin = null
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reset All
                        TextButton(
                            onClick = {
                                tempStartDate = null
                                tempEndDate = null
                                tempStartHour = null
                                tempStartMin = null
                                tempEndHour = null
                                tempEndMin = null
                                appliedStartDate = null
                                appliedEndDate = null
                                appliedStartHour = null
                                appliedStartMin = null
                                appliedEndHour = null
                                appliedEndMin = null
                                showAdvancedFilterDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset All", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                        }

                        // Apply Filters
                        Button(
                            onClick = {
                                appliedStartDate = tempStartDate
                                appliedEndDate = tempEndDate
                                appliedStartHour = tempStartHour
                                appliedStartMin = tempStartMin
                                appliedEndHour = tempEndHour
                                appliedEndMin = tempEndMin
                                showAdvancedFilterDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Apply Filters", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    // PDF Preview floating sheet modal
    selectedPdfDoc?.let { doc ->
        Dialog(
            onDismissRequest = { selectedPdfDoc = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PdfRendererPreviewPanel(
                doc = doc,
                isDark = state.isDarkMode,
                onDismiss = { selectedPdfDoc = null },
                onDownload = {
                    downloadDocumentPdf(context, doc)
                },
                onShare = {
                    shareDocumentPdf(context, doc)
                },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun DocumentItemCard(
    doc: BusinessDocument,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onStatusUpdate: ((String) -> Unit)? = null,
    onConvertToInvoice: (() -> Unit)? = null,
    isDarkMode: Boolean = true
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(doc.createdAt)
    val colorTag = when (doc.status.uppercase()) {
        "PAID", "APPROVED", "OPEN" -> SuccessGreen
        "PENDING", "ACTIVE" -> AccentGold
        "OVERDUE", "CLOSED", "CLOSE" -> OverdueRed
        else -> MaterialTheme.colorScheme.secondary
    }

    val typeIcon = when (doc.type.uppercase()) {
        "INVOICE" -> Icons.Filled.ReceiptLong
        "QUOTE" -> Icons.Filled.Description
        "BILL" -> Icons.Filled.Payment
        "PO" -> Icons.Filled.LocalShipping
        "SO" -> Icons.Filled.Work
        else -> Icons.Filled.Article
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = isDarkMode)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Type badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = typeIcon, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp), 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(doc.type, fontWeight = FontWeight.Black, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text(dateStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title + Serial ID
            Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Text(doc.docNumber, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(10.dp))

            // Client + Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CLIENT", fontSize = 8.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(doc.clientName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₹${String.format(java.util.Locale.US, "%,.2f", doc.totalAmount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Status row chip
                    Box(
                        modifier = Modifier
                            .background(colorTag.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(doc.status, color = colorTag, fontWeight = FontWeight.Black, fontSize = 8.sp)
                    }
                }
            }

            if (doc.type.uppercase() == "QUOTE") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Convert to Invoice Button
                    Button(
                        onClick = { onConvertToInvoice?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1.2f).height(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Cached, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Convert to Invoice", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Status buttons
                    if (doc.status.uppercase() != "OPEN") {
                        Button(
                            onClick = { onStatusUpdate?.invoke("OPEN") },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(0.9f).height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Open", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (doc.status.uppercase() != "CLOSED" && doc.status.uppercase() != "CLOSE") {
                        Button(
                            onClick = { onStatusUpdate?.invoke("CLOSED") },
                            colors = ButtonDefaults.buttonColors(containerColor = OverdueRed),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(0.9f).height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Close", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            val isEditableOrDeletable = doc.type.uppercase() != "INVOICE" && doc.type.uppercase() != "BILL"
            if (isEditableOrDeletable) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onEditClick?.invoke() }, 
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit, 
                            contentDescription = "Edit Document", 
                            modifier = Modifier.size(13.dp), 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete, 
                            contentDescription = "Delete Document", 
                            modifier = Modifier.size(13.dp), 
                            tint = OverdueRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TablePaginationControl(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    itemsPerPage: Int,
    startIndex: Int,
    endIndex: Int,
    onPageChange: (Int) -> Unit,
    onItemsPerPageChange: (Int) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        val availableWidth = maxWidth
        val isVeryCompact = availableWidth < 360.dp
        val isCompact = availableWidth < 500.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left region: Items Per Page Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isVeryCompact) 2.dp else 4.dp)
            ) {
                if (!isVeryCompact) {
                    Text(
                        text = "Show:",
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                listOf(5, 10, 20).forEach { size ->
                    val isSelected = itemsPerPage == size
                    val chipBg = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        if (isDark) Color(0xFF131926).copy(alpha = 0.4f) else Color.White
                    }
                    val chipTextCol = if (isSelected) {
                        Color.White
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                    }
                    Box(
                        modifier = Modifier
                            .background(chipBg, RoundedCornerShape(4.dp))
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else (if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onItemsPerPageChange(size) }
                            .padding(
                                horizontal = if (isVeryCompact) 5.dp else if (isCompact) 6.dp else 8.dp, 
                                vertical = if (isCompact) 3.dp else 4.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = size.toString(),
                            fontSize = if (isCompact) 10.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipTextCol
                        )
                    }
                }
            }

            // Right region: Navigation details & Arrow actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isVeryCompact) 4.dp else 8.dp)
            ) {
                // Page ranges
                Text(
                    text = if (isVeryCompact) "${if (totalItems == 0) 0 else startIndex + 1}-${endIndex}" else "${if (totalItems == 0) 0 else startIndex + 1}-${endIndex} of ${totalItems}",
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Previous Icon Button
                    IconButton(
                        onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                        enabled = currentPage > 1,
                        modifier = Modifier
                            .size(if (isCompact) 24.dp else 28.dp)
                            .background(
                                if (isDark) Color(0xFF131926).copy(alpha = 0.4f) else Color.White,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                                RoundedCornerShape(4.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Previous Page",
                            tint = if (currentPage > 1) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                        )
                    }

                    // Next Icon Button
                    IconButton(
                        onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                        enabled = currentPage < totalPages,
                        modifier = Modifier
                            .size(if (isCompact) 24.dp else 28.dp)
                            .background(
                                if (isDark) Color(0xFF131926).copy(alpha = 0.4f) else Color.White,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                                RoundedCornerShape(4.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Next Page",
                            tint = if (currentPage < totalPages) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableHorizontalScrollIndicator(
    scrollState: ScrollState,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val maxScroll = scrollState.maxValue
    if (maxScroll <= 0) return // Only render if actually scrollable

    val scrollFraction = scrollState.value.toFloat() / maxScroll.toFloat()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Minimalist Scrollbar track
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(4.dp)
                .background(
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(2.dp)
                )
        ) {
            // Sliding thumb
            val thumbWidthFraction = 0.35f
            val thumbWidthRangeFraction = 1f - thumbWidthFraction
            
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerWidth = maxWidth
                val thumbWidth = containerWidth * thumbWidthFraction
                val thumbOffset = containerWidth * thumbWidthRangeFraction * scrollFraction

                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .width(thumbWidth)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun TableHeaderRow(isDark: Boolean) {
    val headerBg = if (isDark) {
        Color(0xFF1E2638)
    } else {
        Color(0xFFF3F4F6)
    }
    val textColor = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f)
    
    Row(
        modifier = Modifier
            .width(1050.dp)
            .background(headerBg, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TYPE", modifier = Modifier.width(110.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
        Text("DOC NUMBER", modifier = Modifier.width(130.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
        Text("CLIENT", modifier = Modifier.width(160.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
        Text("TITLE", modifier = Modifier.width(170.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
        Text("DATE", modifier = Modifier.width(110.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
        Text("AMOUNT", modifier = Modifier.width(130.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text("STATUS", modifier = Modifier.width(110.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("ACTIONS", modifier = Modifier.width(130.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun TableRowItem(
    doc: BusinessDocument,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onStatusUpdate: ((String) -> Unit)? = null,
    onConvertToInvoice: (() -> Unit)? = null,
    isDarkMode: Boolean = true
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(doc.createdAt)
    val colorTag = when (doc.status.uppercase()) {
        "PAID", "APPROVED", "OPEN" -> SuccessGreen
        "PENDING", "ACTIVE" -> AccentGold
        "OVERDUE", "CLOSED", "CLOSE" -> OverdueRed
        else -> MaterialTheme.colorScheme.secondary
    }

    val typeIcon = when (doc.type.uppercase()) {
        "INVOICE" -> Icons.Filled.ReceiptLong
        "QUOTE" -> Icons.Filled.Description
        "BILL" -> Icons.Filled.Payment
        "PO" -> Icons.Filled.LocalShipping
        "SO" -> Icons.Filled.Work
        else -> Icons.Filled.Article
    }

    val rowBg = if (isDarkMode) {
        Color(0xFF131926).copy(alpha = 0.4f)
    } else {
        Color.White.copy(alpha = 0.6f)
    }

    Row(
        modifier = Modifier
            .width(1050.dp)
            .background(rowBg)
            .clickable { onCardClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. TYPE
        Row(
            modifier = Modifier.width(110.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(doc.type, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }

        // 2. DOC NUMBER
        Text(
            text = doc.docNumber,
            modifier = Modifier.width(130.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f)
        )

        // 3. CLIENT
        Text(
            text = doc.clientName,
            modifier = Modifier.width(160.dp),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
        )

        // 4. TITLE
        Text(
            text = doc.title,
            modifier = Modifier.width(170.dp),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
        )

        // 5. DATE
        Text(
            text = dateStr,
            modifier = Modifier.width(110.dp),
            fontSize = 11.sp,
            color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
        )

        // 6. AMOUNT
        Text(
            text = "₹${String.format(java.util.Locale.US, "%,.2f", doc.totalAmount)}",
            modifier = Modifier.width(130.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            color = if (isDarkMode) Color.White else Color.Black
        )

        // 7. STATUS
        Box(
            modifier = Modifier
                .width(110.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            Box(
                modifier = Modifier
                    .background(colorTag.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = doc.status,
                    color = colorTag,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        // 8. ACTIONS
        Row(
            modifier = Modifier.width(130.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isEditableOrDeletable = doc.type.uppercase() != "INVOICE" && doc.type.uppercase() != "BILL"
            
            if (doc.type.uppercase() == "QUOTE") {
                IconButton(
                    onClick = { onConvertToInvoice?.invoke() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cached,
                        contentDescription = "Convert Quote to Invoice",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (doc.status.uppercase() != "OPEN") {
                IconButton(
                    onClick = { onStatusUpdate?.invoke("OPEN") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Open Document",
                        modifier = Modifier.size(14.dp),
                        tint = SuccessGreen
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                if (doc.status.uppercase() != "CLOSED" && doc.status.uppercase() != "CLOSE") {
                    IconButton(
                        onClick = { onStatusUpdate?.invoke("CLOSED") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Document",
                            modifier = Modifier.size(14.dp),
                            tint = OverdueRed
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            if (isEditableOrDeletable) {
                IconButton(
                    onClick = { onEditClick?.invoke() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Document",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Document",
                        modifier = Modifier.size(14.dp),
                        tint = OverdueRed
                    )
                }
            } else {
                IconButton(
                    onClick = onCardClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = "Preview Document",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
 
@Composable
fun CustomThemeDatePickerDialog(
    initialDateStr: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val calendar = remember {
        java.util.Calendar.getInstance().apply {
            if (initialDateStr.isNotBlank()) {
                try {
                    sdf.parse(initialDateStr.trim())?.let {
                        time = it
                    }
                } catch (e: Exception) {}
            }
        }
    }

    var selectedYear by remember { mutableStateOf(calendar.get(java.util.Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(java.util.Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(java.util.Calendar.DAY_OF_MONTH)) }

    val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) CorporateSurfaceDark else Color(0xFFF0FDF4)
            ),
            elevation = CardDefaults.cardElevation(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .width(320.dp)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT DATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else Color(0xFF0C2B1D)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedMonth == 0) {
                                selectedMonth = 11
                                selectedYear -= 1
                            } else {
                                selectedMonth -= 1
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${monthNames[selectedMonth]} $selectedYear",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0C2B1D)
                    )

                    IconButton(
                        onClick = {
                            if (selectedMonth == 11) {
                                selectedMonth = 0
                                selectedYear += 1
                            } else {
                                selectedMonth += 1
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { dayName ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val calcCalendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, selectedYear)
                    set(java.util.Calendar.MONTH, selectedMonth)
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                val firstDayOfWeek = calcCalendar.get(java.util.Calendar.DAY_OF_WEEK)
                val daysInMonth = calcCalendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

                val dayOffset = firstDayOfWeek - 1
                val totalCells = dayOffset + daysInMonth
                val totalRows = (totalCells + 6) / 7

                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - dayOffset + 1
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..daysInMonth) {
                                    val isSelected = selectedDay == dayNumber
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                            .clickable { selectedDay = dayNumber },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White 
                                                    else if (isDark) Color.White.copy(alpha = 0.9f)
                                                    else Color(0xFF0C2B1D)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.height(38.dp)) {
                        Text("Cancel", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val selectedCal = java.util.Calendar.getInstance()
                            selectedCal.set(selectedYear, selectedMonth, selectedDay)
                            onDateSelected(sdf.format(selectedCal.time))
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("OK", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerFieldCompact(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isDark: Boolean,
    textFieldColors: TextFieldColors,
    modifier: Modifier = Modifier
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    if (showCustomPicker) {
        CustomThemeDatePickerDialog(
            initialDateStr = value,
            onDateSelected = onValueChange,
            onDismiss = { showCustomPicker = false },
            isDark = isDark
        )
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp) },
            placeholder = { Text(placeholder, fontSize = 11.sp) },
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(25.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.Event,
                    contentDescription = "Open Date Picker Calendar",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showCustomPicker = true }
        )
    }
}

@Composable
fun AddDocumentDialogForm(
    docToEdit: BusinessDocument? = null,
    viewModel: ErpViewModel,
    onSubmit: (
        type: String,
        title: String,
        client: String,
        amount: Double,
        notes: String,
        hourlyRate: Double,
        docNo: String,
        timerMins: Int,
        status: String,
        issueDateStr: String,
        dueDateStr: String,
        gstRatePct: Double,
        baseAmount: Double
    ) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean = true
) {
    var selectedType by remember { mutableStateOf(docToEdit?.type ?: "INVOICE") }
    var title by remember { mutableStateOf(docToEdit?.title ?: "") }
    var client by remember { mutableStateOf(docToEdit?.clientName ?: "") }
    
    // Support GST and base pricing natively
    var gstRateSelected by remember { mutableStateOf(docToEdit?.gstRatePct ?: 18.0) }
    var baseAmountText by remember { mutableStateOf(docToEdit?.baseAmount?.toString() ?: docToEdit?.totalAmount?.toString() ?: "") }
    
    val calculatedTotalAmount = remember(baseAmountText, gstRateSelected) {
        val base = baseAmountText.toDoubleOrNull() ?: 0.0
        val tax = base * (gstRateSelected / 100.0)
        base + tax
    }
    
    var amountText by remember { mutableStateOf(docToEdit?.totalAmount?.toString() ?: "") }
    
    // Sync amountText when calculations run
    LaunchedEffect(calculatedTotalAmount) {
        if (calculatedTotalAmount > 0.0) {
            amountText = String.format(java.util.Locale.US, "%.2f", calculatedTotalAmount)
        }
    }
    
    var notes by remember { mutableStateOf(docToEdit?.notes ?: "") }
    var hourlyRateText by remember { mutableStateOf(docToEdit?.hourlyRate?.toString() ?: "500") }

    val selectedItems = remember { androidx.compose.runtime.mutableStateListOf<Pair<com.example.data.ErpItem, Int>>() }
    
    LaunchedEffect(selectedItems.toList(), selectedType) {
        if (selectedType == "QUOTE" && selectedItems.isNotEmpty()) {
            val calcBase = selectedItems.sumOf { it.first.rate * it.second }
            val calcGst = selectedItems.sumOf { (it.first.rate * it.second) * (it.first.gstRatePct / 100.0) }
            val calcTotal = calcBase + calcGst
            
            baseAmountText = String.format(java.util.Locale.US, "%.2f", calcBase)
            amountText = String.format(java.util.Locale.US, "%.2f", calcTotal)
            if (calcBase > 0.0) {
                // If the user hasn't tapped an exact tax slab, we can assign the blended rate. Let's do it.
                gstRateSelected = (calcGst / calcBase) * 100.0
            }
        }
    }

    // Expanded properties based on user review preview values
    var customDocNo by remember { mutableStateOf(docToEdit?.docNumber ?: "") }
    var timerMinsText by remember { mutableStateOf(docToEdit?.timerDurationMinutes?.toString() ?: "30") }
    var customStatus by remember { mutableStateOf(docToEdit?.status ?: "") }
    
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val initialIssueStr = docToEdit?.issueDate?.let { try { sdf.format(it) } catch (e: Exception) { "" } } ?: ""
    val initialDueStr = docToEdit?.dueDate?.let { try { sdf.format(it) } catch (e: Exception) { "" } } ?: ""
    var customIssueDateStr by remember { mutableStateOf(initialIssueStr) }
    var customDueDateStr by remember { mutableStateOf(initialDueStr) }

    val statusOptions = remember(selectedType) {
        when (selectedType) {
            "INVOICE" -> listOf("PENDING", "PAID", "OVERDUE", "DRAFT")
            "QUOTE" -> listOf("OPEN", "CLOSED")
            "BILL" -> listOf("PENDING", "PAID", "OVERDUE", "DRAFT")
            "PO" -> listOf("Approved", "Draft", "Completed")
            "SO" -> listOf("Active", "Completed", "IDLE")
            else -> listOf("Draft", "Pending", "Approved")
        }
    }

    LaunchedEffect(selectedType) {
        if (docToEdit == null || customStatus.isEmpty()) {
            customStatus = statusOptions.firstOrNull() ?: ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) CorporateSurfaceDark.copy(alpha = 0.94f) else CorporateSurfaceLight.copy(alpha = 0.98f)),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 480.dp)
                .glassCardAdaptive(shape = RoundedCornerShape(20.dp), isDarkMode = isDark)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    if (docToEdit != null) "Edit Business Document" else "Create Business Document",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Type Toggle Selection
                Text("Select Document Class", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                val typeScrollState = rememberScrollState()
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .horizontalScroll(typeScrollState)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("INVOICE", "QUOTE", "BILL", "PO", "SO").forEach { t ->
                            val isSel = selectedType == t
                            Button(
                                onClick = { selectedType = t },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                    contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(t, fontSize = 8.6.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Left fade & arrow
                    if (typeScrollState.value > 0) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            if (isDark) CorporateSurfaceDark else CorporateSurfaceLight,
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(end = 4.dp)
                                .height(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Right fade & arrow
                    if (typeScrollState.value < typeScrollState.maxValue) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            if (isDark) CorporateSurfaceDark else CorporateSurfaceLight
                                        )
                                    )
                                )
                                .padding(start = 4.dp)
                                .height(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // --- 1. SWIPEABLE ITEM PRESETS CATALOG CAROUSEL ---
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedType == "QUOTE") "Build Multi-Item Quote Roster" else "Select from ERP Preset Item Catalog", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Black, 
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (selectedType == "QUOTE") "Tap to add / increment" else "Tap to pre-fill", 
                        fontSize = 9.sp, 
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                val itemScrollState = rememberScrollState()
                val itemsList by viewModel.items.collectAsState()
                
                if (itemsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No items available in Catalog. Go to Inventory and seed presets.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(itemScrollState)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsList.forEach { pItem ->
                            val qtyInQuote = selectedItems.firstOrNull { it.first.id == pItem.id }?.second ?: 0
                            val isSelected = if (selectedType == "QUOTE") qtyInQuote > 0 else title == pItem.name
                            Card(
                                modifier = Modifier
                                    .width(170.dp)
                                    .clickable {
                                        if (selectedType == "QUOTE") {
                                            val existingIdx = selectedItems.indexOfFirst { it.first.id == pItem.id }
                                            if (existingIdx >= 0) {
                                                val (existingItem, existingQty) = selectedItems[existingIdx]
                                                selectedItems[existingIdx] = Pair(existingItem, existingQty + 1)
                                            } else {
                                                selectedItems.add(Pair(pItem, 1))
                                            }
                                            if (title.isBlank() || title == "") {
                                                title = "Multi-Item Estimate / Quote"
                                            }
                                        } else {
                                            title = pItem.name
                                            baseAmountText = pItem.rate.toString()
                                            gstRateSelected = pItem.gstRatePct
                                            hourlyRateText = pItem.rate.toString()
                                            notes = pItem.description
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pItem.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (qtyInQuote > 0 && selectedType == "QUOTE") {
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("x$qtyInQuote", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                    Text(pItem.category, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("₹${String.format(java.util.Locale.US, "%,.0f", pItem.rate)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("GST ${pItem.gstRatePct.toInt()}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive listed items row specifically for multi-item quotes Setup
                if (selectedType == "QUOTE") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Added Quote Items List", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (selectedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Roster holds 0 items. Click catalog items above to add.", 
                                fontSize = 11.sp, 
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            selectedItems.forEachIndexed { index, (item, qty) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text("Rate: ₹${String.format(java.util.Locale.US, "%,.2f", item.rate)} (+${item.gstRatePct.toInt()}% GST)", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (qty > 1) {
                                                        selectedItems[index] = Pair(item, qty - 1)
                                                    } else {
                                                        selectedItems.removeAt(index)
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                            ) {
                                                Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(12.dp))
                                            }
                                            
                                            Text(qty.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                            
                                            IconButton(
                                                onClick = {
                                                    selectedItems[index] = Pair(item, qty + 1)
                                                },
                                                modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                            }
                                            
                                            Spacer(modifier = Modifier.width(6.dp))
                                            
                                            IconButton(
                                                onClick = {
                                                    selectedItems.removeAt(index)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )

                // 1. Client & Document Info Group
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title/Item", fontSize = 11.sp) },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(25.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text("Vendor / Client Name", fontSize = 11.sp) },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(25.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Combine Document Number override & amount fields side-by-side to optimize space
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = customDocNo,
                        onValueChange = { customDocNo = it },
                        label = { Text("Doc ID Override", fontSize = 11.sp) },
                        placeholder = { Text("INV-XXX", fontSize = 11.sp) },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(25.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = baseAmountText,
                        onValueChange = { 
                            baseAmountText = it
                        },
                        label = { Text("Base Price (₹)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(25.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // --- GST TAX SELECTION ROW NATIVE IN ALL FORMS ---
                Spacer(modifier = Modifier.height(14.dp))
                Text("Select GST Slabs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                        val isSelected = gstRateSelected == rate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { gstRateSelected = rate }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${rate.toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // --- LIVE GST TAX OUTLAY CALCULATION SUMMARY CARD ---
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Base Price Amount:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val baseVal = baseAmountText.toDoubleOrNull() ?: 0.0
                            Text("₹${String.format(java.util.Locale.US, "%,.2f", baseVal)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GST Selected Slab:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${gstRateSelected.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Applicable Tax (CGST+SGST):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val baseVal = baseAmountText.toDoubleOrNull() ?: 0.0
                            val taxVal = baseVal * (gstRateSelected / 100.0)
                            Text("₹${String.format(java.util.Locale.US, "%,.2f", taxVal)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Calculated Total Amount:", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text("₹${String.format(java.util.Locale.US, "%,.2f", calculatedTotalAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (selectedType == "SO") {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = hourlyRateText,
                            onValueChange = { hourlyRateText = it },
                            label = { Text("Hourly Rate (₹)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(25.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = timerMinsText,
                            onValueChange = { timerMinsText = it },
                            label = { Text("Duration Limit (mins)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(25.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Issue & Due date Picker row side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DatePickerFieldCompact(
                        value = customIssueDateStr,
                        onValueChange = { customIssueDateStr = it },
                        label = "Issue Date",
                        placeholder = "Select Date",
                        isDark = isDark,
                        textFieldColors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerFieldCompact(
                        value = customDueDateStr,
                        onValueChange = { customDueDateStr = it },
                        label = "Due Date",
                        placeholder = "Select Date",
                        isDark = isDark,
                        textFieldColors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status chip single compact horizontal scroll
                Text("Initial Workflow Status", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statusOptions.forEach { opt ->
                        val isSelected = customStatus == opt
                        FilterChip(
                            selected = isSelected,
                            onClick = { customStatus = opt },
                            label = { Text(opt, fontSize = 8.5.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes input field (Paragraph Style)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Terms & Conditions (Remarks)", fontSize = 11.sp) },
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = false,
                    minLines = 3,
                    maxLines = 4,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 82.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("Discard", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.height(38.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        onClick = {
                            if (title.isNotBlank() && client.isNotBlank()) {
                                val baseVal = baseAmountText.toDoubleOrNull() ?: 0.0
                                val calculatedAmt = if (baseVal > 0.0) calculatedTotalAmount else (amountText.toDoubleOrNull() ?: 0.0)
                                val hrs = hourlyRateText.toDoubleOrNull() ?: 0.0
                                val mins = timerMinsText.toIntOrNull() ?: 0
                                
                                val formattedNotes = if (selectedType == "QUOTE" && selectedItems.isNotEmpty()) {
                                    val sb = StringBuilder()
                                    sb.append("Multi-Item Estimate Details:\n")
                                    selectedItems.forEach { (item, qty) ->
                                        val lineBase = item.rate * qty
                                        val lineTax = lineBase * (item.gstRatePct / 100.0)
                                        val lineTotal = lineBase + lineTax
                                        sb.append("- ${item.name} ($qty units) @ ₹${String.format(java.util.Locale.US, "%,.2f", item.rate)} (+${item.gstRatePct.toInt()}% GST)\n")
                                        sb.append("  Subtotal: ₹${String.format(java.util.Locale.US, "%,.2f", lineBase)} | Total: ₹${String.format(java.util.Locale.US, "%,.2f", lineTotal)}\n")
                                    }
                                    sb.append("\nSummary:")
                                    val calcBase = selectedItems.sumOf { it.first.rate * it.second }
                                    val calcGst = selectedItems.sumOf { (it.first.rate * it.second) * (it.first.gstRatePct / 100.0) }
                                    sb.append("\n• Total Base: ₹${String.format(java.util.Locale.US, "%,.2f", calcBase)}")
                                    sb.append("\n• Total GST: ₹${String.format(java.util.Locale.US, "%,.2f", calcGst)}")
                                    sb.append("\n• Grand Estimated Total: ₹${String.format(java.util.Locale.US, "%,.2f", calcBase + calcGst)}")
                                    if (notes.isNotBlank() && !notes.startsWith("Multi-Item Estimate Details")) {
                                        sb.append("\n\nVendor Remarks:\n").append(notes)
                                    }
                                    sb.toString()
                                } else {
                                    notes
                                }

                                onSubmit(
                                    selectedType,
                                    title,
                                    client,
                                    calculatedAmt,
                                    formattedNotes,
                                    hrs,
                                    customDocNo,
                                    mins,
                                    customStatus,
                                    customIssueDateStr,
                                    customDueDateStr,
                                    gstRateSelected,
                                    baseVal
                                )
                            }
                        }
                    ) {
                        Text(if (docToEdit != null) "Update Document" else "Save & Create", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PdfRendererPreviewPanel(
    doc: BusinessDocument,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    viewModel: ErpViewModel
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(doc.createdAt)
    val dueStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(doc.dueDate)

    val sheetBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val textPrimary = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val infoPanelBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)
    val dividerColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.LightGray
    val brandColors = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF1E40AF)
    val blueTagBg = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFEFF6FF)
    val blueTagText = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF1E40AF)
    val grandTotalText = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0F172A)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = sheetBg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .widthIn(max = 500.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(sheetBg)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Branded Company Logo Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.logo),
                    contentDescription = "Abielan Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ABIELAN CORP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "INTELLIGENT ERP SYSTEM",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // PDF A4 Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Abielan ERP Document", color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("Digital certified duplicate", color = textSecondary, fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier
                        .background(blueTagBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(doc.type, color = blueTagText, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Invoice specifics block (high contrast, beautifully aligned side-by-side)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(infoPanelBg, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DOCUMENT STATS ID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(doc.docNumber, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("STATEMENT DATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(dateStr, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("DUE DATE TRANSCRIPT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(dueStr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("FIRM TRANS_ID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("TXN-GRP-${doc.id}-A", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Parties details
            Text("ISSUED TO (CLIENT):", fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(doc.clientName, fontSize = 15.sp, fontWeight = FontWeight.Black, color = textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Registered corporate workspace partner profile.", fontSize = 11.sp, color = textSecondary)

            Spacer(modifier = Modifier.height(18.dp))

            // Line breakdown item table
            Text("BILLABLE ITEMS AND COMMODITIES", fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(doc.title, modifier = Modifier.weight(1.5f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("1 Unit", modifier = Modifier.weight(0.5f), fontSize = 12.sp, color = textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(
                    "₹${String.format(java.util.Locale.US, "%,.2f", doc.totalAmount)}",
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = dividerColor, thickness = 1.dp)

            Spacer(modifier = Modifier.height(18.dp))

            // Total financial calculation layout (well-aligned hierarchy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subtotal:", fontSize = 12.sp, color = textSecondary)
                        Text("₹${String.format(java.util.Locale.US, "%,.2f", doc.totalAmount)}", fontSize = 12.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GST (18% equivalent):", fontSize = 11.sp, color = textSecondary)
                        Text("₹${String.format(java.util.Locale.US, "%,.2f", doc.totalAmount * 0.18)}", fontSize = 11.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GRAND TOTAL:", fontSize = 13.sp, fontWeight = FontWeight.Black, color = grandTotalText)
                        Text(
                            "₹${String.format(java.util.Locale.US, "%,.2f", doc.totalAmount * 1.18)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = brandColors
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document Notes
            if (doc.notes.isNotBlank()) {
                Text("NOTES: ", fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                Text(doc.notes, fontSize = 12.sp, color = textSecondary)
            }

            if (doc.type.uppercase() == "QUOTE") {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = dividerColor)
                Spacer(modifier = Modifier.height(12.dp))
                Text("QUOTE WORKFLOW MANAGEMENT", fontSize = 12.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge Indicator
                    Box(
                        modifier = Modifier
                            .background(
                                when (doc.status.uppercase()) {
                                    "OPEN" -> SuccessGreen.copy(alpha = 0.15f)
                                    "CLOSED", "CLOSE" -> OverdueRed.copy(alpha = 0.15f)
                                    else -> textSecondary.copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "STATUS: ${doc.status.uppercase()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when (doc.status.uppercase()) {
                                "OPEN" -> SuccessGreen
                                "CLOSED", "CLOSE" -> OverdueRed
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                    }

                    // Convert Button
                    Button(
                        onClick = {
                            viewModel.convertQuoteToInvoice(doc.id)
                            onDismiss() // Dismiss panel after conversion
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Filled.Cached, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Convert to Invoice", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (doc.status.uppercase() != "OPEN") {
                        Button(
                            onClick = {
                                viewModel.updateDocumentStatus(doc.id, "OPEN")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Status: OPEN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (doc.status.uppercase() != "CLOSED" && doc.status.uppercase() != "CLOSE") {
                        Button(
                            onClick = {
                                viewModel.updateDocumentStatus(doc.id, "CLOSED")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OverdueRed),
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Status: CLOSED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ACTION CONTROL TRIGGERS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close/Cancel Trigger
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Preview",
                        modifier = Modifier.size(22.dp),
                        tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // PDF Download trigger
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Download PDF",
                        modifier = Modifier.size(22.dp),
                        tint = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF1E40AF)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Share trigger
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share Document",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

fun generatePdfDocumentFile(context: Context, doc: BusinessDocument): File? {
    try {
        val pdfDocument = PdfDocument()
        // Page definition: A4 style (595 x 842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paint definitions
        val bgPaint = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // Brands / Colors (Forest green: 0xFF15803D, Sandalwood gold: 0xFFB58E49)
        val primaryPaint = Paint().apply {
            color = AndroidColor.parseColor("#15803D")
            isAntiAlias = true
        }

        val textPrimaryPaint = Paint().apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 12f
            isAntiAlias = true
        }

        // Draw a premium top banner header line in Forest Green and Sandalwood Gold
        val bannerPaint1 = Paint().apply { color = AndroidColor.parseColor("#15803D") }
        val bannerPaint2 = Paint().apply { color = AndroidColor.parseColor("#B58E49") }
        canvas.drawRect(0f, 0f, 595f, 15f, bannerPaint1)
        canvas.drawRect(0f, 15f, 595f, 20f, bannerPaint2)

        // Draw the Logo at top left from app drawable
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, com.example.R.drawable.logo)
        if (drawable != null) {
            drawable.setBounds(40, 45, 75, 80)
            drawable.draw(canvas)
        } else {
            // Fallback: A green & gold rounded rect emblem
            val emblemPaint = Paint().apply {
                color = AndroidColor.parseColor("#15803D")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(40f, 45f, 75f, 80f, 8f, 8f, emblemPaint)
            
            val emblemInnerPaint = Paint().apply {
                color = AndroidColor.parseColor("#B58E49")
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(43f, 48f, 72f, 77f, 6f, 6f, emblemInnerPaint)
        }

        // System logo description text
        val titlePaint = Paint().apply {
            color = AndroidColor.parseColor("#15803D")
            textSize = 16f
            strokeWidth = 2f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("ABIELAN ERP CORP", 85f, 62f, titlePaint)

        val subTitlePaint = Paint().apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 8f
            isAntiAlias = true
        }
        canvas.drawText("INTELLIGENT ERP SYSTEM  •  CORPORATE LEDGER", 85f, 75f, subTitlePaint)

        // Badge indicator right-aligned
        val badgeBgPaint = Paint().apply {
            color = AndroidColor.parseColor("#EFF6FF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(440f, 48f, 545f, 75f, 6f, 6f, badgeBgPaint)

        val badgeTextPaint = Paint().apply {
            color = AndroidColor.parseColor("#1E40AF")
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(doc.type.uppercase(), 492.5f, 65f, badgeTextPaint)

        // Divider
        val linePaint = Paint().apply {
            color = AndroidColor.parseColor("#E2E8F0")
            strokeWidth = 1f
        }
        canvas.drawLine(40f, 100f, 545f, 100f, linePaint)

        // Metadata block (Date, ID, Client)
        val metaLabelPaint = Paint().apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 9f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val metaValPaint = Paint().apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 11f
            isAntiAlias = true
            isFakeBoldText = true
        }

        // Row 1
        canvas.drawText("DOCUMENT ID", 40f, 125f, metaLabelPaint)
        canvas.drawText(doc.docNumber, 40f, 142f, metaValPaint)

        canvas.drawText("DUPLICATE DATE", 220f, 125f, metaLabelPaint)
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        canvas.drawText(format.format(doc.createdAt), 220f, 142f, metaValPaint)

        canvas.drawText("DUE DATE TRANSCRIPT", 390f, 125f, metaLabelPaint)
        canvas.drawText(format.format(doc.dueDate), 390f, 142f, metaValPaint)

        // Row 2
        canvas.drawText("ISSUED TO (CLIENT PARTNER)", 40f, 180f, metaLabelPaint)
        val clientPaint = Paint().apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val truncatedClientName = if (doc.clientName.length > 32) doc.clientName.substring(0, 29) + "..." else doc.clientName
        canvas.drawText(truncatedClientName, 40f, 200f, clientPaint)

        canvas.drawText("GSTIN REGISTRY / STATUS", 390f, 180f, metaLabelPaint)
        canvas.drawText(doc.status, 390f, 200f, metaValPaint)

        canvas.drawLine(40f, 225f, 545f, 225f, linePaint)

        // Table Header
        canvas.drawText("BILLABLE ITEMS AND COMMODITIES REPORT", 40f, 250f, metaLabelPaint)
        canvas.drawLine(40f, 260f, 545f, 260f, linePaint)

        // Item title
        val itemTitlePaint = Paint().apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val truncatedTitle = if (doc.title.length > 28) doc.title.substring(0, 25) + "..." else doc.title
        canvas.drawText(truncatedTitle, 40f, 285f, itemTitlePaint)
        canvas.drawText("1 Unit", 280f, 285f, textPrimaryPaint)
        
        val pricePaint = Paint().apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("₹${String.format(java.util.Locale.US, "%,.2f", doc.totalAmount)}", 545f, 285f, pricePaint)

        canvas.drawLine(40f, 305f, 545f, 305f, linePaint)

        // Calculations
        val totalLabelPaint = Paint().apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 10f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val totalValPaint = Paint().apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("Subtotal:", 440f, 335f, totalLabelPaint)
        canvas.drawText("₹${String.format(java.util.Locale.US, "%,.2f", doc.totalAmount)}", 545f, 335f, totalValPaint)

        canvas.drawText("GST (18% equivalent):", 440f, 355f, totalLabelPaint)
        canvas.drawText("₹${String.format(java.util.Locale.US, "%,.2f", doc.totalAmount * 0.18)}", 545f, 355f, totalValPaint)

        val grandLabelPaint = Paint().apply {
            color = AndroidColor.parseColor("#15803D")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val grandValPaint = Paint().apply {
            color = AndroidColor.parseColor("#15803D")
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawLine(350f, 370f, 545f, 370f, linePaint)
        canvas.drawText("GRAND TOTAL:", 440f, 395f, grandLabelPaint)
        canvas.drawText("₹${String.format(java.util.Locale.US, "%,.2f", doc.totalAmount * 1.18)}", 545f, 395f, grandValPaint)

        // Notes section
        if (doc.notes.isNotBlank()) {
            canvas.drawText("NOTES & EXCLUSIONS:", 40f, 435f, metaLabelPaint)
            val notesPaint = Paint().apply {
                color = AndroidColor.parseColor("#475569")
                textSize = 9f
                isAntiAlias = true
            }
            // Line wrapping helper for notes to prevent PDF text collapsing
            val words = doc.notes.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""
            for (word in words) {
                if ((currentLine + " " + word).length > 80) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            var yOffset = 452f
            for (line in lines.take(3)) {
                canvas.drawText(line, 40f, yOffset, notesPaint)
                yOffset += 14f
            }
        }

        // Digital Certified Stamps & Signatures
        val stampBg = Paint().apply {
            color = AndroidColor.parseColor("#DCFCE7")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(40f, 680f, 200f, 740f, 10f, 10f, stampBg)

        val stampBorder = Paint().apply {
            color = AndroidColor.parseColor("#16A34A")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(40f, 680f, 200f, 740f, 10f, 10f, stampBorder)

        val stampText1 = Paint().apply {
            color = AndroidColor.parseColor("#16A34A")
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ABIELAN CRYPTO STAMP", 120f, 705f, stampText1)

        val stampText2 = Paint().apply {
            color = AndroidColor.parseColor("#15803D")
            textSize = 8f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("AUTONOMOUSLY VERIFIED", 120f, 722f, stampText2)

        // Footer copyright / disclaimer
        val footerPaint = Paint().apply {
            color = AndroidColor.parseColor("#94A3B8")
            textSize = 8f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This is an official system generated business transcript. Certified autonomously from Abielan ERP System.", 297.5f, 780f, footerPaint)

        pdfDocument.finishPage(page)

        // Create temporary PDF file inside app private files/cache directory
        val pdfFile = File(context.filesDir, "${doc.docNumber}.pdf")
        val outStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outStream)
        outStream.close()
        pdfDocument.close()

        return pdfFile
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun downloadDocumentPdf(context: Context, doc: BusinessDocument) {
    try {
        val pdfFile = generatePdfDocumentFile(context, doc)
        if (pdfFile == null) {
            Toast.makeText(context, "Error compiling the PDF transcript source.", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = "${doc.docNumber}.pdf"
        val contentResolver = context.contentResolver
        var isDownloadedSuccessfully = false
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    pdfFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                isDownloadedSuccessfully = true
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, fileName)
            pdfFile.copyTo(destFile, overwrite = true)
            isDownloadedSuccessfully = true
        }

        if (isDownloadedSuccessfully) {
            Toast.makeText(context, "Successfully downloaded: $fileName to Downloads!", Toast.LENGTH_LONG).show()
            showDownloadNotification(context, doc, pdfFile)
        } else {
            Toast.makeText(context, "Failed to create public downloads entry.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun showDownloadNotification(context: Context, doc: BusinessDocument, pdfFile: File) {
    try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "document_downloads_channel"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Document Downloads (Abielan ERP)",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when ledger documents and PDF transcripts are generated output successfully"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            pdfFile
        )
        
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            doc.docNumber.hashCode(),
            viewIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Finished")
            .setContentText("Ledger receipt ${doc.docNumber} downloaded successfully. Tap to review.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            
        notificationManager.notify(doc.docNumber.hashCode(), builder.build())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun shareDocumentPdf(context: Context, doc: BusinessDocument) {
    try {
        val pdfFile = generatePdfDocumentFile(context, doc)
        if (pdfFile == null) {
            Toast.makeText(context, "Error compiling the PDF transcript source.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            pdfFile
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ledger Doc: ${doc.docNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, "Share Certified Ledger PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
