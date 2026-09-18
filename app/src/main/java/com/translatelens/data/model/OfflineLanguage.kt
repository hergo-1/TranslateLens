package com.translatelens.data.model

data class OfflineLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val modelSizeBytes: Long = 0L,
    val isDownloaded: Boolean = false,
    val downloadUrl: String? = null
) {
    val modelId: String get() = code

    val displaySize: String
        get() = when {
            code == "en" -> "مدمج"
            modelSizeBytes <= 0 -> "الحجم غير متاح"
            modelSizeBytes >= 1024 * 1024 -> "%.2f MB".format(modelSizeBytes / (1024.0 * 1024))
            modelSizeBytes >= 1024 -> "%.2f KB".format(modelSizeBytes / 1024.0)
            else -> "$modelSizeBytes B"
        }
}

object OfflineLanguages {
    val AVAILABLE_LANGUAGES = listOf(
        OfflineLanguage("en", "English", "English", 0L, isDownloaded = true),
        OfflineLanguage("ar", "Arabic", "العربية", 0L),
        OfflineLanguage("fr", "French", "Français", 0L),
        OfflineLanguage("de", "German", "Deutsch", 0L),
        OfflineLanguage("es", "Spanish", "Español", 0L)
    )

    fun find(code: String): OfflineLanguage? = AVAILABLE_LANGUAGES.firstOrNull { it.code == code }

    fun isSupported(code: String): Boolean = find(code) != null
}
