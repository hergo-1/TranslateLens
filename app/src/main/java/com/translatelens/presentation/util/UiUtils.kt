package com.translatelens.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.produceState

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
