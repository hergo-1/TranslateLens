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
import com.translatelens.data.image.TextGrouping
import com.translatelens.data.image.imageResult
import com.translatelens.data.model.TranslatedImageResult
import com.translatelens.data.model.TranslatedRegion
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
                    recycleQuietly(bitmap)
                    return Result.failure(e)
                }
                if (ocr.textBlocks.isEmpty()) {
                    recycleQuietly(bitmap)
                    return Result.failure(IllegalStateException("لم يتم العثور على نص في الصورة"))
                }
                onProgress(0.4f)
                val paragraphs = TextGrouping.groupIntoParagraphs(ocr.textBlocks)
                if (paragraphs.isEmpty()) {
                    recycleQuietly(bitmap)
                    return Result.failure(IllegalStateException("لم يتم العثور على نص في الصورة"))
                }
                onProgress(0.5f)
                val paragraphTexts = paragraphs.map { it.text }
                val translatedParagraphs = translateParagraphs(paragraphTexts, sourceLang, targetLang).getOrElse { e ->
                    recycleQuietly(bitmap)
                    return Result.failure(e)
                }
                onProgress(0.7f)
                val regions = paragraphs.mapIndexed { index, p ->
                    TranslatedRegion(
                        index = index,
                        originalText = p.text,
                        translatedText = translatedParagraphs[index],
                        sourceLanguage = sourceLang,
                        targetLanguage = targetLang,
                        boundingBox = p.boundingBox,
                        cornerPoints = p.cornerPoints
                    )
                }
                val translatedBitmap = withContext(Dispatchers.Default) {
                    renderer.renderRegions(bitmap, regions, targetLang)
                }
                recycleQuietly(bitmap)
                onProgress(0.85f)
                val normalizedBitmap = imageFiles.decode(imagePath)
                val normalizedOriginal = try {
                    imageFiles.write(normalizedBitmap, "original")
                } finally {
                    recycleQuietly(normalizedBitmap)
                }
                val translatedPath = imageFiles.write(translatedBitmap, "translated")
                recycleQuietly(translatedBitmap)
                onProgress(1f)
                val translations = regions.map {
                    TranslationResult(it.originalText, it.translatedText, sourceLang, targetLang)
                }
                return Result.success(
                    TranslatedImageResult(
                        originalImagePath = normalizedOriginal,
                        translatedImagePath = translatedPath,
                        ocrResult = ocr,
                        translations = translations,
                        regions = regions
                    )
                )
            } catch (cancelled: CancellationException) {
                recycleQuietly(bitmap)
                throw cancelled
            } catch (e: Exception) {
                recycleQuietly(bitmap)
                return Result.failure(e)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun translateParagraphs(
        paragraphs: List<String>,
        sourceLang: String,
        targetLang: String
    ): Result<List<String>> {
        val batch = translationRepository.translateBatch(paragraphs, sourceLang, targetLang)
        if (batch.isSuccess) {
            return Result.success(batch.getOrThrow().map { it.translatedText })
        }
        val out = mutableListOf<String>()
        for (p in paragraphs) {
            val single = translationRepository.translate(p, sourceLang, targetLang).getOrElse {
                return Result.failure(it)
            }
            out.add(single.translatedText)
        }
        return Result.success(out)
    }

    override suspend fun rerender(
        result: TranslatedImageResult,
        translations: List<String>
    ): Result<TranslatedImageResult> {
        val base = currentRegions(result)
        if (translations.size != base.size) {
            return Result.failure(IllegalStateException("عدد الترجمات لا يطابق عدد المناطق"))
        }
        val updated = base.mapIndexed { i, r -> r.copy(translatedText = translations[i]) }
        return updateRegions(result, updated)
    }

    override suspend fun updateRegions(
        result: TranslatedImageResult,
        regions: List<TranslatedRegion>
    ): Result<TranslatedImageResult> = imageResult {
        val base = currentRegions(result)
        require(regions.size == base.size) { "عدد المناطق لا يطابق المناطق المكتشفة" }
        val target = regions.firstOrNull()?.targetLanguage
            ?: result.translations.firstOrNull()?.targetLanguage ?: "ar"
        val bitmap = imageFiles.decode(result.originalImagePath)
        try {
            val out = withContext(Dispatchers.Default) {
                renderer.renderRegions(bitmap, regions, target)
            }
            recycleQuietly(bitmap)
            val newPath = imageFiles.write(out, "translated_edit")
            recycleQuietly(out)
            val translations = regions.map {
                TranslationResult(it.originalText, it.translatedText, it.sourceLanguage, it.targetLanguage)
            }
            result.copy(translatedImagePath = newPath, translations = translations, regions = regions)
        } finally {
            recycleQuietly(bitmap)
        }
    }

    private fun currentRegions(result: TranslatedImageResult): List<TranslatedRegion> {
        if (result.regions.isNotEmpty()) return result.regions
        return result.ocrResult.textBlocks.mapIndexed { index, block ->
            val t = result.translations.getOrNull(index)
            TranslatedRegion(
                index = index,
                originalText = block.text,
                translatedText = t?.translatedText ?: block.text,
                sourceLanguage = t?.sourceLanguage ?: "en",
                targetLanguage = t?.targetLanguage ?: "ar",
                boundingBox = block.boundingBox,
                cornerPoints = block.cornerPoints
            )
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

    private fun recycleQuietly(bitmap: Bitmap) {
        try {
            if (!bitmap.isRecycled) bitmap.recycle()
        } catch (_: Exception) {
        }
    }
}
