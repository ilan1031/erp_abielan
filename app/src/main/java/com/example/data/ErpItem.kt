package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "erp_items")
data class ErpItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rate: Double,
    val gstRatePct: Double = 18.0, // Default 18% GST rate
    val category: String = "Service", // "Service", "Product", "Goods", "License"
    val description: String = "",
    val trackStock: Boolean = false,
    val stockQuantity: Int = 0,
    val lowStockThreshold: Int = 5,
    val vendorName: String = ""
)
