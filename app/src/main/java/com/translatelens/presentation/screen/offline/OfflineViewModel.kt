package com.translatelens.presentation.screen.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translatelens.data.model.OfflineLanguage
import com.translatelens.data.repository.SettingsRepositoryImpl
import com.translatelens.data.repository.TranslationRepositoryImpl
import com.translatelens.domain.repository.SettingsRepository
import com.translatelens.domain.repository.TranslationRepository
import com.translatelens.presentation.util.languageDisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    val checking: Boolean = true,
    val targetReady: Boolean? = null,
    val downloading: String? = null,
    val repairing: String? = null,
    val error: String? = null,
    val notice: String? = null
)

@HiltViewModel
class OfflineViewModel @Inject constructor(
    private val repo: TranslationRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(OfflineUiState())
    val ui: StateFlow<OfflineUiState> = _ui.asStateFlow()

    private var refreshJob: Job? = null
    private var opJob: Job? = null

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
            _ui.value = _ui.value.copy(checking = true)
            try {
                val target = _ui.value.targetLang
                (repo as? TranslationRepositoryImpl)?.refreshModels(target)
                val targetDeferred = async {
                    try {
                        repo.isModelDownloaded(target, target)
                    } catch (_: Exception) {
                        false
                    }
                }
                val rowChecks = _ui.value.rows.map { row ->
                    async {
                        try {
                            repo.isModelDownloaded(row.language.code, target)
                        } catch (_: Exception) {
                            false
                        }
                    }
                }
                _ui.value = _ui.value.copy(targetReady = targetDeferred.await())
                val results = rowChecks.awaitAll()
                val mismatch = results.zip(_ui.value.rows) { ok, row -> ok != row.installed }.any { it }
                if (mismatch) {
                    (repo as? TranslationRepositoryImpl)?.refreshModels(target)
                }
            } finally {
                _ui.value = _ui.value.copy(checking = false)
            }
        }
    }

    fun targetDisplayName(): String = languageDisplayName(_ui.value.targetLang)

    fun download(lang: OfflineLanguage) {
        val target = _ui.value.targetLang
        if (_ui.value.downloading != null || _ui.value.repairing != null) return
        opJob?.cancel()
        opJob = viewModelScope.launch {
            _ui.value = _ui.value.copy(downloading = lang.code, error = null, notice = null)
            try {
                repo.downloadLanguageModel(lang, target)
                    .onSuccess {
                        _ui.value = _ui.value.copy(notice = "تم تنزيل النموذج بنجاح")
                    }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        _ui.value = _ui.value.copy(error = e.message ?: "فشل التنزيل")
                    }
            } finally {
                _ui.value = _ui.value.copy(downloading = null)
                opJob = null
            }
            refresh()
        }
    }

    fun delete(code: String) {
        val target = _ui.value.targetLang
        if (_ui.value.downloading != null || _ui.value.repairing != null) return
        opJob?.cancel()
        opJob = viewModelScope.launch {
            _ui.value = _ui.value.copy(error = null, notice = null)
            try {
                repo.deleteLanguageModel(code, target)
                    .onSuccess {
                        _ui.value = _ui.value.copy(notice = "تم حذف النموذج")
                    }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        _ui.value = _ui.value.copy(error = e.message ?: "فشل الحذف")
                    }
            } finally {
                opJob = null
            }
            refresh()
        }
    }

    fun repair(code: String) {
        val target = _ui.value.targetLang
        if (_ui.value.downloading != null || _ui.value.repairing != null) return
        opJob?.cancel()
        opJob = viewModelScope.launch {
            _ui.value = _ui.value.copy(repairing = code, error = null, notice = null)
            try {
                repo.repairPair(code, target)
                    .onSuccess {
                        _ui.value = _ui.value.copy(notice = "تم إصلاح النماذج بنجاح")
                    }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        _ui.value = _ui.value.copy(error = e.message ?: "فشل الإصلاح")
                    }
            } finally {
                _ui.value = _ui.value.copy(repairing = null)
                opJob = null
            }
            refresh()
        }
    }

    fun cancelOp() {
        opJob?.cancel()
        opJob = null
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }

    fun consumeNotice() {
        _ui.value = _ui.value.copy(notice = null)
    }
}
