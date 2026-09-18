package com.translatelens.domain.repository

import com.translatelens.data.model.TranslatedImageResult

interface ImageTranslationRepository {
    suspend fun translateImage(
        imagePath: String,
        sourceLang: String,
        targetLang: String,
        onProgress: (Float) -> Unit = {}
    ): Result<TranslatedImageResult>

    suspend fun rerender(
        result: TranslatedImageResult,
        translations: List<String>
    ): Result<TranslatedImageResult>

    suspend fun saveTranslatedImage(
        result: TranslatedImageResult,
        quality: Int = 95
    ): Result<String>

    suspend fun shareTranslatedImage(result: TranslatedImageResult): Result<Unit>
}
