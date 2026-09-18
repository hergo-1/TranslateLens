package com.translatelens.presentation.navigation

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val CAMERA = "camera"
    const val TEXT_TRANSLATE = "text_translate"
    const val OFFLINE = "offline"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val RESULT = "result?imagePath={imagePath}"

    fun result(imagePath: String): String {
        return "result?imagePath=" + android.net.Uri.encode(imagePath)
    }
}
