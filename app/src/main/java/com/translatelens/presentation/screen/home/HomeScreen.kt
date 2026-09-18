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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "TranslateLens",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            "ترجمة النصوص داخل الصور مع الحفاظ على مكانها",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        PrimaryAction(
            icon = Icons.Filled.Image,
            label = "ترجمة صورة",
            onClick = { picker.launch("image/*") },
            primary = true
        )
        PrimaryAction(
            icon = Icons.Filled.CameraAlt,
            label = "ترجمة بالكاميرا",
            onClick = onCamera,
            primary = true
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
            "English ← العربية يعمل بدون إنترنت بعد تنزيل النموذج",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PrimaryAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean
) {
    if (primary) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
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
                    Card(
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                label.first,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                label.second,
                                style = MaterialTheme.typography.labelLarge,
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
