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
    suspend fun downloadLanguageModel(language: OfflineLanguage): Result<Unit>
    suspend fun deleteLanguageModel(languageCode: String): Result<Unit>
    suspend fun isLanguageDownloaded(languageCode: String): Boolean
}