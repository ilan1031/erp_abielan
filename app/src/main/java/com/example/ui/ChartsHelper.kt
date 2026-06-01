package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.AccentGold
import com.example.ui.theme.glassCardAdaptive
import kotlin.math.atan2
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Calendar
import java.util.Random
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.automirrored.filled.TrendingUp

data class ChartDataItem(val label: String, val value: Float, val color: Color)

@Composable
fun RenderDashboardChart(
    chartType: String,
    dataItems: List<ChartDataItem>,
    modifier: Modifier = Modifier
) {
    if (dataItems.isEmpty()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No transaction records found for selected filters.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (chartType.uppercase()) {
            "BAR" -> BarChartCompose(dataItems = dataItems)
            "PIE" -> PieChartCompose(dataItems = dataItems, isDonut = false)
            "DONUT" -> PieChartCompose(dataItems = dataItems, isDonut = true)
            "LINE" -> LineAreaChartCompose(dataItems = dataItems, isArea = false)
            "AREA" -> LineAreaChartCompose(dataItems = dataItems, isArea = true)
            else -> BarChartCompose(dataItems = dataItems)
        }
    }
}

@Composable
fun BarChartCompose(dataItems: List<ChartDataItem>) {
    val maxValue = dataItems.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1f
    var triggerAnimation by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(dataItems) {
        triggerAnimation = true
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (triggerAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "BarChartGlobalAnimation"
    )
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Interactive tooltip area on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hoveredIndex != null && hoveredIndex!! in dataItems.indices) {
                val item = dataItems[hoveredIndex!!]
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(item.color, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "${item.label}: ₹${String.format(java.util.Locale.US, "%,.2f", item.value)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Hover / Tap on any bar to see precise values",
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            dataItems.forEachIndexed { index, item ->
                val totalHeightRatio = (item.value / maxValue) * animatedProgress
                val isHovered = hoveredIndex == index
                val barAlpha = if (hoveredIndex == null) 1f else if (isHovered) 1f else 0.4f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp)
                        .pointerInput(item) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val type = event.type
                                    val isPressed = event.changes.any { it.pressed }
                                    
                                    if (type == PointerEventType.Enter || type == PointerEventType.Move || isPressed) {
                                        hoveredIndex = index
                                    } else if (type == PointerEventType.Exit || !isPressed) {
                                        if (hoveredIndex == index) {
                                            hoveredIndex = null
                                        }
                                    }
                                }
                            }
                        }
                        .clickable {
                            hoveredIndex = index
                        }
                ) {
                    // Bar Value
                    Text(
                        text = "₹${formatCurrencyShort(item.value)}",
                        fontSize = 10.sp,
                        fontWeight = if (isHovered) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    // The bar drawing
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        val barWidth = size.width
                        val totalBarHeight = size.height
                        val currentBarHeight = totalBarHeight * totalHeightRatio

                        drawRoundRect(
                            color = item.color.copy(alpha = barAlpha),
                            topLeft = Offset(0f, totalBarHeight - currentBarHeight),
                            size = Size(barWidth, currentBarHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bar label
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isHovered) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PieChartCompose(dataItems: List<ChartDataItem>, isDonut: Boolean = false) {
    var triggerAnimation by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(dataItems) {
        triggerAnimation = true
    }

    val totalSum = dataItems.sumOf { it.value.toDouble() }.toFloat().takeIf { it > 0 } ?: 1f
    val animatedProgress by animateFloatAsState(
        targetValue = if (triggerAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Interactive tooltip area on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hoveredIndex != null && hoveredIndex!! in dataItems.indices) {
                val item = dataItems[hoveredIndex!!]
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(item.color, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "${item.label}: ₹${String.format(java.util.Locale.US, "%,.2f", item.value)} (${String.format(java.util.Locale.US, "%.1f", (item.value / totalSum) * 100)}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Hover / Drag over slices or legend to see precise values",
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Pie/Donut canvas
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dataItems) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val position = event.changes.firstOrNull()?.position
                                    val isPressed = event.changes.any { it.pressed }
                                    val type = event.type
                                    
                                    if (position != null && (type == PointerEventType.Enter || type == PointerEventType.Move || isPressed)) {
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        val dx = position.x - cx
                                        val dy = position.y - cy
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0) {
                                            angle += 360f
                                        }
                                        
                                        var currentAngle = 0f
                                        var found: Int? = null
                                        dataItems.forEachIndexed { idx, item ->
                                            val sweep = (item.value / totalSum) * 360f
                                            if (angle >= currentAngle && angle <= (currentAngle + sweep)) {
                                                found = idx
                                            }
                                            currentAngle += sweep
                                        }
                                        hoveredIndex = found
                                    } else if (type == PointerEventType.Exit || !isPressed) {
                                        hoveredIndex = null
                                    }
                                }
                            }
                        }
                ) {
                    var currentStartAngle = 0f
                    dataItems.forEachIndexed { idx, item ->
                        val sweepAngle = (item.value / totalSum) * 360f * animatedProgress
                        val isHovered = hoveredIndex == idx
                        val alpha = if (hoveredIndex == null) 1f else if (isHovered) 1f else 0.4f
                        
                        if (isDonut) {
                            drawArc(
                                color = item.color.copy(alpha = alpha),
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                size = size,
                                style = Stroke(width = if (isHovered) 28.dp.toPx() else 22.dp.toPx())
                            )
                        } else {
                            drawArc(
                                color = item.color.copy(alpha = alpha),
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                size = if (isHovered) size.times(1.05f) else size
                            )
                        }
                        currentStartAngle += sweepAngle
                    }
                }
                if (isDonut) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (hoveredIndex != null) dataItems[hoveredIndex!!].label else "Total",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(70.dp)
                        )
                        Text(
                            text = "₹${formatCurrencyShort(if (hoveredIndex != null) dataItems[hoveredIndex!!].value else totalSum)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Legend
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                dataItems.forEachIndexed { index, item ->
                    val isHovered = hoveredIndex == index
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isHovered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { hoveredIndex = if (isHovered) null else index }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(item.color.copy(alpha = if (hoveredIndex == null || isHovered) 1f else 0.4f), RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.label}: ${String.format(java.util.Locale.US, "%.1f", (item.value / totalSum) * 100)}%",
                            fontSize = 11.sp,
                            fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Normal,
                            color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LineAreaChartCompose(dataItems: List<ChartDataItem>, isArea: Boolean = false) {
    val maxValue = dataItems.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1f
    var triggerAnimation by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var canvasWidth by remember { mutableStateOf(1f) }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (triggerAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(dataItems) {
        triggerAnimation = true
    }

    val chartLineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)

    Column(modifier = Modifier.fillMaxSize()) {
        // Precise Hover Tooltip Card at the top center of chart area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hoveredIndex != null && hoveredIndex!! in dataItems.indices) {
                val item = dataItems[hoveredIndex!!]
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(item.color, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "${item.label}: ₹${String.format(java.util.Locale.US, "%,.2f", item.value)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Hover / Drag over graph to see precise values",
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .onSizeChanged { size -> canvasWidth = size.width.toFloat() }
                .pointerInput(dataItems) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.firstOrNull()?.position
                            val isPressed = event.changes.any { it.pressed }
                            val type = event.type
                            
                            if (position != null && (type == PointerEventType.Enter || type == PointerEventType.Move || isPressed)) {
                                val spacing = canvasWidth / (dataItems.size - 1).coerceAtLeast(1)
                                val index = (position.x / spacing).roundToInt().coerceIn(0, dataItems.lastIndex)
                                hoveredIndex = index
                            } else if (type == PointerEventType.Exit || !isPressed) {
                                hoveredIndex = null
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val spacing = width / (dataItems.size - 1).coerceAtLeast(1)

            val points = dataItems.mapIndexed { index, item ->
                val x = index * spacing
                val y = height - (height * (item.value / maxValue) * animatedProgress)
                Offset(x, y)
            }

            // 1. Draw horizontal background gridlines (realistic)
            val gridLevels = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
            gridLevels.forEach { ratio ->
                val gy = height * (1f - ratio)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, gy),
                    end = Offset(width, gy),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 2. Draw vertical background gridlines
            points.forEach { p ->
                drawLine(
                    color = gridColor,
                    start = Offset(p.x, 0f),
                    end = Offset(p.x, height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 3. Draw vertical hover indicator line
            if (hoveredIndex != null && hoveredIndex!! in points.indices) {
                val hoveredPoint = points[hoveredIndex!!]
                drawLine(
                    color = chartLineColor.copy(alpha = 0.4f),
                    start = Offset(hoveredPoint.x, 0f),
                    end = Offset(hoveredPoint.x, height),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Path for Line
            val linePath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
            }

            // Path for Area Shading
            if (isArea && points.isNotEmpty()) {
                val areaPath = Path().apply {
                    moveTo(points.first().x, height)
                    lineTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                    lineTo(points.last().x, height)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            chartLineColor.copy(alpha = 0.4f),
                            chartLineColor.copy(alpha = 0.0f)
                        )
                    )
                )
            }

            // Draw line
            drawPath(
                path = linePath,
                color = chartLineColor,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw joint highlights
            points.forEachIndexed { index, p ->
                val isHovered = hoveredIndex == index
                if (isHovered) {
                    drawCircle(
                        color = dataItems[index].color.copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = p
                    )
                }
                drawCircle(
                    color = dataItems[index].color,
                    radius = if (isHovered) 7.dp.toPx() else 5.dp.toPx(),
                    center = p
                )
                drawCircle(
                    color = Color.White,
                    radius = if (isHovered) 3.dp.toPx() else 2.dp.toPx(),
                    center = p
                )
            }
        }

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dataItems.forEachIndexed { index, item ->
                val isHovered = hoveredIndex == index
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = if (isHovered) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun formatCurrencyShort(value: Float): String {
    return when {
        value >= 100000 -> "${String.format(java.util.Locale.US, "%.1f", value / 100000f)}L"
        value >= 1000 -> "${String.format(java.util.Locale.US, "%.1f", value / 1000f)}K"
        else -> value.toInt().toString()
    }
}

// Interactive Financial Candlestick Data Models & Visualizer Components
data class CandleModel(
    val dateTimeStamp: String,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Double
)

fun generateCandleData(interval: String): List<CandleModel> {
    val items = mutableListOf<CandleModel>()
    val count = 45
    // Base price & volatility depending on interval selected
    val basePrice = when (interval) {
        "1D" -> 45200.0
        "1W" -> 44800.0
        "1M" -> 43000.0
        "3M" -> 42100.0
        "6M" -> 40500.0
        "1Y" -> 38200.0
        "5Y" -> 22000.0
        "All" -> 15000.0
        else -> 45000.0
    }
    val volatility = when (interval) {
        "1D" -> 45.0
        "1W" -> 180.0
        "1M" -> 450.0
        "3M" -> 850.0
        "6M" -> 1400.0
        "1Y" -> 2800.0
        "5Y" -> 5500.0
        "All" -> 11000.0
        else -> 200.0
    }
    val seed = when (interval) {
        "1D" -> 123
        "1W" -> 456
        "1M" -> 789
        "3M" -> 111
        "6M" -> 222
        "1Y" -> 333
        "5Y" -> 444
        "All" -> 555
        else -> 999
    }
    
    val random = java.util.Random(seed.toLong())
    var currentPrice = basePrice
    val now = Date()
    
    val sdfTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
    val sdfDateShort = java.text.SimpleDateFormat("dd MMM", java.util.Locale.US)
    val sdfYearOnly = java.text.SimpleDateFormat("yyyy", java.util.Locale.US)
    val sdfMonthYear = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.US)
    val sdfDayDate = java.text.SimpleDateFormat("EEE, dd MMM yyyy", java.util.Locale.US)

    for (i in 0 until count) {
        // Build realistic trading price actions
        val change = (random.nextDouble() - 0.46) * volatility
        val open = currentPrice
        val close = currentPrice + change
        val deltaHigh = random.nextDouble() * (volatility * 0.42)
        val deltaLow = random.nextDouble() * (volatility * 0.42)
        val high = Math.max(open, close) + deltaHigh
        val low = Math.min(open, close) - deltaLow
        val volume = 1500.0 + random.nextDouble() * 9500.0
        
        val calStart = java.util.Calendar.getInstance()
        calStart.time = now
        val calEnd = java.util.Calendar.getInstance()
        calEnd.time = now

        var displayStamp = ""

        when (interval) {
            "1D" -> { // Minute-by-Minute
                calStart.add(java.util.Calendar.MINUTE, - (count - 1 - i))
                calEnd.time = calStart.time
                calEnd.add(java.util.Calendar.MINUTE, 1)
                displayStamp = "${sdfTime.format(calStart.time)} - ${sdfTime.format(calEnd.time)} ${sdfDateShort.format(calStart.time).uppercase()} ${sdfYearOnly.format(calStart.time)}"
            }
            "1W" -> { // Hour-by-Hour
                calStart.add(java.util.Calendar.HOUR_OF_DAY, - (count - 1 - i))
                calEnd.time = calStart.time
                calEnd.add(java.util.Calendar.HOUR_OF_DAY, 1)
                displayStamp = "${sdfTime.format(calStart.time)} - ${sdfTime.format(calEnd.time)} ${sdfDateShort.format(calStart.time).uppercase()} ${sdfYearOnly.format(calStart.time)}"
            }
            "1M" -> { // Day-by-Day (1 Month)
                calStart.add(java.util.Calendar.DAY_OF_YEAR, - (count - 1 - i))
                displayStamp = "${sdfDayDate.format(calStart.time).uppercase()}"
            }
            "3M", "6M" -> { // Day-by-Day (Several Months)
                calStart.add(java.util.Calendar.DAY_OF_YEAR, - (count - 1 - i) * (if (interval == "3M") 2 else 4))
                displayStamp = "${sdfDayDate.format(calStart.time).uppercase()}"
            }
            "1Y" -> { // Week-by-Week (1 Year)
                calStart.add(java.util.Calendar.WEEK_OF_YEAR, - (count - 1 - i))
                calEnd.time = calStart.time
                calEnd.add(java.util.Calendar.DAY_OF_YEAR, 7)
                displayStamp = "${sdfDateShort.format(calStart.time).uppercase()} - ${sdfDateShort.format(calEnd.time).uppercase()} ${sdfYearOnly.format(calStart.time)}"
            }
            "5Y" -> { // Month-by-Month (5 Years)
                calStart.add(java.util.Calendar.MONTH, - (count - 1 - i))
                displayStamp = "${sdfMonthYear.format(calStart.time).uppercase()}"
            }
            "All" -> { // Year-by-Year (All Time)
                calStart.add(java.util.Calendar.YEAR, - (count - 1 - i))
                displayStamp = "${sdfYearOnly.format(calStart.time)}"
            }
        }
        
        items.add(
            CandleModel(
                dateTimeStamp = displayStamp,
                open = open,
                close = close,
                high = high,
                low = low,
                volume = volume
            )
        )
        currentPrice = close
    }
    return items
}

@Composable
fun CandlestickStockChart(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedInterval by remember { mutableStateOf("1W") } // Default timeframe matches screenshot
    val candles = remember(selectedInterval) { generateCandleData(selectedInterval) }
    
    // Split hover indices for compact and dialog views
    var compactHoveredIndex by remember { mutableStateOf<Int?>(null) }
    var dialogHoveredIndex by remember { mutableStateOf<Int?>(null) }
    var showFullViewDialog by remember { mutableStateOf(false) }

    // Compact view exhibits a limited/clean data scope initially so it is highly legible
    val compactCandles = remember(candles) { candles.takeLast(15) }
    val compactActiveCandle = if (compactHoveredIndex != null && compactHoveredIndex in compactCandles.indices) {
        compactCandles[compactHoveredIndex!!]
    } else {
        compactCandles.lastOrNull()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .glassCardAdaptive(shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Info Widget (Large Stats & Details)
            CandlestickHeaderWidget(
                candles = compactCandles,
                hoveredIndex = compactHoveredIndex,
                selectedInterval = selectedInterval
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Drawing Canvas Box (Increased height for higher clarity and professional layout ratio!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .background(
                        if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                // Main Interactive Canvas
                CandlestickCanvasWidget(
                    candles = compactCandles,
                    hoveredIndex = compactHoveredIndex,
                    isDarkMode = isDarkMode,
                    onHoverIndexChanged = { compactHoveredIndex = it }
                )

                // Fullscreen expansion overlay button inside the chart
                IconButton(
                    onClick = { showFullViewDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Full View Chart Analysis",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Day, Week, Month, Year filter buttons row with round borders precisely at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val intervals = listOf(
                    "1D" to "1D",
                    "1W" to "1W",
                    "1M" to "1M",
                    "3M" to "3M",
                    "6M" to "6M",
                    "1Y" to "1Y",
                    "5Y" to "5Y",
                    "All" to "All"
                )

                intervals.forEach { (key, label) ->
                    val isSel = selectedInterval == key
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSel) 1.dp else 0.dp,
                                color = if (isSel) {
                                    if (isDarkMode) Color.White else Color.Black
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedInterval = key
                                compactHoveredIndex = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) {
                                if (isDarkMode) Color.White else Color.Black
                            } else {
                                if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                            }
                        )
                    }
                }

                // Trending trend-line symbol on the far right matching the green visual feedback in screenshot
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Trend Performance",
                    tint = SuccessGreen.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // --- FULL SCREEN POP OUT DIALOG ---
    if (showFullViewDialog) {
        Dialog(
            onDismissRequest = { showFullViewDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "ABIELAN ERP • PERFORMANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                "Ledger Analytics Index",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        val context = LocalContext.current
                        Button(
                            onClick = {
                                exportGraphValues(context, selectedInterval, candles, isDarkMode)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Export Data",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Graph", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = { showFullViewDialog = false },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close View",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Deep Stats Panel
                    CandlestickHeaderWidget(
                        candles = candles,
                        hoveredIndex = dialogHoveredIndex,
                        selectedInterval = selectedInterval
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic canvas inside Dialog (expanded for high-precision tracing!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(
                                if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        CandlestickCanvasWidget(
                            candles = candles,
                            hoveredIndex = dialogHoveredIndex,
                            isDarkMode = isDarkMode,
                            onHoverIndexChanged = { dialogHoveredIndex = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Interval selector in dialog with custom granular intervals
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val dialogIntervals = listOf(
                            "1D" to "1D",
                            "1W" to "1W",
                            "1M" to "1M",
                            "1Y" to "1Y",
                            "5Y" to "5Y",
                            "All" to "All"
                        )

                        dialogIntervals.forEach { (key, label) ->
                            val isSel = selectedInterval == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .clickable {
                                        selectedInterval = key
                                        dialogHoveredIndex = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Slide finger across the graph to inspect ledger transactions & balances.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun CandlestickHeaderWidget(
    candles: List<CandleModel>,
    hoveredIndex: Int?,
    selectedInterval: String
) {
    val activeCandle = if (hoveredIndex != null && hoveredIndex in candles.indices) {
        candles[hoveredIndex]
    } else {
        candles.lastOrNull()
    }

    if (activeCandle == null) return

    val labelText = when (selectedInterval) {
        "1D" -> "Hourly"
        "1W" -> "Daily Summary"
        "1M", "3M", "6M" -> "Monthly Summary"
        "1Y" -> "Quarterly Summary"
        "5Y" -> "5-Year Period"
        "All" -> "Annual Summary"
        else -> "Overview"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${labelText.uppercase()} • ₹ LEDGER SUMMARY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            
            val priceChange = activeCandle.close - activeCandle.open
            val percentChange = (priceChange / activeCandle.open) * 100.0
            val isBully = priceChange >= 0
            
            Text(
                text = if (isBully) {
                    "+₹${String.format(java.util.Locale.US, "%,.2f", priceChange)} (+${String.format(java.util.Locale.US, "%.2f", percentChange)}%)"
                } else {
                    "-₹${String.format(java.util.Locale.US, "%,.2f", Math.abs(priceChange))} (${String.format(java.util.Locale.US, "%.2f", percentChange)}%)"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isBully) SuccessGreen else OverdueRed
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Large Close Price and Active Timestamp Row (out of range of the graph canvas!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "₹${String.format(java.util.Locale.US, "%,.2f", activeCandle.close)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = activeCandle.dateTimeStamp,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Metrics breakdown row (O, H, L, V mapped to Start, Peak, Floor, Tx Vol) to start and end edges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val metrics = listOf(
                "Start: ₹" to activeCandle.open,
                "Peak: ₹" to activeCandle.high,
                "Floor: ₹" to activeCandle.low,
                "Tx Vol: " to activeCandle.volume
            )

            metrics.forEachIndexed { i, (labelPrefix, valDouble) ->
                val metricText = if (i == 3) {
                    "$labelPrefix${String.format(java.util.Locale.US, "%,.0f", valDouble)}"
                } else {
                    "$labelPrefix${String.format(java.util.Locale.US, "%,.0f", valDouble)}"
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = metricText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun CandlestickCanvasWidget(
    candles: List<CandleModel>,
    hoveredIndex: Int?,
    isDarkMode: Boolean,
    onHoverIndexChanged: (Int?) -> Unit
) {
    var canvasWidth by remember { mutableStateOf(1f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val labelWidthPx = remember(density) { with(density) { 62.dp.toPx() } }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .onSizeChanged { size -> canvasWidth = size.width.toFloat() }
            .pointerInput(candles) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.firstOrNull()?.position
                        val isPressed = event.changes.any { it.pressed }
                        val type = event.type
                        
                        if (position != null && (type == PointerEventType.Enter || type == PointerEventType.Move || isPressed)) {
                            val chartW = (canvasWidth - labelWidthPx).coerceAtLeast(1f)
                            val spacing = chartW / candles.size.coerceAtLeast(1)
                            val index = (position.x / spacing).toInt().coerceIn(0, candles.lastIndex)
                            onHoverIndexChanged(index)
                        } else if (type == PointerEventType.Exit || !isPressed) {
                            onHoverIndexChanged(null)
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        if (candles.isEmpty()) return@Canvas

        val chartWidth = (width - labelWidthPx).coerceAtLeast(1f)

        val maxHigh = candles.maxOf { it.high }
        val minLow = candles.minOf { it.low }
        val priceRange = (maxHigh - minLow).coerceAtLeast(1.0)

        // Utility mapping pricing value to local canvas pixel Y
        fun priceToY(price: Double): Float {
            val ratio = (price - minLow) / priceRange
            return (height * (1f - ratio)).toFloat()
        }

        // Draw horizontal price guide line grid
        val levels = listOf(0.15, 0.4, 0.65, 0.9)
        levels.forEach { levelRatio ->
            val py = (height * levelRatio).toFloat()
            drawLine(
                color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                start = Offset(0f, py),
                end = Offset(chartWidth, py),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )

            // Draw tiny price labels right at the start/end edges (End Edge Right Aligned)
            val priceVal = minLow + (1.0 - levelRatio) * priceRange
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = if (isDarkMode) android.graphics.Color.GRAY else android.graphics.Color.DKGRAY
                    textSize = 6.8f.dp.toPx() // reduced text size
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    "₹${String.format(java.util.Locale.US, "%,.0f", priceVal)}",
                    width - 2.dp.toPx(),
                    py - 4f,
                    paint
                )
            }
        }

        // Draw volume and candlestick bars
        val sizeCount = candles.size
        val step = chartWidth / sizeCount
        val candleSpacing = step * 0.28f
        val candleWidth = step - candleSpacing

        candles.forEachIndexed { idx, item ->
            val isBully = item.close >= item.open
            val themeColor = if (isBully) SuccessGreen else OverdueRed
            
            val candleX = idx * step + (candleSpacing / 2f)
            val centerX = candleX + (candleWidth / 2f)

            // 1. Draw Volume Bars at the very bottom (15% height)
            val maxVolume = candles.maxOf { it.volume }
            val volumeHeightPercent = (item.volume / maxVolume) * 0.15
            val volHeight = (height * volumeHeightPercent).toFloat()
            val volTop = height - volHeight
            drawRect(
                color = themeColor.copy(alpha = 0.22f),
                topLeft = Offset(candleX, volTop),
                size = Size(candleWidth, volHeight)
            )

            // 2. Draw Candlestick Wick (High -> Low line)
            val yHigh = priceToY(item.high)
            val yLow = priceToY(item.low)
            drawLine(
                color = themeColor,
                start = Offset(centerX, yHigh),
                end = Offset(centerX, yLow),
                strokeWidth = 1.6f.dp.toPx()
            )

            // 3. Draw Candlestick Solid Body
            val yOpen = priceToY(item.open)
            val yClose = priceToY(item.close)
            val topBody = Math.min(yOpen, yClose)
            val bottomBody = Math.max(yOpen, yClose)
            val bodyHeight = (bottomBody - topBody).coerceAtLeast(2f)

            drawRect(
                color = themeColor,
                topLeft = Offset(candleX, topBody),
                size = Size(candleWidth, bodyHeight)
            )
        }

        // 4. Draw Single Thin Vertical Scrubber Line on Hover / Drag Scrum (Exactly matching uploaded screenshot!)
        if (hoveredIndex != null && hoveredIndex in candles.indices) {
            val candleX = hoveredIndex * step + (candleSpacing / 2f)
            val centerX = candleX + (candleWidth / 2f)

            drawLine(
                color = if (isDarkMode) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.35f),
                start = Offset(centerX, 0f),
                end = Offset(centerX, height),
                strokeWidth = 1.2f.dp.toPx()
            )
        }
    }
}

fun exportGraphValues(context: Context, interval: String, candles: List<CandleModel>, isDarkMode: Boolean) {
    val width = 1000
    val height = 600
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Paint backgrounds
    val bgPaint = android.graphics.Paint().apply {
        color = if (isDarkMode) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Draw the Logo at top left
    val logoSize = 45
    val logoLeft = 40
    val logoTop = 15
    val logoDrawable = androidx.core.content.ContextCompat.getDrawable(context, com.example.R.drawable.logo)
    if (logoDrawable != null) {
        logoDrawable.setBounds(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
        logoDrawable.draw(canvas)
    } else {
        // Fallback: A green & gold rounded rect emblem
        val emblemPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#15803D")
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(logoLeft.toFloat(), logoTop.toFloat(), (logoLeft + logoSize).toFloat(), (logoTop + logoSize).toFloat(), 8f, 8f, emblemPaint)
        
        val emblemInnerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#B58E49")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect((logoLeft + 4).toFloat(), (logoTop + 4).toFloat(), (logoLeft + logoSize - 4).toFloat(), (logoTop + logoSize - 4).toFloat(), 6f, 6f, emblemInnerPaint)
    }

    // Draw a neat title shifted to the right of the logo
    val titlePaint = android.graphics.Paint().apply {
        color = if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        textSize = 24f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("Abielan ERP Ledger Performance ($interval)", (logoLeft + logoSize + 15).toFloat(), 46f, titlePaint)

    // Find min and max
    if (candles.isNotEmpty()) {
        val maxHigh = candles.maxOf { it.high }
        val minLow = candles.minOf { it.low }
        val priceRange = (maxHigh - minLow).coerceAtLeast(1.0)

        // Draw horizontal grid lines and prices
        val levels = listOf(0.2, 0.45, 0.7, 0.9)
        val gridPaint = android.graphics.Paint().apply {
            color = if (isDarkMode) android.graphics.Color.parseColor("#2D3748") else android.graphics.Color.parseColor("#E2E8F0")
            strokeWidth = 2f
            style = android.graphics.Paint.Style.STROKE
        }
        val labelPaint = android.graphics.Paint().apply {
            color = if (isDarkMode) android.graphics.Color.GRAY else android.graphics.Color.DKGRAY
            textSize = 18f
            isAntiAlias = true
        }

        val plotTop = 100f
        val plotBottom = 520f
        val plotHeight = plotBottom - plotTop

        levels.forEach { levelRatio ->
            val py = plotTop + (plotHeight * levelRatio).toFloat()
            canvas.drawLine(40f, py, width - 150f, py, gridPaint)
            // draw label
            val priceVal = maxHigh - (levelRatio * priceRange)
            canvas.drawText(
                "₹${String.format(java.util.Locale.US, "%,.0f", priceVal)}",
                width - 130f,
                py + 6f,
                labelPaint
            )
        }

        // Plot candles
        val plotWidth = width - 200f
        val sizeCount = candles.size
        val step = plotWidth / sizeCount
        val candleSpacing = step * 0.28f
        val candleWidth = step - candleSpacing

        candles.forEachIndexed { idx, item ->
            val isBully = item.close >= item.open
            val themeColor = if (isBully) {
                android.graphics.Color.parseColor("#15803D") // SuccessGreen
            } else {
                android.graphics.Color.parseColor("#EF4444") // OverdueRed
            }

            val x = 40f + idx * step + (candleSpacing / 2f)
            
            // Map prices to Y
            fun valToY(price: Double): Float {
                val ratio = (price - minLow) / priceRange
                return (plotBottom - (plotHeight * ratio)).toFloat()
            }

            val yOpen = valToY(item.open)
            val yClose = valToY(item.close)
            val yHigh = valToY(item.high)
            val yLow = valToY(item.low)

            // Draw wick
            val wickPaint = android.graphics.Paint().apply {
                color = themeColor
                strokeWidth = 3f
                style = android.graphics.Paint.Style.STROKE
            }
            canvas.drawLine(x + (candleWidth / 2f), yHigh, x + (candleWidth / 2f), yLow, wickPaint)

            // Draw body
            val bodyPaint = android.graphics.Paint().apply {
                color = themeColor
                style = android.graphics.Paint.Style.FILL
            }
            val topBody = Math.min(yOpen, yClose)
            val bottomBody = Math.max(yOpen, yClose)
            val bodyHeight = (bottomBody - topBody).coerceAtLeast(3f)
            canvas.drawRect(x, topBody, x + candleWidth, topBody + bodyHeight, bodyPaint)
        }
    }

    // Capture date / footer
    val footerPaint = android.graphics.Paint().apply {
        color = if (isDarkMode) android.graphics.Color.GRAY else android.graphics.Color.DKGRAY
        textSize = 16f
        isAntiAlias = true
    }
    canvas.drawText("Generated by Abielan ERP System Autonomous Ledger Tracker.", 40f, height - 30f, footerPaint)

    // Save bitmap to file in context cache directory
    val file = java.io.File(context.cacheDir, "Abielan_Ledger_Graph_${System.currentTimeMillis()}.png")
    try {
        val outStream = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.flush()
        outStream.close()

        // Share the PNG file
        val fileUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_SUBJECT, "Abielan ERP Graph Export: $interval Ledger")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Ledger Graph").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
        Toast.makeText(context, "Graph exported as PNG!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Export PNG error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

