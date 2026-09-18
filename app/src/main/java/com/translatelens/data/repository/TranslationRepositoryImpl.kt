package com.translatelens.data.repository

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
import com.translatelens.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TranslationRepositoryImpl : TranslationRepository {
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
        val src = mlKitCode(sourceLang)
        val dst = mlKitCode(targetLang)
        mutex.withLock {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(src)
                .setTargetLanguage(dst)
                .build()
            val translator = Translation.getClient(options)
            try {
                val out = translator.translate(text).awaitFinished()
                TranslationResult(text, out, sourceLang, targetLang)
            } finally {
                try {
                    translator.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): Result<List<TranslationResult>> = imageResult {
        require(texts.isNotEmpty()) { "لا توجد نصوص للترجمة" }
        val src = mlKitCode(sourceLang)
        val dst = mlKitCode(targetLang)
        mutex.withLock {
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

    override suspend fun downloadLanguageModel(language: OfflineLanguage): Result<Unit> = imageResult {
        mutex.withLock {
            val model = modelFor(language.code)
            modelManager.download(model, anyNetwork).awaitFinished()
        }
        refreshModels()
    }

    override suspend fun deleteLanguageModel(languageCode: String): Result<Unit> = imageResult {
        mutex.withLock {
            val model = modelFor(languageCode)
            modelManager.deleteDownloadedModel(model).awaitFinished()
        }
        refreshModels()
    }

    override suspend fun isLanguageDownloaded(languageCode: String): Boolean {
        return try {
            mutex.withLock {
                val model = modelFor(languageCode)
                modelManager.isModelDownloaded(model).awaitFinished()
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun refreshModels() {
        try {
            val downloaded = mutex.withLock {
                modelManager.getDownloadedModels(TranslateRemoteModel::class.java).awaitFinished()
            }
            val codes = downloaded.mapNotNull { model ->
                runCatching { fromMlKit(model.language) }.getOrNull()
            }.toSet()
            _languages.value = OfflineLanguages.AVAILABLE_LANGUAGES.map { lang ->
                if (lang.code == "en") lang.copy(isDownloaded = true)
                else lang.copy(isDownloaded = codes.contains(lang.code))
            }
        } catch (_: Exception) {
        }
    }

    private fun modelFor(code: String): TranslateRemoteModel {
        return TranslateRemoteModel.Builder(mlKitCode(code)).build()
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
}
