package com.translatelens.domain.repository

import android.graphics.Bitmap
import com.translatelens.data.model.OcrResult

interface OcrRepository {
    suspend fun recognizeText(imagePath: String): Result<OcrResult>
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): Result<OcrResult>
}
