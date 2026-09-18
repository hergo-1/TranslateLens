package com.translatelens.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.translatelens.data.image.ImageFiles
import com.translatelens.data.image.Renderer
import com.translatelens.data.image.imageResult
import com.translatelens.data.model.TranslatedImageResult
import com.translatelens.data.model.TranslationResult
import com.translatelens.domain.repository.ImageTranslationRepository
import com.translatelens.domain.repository.OcrRepository
import com.translatelens.domain.repository.TranslationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ImageTranslationRepositoryImpl(
    private val context: Context,
    private val ocrRepository: OcrRepository,
    private val translationRepository: TranslationRepository,
    private val imageFiles: ImageFiles,
    private val renderer: Renderer
) : ImageTranslationRepository {

    override suspend fun translateImage(
        imagePath: String,
        sourceLang: String,
        targetLang: String,
        onProgress: (Float) -> Unit
    ): Result<TranslatedImageResult> {
        try {
            onProgress(0.05f)
            val bitmap = imageFiles.decode(imagePath)
            try {
                onProgress(0.2f)
                val ocr = ocrRepository.recognizeTextFromBitmap(bitmap).getOrElse { e ->
                    bitmap.recycle()
                    return Result.failure(e)
                }
                if (ocr.textBlocks.isEmpty()) {
                    bitmap.recycle()
                    return Result.failure(IllegalStateException("لم يتم العثور على نص في الصورة"))
                }
                onProgress(0.45f)
                val texts = ocr.textBlocks.map { it.text }
                val translations = translationRepository.translateBatch(texts, sourceLang, targetLang).getOrElse { e ->
                    bitmap.recycle()
                    return Result.failure(e)
                }
                onProgress(0.7f)
                val translatedBitmap = withContext(Dispatchers.Default) {
                    renderer.render(bitmap, ocr, translations.map { it.translatedText }, targetLang)
                }
                bitmap.recycle()
                onProgress(0.85f)
                val normalizedBitmap = imageFiles.decode(imagePath)
                val normalizedOriginal = try {
                    imageFiles.write(normalizedBitmap, "original")
                } finally {
                    try {
                        normalizedBitmap.recycle()
                    } catch (_: Exception) {
                    }
                }
                val translatedPath = imageFiles.write(translatedBitmap, "translated")
                translatedBitmap.recycle()
                onProgress(1f)
                return Result.success(
                    TranslatedImageResult(
                        originalImagePath = normalizedOriginal,
                        translatedImagePath = translatedPath,
                        ocrResult = ocr,
                        translations = translations
                    )
                )
            } catch (cancelled: CancellationException) {
                try {
                    bitmap.recycle()
                } catch (_: Exception) {
                }
                throw cancelled
            } catch (e: Exception) {
                try {
                    bitmap.recycle()
                } catch (_: Exception) {
                }
                return Result.failure(e)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun rerender(
        result: TranslatedImageResult,
        translations: List<String>
    ): Result<TranslatedImageResult> = imageResult {
        require(translations.size == result.ocrResult.textBlocks.size) {
            "عدد الترجمات لا يطابق عدد الكتل"
        }
        val target = result.translations.firstOrNull()?.targetLanguage ?: "ar"
        val base = imageFiles.decode(result.originalImagePath)
        try {
            val out = withContext(Dispatchers.Default) {
                renderer.render(base, result.ocrResult, translations, target)
            }
            base.recycle()
            val newPath = imageFiles.write(out, "translated_edit")
            out.recycle()
            val newTranslations = result.ocrResult.textBlocks.mapIndexed { i, block ->
                TranslationResult(
                    originalText = block.text,
                    translatedText = translations[i],
                    sourceLanguage = result.translations.getOrNull(i)?.sourceLanguage ?: "en",
                    targetLanguage = target
                )
            }
            result.copy(translatedImagePath = newPath, translations = newTranslations)
        } finally {
            try {
                if (!base.isRecycled) base.recycle()
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun saveTranslatedImage(
        result: TranslatedImageResult,
        quality: Int
    ): Result<String> = imageResult {
        withContext(Dispatchers.IO) {
            val srcFile = File(result.translatedImagePath)
            if (!srcFile.exists()) throw IllegalStateException("الصورة المترجمة غير موجودة")
            if (Build.VERSION.SDK_INT >= 29) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "TranslateLens_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TranslateLens")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("فشل الحفظ في المعرض")
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        srcFile.inputStream().use { it.copyTo(out) }
                    } ?: throw IllegalStateException("فشل كتابة الصورة")
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri.toString()
                } catch (e: Exception) {
                    try {
                        resolver.delete(uri, null, null)
                    } catch (_: Exception) {
                    }
                    throw e
                }
            } else {
                @Suppress("DEPRECATION")
                val uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileprovider",
                    srcFile
                )
                uri.toString()
            }
        }
    }

    override suspend fun shareTranslatedImage(result: TranslatedImageResult): Result<Unit> = imageResult {
        withContext(Dispatchers.Main) {
            val file = File(result.translatedImagePath)
            if (!file.exists()) throw IllegalStateException("الصورة غير موجودة للمشاركة")
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newUri(context.contentResolver, "image", uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة الصورة").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun decodeForPreview(): suspend (String) -> Bitmap = { imageFiles.decode(it) }
}
