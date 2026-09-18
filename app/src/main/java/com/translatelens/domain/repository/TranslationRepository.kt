package com.translatelens.domain.repository

import com.translatelens.data.model.OfflineLanguage
import com.translatelens.data.model.TranslationResult
import kotlinx.coroutines.flow.Flow

interface TranslationRepository {
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<TranslationResult>

    suspend fun translateBatch(
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): Result<List<TranslationResult>>

    val availableOfflineLanguages: Flow<List<OfflineLanguage>>
    suspend fun downloadLanguageModel(language: OfflineLanguage, targetLang: String): Result<Unit>
    suspend fun deleteLanguageModel(languageCode: String, targetLang: String): Result<Unit>
    suspend fun repairPair(sourceLang: String, targetLang: String): Result<Unit>
    suspend fun isModelDownloaded(sourceLang: String, targetLang: String): Boolean
    suspend fun isLanguageDownloaded(languageCode: String): Boolean
}
