package com.translatelens.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.translatelens.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {
    private val dataStore: DataStore<Preferences> = context.settingsDataStore

    override val themeMode: Flow<Int> = dataStore.data.map { it[Keys.THEME_MODE] ?: 0 }
    override val sourceLanguage: Flow<String> = dataStore.data.map { it[Keys.SOURCE_LANG] ?: "en" }
    override val targetLanguage: Flow<String> = dataStore.data.map { it[Keys.TARGET_LANG] ?: "ar" }
    override val autoDetectSource: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_DETECT] ?: false }
    override val saveHistory: Flow<Boolean> = dataStore.data.map { it[Keys.SAVE_HISTORY] ?: true }
    override val imageQuality: Flow<Int> = dataStore.data.map { it[Keys.IMAGE_QUALITY] ?: 0 }

    override suspend fun setThemeMode(mode: Int) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    override suspend fun setSourceLanguage(lang: String) {
        dataStore.edit { it[Keys.SOURCE_LANG] = lang }
    }

    override suspend fun setTargetLanguage(lang: String) {
        dataStore.edit { it[Keys.TARGET_LANG] = lang }
    }

    override suspend fun setAutoDetectSource(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_DETECT] = enabled }
    }

    override suspend fun setSaveHistory(enabled: Boolean) {
        dataStore.edit { it[Keys.SAVE_HISTORY] = enabled }
    }

    override suspend fun setImageQuality(quality: Int) {
        dataStore.edit { it[Keys.IMAGE_QUALITY] = quality }
    }

    private object Keys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val AUTO_DETECT = booleanPreferencesKey("auto_detect")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val IMAGE_QUALITY = intPreferencesKey("image_quality")
    }
}
