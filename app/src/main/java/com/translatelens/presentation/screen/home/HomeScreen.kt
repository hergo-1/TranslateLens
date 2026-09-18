package com.translatelens.presentation.screen.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.translatelens.data.image.ImageFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HomeScreen(
    onCamera: () -> Unit,
    onText: () -> Unit,
    onOffline: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onImagePicked: (String) -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val files = ImageFiles(context)
                    val bmp = files.decode(uri)
                    try {
                        val path = files.write(bmp, "gallery")
                        withContext(Dispatchers.Main) { onImagePicked(path) }
                    } finally {
                        try { bmp.recycle() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "TranslateLens",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            "ترجمة النصوص داخل الصور مع الحفاظ على مكانها",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCamera, modifier = Modifier.fillMaxWidth()) { Text("ترجمة بالكاميرا") }
        Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("ترجمة صورة") }
        OutlinedButton(onClick = onText, modifier = Modifier.fillMaxWidth()) { Text("ترجمة نص") }
        OutlinedButton(onClick = onOffline, modifier = Modifier.fillMaxWidth()) { Text("اللغات بدون إنترنت") }
        OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("السجل") }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("الإعدادات") }
        Spacer(Modifier.weight(1f))
        Text(
            "English → العربية يعمل بدون إنترنت بعد تنزيل النموذج",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

suspend fun copyUriToTemp(context: android.content.Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val out = File.createTempFile("shared_", ".png", dir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        }
        out.absolutePath
    }
}
