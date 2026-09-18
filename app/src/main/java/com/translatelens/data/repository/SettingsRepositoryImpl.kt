package com.translatelens.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    override val introSeen: Flow<Boolean> = dataStore.data.map { it[Keys.INTRO_SEEN] ?: false }
    override val modelSizes: Flow<Map<String, Long>> = dataStore.data.map { prefs ->
        prefs.asMap().entries.mapNotNull { (key, value) ->
            val name = key.name
            if (name.startsWith(MODEL_SIZE_PREFIX) && value is Long && value > 0) {
                name.removePrefix(MODEL_SIZE_PREFIX) to value
            } else {
                null
            }
        }.toMap()
    }

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

    override suspend fun setIntroSeen() {
        dataStore.edit { it[Keys.INTRO_SEEN] = true }
    }

    override suspend fun setModelSize(pairKey: String, bytes: Long) {
        if (bytes <= 0) return
        dataStore.edit { it[longPreferencesKey(MODEL_SIZE_PREFIX + pairKey)] = bytes }
    }

    override suspend fun clearModelSize(pairKey: String) {
        dataStore.edit { it.remove(longPreferencesKey(MODEL_SIZE_PREFIX + pairKey)) }
    }

    private object Keys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val AUTO_DETECT = booleanPreferencesKey("auto_detect")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val IMAGE_QUALITY = intPreferencesKey("image_quality")
        val INTRO_SEEN = booleanPreferencesKey("intro_seen")
    }

    companion object {
        private const val MODEL_SIZE_PREFIX = "model_size_"

        fun pairKey(source: String, target: String): String {
            return "${source.lowercase()}_${target.lowercase()}"
        }
    }
}
