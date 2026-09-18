package com.translatelens.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.translatelens.data.image.ImageFiles
import com.translatelens.data.image.awaitFinished
import com.translatelens.data.image.imageResult
import com.translatelens.data.model.OcrResult
import com.translatelens.data.model.TextBlock
import com.translatelens.domain.repository.OcrRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OcrRepositoryImpl(context: Context) : OcrRepository {
    private val files = ImageFiles(context)

    override suspend fun recognizeText(imagePath: String): Result<OcrResult> = imageResult {
        val bitmap = files.decode(imagePath)
        try {
            recognizeTextFromBitmap(bitmap).getOrThrow()
        } finally {
            try {
                bitmap.recycle()
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun recognizeTextFromBitmap(bitmap: Bitmap): Result<OcrResult> = imageResult {
        withContext(Dispatchers.Default) {
            require(!bitmap.isRecycled) { "الصورة غير صالحة" }
            val (work, scale) = maybeUpscale(bitmap)
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    val text = recognizer.process(InputImage.fromBitmap(work, 0)).awaitFinished()
                    val blocks = mutableListOf<TextBlock>()
                    text.textBlocks.forEachIndexed { blockIndex, block ->
                        val ordered = block.lines.sortedWith(
                            compareBy(
                                { it.boundingBox?.centerY() ?: 0 },
                                { it.boundingBox?.centerX() ?: 0 }
                            )
                        )
                        for (line in ordered) {
                            val bounds = line.boundingBox ?: continue
                            if (line.text.isBlank() || bounds.isEmpty) continue
                            if (bounds.width() < 4 || bounds.height() < 4) continue
                            if (bounds.height() < work.height * MIN_RELATIVE_HEIGHT) continue
                            val scaled = RectF(
                                bounds.left / scale,
                                bounds.top / scale,
                                bounds.right / scale,
                                bounds.bottom / scale
                            )
                            blocks.add(
                                TextBlock(
                                    text = line.text.trim(),
                                    boundingBox = scaled,
                                    cornerPoints = line.cornerPoints?.map {
                                        PointF(it.x.toFloat() / scale, it.y.toFloat() / scale)
                                    }.orEmpty(),
                                    language = line.recognizedLanguage,
                                    blockIndex = blockIndex
                                )
                            )
                        }
                    }
                    check(blocks.isNotEmpty()) {
                        "لم يتم العثور على نص واضح في الصورة. حاول الاقتراب من النص أو تحسين الإضاءة."
                    }
                    OcrResult(blocks, bitmap.width, bitmap.height)
                } finally {
                    try {
                        recognizer.close()
                    } catch (_: Exception) {
                    }
                }
            } finally {
                if (work !== bitmap) {
                    try {
                        work.recycle()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun maybeUpscale(bitmap: Bitmap): Pair<Bitmap, Float> {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest >= OCR_MIN_SIDE || largest <= 0) return bitmap to 1f
        val scale = OCR_MIN_SIDE.toFloat() / largest
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return try {
            Bitmap.createScaledBitmap(bitmap, w, h, true) to scale
        } catch (_: Exception) {
            bitmap to 1f
        }
    }

    companion object {
        private const val OCR_MIN_SIDE = 1280
        private const val MIN_RELATIVE_HEIGHT = 0.006f
    }
}
