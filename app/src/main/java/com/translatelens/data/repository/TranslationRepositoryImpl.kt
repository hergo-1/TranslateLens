package com.translatelens.data.repository

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.translatelens.data.image.awaitFinished
import com.translatelens.data.image.imageResult
import com.translatelens.data.model.OfflineLanguage
import com.translatelens.data.model.OfflineLanguages
import com.translatelens.data.model.TranslationResult
import com.translatelens.domain.repository.SettingsRepository
import com.translatelens.domain.repository.TranslationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class TranslationRepositoryImpl(
    private val context: Context,
    private val settings: SettingsRepository
) : TranslationRepository {
    private val _languages = MutableStateFlow(OfflineLanguages.AVAILABLE_LANGUAGES)
    override val availableOfflineLanguages: StateFlow<List<OfflineLanguage>> = _languages.asStateFlow()

    private val mutex = Mutex()
    private val modelManager = RemoteModelManager.getInstance()
    private val anyNetwork = DownloadConditions.Builder().build()

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<TranslationResult> = imageResult {
        require(text.isNotBlank()) { "النص فارغ" }
        translateInternal(listOf(text), sourceLang, targetLang).first()
    }

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): Result<List<TranslationResult>> = imageResult {
        require(texts.isNotEmpty()) { "لا توجد نصوص للترجمة" }
        translateInternal(texts, sourceLang, targetLang)
    }

    private suspend fun translateInternal(
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): List<TranslationResult> {
        val missing = missingModels(sourceLang, targetLang)
        if (missing.isNotEmpty()) {
            throw IllegalStateException(
                "نماذج الترجمة غير مكتملة. نزّل من شاشة اللغات: " + missing.joinToString("، ")
            )
        }
        val src = mlKitCode(sourceLang)
        val dst = mlKitCode(targetLang)
        return mutex.withLock {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(src)
                .setTargetLanguage(dst)
                .build()
            val translator = Translation.getClient(options)
            try {
                texts.map { text ->
                    if (text.isBlank()) {
                        TranslationResult(text, text, sourceLang, targetLang)
                    } else {
                        val out = translator.translate(text).awaitFinished()
                        TranslationResult(text, out, sourceLang, targetLang)
                    }
                }
            } finally {
                try {
                    translator.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    override suspend fun downloadLanguageModel(language: OfflineLanguage, targetLang: String): Result<Unit> = imageResult {
        val before = measureModelsSize()
        mutex.withLock {
            modelManager.download(modelFor(language.code), anyNetwork).awaitFinished()
            if (!sameLanguage(language.code, targetLang)) {
                modelManager.download(modelFor(targetLang), anyNetwork).awaitFinished()
            }
        }
        refreshModels(targetLang)
        if (!isModelDownloaded(language.code, targetLang)) {
            throw IllegalStateException("اكتمل التنزيل لكن تعذر تأكيد تثبيت النموذج. أعد المحاولة.")
        }
        val after = measureModelsSize()
        if (before != null && after != null && after > before) {
            settings.setModelSize(SettingsRepositoryImpl.pairKey(language.code, targetLang), after - before)
        }
    }

    override suspend fun deleteLanguageModel(languageCode: String, targetLang: String): Result<Unit> = imageResult {
        val sourceExisted = try {
            mutex.withLock { checkDownloadedInternal(languageCode) }
        } catch (_: Exception) {
            false
        }
        mutex.withLock {
            try {
                modelManager.deleteDownloadedModel(modelFor(languageCode)).awaitFinished()
            } catch (e: Exception) {
                if (sourceExisted && checkDownloadedInternal(languageCode)) throw e
            }
        }
        settings.clearModelSize(SettingsRepositoryImpl.pairKey(languageCode, targetLang))
        refreshModels(targetLang)
        val stillThere = try {
            mutex.withLock { checkDownloadedInternal(languageCode) }
        } catch (_: Exception) {
            false
        }
        if (stillThere) {
            throw IllegalStateException("تعذر حذف النموذج. أغلق التطبيق وأعد المحاولة.")
        }
    }

    override suspend fun repairPair(sourceLang: String, targetLang: String): Result<Unit> = imageResult {
        mutex.withLock {
            try {
                modelManager.deleteDownloadedModel(modelFor(sourceLang)).awaitFinished()
            } catch (_: Exception) {
            }
            if (!sameLanguage(sourceLang, targetLang)) {
                try {
                    modelManager.deleteDownloadedModel(modelFor(targetLang)).awaitFinished()
                } catch (_: Exception) {
                }
            }
            try {
                modelManager.download(modelFor(sourceLang), anyNetwork).awaitFinished()
            } catch (e: Exception) {
                throw IllegalStateException("فشل تنزيل نموذج ${displayName(sourceLang)}. تحقق من الإنترنت والمساحة.")
            }
            if (!sameLanguage(sourceLang, targetLang)) {
                try {
                    modelManager.download(modelFor(targetLang), anyNetwork).awaitFinished()
                } catch (e: Exception) {
                    throw IllegalStateException("فشل تنزيل نموذج ${displayName(targetLang)}. تحقق من الإنترنت والمساحة.")
                }
            }
        }
        settings.clearModelSize(SettingsRepositoryImpl.pairKey(sourceLang, targetLang))
        refreshModels(targetLang)
        if (!isModelDownloaded(sourceLang, targetLang)) {
            throw IllegalStateException("تعذر إصلاح النماذج. تحقق من الإنترنت والمساحة وأعد المحاولة.")
        }
    }

    override suspend fun isModelDownloaded(sourceLang: String, targetLang: String): Boolean {
        return try {
            mutex.withLock {
                checkDownloadedInternal(sourceLang) && checkDownloadedInternal(targetLang)
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun missingModels(sourceLang: String, targetLang: String): List<String> {
        return try {
            mutex.withLock {
                val missing = mutableListOf<String>()
                if (!checkDownloadedInternal(sourceLang)) {
                    missing.add(displayName(sourceLang))
                }
                if (!sameLanguage(sourceLang, targetLang) && !checkDownloadedInternal(targetLang)) {
                    missing.add(displayName(targetLang))
                }
                missing
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun checkDownloadedInternal(code: String): Boolean {
        return try {
            modelManager.isModelDownloaded(modelFor(code)).awaitFinished()
        } catch (_: Exception) {
            false
        }
    }

    private fun sameLanguage(a: String, b: String): Boolean {
        return mlKitCode(a) == mlKitCode(b)
    }

    override suspend fun isLanguageDownloaded(languageCode: String): Boolean {
        return isModelDownloaded(languageCode, "ar")
    }

    suspend fun refreshModels(targetLang: String = "ar") {
        try {
            val downloaded = mutex.withLock {
                modelManager.getDownloadedModels(TranslateRemoteModel::class.java).awaitFinished()
            }
            val codes = downloaded.mapNotNull { model ->
                runCatching { fromMlKit(model.language) }.getOrNull()
            }.toSet()
            _languages.value = OfflineLanguages.AVAILABLE_LANGUAGES.map { lang ->
                lang.copy(isDownloaded = codes.contains(lang.code))
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun measureModelsSize(): Long? = withContext(Dispatchers.IO) {
        try {
            dirSize(context.filesDir)
        } catch (_: Exception) {
            null
        }
    }

    private fun dirSize(root: File): Long {
        var total = 0L
        val stack = ArrayDeque<File>()
        stack.add(root)
        var visited = 0
        while (stack.isNotEmpty() && visited < MAX_SCAN_FILES) {
            val f = stack.removeLast()
            visited++
            try {
                if (f.isFile) {
                    total += f.length()
                } else if (f.isDirectory) {
                    f.listFiles()?.forEach { stack.add(it) }
                }
            } catch (_: Exception) {
            }
        }
        return total
    }

    private fun modelFor(code: String): TranslateRemoteModel {
        return TranslateRemoteModel.Builder(mlKitCode(code)).build()
    }

    private fun displayName(code: String): String {
        return OfflineLanguages.AVAILABLE_LANGUAGES.firstOrNull { it.code == code }?.nativeName ?: code
    }

    private fun mlKitCode(code: String): String {
        val c = code.lowercase().substringBefore('-').substringBefore('_')
        return when (c) {
            "en" -> TranslateLanguage.ENGLISH
            "ar" -> TranslateLanguage.ARABIC
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            "es" -> TranslateLanguage.SPANISH
            "it" -> TranslateLanguage.ITALIAN
            "pt" -> TranslateLanguage.PORTUGUESE
            "ru" -> TranslateLanguage.RUSSIAN
            "tr" -> TranslateLanguage.TURKISH
            "ur" -> TranslateLanguage.URDU
            "fa" -> TranslateLanguage.PERSIAN
            else -> TranslateLanguage.ENGLISH
        }
    }

    private fun fromMlKit(mlKit: String): String = when (mlKit) {
        TranslateLanguage.ENGLISH -> "en"
        TranslateLanguage.ARABIC -> "ar"
        TranslateLanguage.FRENCH -> "fr"
        TranslateLanguage.GERMAN -> "de"
        TranslateLanguage.SPANISH -> "es"
        TranslateLanguage.ITALIAN -> "it"
        TranslateLanguage.PORTUGUESE -> "pt"
        TranslateLanguage.RUSSIAN -> "ru"
        TranslateLanguage.TURKISH -> "tr"
        TranslateLanguage.URDU -> "ur"
        TranslateLanguage.PERSIAN -> "fa"
        else -> mlKit
    }

    companion object {
        private const val MAX_SCAN_FILES = 20000
    }
}
