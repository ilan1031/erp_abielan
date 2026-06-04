package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_documents")
data class BusinessDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "QUOTE", "INVOICE", "BILL", "PO", "SO"
    val title: String,
    val docNumber: String,
    val clientName: String,
    val issueDate: Long,
    val dueDate: Long,
    val totalAmount: Double,
    val status: String, // "Draft", "Approved", "Paid", "Pending", "Overdue", "Active", "Completed"
    val hourlyRate: Double = 0.0, // For SO Timer
    val timerDurationMinutes: Int = 0, // For SO Timer Limit
    val elapsedSeconds: Long = 0, // Current work duration
    val timerState: String = "IDLE", // "IDLE", "RUNNING", "PAUSED", "COMPLETED"
    val notes: String = "",
    val gstRatePct: Double = 0.0,
    val baseAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
