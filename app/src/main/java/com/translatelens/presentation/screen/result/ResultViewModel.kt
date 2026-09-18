package com.translatelens.presentation.screen.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translatelens.data.entity.TranslationHistory
import com.translatelens.data.model.TranslatedImageResult
import com.translatelens.domain.repository.HistoryRepository
import com.translatelens.domain.repository.ImageTranslationRepository
import com.translatelens.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val isLoading: Boolean = true,
    val progress: Float = 0f,
    val result: TranslatedImageResult? = null,
    val error: String? = null,
    val showOriginal: Boolean = false,
    val message: String? = null,
    val editedTranslations: List<String>? = null
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val imageRepo: ImageTranslationRepository,
    private val historyRepo: HistoryRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {
    private val imagePath: String = savedStateHandle.get<String>("imagePath") ?: ""

    private val _ui = MutableStateFlow(ResultUiState())
    val ui: StateFlow<ResultUiState> = _ui.asStateFlow()

    private var job: Job? = null

    init {
        retry()
    }

    fun retry() {
        job?.cancel()
        job = viewModelScope.launch {
            _ui.value = ResultUiState(isLoading = true, progress = 0f)
            try {
                val src = settingsRepo.sourceLanguage.first()
                val dst = settingsRepo.targetLanguage.first()
                val saveH = settingsRepo.saveHistory.first()
                val res = imageRepo.translateImage(imagePath, src, dst) { p ->
                    _ui.value = _ui.value.copy(progress = p)
                }.getOrElse { e ->
                    _ui.value = ResultUiState(isLoading = false, error = e.message ?: "فشل الترجمة")
                    return@launch
                }
                if (saveH) {
                    historyRepo.saveHistory(
                        TranslationHistory(
                            originalImagePath = res.originalImagePath,
                            translatedImagePath = res.translatedImagePath,
                            sourceLanguage = src,
                            targetLanguage = dst,
                            originalText = res.ocrResult.fullText,
                            translatedText = res.translations.joinToString(" ") { it.translatedText }
                        )
                    )
                }
                _ui.value = ResultUiState(isLoading = false, progress = 1f, result = res)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = ResultUiState(isLoading = false, error = e.message ?: "فشل الترجمة")
            }
        }
    }

    fun toggleOriginal(show: Boolean) {
        _ui.value = _ui.value.copy(showOriginal = show)
    }

    fun copyOriginal() {
        val text = _ui.value.result?.ocrResult?.fullText ?: return
        copyToClipboard("original", text)
        _ui.value = _ui.value.copy(message = "تم نسخ النص الأصلي")
    }

    fun copyTranslated() {
        val r = _ui.value.result ?: return
        val edited = _ui.value.editedTranslations
        val text = if (edited != null) edited.joinToString("\n")
        else r.translations.joinToString("\n") { it.translatedText }
        copyToClipboard("translated", text)
        _ui.value = _ui.value.copy(message = "تم نسخ الترجمة")
    }

    fun save() {
        val r = _ui.value.result ?: return
        viewModelScope.launch {
            try {
                val quality = settingsRepo.imageQuality.first()
                imageRepo.saveTranslatedImage(r, quality)
                _ui.value = _ui.value.copy(message = "تم حفظ الصورة في المعرض")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(message = "فشل الحفظ: ${e.message}")
            }
        }
    }

    fun share() {
        val r = _ui.value.result ?: return
        viewModelScope.launch {
            try {
                imageRepo.shareTranslatedImage(r)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(message = "فشل المشاركة: ${e.message}")
            }
        }
    }

    fun applyEdits(newTranslations: List<String>) {
        val r = _ui.value.result ?: return
        if (newTranslations.size != r.translations.size) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true)
            try {
                val updated = imageRepo.rerender(r, newTranslations).getOrElse { e ->
                        _ui.value = _ui.value.copy(isLoading = false, message = "فشل إعادة الرسم: ${e.message}")
                        return@launch
                    }
                _ui.value = _ui.value.copy(isLoading = false, result = updated, editedTranslations = newTranslations, message = "تم تطبيق التعديل")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(isLoading = false, message = "فشل التعديل: ${e.message}")
            }
        }
    }

    fun consumeMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    private fun copyToClipboard(label: String, text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
        } catch (_: Exception) {
        }
    }
}
