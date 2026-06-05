package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ErpItem
import com.example.ui.theme.glassCardAdaptive
import com.example.viewmodel.ErpViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryScreenModule(viewModel: ErpViewModel) {
    val state by viewModel.uiState.collectAsState()
    val itemsList by viewModel.items.collectAsState()
    val scrollState = rememberScrollState()
    
    // Sub-tab selection: "catalog" (Items & Stock), "tax" (Tax Setup & Slabs)
    var selectedSubTab by remember { mutableStateOf("catalog") }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ErpItem?>(null) }
    
    // Dynamic Search & Category Filtering + List/Grid Toggle & Pagination
    var searchQuery by remember { mutableStateOf("") }
    var activeCategoryFilter by remember { mutableStateOf("ALL") } // "ALL", "PRODUCTS", "SERVICES", "LOW_STOCK"
    var isGridView by remember { mutableStateOf(false) } // Default: false (means tabular list view)
    var currentPage by remember { mutableStateOf(1) }
    var itemsPerPage by remember { mutableStateOf(10) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Animated Content depending on active sub-tab (rendered statically here inside conditional structure)
        if (selectedSubTab == "catalog") {
            // --- SECTION 1: ITEMS CATALOG & LIVE STOCK METRICS ---

            // Filtering logic
            val filteredItemsList = itemsList.filter { item ->
                val query = searchQuery.lowercase().trim()
                val matchesSearch = query.isEmpty() ||
                        item.name.lowercase().contains(query) ||
                        item.category.lowercase().contains(query) ||
                        item.description.lowercase().contains(query)

                val matchesCat = when (activeCategoryFilter) {
                    "PRODUCTS" -> item.trackStock
                    "SERVICES" -> !item.trackStock
                    "LOW_STOCK" -> item.trackStock && (item.stockQuantity <= item.lowStockThreshold)
                    else -> true
                }
                matchesSearch && matchesCat
            }

            val totalPages = if (filteredItemsList.isEmpty()) 1 else java.lang.Math.ceil(filteredItemsList.size.toDouble() / itemsPerPage).toInt()
            val safeCurrentPage = currentPage.coerceIn(1, totalPages)
            val startIndex = (safeCurrentPage - 1) * itemsPerPage
            val endIndex = (startIndex + itemsPerPage).coerceAtMost(filteredItemsList.size)
            val paginatedItems = if (filteredItemsList.isEmpty()) emptyList() else filteredItemsList.subList(startIndex, endIndex)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                item {
                    // Broad Header (Moved Inside Scroll, up to navbar)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Inventory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Inventory & Tax Hub",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Text(
                        text = "Track active asset warehouse levels, configure low-stock signals, and manage tax mappings.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                item {
                    // Custom Sub-Tabs Switcher (Pill style, Inside Scroll, up to navbar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(
                                if (state.isDarkMode) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFE2E8F0),
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
                                    if (selectedSubTab == "catalog") MaterialTheme.colorScheme.primary 
                                    else (if (state.isDarkMode) Color(0xFF1E293B) else Color.White)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selectedSubTab == "catalog") MaterialTheme.colorScheme.primary
                                            else (if (state.isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedSubTab = "catalog" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Warehouse,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (selectedSubTab == "catalog") Color.White else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Stock & Items Catalog",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedSubTab == "catalog") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selectedSubTab == "tax") MaterialTheme.colorScheme.primary 
                                    else (if (state.isDarkMode) Color(0xFF1E293B) else Color.White)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selectedSubTab == "tax") MaterialTheme.colorScheme.primary
                                            else (if (state.isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedSubTab = "tax" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Percent,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (selectedSubTab == "tax") Color.White else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GST Tax Setup Slabs",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedSubTab == "tax") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                item {
                    // Stock stats cards summary row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val totalItems = itemsList.size
                        val trackedItemsCount = itemsList.count { it.trackStock }
                        val lowStockItemsCount = itemsList.count { it.trackStock && it.stockQuantity <= it.lowStockThreshold }
                        
                        Card(
                            modifier = Modifier.weight(1.0f),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(75.dp)
                                    .glassCardAdaptive(shape = RoundedCornerShape(12.dp), isDarkMode = state.isDarkMode)
                                    .padding(12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column {
                                    Text("Total Items", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text("$totalItems SKU", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        
                        Card(
                            modifier = Modifier.weight(1.0f),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(75.dp)
                                    .glassCardAdaptive(shape = RoundedCornerShape(12.dp), isDarkMode = state.isDarkMode)
                                    .padding(12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column {
                                    Text("Stock Tracked", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text("$trackedItemsCount Products", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                        
                        Card(
                            modifier = Modifier.weight(1.0f),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(75.dp)
                                    .glassCardAdaptive(shape = RoundedCornerShape(12.dp), isDarkMode = state.isDarkMode)
                                    .padding(12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column {
                                    Text("Low Stock Warnings", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text(
                                        text = "$lowStockItemsCount Alert", 
                                        fontSize = 16.sp, 
                                        fontWeight = FontWeight.Black, 
                                        color = if (lowStockItemsCount > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }

                    // SKU Items Listing Cards Title
                    Text(
                        text = "Preset SKUs & Warehouse Availability",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                stickyHeader {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 4.dp)
                    ) {
                        // --- SEARCH BAR & TOGGLE ---
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Search box
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .border(
                                        1.dp,
                                        if (state.isDarkMode) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
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
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search Name, Class, Specifications...",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { 
                                            searchQuery = it 
                                            currentPage = 1
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { 
                                            searchQuery = ""
                                            currentPage = 1
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Layout toggle styled to look like button
                            OutlinedButton(
                                onClick = { isGridView = !isGridView },
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
                                    imageVector = if (isGridView) Icons.Filled.List else Icons.Filled.GridView,
                                    contentDescription = "Toggle Layout",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // '+' Add Predefined SKU Button
                            Button(
                                onClick = { showAddItemDialog = true },
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
                                    contentDescription = "Add Predefined SKU",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Category Filter pills row with Horizontal Scroll
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val filters = listOf(
                                "ALL" to "All Classes",
                                "PRODUCTS" to "Stock Managed",
                                "SERVICES" to "Non-Stock/Services",
                                "LOW_STOCK" to "Low Stock Alerts"
                            )
                            filters.forEach { (filterKey, filterLabel) ->
                                val isSelected = activeCategoryFilter == filterKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        activeCategoryFilter = filterKey
                                        currentPage = 1
                                    },
                                    label = { Text(filterLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                    }
                }

                item {
                    if (filteredItemsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No items found matching the filters.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    } else {
                if (!isGridView) {
                    // Corporate Table List View (Exactly like Finance tab layout design)
                    val tableScrollState = rememberScrollState()
                    val containerBg = if (state.isDarkMode) Color(0xFF131926).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f)
                    val borderColor = if (state.isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = containerBg)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                Column(
                                    modifier = Modifier
                                        .wrapContentHeight()
                                        .horizontalScroll(tableScrollState)
                                ) {
                                    InventoryTableHeaderRow(isDark = state.isDarkMode)
                                    
                                    Column(
                                        modifier = Modifier
                                            .width(1010.dp)
                                            .padding(bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        paginatedItems.forEach { pItem ->
                                            InventoryTableRowItem(
                                                pItem = pItem,
                                                isDark = state.isDarkMode,
                                                viewModel = viewModel,
                                                onEditClick = { editingItem = pItem }
                                            )
                                            HorizontalDivider(
                                                color = borderColor,
                                                thickness = 1.dp
                                            )
                                        }
                                    }
                                }
                            }

                            // Dynamic compact horizontal scroll bar
                            TableHorizontalScrollIndicator(
                                scrollState = tableScrollState,
                                isDark = state.isDarkMode,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )

                            HorizontalDivider(color = borderColor, thickness = 1.dp)

                            // Pagination controls matching Finance Screen layout
                            InventoryPaginationControl(
                                currentPage = safeCurrentPage,
                                totalPages = totalPages,
                                totalItems = filteredItemsList.size,
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
                    // Modern Compact Grid View - Redesigned to be highly responsive and spacious, eliminating squeezing
                    val configuration = LocalConfiguration.current
                    val isWideScreen = configuration.screenWidthDp >= 600
                    val columnsCount = if (isWideScreen) 2 else 1
                    val chunkedItems = paginatedItems.chunked(columnsCount)
                    val containerBg = if (state.isDarkMode) Color(0xFF131926).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f)
                    val borderColor = if (state.isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = containerBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                chunkedItems.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        rowItems.forEach { pItem ->
                                            val isLowStock = pItem.trackStock && pItem.stockQuantity <= pItem.lowStockThreshold
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(vertical = 2.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .glassCardAdaptive(shape = RoundedCornerShape(16.dp), isDarkMode = state.isDarkMode)
                                                        .border(
                                                            width = if (isLowStock) 1.dp else 0.dp,
                                                            color = if (isLowStock) Color(0xFFEF4444).copy(alpha = 0.4f) else Color.Transparent,
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                        .padding(14.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Row 1: Category Tag and Edit & Delete Actions on top of the card
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = pItem.category.uppercase(java.util.Locale.US),
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Black,
                                                                    letterSpacing = 0.4.sp,
                                                                    color = MaterialTheme.colorScheme.secondary,
                                                                    maxLines = 1,
                                                                    softWrap = false,
                                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                                )
                                                            }
                                                            
                                                            // Optional tiny status indicator (low stock warning)
                                                            if (isLowStock) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "LOW ALERT",
                                                                        fontSize = 8.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFFEF4444),
                                                                        maxLines = 1,
                                                                        softWrap = false,
                                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.width(4.dp))

                                                        // Action buttons side-by-side on top-right (Strictly compact sizing to prevent overlaps)
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            IconButton(
                                                                onClick = { editingItem = pItem },
                                                                modifier = Modifier
                                                                    .size(22.dp)
                                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Edit,
                                                                    contentDescription = "Edit Preset SKU",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                            }

                                                            IconButton(
                                                                onClick = { viewModel.removeItem(pItem) },
                                                                modifier = Modifier
                                                                    .size(22.dp)
                                                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Delete,
                                                                    contentDescription = "Remove Preset SKU",
                                                                    tint = MaterialTheme.colorScheme.error,
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                            }
                                                        }
                                                    }
 
                                                    // Row 2: Name
                                                    Text(
                                                        text = pItem.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (state.isDarkMode) Color.White else Color.Black,
                                                        maxLines = 2,
                                                        lineHeight = 17.sp,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
 
                                                    // Row 3: Description (clean, reduced text height)
                                                    if (pItem.description.isNotBlank()) {
                                                        Text(
                                                            text = pItem.description,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            maxLines = 1,
                                                            lineHeight = 13.sp,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    if (pItem.vendorName.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Vendor: ", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                                            Text(
                                                                text = pItem.vendorName,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.clickable {
                                                                    viewModel.selectAndNavigateToPartnerProfile(pItem.vendorName, "VENDOR")
                                                                }
                                                            )
                                                        }
                                                    }
 
                                                    // Row 4: Pricing & GST Details in a neat, balanced horizontal row
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Rate: ₹${String.format(java.util.Locale.US, "%,.2f", pItem.rate)}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "${pItem.gstRatePct.toInt()}% GST",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
 
                                                    HorizontalDivider(
                                                        color = if (state.isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                                                        thickness = 1.dp,
                                                        modifier = Modifier.padding(vertical = 1.dp)
                                                    )
 
                                                    // Row 5: Stock Status Badge & Counter Modifier Controls
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Stock Info Level Badge (Styled with weight & ellipsis to prevent overlaps on thin cards)
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        ) {
                                                            if (pItem.trackStock) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(
                                                                            if (isLowStock) Color(0xFFEF4444).copy(alpha = 0.12f)
                                                                            else Color(0xFF10B981).copy(alpha = 0.12f),
                                                                            RoundedCornerShape(6.dp)
                                                                        )
                                                                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "${pItem.stockQuantity} in Stock",
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 9.sp,
                                                                        color = if (isLowStock) Color(0xFFEF4444) else Color(0xFF10B981),
                                                                        maxLines = 1,
                                                                        softWrap = false,
                                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            } else {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "⚙ Unlimited Service",
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 9.sp,
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        maxLines = 1,
                                                                        softWrap = false,
                                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
 
                                                        Spacer(modifier = Modifier.width(4.dp))

                                                        // Adjust stock quantity controls on the right side
                                                        if (pItem.trackStock) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                            ) {
                                                                // Minus Button
                                                                IconButton(
                                                                    onClick = { viewModel.adjustItemStock(pItem.id, (pItem.stockQuantity - 1).coerceAtLeast(0)) },
                                                                    modifier = Modifier
                                                                        .size(22.dp)
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Filled.Remove,
                                                                        contentDescription = "Decrease Stock Level",
                                                                        modifier = Modifier.size(9.dp)
                                                                    )
                                                                }
 
                                                                Text(
                                                                    text = pItem.stockQuantity.toString(),
                                                                    fontWeight = FontWeight.Black,
                                                                    fontSize = 10.sp,
                                                                    modifier = Modifier.width(18.dp),
                                                                    textAlign = TextAlign.Center
                                                                )
 
                                                                // Plus Button
                                                                IconButton(
                                                                    onClick = { viewModel.adjustItemStock(pItem.id, pItem.stockQuantity + 1) },
                                                                    modifier = Modifier
                                                                        .size(22.dp)
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Filled.Add,
                                                                        contentDescription = "Increase Stock Level",
                                                                        modifier = Modifier.size(9.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (rowItems.size < columnsCount) {
                                                        repeat(columnsCount - rowItems.size) {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = borderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Compact Pagination matching clean layout
                            InventoryPaginationControl(
                                currentPage = safeCurrentPage,
                                totalPages = totalPages,
                                totalItems = filteredItemsList.size,
                                itemsPerPage = itemsPerPage,
                                startIndex = startIndex,
                                endIndex = endIndex,
                                onPageChange = { currentPage = it },
                                onItemsPerPageChange = { 
                                    itemsPerPage = it 
                                    currentPage = 1
                                },
                                isDark = state.isDarkMode,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
} else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Broad Header (Inside Scroll)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inventory & Tax Hub",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Text(
                    text = "Track active asset warehouse levels, configure low-stock signals, and manage tax mappings.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Custom Sub-Tabs Switcher (Pill style, Inside Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(
                            if (state.isDarkMode) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFE2E8F0),
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
                                if (selectedSubTab == "catalog") MaterialTheme.colorScheme.primary 
                                else (if (state.isDarkMode) Color(0xFF1E293B) else Color.White)
                            )
                            .border(
                                width = 1.dp,
                                color = if (selectedSubTab == "catalog") MaterialTheme.colorScheme.primary
                                        else (if (state.isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedSubTab = "catalog" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Warehouse,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (selectedSubTab == "catalog") Color.White else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Stock & Items Catalog",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSubTab == "catalog") Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedSubTab == "tax") MaterialTheme.colorScheme.primary 
                                else (if (state.isDarkMode) Color(0xFF1E293B) else Color.White)
                            )
                            .border(
                                width = 1.dp,
                                color = if (selectedSubTab == "tax") MaterialTheme.colorScheme.primary
                                        else (if (state.isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedSubTab = "tax" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Percent,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (selectedSubTab == "tax") Color.White else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GST Tax Setup Slabs",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSubTab == "tax") Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // --- SECTION 2: GST TAX SETUP SLABS & MAPPINGS ("also this tax setup as another") ---
            Text(
                text = "GST Bracket Setup System",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "These structures govern standard domestic and interstate billing calculations (CGST split equally with SGST, IGST comprising the total percentage).",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val slabs = listOf(
                Triple("0% Slab", 0.0, "Essential food grains, healthcare basics, simple services and primary education items."),
                Triple("5% Slab", 5.0, "Bulk essential commodities, diagnostic support, packaged domestic foods, simple contracts."),
                Triple("12% Slab", 12.0, "Standard business computers, software products, professional licenses, construction units."),
                Triple("18% Slab", 18.0, "General services, consulting work, industrial accessories, server-level virtual nodes, licenses."),
                Triple("28% Slab", 28.0, "Luxury automotive goods, custom graphics computing hardware, premium air leases, high-end assets.")
            )

            slabs.forEach { (titleLabel, pctVal, descriptionText) ->
                val mappedItemsCount = itemsList.count { it.gstRatePct == pctVal }
                val cgsgstSplit = pctVal / 2.0
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCardAdaptive(shape = RoundedCornerShape(16.dp), isDarkMode = state.isDarkMode)
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${pctVal.toInt()}% GST",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = titleLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$mappedItemsCount Items Mapped",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = descriptionText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                lineHeight = 15.sp
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Show tax split details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("CGST Split", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text("${cgsgstSplit}%", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("SGST Split", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text("${cgsgstSplit}%", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("IGST Split", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text("${pctVal.toInt()}%", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // If there are specific items mapped, list their names beautifully as a bullet point row
                            val mappedSKUs = itemsList.filter { it.gstRatePct == pctVal }
                            if (mappedSKUs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Mapped SKUs: " + mappedSKUs.joinToString(", ") { it.name },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}


    // Modern Modal Dialog Form for adding and editing predefined SKU Catalog Preset
    if (showAddItemDialog || editingItem != null) {
        val isEditMode = editingItem != null
        Dialog(onDismissRequest = { 
            showAddItemDialog = false 
            editingItem = null
        }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (isEditMode) "Edit Predefined SKU Item" else "Add New Predefined SKU Item",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isEditMode) "Modify the parameters below to update this product or service preset item." else "Fill in the parameters below to register a product or service preset item.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    var itemName by remember(editingItem) { mutableStateOf(editingItem?.name ?: "") }
                    var itemPrice by remember(editingItem) { mutableStateOf(editingItem?.rate?.toString() ?: "") }
                    var itemCategory by remember(editingItem) { mutableStateOf(editingItem?.category ?: "Product") }
                    var itemGstSelected by remember(editingItem) { mutableStateOf(editingItem?.gstRatePct ?: 18.0) }
                    var itemDesc by remember(editingItem) { mutableStateOf(editingItem?.description ?: "") }
                    var itemVendorName by remember(editingItem) { mutableStateOf(editingItem?.vendorName ?: "") }
                    var showVendorDropdown by remember { mutableStateOf(false) }
                    val partnersList by viewModel.partners.collectAsState()
                    val vendorSuggestions = remember(itemVendorName, partnersList) {
                        partnersList.filter {
                            it.type == "VENDOR" && (itemVendorName.isBlank() || it.name.contains(itemVendorName, ignoreCase = true))
                        }
                    }
                    
                    // Option 1 vs Option 2 selection (with stock management or as non-stock service)
                    var enableStockManagement by remember(editingItem) { mutableStateOf(editingItem?.trackStock ?: true) }
                    var initialStockInput by remember(editingItem) { mutableStateOf(editingItem?.stockQuantity?.toString() ?: "50") }
                    var lowStockInput by remember(editingItem) { mutableStateOf(editingItem?.lowStockThreshold?.toString() ?: "10") }
 
                    val inputColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
 
                    // Dual stock-tracking options selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (enableStockManagement) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .clickable { 
                                    enableStockManagement = true 
                                    itemCategory = "Product"
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle, 
                                    tint = if (enableStockManagement) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp),
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Stock Tracked", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (enableStockManagement) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (!enableStockManagement) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .clickable { 
                                    enableStockManagement = false 
                                    itemCategory = "Service"
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(
                                    imageVector = Icons.Filled.RemoveCircleOutline, 
                                    tint = if (!enableStockManagement) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp),
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Non-Stock / Service", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (!enableStockManagement) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item Name") },
                        placeholder = { Text("e.g. Dell XPS Monitor") },
                        singleLine = true,
                        colors = inputColors,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = itemPrice,
                            onValueChange = { itemPrice = it },
                            label = { Text("Base Price (₹)") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            colors = inputColors,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = itemCategory,
                            onValueChange = { itemCategory = it },
                            label = { Text("Item Class / Category") },
                            placeholder = { Text("e.g. Goods, Licensing") },
                            singleLine = true,
                            colors = inputColors,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = itemDesc,
                        onValueChange = { itemDesc = it },
                        label = { Text("Description") },
                        placeholder = { Text("Physical and billing specifications") },
                        singleLine = true,
                        colors = inputColors,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    )

                    // Associated Vendor Dropdown/Input
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                        Column {
                            OutlinedTextField(
                                value = itemVendorName,
                                onValueChange = { 
                                    itemVendorName = it
                                    showVendorDropdown = true
                                },
                                label = { Text("Associated Vendor Name") },
                                placeholder = { Text("e.g. Dell Distributor Co.") },
                                singleLine = true,
                                colors = inputColors,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { showVendorDropdown = !showVendorDropdown }) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown, 
                                            contentDescription = "Toggle Vendors",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                            if (showVendorDropdown && vendorSuggestions.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(4.dp)) {
                                        vendorSuggestions.take(4).forEach { vendor ->
                                            Text(
                                                text = vendor.name,
                                                fontSize = 12.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        itemVendorName = vendor.name
                                                        showVendorDropdown = false
                                                    }
                                                    .padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (enableStockManagement) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = initialStockInput,
                                onValueChange = { initialStockInput = it },
                                label = { Text("Initial Stock") },
                                placeholder = { Text("50") },
                                singleLine = true,
                                colors = inputColors,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = lowStockInput,
                                onValueChange = { lowStockInput = it },
                                label = { Text("Alert Threshold") },
                                placeholder = { Text("10") },
                                singleLine = true,
                                colors = inputColors,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = "Default GST Rate Mapping Option", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rateVal ->
                            val isSelected = itemGstSelected == rateVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { itemGstSelected = rateVal }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rateVal.toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { 
                            showAddItemDialog = false 
                            editingItem = null
                        }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val parsedPrice = itemPrice.toDoubleOrNull() ?: 0.0
                                val parsedStock = initialStockInput.toIntOrNull() ?: 0
                                val parsedThreshold = lowStockInput.toIntOrNull() ?: 5
                                
                                if (itemName.isNotBlank() && parsedPrice > 0.0) {
                                    if (isEditMode && editingItem != null) {
                                        viewModel.updateItem(
                                            editingItem!!.copy(
                                                name = itemName,
                                                rate = parsedPrice,
                                                gstRatePct = itemGstSelected,
                                                category = itemCategory,
                                                description = itemDesc,
                                                trackStock = enableStockManagement,
                                                stockQuantity = if (enableStockManagement) parsedStock else 0,
                                                lowStockThreshold = if (enableStockManagement) parsedThreshold else 5,
                                                vendorName = itemVendorName
                                            )
                                        )
                                    } else {
                                        viewModel.createNewItem(
                                            name = itemName,
                                            rate = parsedPrice,
                                            gstRatePct = itemGstSelected,
                                            category = itemCategory,
                                            description = itemDesc,
                                            trackStock = enableStockManagement,
                                            stockQuantity = if (enableStockManagement) parsedStock else 0,
                                            lowStockThreshold = if (enableStockManagement) parsedThreshold else 5,
                                            vendorName = itemVendorName
                                        )
                                    }
                                    showAddItemDialog = false
                                    editingItem = null
                                }
                            }
                        ) {
                            Text(if (isEditMode) "Save Changes" else "Create Predefined SKU")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryTableHeaderRow(isDark: Boolean) {
    val headerBg = if (isDark) {
        Color(0xFF1E2638)
    } else {
        Color(0xFFF3F4F6)
    }
    val textColor = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f)
    
    Row(
        modifier = Modifier
            .width(1010.dp)
            .background(headerBg, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("NAME & CLASS", modifier = Modifier.width(220.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
        Text("DESCRIPTION", modifier = Modifier.width(250.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
        Text("BASE RATE", modifier = Modifier.width(150.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
        Text("TAX SLAB", modifier = Modifier.width(110.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
        Text("STOCK LEVEL", modifier = Modifier.width(180.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor, textAlign = TextAlign.Center)
        Text("ACTIONS", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor, textAlign = TextAlign.Center)
    }
}

@Composable
fun InventoryTableRowItem(
    pItem: ErpItem,
    isDark: Boolean,
    viewModel: ErpViewModel,
    onEditClick: () -> Unit
) {
    val rowBg = if (isDark) {
        Color(0xFF131926).copy(alpha = 0.4f)
    } else {
        Color.White.copy(alpha = 0.6f)
    }
    val isLowStock = pItem.trackStock && pItem.stockQuantity <= pItem.lowStockThreshold

    Row(
        modifier = Modifier
            .width(1010.dp)
            .background(rowBg)
            .border(
                width = if (isLowStock) 1.dp else 0.dp,
                color = if (isLowStock) Color(0xFFEF4444).copy(alpha = 0.25f) else Color.Transparent
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. NAME & CLASS
        Column(modifier = Modifier.width(220.dp)) {
            Text(
                text = pItem.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = pItem.category,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (pItem.vendorName.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vendor: ",
                        fontSize = 10.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                    )
                    Text(
                        text = pItem.vendorName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            viewModel.selectAndNavigateToPartnerProfile(pItem.vendorName, "VENDOR")
                        }
                    )
                }
            }
        }

        // 2. DESCRIPTION
        Text(
            text = if (pItem.description.isNotBlank()) pItem.description else "No description provided.",
            modifier = Modifier.width(250.dp),
            fontSize = 11.sp,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
        )

        // 3. BASE RATE
        Text(
            text = "₹${String.format(java.util.Locale.US, "%,.2f", pItem.rate)}",
            modifier = Modifier.width(150.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )

        // 4. TAX SLAB
        Box(
            modifier = Modifier.width(110.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${pItem.gstRatePct.toInt()}% GST",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 5. STOCK LEVEL & CONTROLS
        Row(
            modifier = Modifier.width(180.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (pItem.trackStock) {
                // Minus Button
                IconButton(
                    onClick = { viewModel.adjustItemStock(pItem.id, pItem.stockQuantity - 1) },
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Decrease Stock Level",
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(50.dp)
                ) {
                    Text(
                        text = pItem.stockQuantity.toString(),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = if (isLowStock) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                    )
                    if (isLowStock) {
                        Text("Low Alert", fontSize = 8.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
                
                // Plus Button
                IconButton(
                    onClick = { viewModel.adjustItemStock(pItem.id, pItem.stockQuantity + 1) },
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Increase Stock Level",
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚙ Unlimited Service",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 6. ACTIONS
        Row(
            modifier = Modifier.width(100.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit Preset SKU",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { viewModel.removeItem(pItem) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove Preset SKU",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun InventoryPaginationControl(
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
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: space is now used to show the count beautifully
        Text(
            text = "Showing ${if (totalItems == 0) 0 else startIndex + 1}–${endIndex} of ${totalItems} entries",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )

        // Right side: dropdown for data limit and navigation arrows moved here
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var dropdownExpanded by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .background(
                            if (isDark) Color(0xFF131926).copy(alpha = 0.4f) else Color.White,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Limit: $itemsPerPage",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f)
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select Limit",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(if (isDark) Color(0xFF1F2937) else Color.White)
                ) {
                    listOf(5, 10, 20, 50).forEach { size ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$size per page",
                                    fontSize = 11.sp,
                                    fontWeight = if (itemsPerPage == size) FontWeight.Bold else FontWeight.Normal,
                                    color = if (itemsPerPage == size) MaterialTheme.colorScheme.primary else (if (isDark) Color.White else Color.Black)
                                )
                            },
                            onClick = {
                                onItemsPerPageChange(size)
                                dropdownExpanded = false
                            },
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Previous Icon Button
                IconButton(
                    onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                    enabled = currentPage > 1,
                    modifier = Modifier
                        .size(24.dp)
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
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Next Icon Button
                IconButton(
                    onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                    enabled = currentPage < totalPages,
                    modifier = Modifier
                        .size(24.dp)
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
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}


