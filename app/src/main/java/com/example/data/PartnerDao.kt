package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerDao {

    @Query("SELECT * FROM partners ORDER BY name ASC")
    fun getAllPartners(): Flow<List<Partner>>

    @Query("SELECT * FROM partners WHERE type = :type ORDER BY name ASC")
    fun getPartnersByType(type: String): Flow<List<Partner>>

    @Query("SELECT * FROM partners WHERE id = :id LIMIT 1")
    suspend fun getPartnerById(id: Int): Partner?

    @Query("SELECT * FROM partners WHERE id = :id LIMIT 1")
    fun getPartnerByIdFlow(id: Int): Flow<Partner?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: Partner): Long

    @Update
    suspend fun updatePartner(partner: Partner)

    @Delete
    suspend fun deletePartner(partner: Partner)
}
