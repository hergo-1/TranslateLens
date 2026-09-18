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
            bitmap.recycle()
        }
    }

    override suspend fun recognizeTextFromBitmap(bitmap: Bitmap): Result<OcrResult> = imageResult {
        withContext(Dispatchers.Default) {
            require(!bitmap.isRecycled) { "الصورة غير صالحة" }
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitFinished()
                val blocks = text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                    val bounds = line.boundingBox ?: return@mapNotNull null
                    if (line.text.isBlank() || bounds.isEmpty) return@mapNotNull null
                    TextBlock(
                        text = line.text,
                        boundingBox = RectF(bounds),
                        cornerPoints = line.cornerPoints?.map {
                            PointF(it.x.toFloat(), it.y.toFloat())
                        }.orEmpty(),
                        language = line.recognizedLanguage
                    )
                }
                check(blocks.isNotEmpty()) {
                    "لم يتم العثور على نص لاتيني واضح. التعرف على النص العربي غير مدعوم؛ يمكن الترجمة إلى العربية."
                }
                OcrResult(blocks, bitmap.width, bitmap.height)
            } finally {
                recognizer.close()
            }
        }
    }
}
