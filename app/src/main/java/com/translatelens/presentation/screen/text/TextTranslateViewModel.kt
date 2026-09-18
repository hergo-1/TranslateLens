package com.translatelens.presentation.screen.text

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translatelens.domain.repository.SettingsRepository
import com.translatelens.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TextUiState(
    val input: String = "",
    val output: String = "",
    val source: String = "en",
    val target: String = "ar",
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TextTranslateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val translationRepo: TranslationRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(TextUiState())
    val ui: StateFlow<TextUiState> = _ui.asStateFlow()
    val sources = listOf("en", "ar", "fr", "de", "es")
    val targets = listOf("ar", "en", "fr", "de", "es")

    init {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                source = settingsRepo.sourceLanguage.first(),
                target = settingsRepo.targetLanguage.first()
            )
        }
    }

    fun setInput(v: String) { _ui.value = _ui.value.copy(input = v) }
    fun setSource(v: String) { _ui.value = _ui.value.copy(source = v) }
    fun setTarget(v: String) { _ui.value = _ui.value.copy(target = v) }

    fun translate() {
        val s = _ui.value
        if (s.input.isBlank()) return
        viewModelScope.launch {
            _ui.value = s.copy(loading = true, error = null)
            val r = translationRepo.translate(s.input, s.source, s.target)
            r.onSuccess {
                _ui.value = _ui.value.copy(loading = false, output = it.translatedText)
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "فشل الترجمة")
            }
        }
    }

    fun copy() {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("t", _ui.value.output))
        } catch (_: Exception) {
        }
    }
}
