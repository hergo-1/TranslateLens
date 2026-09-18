package com.translatelens.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.translatelens.data.dao.TranslationHistoryDao
import com.translatelens.data.entity.TranslationHistory
import com.translatelens.data.image.imageResult
import com.translatelens.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

class HistoryRepositoryImpl(
    private val dao: TranslationHistoryDao
) : HistoryRepository {
    override suspend fun saveHistory(history: TranslationHistory): Result<Long> = imageResult {
        dao.insert(history)
    }

    override suspend fun updateHistory(history: TranslationHistory): Result<Unit> = imageResult {
        dao.update(history)
    }

    override suspend fun deleteHistory(id: Long): Result<Unit> = imageResult {
        dao.deleteById(id)
    }

    override suspend fun deleteHistoryItem(history: TranslationHistory): Result<Unit> = imageResult {
        dao.delete(history)
    }

    override suspend fun clearAllHistory(): Result<Unit> = imageResult {
        dao.clearAll()
    }

    override fun getAllHistory(): Flow<PagingData<TranslationHistory>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { dao.getAllPaged() }
        ).flow
    }

    override fun getFavorites(): Flow<PagingData<TranslationHistory>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { dao.getFavoritesPaged() }
        ).flow
    }

    override suspend fun getById(id: Long): TranslationHistory? {
        return try {
            dao.getById(id)
        } catch (_: Exception) {
            null
        }
    }
}
