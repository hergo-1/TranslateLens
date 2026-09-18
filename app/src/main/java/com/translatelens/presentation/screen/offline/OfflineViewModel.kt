package com.translatelens.presentation.screen.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translatelens.data.model.OfflineLanguage
import com.translatelens.data.repository.SettingsRepositoryImpl
import com.translatelens.data.repository.TranslationRepositoryImpl
import com.translatelens.domain.repository.SettingsRepository
import com.translatelens.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LanguageRow(
    val language: OfflineLanguage,
    val targetLang: String,
    val installed: Boolean,
    val installedBytes: Long?
)

data class OfflineUiState(
    val rows: List<LanguageRow> = emptyList(),
    val targetLang: String = "ar",
    val downloading: String? = null,
    val error: String? = null
)

@HiltViewModel
class OfflineViewModel @Inject constructor(
    private val repo: TranslationRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(OfflineUiState())
    val ui: StateFlow<OfflineUiState> = _ui.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                repo.availableOfflineLanguages,
                settings.targetLanguage,
                settings.modelSizes
            ) { langs, target, sizes ->
                Triple(langs, target, sizes)
            }.collect { (langs, target, sizes) ->
                val rows = langs.map { lang ->
                    val key = SettingsRepositoryImpl.pairKey(lang.code, target)
                    LanguageRow(
                        language = lang,
                        targetLang = target,
                        installed = lang.isDownloaded,
                        installedBytes = sizes[key]
                    )
                }
                _ui.value = _ui.value.copy(rows = rows, targetLang = target)
            }
        }
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val target = _ui.value.targetLang
            (repo as? TranslationRepositoryImpl)?.refreshModels(target)
            _ui.value.rows.forEach { row ->
                val ok = repo.isModelDownloaded(row.language.code, target)
                if (ok != row.installed) {
                    (repo as? TranslationRepositoryImpl)?.refreshModels(target)
                    return@launch
                }
            }
        }
    }

    fun download(lang: OfflineLanguage) {
        val target = _ui.value.targetLang
        if (_ui.value.downloading != null) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(downloading = lang.code, error = null)
            repo.downloadLanguageModel(lang, target).onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(error = e.message ?: "فشل التنزيل")
            }
            _ui.value = _ui.value.copy(downloading = null)
            refresh()
        }
    }

    fun delete(code: String) {
        val target = _ui.value.targetLang
        viewModelScope.launch {
            repo.deleteLanguageModel(code, target).onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(error = e.message ?: "فشل الحذف")
            }
            refresh()
        }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }
}
