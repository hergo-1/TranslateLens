package com.translatelens.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<Int>
    val sourceLanguage: Flow<String>
    val targetLanguage: Flow<String>
    val autoDetectSource: Flow<Boolean>
    val saveHistory: Flow<Boolean>
    val imageQuality: Flow<Int>
    val modelSizes: Flow<Map<String, Long>>

    suspend fun setThemeMode(mode: Int)
    suspend fun setSourceLanguage(lang: String)
    suspend fun setTargetLanguage(lang: String)
    suspend fun setAutoDetectSource(enabled: Boolean)
    suspend fun setSaveHistory(enabled: Boolean)
    suspend fun setImageQuality(quality: Int)
    suspend fun setModelSize(pairKey: String, bytes: Long)
    suspend fun clearModelSize(pairKey: String)
}
