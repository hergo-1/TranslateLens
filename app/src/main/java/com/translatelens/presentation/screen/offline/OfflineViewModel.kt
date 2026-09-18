package com.translatelens.presentation.screen.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translatelens.data.model.OfflineLanguage
import com.translatelens.data.repository.TranslationRepositoryImpl
import com.translatelens.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OfflineUiState(
    val languages: List<OfflineLanguage> = emptyList(),
    val downloading: String? = null,
    val error: String? = null
)

@HiltViewModel
class OfflineViewModel @Inject constructor(
    private val repo: TranslationRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(OfflineUiState())
    val ui: StateFlow<OfflineUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo.availableOfflineLanguages.collect { langs ->
                _ui.value = _ui.value.copy(languages = langs)
            }
        }
        viewModelScope.launch {
            (repo as? TranslationRepositoryImpl)?.refreshModels()
        }
    }

    fun download(lang: OfflineLanguage) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(downloading = lang.code, error = null)
            repo.downloadLanguageModel(lang).onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(error = e.message ?: "فشل التنزيل")
            }
            _ui.value = _ui.value.copy(downloading = null)
        }
    }

    fun delete(code: String) {
        viewModelScope.launch {
            repo.deleteLanguageModel(code).onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(error = e.message ?: "فشل الحذف")
            }
        }
    }
}
