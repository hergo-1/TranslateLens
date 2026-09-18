package com.translatelens.data.model

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.atan2

data class TextBlock(
    val text: String,
    val boundingBox: RectF,
    val cornerPoints: List<PointF>,
    val language: String? = null,
    val confidence: Float = 1.0f
) {
    val centerX: Float get() = boundingBox.centerX()
    val centerY: Float get() = boundingBox.centerY()
    val width: Float get() = boundingBox.width()
    val height: Float get() = boundingBox.height()
    val angle: Float get() = calculateAngle()

    private fun calculateAngle(): Float {
        if (cornerPoints.size >= 2) {
            val p1 = cornerPoints[0]
            val p2 = cornerPoints[1]
            return atan2(p2.y - p1.y, p2.x - p1.x)
        }
        return 0f
    }

    fun toRect(): Rect = Rect(
        boundingBox.left.toInt(),
        boundingBox.top.toInt(),
        boundingBox.right.toInt(),
        boundingBox.bottom.toInt()
    )
}

data class OcrResult(
    val textBlocks: List<TextBlock>,
    val imageWidth: Int,
    val imageHeight: Int,
    val fullText: String = textBlocks.joinToString(" ") { it.text }
)

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String
)

data class TranslatedImageResult(
    val originalImagePath: String,
    val translatedImagePath: String,
    val ocrResult: OcrResult,
    val translations: List<TranslationResult>
)