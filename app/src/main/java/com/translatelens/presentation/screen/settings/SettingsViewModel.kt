package com.translatelens.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translatelens.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {
    val themeMode = repo.themeMode
    val source = repo.sourceLanguage
    val target = repo.targetLanguage
    val saveHistory = repo.saveHistory
    val quality = repo.imageQuality

    fun setTheme(mode: Int) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setSource(lang: String) = viewModelScope.launch { repo.setSourceLanguage(lang) }
    fun setTarget(lang: String) = viewModelScope.launch { repo.setTargetLanguage(lang) }
    fun setSaveHistory(v: Boolean) = viewModelScope.launch { repo.setSaveHistory(v) }
    fun setQuality(q: Int) = viewModelScope.launch { repo.setImageQuality(q) }
}
