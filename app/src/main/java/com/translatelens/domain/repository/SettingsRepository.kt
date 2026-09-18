package com.translatelens.domain.repository

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<Int> // 0 = system, 1 = light, 2 = dark
    val sourceLanguage: Flow<String>
    val targetLanguage: Flow<String>
    val autoDetectSource: Flow<Boolean>
    val saveHistory: Flow<Boolean>
    val imageQuality: Flow<Int> // 0 = high, 1 = medium, 2 = low

    suspend fun setThemeMode(mode: Int)
    suspend fun setSourceLanguage(lang: String)
    suspend fun setTargetLanguage(lang: String)
    suspend fun setAutoDetectSource(enabled: Boolean)
    suspend fun setSaveHistory(enabled: Boolean)
    suspend fun setImageQuality(quality: Int)
}