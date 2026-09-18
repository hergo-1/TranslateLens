package com.translatelens.domain.repository

import androidx.paging.PagingData
import com.translatelens.data.entity.TranslationHistory
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    suspend fun saveHistory(history: TranslationHistory): Result<Long>
    suspend fun updateHistory(history: TranslationHistory): Result<Unit>
    suspend fun deleteHistory(id: Long): Result<Unit>
    suspend fun deleteHistoryItem(history: TranslationHistory): Result<Unit>
    suspend fun clearAllHistory(): Result<Unit>
    fun getAllHistory(): Flow<PagingData<TranslationHistory>>
    fun getFavorites(): Flow<PagingData<TranslationHistory>>
    suspend fun getById(id: Long): TranslationHistory?
}
