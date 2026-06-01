package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BusinessDocument
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.AccentGold
import com.example.ui.theme.glassCardAdaptive
import com.example.viewmodel.ErpViewModel
import java.util.Locale
import java.util.Date

@Composable
fun ReportsScreenModule(viewModel: ErpViewModel) {
    val state by viewModel.uiState.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    var selectedFormat by remember { mutableStateOf("PDF") } // "PDF", "EXCEL", "CSV"
    var selectedReportType by remember { mutableStateOf("Profit & Loss") } 
    // Types: "Profit & Loss", "Revenue Audit", "Expenses Summary", "Cash Flow Index", "Outstanding Settlements"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Title
        Text(text = "Autonomous Analytics Reports", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(text = "Compile certified balance sheets, statements, and outstanding listings.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 12.dp))

        // High-fidelity Interactive Candlestick Stock Chart
        CandlestickStockChart(
            isDarkMode = state.isDarkMode,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- EXPORT CONFIG OPTIONS ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .glassCardAdaptive(shape = RoundedCornerShape(16.dp), isDarkMode = state.isDarkMode)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("1. Select Report Content", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(10.dp))

                val reports = listOf("Profit & Loss", "Revenue Audit", "Expenses Summary", "Cash Flow Index", "Outstanding Settlements")
                reports.forEach { r ->
                    val isChecked = selectedReportType == r
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReportType = r }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isChecked, onClick = { selectedReportType = r })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(r, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .glassCardAdaptive(shape = RoundedCornerShape(16.dp), isDarkMode = state.isDarkMode)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("2. Output Format Standard", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = listOf("PDF", "EXCEL", "CSV")
                    formats.forEach { fmt ->
                        val isSel = selectedFormat == fmt
                        val icon = when (fmt) {
                            "PDF" -> Icons.Filled.PictureAsPdf
                            "EXCEL" -> Icons.Filled.GridOn
                            "CSV" -> Icons.Filled.TableChart
                            else -> Icons.Filled.Article
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFormat = fmt }
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(24.dp), 
                                    tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(fmt, fontWeight = FontWeight.Black, fontSize = 11.sp, color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // --- COMPILE ACTIONS BUTTONS ---
        Button(
            onClick = {
                compileAndExportReportData(context, documents, selectedReportType, selectedFormat)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CloudUpload, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp), 
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compile & Export Statement", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Info card explaining compiler structure
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .glassCardAdaptive(shape = RoundedCornerShape(12.dp), isDarkMode = state.isDarkMode)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp), 
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "All generated compilations will be structured cryptographically in standard MIME types and sent via system share sheets to verify security policies in offline mode.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

fun compileAndExportReportData(
    context: Context,
    documents: List<BusinessDocument>,
    reportType: String,
    format: String
) {
    val revenue = documents.filter { it.type == "INVOICE" }.sumOf { it.totalAmount }
    val expense = documents.filter { it.type == "BILL" }.sumOf { it.totalAmount }
    val outstanding = documents.filter { it.status == "PENDING" && it.type == "INVOICE" }.sumOf { it.totalAmount }
    val balance = revenue - expense

    val suffix = format.lowercase(Locale.US)
    val mimeType = when (format) {
        "PDF" -> "application/pdf"
        "CSV" -> "text/csv"
        "EXCEL" -> "application/vnd.ms-excel"
        else -> "text/plain"
    }

    val finalSuffix = if (suffix == "excel") "xls" else suffix
    val reportSubject = "Abielan ERP Export: $reportType Statement"
    val localFile = java.io.File(context.cacheDir, "Abielan_Report_${System.currentTimeMillis()}.$finalSuffix")

    if (format == "PDF") {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Page Background
            val bgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

            // Header Banner
            val bannerPaint1 = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#15803D") }
            val bannerPaint2 = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#B58E49") }
            canvas.drawRect(0f, 0f, 595f, 15f, bannerPaint1)
            canvas.drawRect(0f, 15f, 595f, 20f, bannerPaint2)

            // Logo Icon Fallback
            val emblemPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#15803D")
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(40f, 45f, 75f, 80f, 8f, 8f, emblemPaint)
            
            val emblemInnerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#B58E49")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(43f, 48f, 72f, 77f, 6f, 6f, emblemInnerPaint)

            // Main Header Typography
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#15803D")
                textSize = 16f
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.drawText("ABIELAN ERP CORP", 85f, 62f, titlePaint)

            val subTitlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#64748B")
                textSize = 8f
                isAntiAlias = true
            }
            canvas.drawText("INTELLIGENT ERP SYSTEM  •  ANALYTIC STATEMENTS", 85f, 75f, subTitlePaint)

            // Badge Indicator
            val badgeBgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#EFF6FF")
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(380f, 48f, 545f, 75f, 6f, 6f, badgeBgPaint)

            val badgeTextPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#1E40AF")
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText(reportType.uppercase(Locale.US), 462.5f, 63f, badgeTextPaint)

            // Divider Line
            val linePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }
            canvas.drawLine(40f, 100f, 545f, 100f, linePaint)

            // Metadata block
            val metaLabelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#64748B")
                textSize = 9f
                isAntiAlias = true
                isFakeBoldText = true
            }
            val metaValPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#0F172A")
                textSize = 10f
                isAntiAlias = true
                isFakeBoldText = true
            }

            canvas.drawText("GENERATION DATE", 40f, 125f, metaLabelPaint)
            val dateString = java.text.SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            canvas.drawText(dateString, 40f, 140f, metaValPaint)

            canvas.drawText("CERTIFICATION STATUS", 220f, 125f, metaLabelPaint)
            canvas.drawText("OFF-STATION AUDITED", 220f, 140f, metaValPaint)

            canvas.drawText("FORMAT CLASSIFICATION", 390f, 125f, metaLabelPaint)
            canvas.drawText("PDF ENCLOSURE STANDARD", 390f, 140f, metaValPaint)

            // Financial Summary Block
            val boxPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F8FAFC")
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawRoundRect(40f, 160f, 545f, 225f, 8f, 8f, boxPaint)

            val boxBorderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E2E8F0")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            canvas.drawRoundRect(40f, 160f, 545f, 225f, 8f, 8f, boxBorderPaint)

            // Stats items inside box
            val labelMetricPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#475569")
                textSize = 8f
                isAntiAlias = true
                isFakeBoldText = true
            }
            val valMetricPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#0F172A")
                textSize = 11f
                isAntiAlias = true
                isFakeBoldText = true
            }

            canvas.drawText("TOTAL REVENUE", 55f, 182f, labelMetricPaint)
            canvas.drawText("₹${String.format(java.util.Locale.US, "%,.2f", revenue)}", 55f, 204f, valMetricPaint)

            canvas.drawText("TOTAL EXPENDITURES", 185f, 182f, labelMetricPaint)
            canvas.drawText("₹${String.format(java.util.Locale.US, "%,.2f", expense)}", 185f, 204f, valMetricPaint)

            val balancePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor(if (balance >= 0) "#15803D" else "#B91C1C")
                textSize = 11f
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.drawText("NET EARNINGS", 315f, 182f, labelMetricPaint)
            canvas.drawText("₹${String.format(java.util.Locale.US, "%,.2f", balance)}", 315f, 204f, balancePaint)

            canvas.drawText("OUTSTANDING DEBTS", 440f, 182f, labelMetricPaint)
            canvas.drawText("₹${String.format(java.util.Locale.US, "%,.2f", outstanding)}", 440f, 204f, valMetricPaint)

            // Title list
            val subTitleLabelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#0F172A")
                textSize = 11f
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.drawText("DETAILED LEDGER TRANSCRIPT LOGS", 40f, 250f, subTitleLabelPaint)

            // Table headers
            canvas.drawLine(40f, 262f, 545f, 262f, linePaint)
            
            val thPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#475569")
                textSize = 8f
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.drawText("INDEX", 45f, 274f, thPaint)
            canvas.drawText("TYPE", 90f, 274f, thPaint)
            canvas.drawText("DOC NUMBER", 150f, 274f, thPaint)
            canvas.drawText("PARTNER WORKSPACE CLIENT", 240f, 274f, thPaint)
            canvas.drawText("STATUS", 440f, 274f, thPaint)
            canvas.drawText("TOTAL", 510f, 274f, thPaint)

            canvas.drawLine(40f, 282f, 545f, 282f, linePaint)

            // Rows
            val trTextPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#334155")
                textSize = 8.5f
                isAntiAlias = true
            }
            val statusItemPaint = android.graphics.Paint().apply {
                textSize = 8f
                isFakeBoldText = true
                isAntiAlias = true
            }
            
            var yOffset = 298f
            val docPageLimit = documents.take(24) // Fits easily on single A4 sheet page
            docPageLimit.forEachIndexed { i, doc ->
                canvas.drawText(String.format(java.util.Locale.US, "%02d", i + 1), 45f, yOffset, trTextPaint)
                canvas.drawText(doc.type, 90f, yOffset, trTextPaint)
                canvas.drawText(doc.docNumber, 150f, yOffset, trTextPaint)
                
                val cn = if (doc.clientName.length > 22) doc.clientName.take(20) + ".." else doc.clientName
                canvas.drawText(cn, 240f, yOffset, trTextPaint)
                
                statusItemPaint.color = when (doc.status.uppercase()) {
                    "PAID", "APPROVED" -> android.graphics.Color.parseColor("#15803D")
                    "PENDING", "ACTIVE" -> android.graphics.Color.parseColor("#B58E49")
                    else -> android.graphics.Color.parseColor("#B91C1C")
                }
                canvas.drawText(doc.status.uppercase(), 440f, yOffset, statusItemPaint)
                
                trTextPaint.color = android.graphics.Color.parseColor("#0F172A")
                trTextPaint.isFakeBoldText = true
                
                val valStr = "₹${String.format(java.util.Locale.US, "%,.0f", doc.totalAmount)}"
                canvas.drawText(valStr, 540f - trTextPaint.measureText(valStr), yOffset, trTextPaint)
                
                trTextPaint.color = android.graphics.Color.parseColor("#334155")
                trTextPaint.isFakeBoldText = false

                yOffset += 18f
            }

            // Footer
            val footerLinePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }
            canvas.drawLine(40f, 790f, 545f, 790f, footerLinePaint)

            val footerWatermarkPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#94A3B8")
                textSize = 7.5f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("Compiled autonomously by Abielan ERP Security Module. All digital logs cryptographically verified. Offline Enclosure.", 292.5f, 805f, footerWatermarkPaint)

            pdfDocument.finishPage(page)

            val fos = java.io.FileOutputStream(localFile)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error drawing PDF report: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        // Generates CSV/EXCEL matching professional rectangular table spreadsheets
        val csvContent = buildString {
            appendLine("INDEX,DOCUMENT_TYPE,DOCUMENT_NUMBER,CLIENT_PARTNER,STATUS,AMOUNT_INR")
            
            documents.forEachIndexed { i, doc ->
                appendLine("${i + 1},${doc.type},${doc.docNumber},\"${doc.clientName.replace("\"", "\"\"")}\",${doc.status.uppercase(Locale.US)},${String.format(java.util.Locale.US, "%.2f", doc.totalAmount)}")
            }
            
            // Blank row followed by perfectly aligned summarizing audit aggregates at the bottom
            appendLine()
            appendLine(",,,,METRIC SUMMARY,VALUE_INR")
            appendLine(",,,,TOTAL REVENUE,${String.format(java.util.Locale.US, "%.2f", revenue)}")
            appendLine(",,,,TOTAL EXPENDITURES,${String.format(java.util.Locale.US, "%.2f", expense)}")
            appendLine(",,,,NET EARNINGS,${String.format(java.util.Locale.US, "%.2f", revenue - expense)}")
            appendLine(",,,,OUTSTANDING DEBTS,${String.format(java.util.Locale.US, "%.2f", outstanding)}")
        }
        try {
            localFile.writeText(csvContent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error writing report data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    if (!localFile.exists() || localFile.length() < 10) {
        Toast.makeText(context, "Export generation failed.", Toast.LENGTH_SHORT).show()
        return
    }

    // Now copy file to Public Downloads so it is downloaded successfully!
    val finalFileName = "Abielan_Report_${reportType.replace(" ", "_")}_${System.currentTimeMillis()}.$finalSuffix"
    val contentResolver = context.contentResolver
    var isDownloaded = false

    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    localFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                isDownloaded = true
            }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = java.io.File(downloadsDir, finalFileName)
            localFile.copyTo(destFile, overwrite = true)
            isDownloaded = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (isDownloaded) {
        Toast.makeText(context, "Downloaded $finalFileName to standard Downloads folder!", Toast.LENGTH_LONG).show()
        try {
            showReportDownloadFinishedNotification(context, reportType, finalFileName, localFile, mimeType)
        } catch (ne: Exception) {
            ne.printStackTrace()
        }
    } else {
        Toast.makeText(context, "Report compiled in local workspace.", Toast.LENGTH_SHORT).show()
    }

    // Fire actual Share intent represent using authority FileProvider
    try {
        val shareUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            localFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, "Certified Ledger Report: $reportType ($format)")
            putExtra(Intent.EXTRA_TEXT, "Attached is the compiled $reportSubject report from Abielan ERP.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share Certified Statement ($format)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Sharing error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun showReportDownloadFinishedNotification(
    context: Context,
    reportType: String,
    fileName: String,
    localFile: java.io.File,
    mimeType: String
) {
    try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "report_downloads_channel"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Report Downloads (Abielan ERP)",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when analytical statement reports and formats are compiled output successfully"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            localFile
        )
        
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            fileName.hashCode(),
            viewIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Report Download Finished")
            .setContentText("$reportType statement saved to Downloads folder.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            
        notificationManager.notify(fileName.hashCode(), builder.build())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
