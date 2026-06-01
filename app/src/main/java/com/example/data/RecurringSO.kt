package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_sos")
data class RecurringSO(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serviceName: String,
    val numPersons: Int,
    val hourlyRate: Double,
    val durationMinutes: Int,
    val elapsedSeconds: Long = 0,
    val timerState: String = "IDLE", // "IDLE", "RUNNING", "PAUSED", "COMPLETED"
    val isActive: Boolean = false,
    val associatedInvoiceId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
