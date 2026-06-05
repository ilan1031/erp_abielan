package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partners")
data class Partner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "CLIENT", "VENDOR"
    val email: String = "",
    val phone: String = "",
    val company: String = "",
    val gstin: String = "",
    val address: String = "",
    val notes: String = "",
    val status: String = "Active", // "Active", "Inactive"
    val createdAt: Long = System.currentTimeMillis()
)
