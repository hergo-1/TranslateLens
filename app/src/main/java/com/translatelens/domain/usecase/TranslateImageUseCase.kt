package com.translatelens.domain.usecase

import com.translatelens.data.entity.TranslationHistory
import com.translatelens.data.model.TranslatedImageResult
import com.translatelens.domain.repository.HistoryRepository
import com.translatelens.domain.repository.ImageTranslationRepository

class TranslateImageUseCase(
    private val imageRepo: ImageTranslationRepository,
    private val historyRepo: HistoryRepository
) {
    suspend fun execute(
        imagePath: String,
        sourceLang: String,
        targetLang: String,
        saveHistory: Boolean,
        onProgress: (Float) -> Unit = {}
    ): Result<TranslatedImageResult> {
        val result = imageRepo.translateImage(imagePath, sourceLang, targetLang, onProgress)
        if (result.isSuccess && saveHistory) {
            val r = result.getOrNull()!!
            val history = TranslationHistory(
                originalImagePath = r.originalImagePath,
                translatedImagePath = r.translatedImagePath,
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                originalText = r.ocrResult.fullText,
                translatedText = r.translations.joinToString(" ") { it.translatedText }
            )
            historyRepo.saveHistory(history)
        }
        return result
    }
}
