package com.translatelens.presentation.screen.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translatelens.data.entity.TranslationHistory
import com.translatelens.data.model.TranslatedImageResult
import com.translatelens.data.model.TranslatedRegion
import com.translatelens.domain.repository.HistoryRepository
import com.translatelens.domain.repository.ImageTranslationRepository
import com.translatelens.domain.repository.SettingsRepository
import com.translatelens.domain.repository.TranslationRepository
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
    val sourceLang: String = "en",
    val targetLang: String = "ar",
    val editingRegions: List<TranslatedRegion>? = null,
    val retranslatingIndex: Int? = null
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val imageRepo: ImageTranslationRepository,
    private val historyRepo: HistoryRepository,
    private val settingsRepo: SettingsRepository,
    private val translationRepo: TranslationRepository
) : ViewModel() {
    private val imagePath: String = savedStateHandle.get<String>("imagePath") ?: ""

    private val _ui = MutableStateFlow(ResultUiState())
    val ui: StateFlow<ResultUiState> = _ui.asStateFlow()

    private var job: Job? = null

    init {
        retry()
    }

    fun isModelError(): Boolean {
        val err = _ui.value.error ?: return false
        return err.contains("نموذج") || err.contains("نماذج") ||
            err.contains("model", ignoreCase = true)
    }

    fun repairAndRetry() {
        val src = _ui.value.sourceLang.ifEmpty { "en" }
        val dst = _ui.value.targetLang.ifEmpty { "ar" }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, progress = 0f, error = null)
            val repaired = translationRepo.repairPair(src, dst)
            if (repaired.isFailure) {
                val e = repaired.exceptionOrNull()
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    message = e?.message ?: "فشل إصلاح النماذج"
                )
                return@launch
            }
            _ui.value = _ui.value.copy(message = "تم إصلاح النماذج، جارٍ إعادة الترجمة")
            retry()
        }
    }

    fun retry() {
        job?.cancel()
        job = viewModelScope.launch {
            _ui.value = ResultUiState(isLoading = true, progress = 0f)
            try {
                val src = settingsRepo.sourceLanguage.first()
                val dst = settingsRepo.targetLanguage.first()
                val saveH = settingsRepo.saveHistory.first()
                _ui.value = _ui.value.copy(sourceLang = src, targetLang = dst)
                val res = imageRepo.translateImage(imagePath, src, dst) { p ->
                    _ui.value = _ui.value.copy(progress = p)
                }.getOrElse { e ->
                    _ui.value = ResultUiState(
                        isLoading = false,
                        error = e.message ?: "فشل الترجمة",
                        sourceLang = src,
                        targetLang = dst
                    )
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
                _ui.value = ResultUiState(
                    isLoading = false,
                    progress = 1f,
                    result = res,
                    sourceLang = src,
                    targetLang = dst
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(isLoading = false, error = e.message ?: "فشل الترجمة")
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
        val text = currentRegions().joinToString("\n") { it.translatedText }
            .ifEmpty { r.translations.joinToString("\n") { it.translatedText } }
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

    fun openEditor() {
        val r = _ui.value.result ?: return
        if (r.regions.isEmpty()) {
            _ui.value = _ui.value.copy(message = "لا توجد مناطق نصية قابلة للتعديل")
            return
        }
        _ui.value = _ui.value.copy(editingRegions = r.regions)
    }

    fun closeEditor() {
        _ui.value = _ui.value.copy(editingRegions = null, retranslatingIndex = null)
    }

    fun editRegionText(index: Int, text: String) {
        _ui.value = _ui.value.copy(
            editingRegions = _ui.value.editingRegions?.map {
                if (it.index == index) it.copy(translatedText = text) else it
            }
        )
    }

    fun editRegionFontScale(index: Int, scale: Float) {
        _ui.value = _ui.value.copy(
            editingRegions = _ui.value.editingRegions?.map {
                if (it.index == index) it.copy(fontScale = scale.coerceIn(0.6f, 1.6f)) else it
            }
        )
    }

    fun editRegionAlignment(index: Int, alignment: Int) {
        _ui.value = _ui.value.copy(
            editingRegions = _ui.value.editingRegions?.map {
                if (it.index == index) it.copy(alignment = alignment) else it
            }
        )
    }

    fun retranslateRegion(index: Int) {
        val region = _ui.value.editingRegions?.firstOrNull { it.index == index } ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(retranslatingIndex = index)
            try {
                val res = translationRepo.translate(
                    region.originalText,
                    region.sourceLanguage,
                    region.targetLanguage
                )
                res.onSuccess {
                    editRegionText(index, it.translatedText)
                }.onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    _ui.value = _ui.value.copy(message = "فشل إعادة الترجمة: ${e.message}")
                }
            } finally {
                _ui.value = _ui.value.copy(retranslatingIndex = null)
            }
        }
    }

    fun saveEdits() {
        val r = _ui.value.result ?: return
        val edited = _ui.value.editingRegions ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true)
            try {
                val updated = imageRepo.updateRegions(r, edited).getOrElse { e ->
                    _ui.value = _ui.value.copy(isLoading = false, message = "فشل الحفظ: ${e.message}")
                    return@launch
                }
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    result = updated,
                    editingRegions = null,
                    message = "تم حفظ التعديلات وتحديث الصورة"
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(isLoading = false, message = "فشل الحفظ: ${e.message}")
            }
        }
    }

    fun consumeMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    private fun currentRegions(): List<TranslatedRegion> {
        val r = _ui.value.result ?: return emptyList()
        return if (r.regions.isNotEmpty()) r.regions else emptyList()
    }

    private fun copyToClipboard(label: String, text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
        } catch (_: Exception) {
        }
    }
}
