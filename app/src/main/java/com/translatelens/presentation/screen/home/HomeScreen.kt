package com.translatelens.presentation.screen.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.translatelens.data.image.ImageFiles
import com.translatelens.presentation.component.AppBackground
import com.translatelens.presentation.component.GlassCard
import com.translatelens.presentation.component.GradientButton
import com.translatelens.presentation.component.GradientTitle
import com.translatelens.presentation.component.ThemeToggle
import com.translatelens.presentation.component.bodyColor
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
                        try {
                            bmp.recycle()
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    AppBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Spacer(Modifier.weight(1f))
                        ThemeToggle()
                    }
                    GradientTitle(
                        text = "TranslateLens",
                        align = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "ترجمة النصوص داخل الصور مع الحفاظ على مكانها",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(Modifier.height(8.dp))
                    GradientButton(
                        text = "ترجمة صورة",
                        icon = Icons.Filled.Image,
                        onClick = { picker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    GradientButton(
                        text = "ترجمة بالكاميرا",
                        icon = Icons.Filled.CameraAlt,
                        onClick = onCamera,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    SecondaryGrid(
                        items = listOf(
                            Icons.Filled.TextFields to "ترجمة نص" to onText,
                            Icons.Filled.Language to "اللغات بدون إنترنت" to onOffline,
                            Icons.Filled.History to "السجل" to onHistory,
                            Icons.Filled.Settings to "الإعدادات" to onSettings
                        )
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "English -- العربية يعمل بدون إنترنت بعد تنزيل النموذج",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryGrid(items: List<Pair<Pair<ImageVector, String>, () -> Unit>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (label, onClick) ->
                    GlassCard(
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                label.first,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                label.second,
                                style = MaterialTheme.typography.labelLarge,
                                color = bodyColor(),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
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
