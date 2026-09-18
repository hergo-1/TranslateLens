package com.translatelens.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.translatelens.data.entity.TranslationHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: TranslationHistory): Long

    @Update
    suspend fun update(history: TranslationHistory)

    @Delete
    suspend fun delete(history: TranslationHistory)

    @Query("DELETE FROM translation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM translation_history")
    suspend fun clearAll()

    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllPaged(): PagingSource<Int, TranslationHistory>

    @Query("SELECT * FROM translation_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoritesPaged(): PagingSource<Int, TranslationHistory>

    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TranslationHistory>>

    @Query("SELECT * FROM translation_history WHERE id = :id")
    suspend fun getById(id: Long): TranslationHistory?

    @Query("SELECT COUNT(*) FROM translation_history")
    suspend fun getCount(): Int
}
