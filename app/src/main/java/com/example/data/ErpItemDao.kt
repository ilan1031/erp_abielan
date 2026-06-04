package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ErpItemDao {
    @Query("SELECT * FROM erp_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ErpItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ErpItem): Long

    @Update
    suspend fun updateItem(item: ErpItem)

    @Delete
    suspend fun deleteItem(item: ErpItem)
}
