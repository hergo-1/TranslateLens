package com.translatelens.presentation.util

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun rememberImageBitmap(path: String?): ImageBitmap? {
    return produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = try {
            if (path.isNullOrEmpty()) null
            else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = 2
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }.value
}

fun languageDisplayName(code: String): String {
    return when (code.lowercase().substringBefore('-')) {
        "en" -> "English"
        "ar" -> "العربية"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "es" -> "Español"
        "it" -> "Italiano"
        "pt" -> "Português"
        "ru" -> "Русский"
        "tr" -> "Türkçe"
        "ur" -> "اردو"
        "fa" -> "فارسی"
        else -> code
    }
}

fun isRtlLanguage(code: String): Boolean {
    return when (code.lowercase().substringBefore('-').substringBefore('_')) {
        "ar", "fa", "ur", "he", "iw", "ps" -> true
        else -> false
    }
}

fun directionLabel(code: String): String {
    return if (isRtlLanguage(code)) "RTL" else "LTR"
}

fun pairLabel(source: String, target: String): String {
    return "${languageDisplayName(source)} → ${languageDisplayName(target)}"
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "—"
    return when {
        bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024L -> "%.0f MB".format(bytes / (1024.0 * 1024))
        bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

fun formatDate(timestamp: Long): String {
    return try {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
            .format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}
