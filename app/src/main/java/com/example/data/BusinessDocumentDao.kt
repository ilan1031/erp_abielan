package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDocumentDao {

    // --- Business Documents Queries ---
    @Query("SELECT * FROM business_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<BusinessDocument>>

    @Query("SELECT * FROM business_documents WHERE type = :type ORDER BY createdAt DESC")
    fun getDocumentsByType(type: String): Flow<List<BusinessDocument>>

    @Query("SELECT * FROM business_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Int): BusinessDocument?

    @Query("SELECT * FROM business_documents WHERE id = :id LIMIT 1")
    fun getDocumentByIdFlow(id: Int): Flow<BusinessDocument?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: BusinessDocument): Long

    @Update
    suspend fun updateDocument(doc: BusinessDocument)

    @Delete
    suspend fun deleteDocument(doc: BusinessDocument)

    // --- Recurring SO Queries ---
    @Query("SELECT * FROM recurring_sos ORDER BY id ASC")
    fun getAllRecurringSOs(): Flow<List<RecurringSO>>

    @Query("SELECT * FROM recurring_sos WHERE id = :id LIMIT 1")
    suspend fun getRecurringSOById(id: Int): RecurringSO?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringSO(so: RecurringSO): Long

    @Update
    suspend fun updateRecurringSO(so: RecurringSO)

    @Delete
    suspend fun deleteRecurringSO(so: RecurringSO)
}
