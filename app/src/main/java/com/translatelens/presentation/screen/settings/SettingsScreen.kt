package com.translatelens.presentation.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.translatelens.presentation.component.AppBackground
import com.translatelens.presentation.component.AppFilterChip
import com.translatelens.presentation.component.DeleteButton
import com.translatelens.presentation.component.GlassCard
import com.translatelens.presentation.component.InnerTopBar
import com.translatelens.presentation.component.SectionTitle
import com.translatelens.presentation.component.ThemeToggle
import com.translatelens.presentation.util.formatBytes
import com.translatelens.presentation.util.languageDisplayName

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenOffline: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val src by viewModel.source.collectAsState(initial = "en")
    val dst by viewModel.target.collectAsState(initial = "ar")
    val saveH by viewModel.saveHistory.collectAsState(initial = true)
    val quality by viewModel.quality.collectAsState(initial = 0)
    val sizes by viewModel.modelSizes.collectAsState(initial = emptyMap())

    AppBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InnerTopBar(
                title = "الإعدادات",
                onBack = onBack,
                action = { ThemeToggle() }
            )
            GlassCard(modifier = Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SectionTitle("اللغات")
                    Text("لغة المصدر", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("en", "fr", "de", "es", "ar").forEach { code ->
                            AppFilterChip(
                                selected = src == code,
                                onClick = { viewModel.setSource(code) },
                                label = languageDisplayName(code)
                            )
                        }
                    }
                    Text("لغة الهدف", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ar", "en").forEach { code ->
                            AppFilterChip(
                                selected = dst == code,
                                onClick = { viewModel.setTarget(code) },
                                label = languageDisplayName(code)
                            )
                        }
                    }
                    OutlinedButton(onClick = onOpenOffline, modifier = Modifier.fillMaxWidth()) {
                        Text("إدارة اللغات Offline")
                    }
                    val totalModels = sizes.values.sum()
                    Text(
                        "مساحة نماذج Offline المستخدمة: ${formatBytes(totalModels)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SectionTitle("السجل")
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("حفظ السجل", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = saveH, onCheckedChange = { viewModel.setSaveHistory(it) })
                    }
                    DeleteButton(
                        text = "حذف السجل",
                        icon = Icons.Filled.Delete,
                        onClick = { viewModel.clearHistory() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SectionTitle("جودة الصور")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppFilterChip(selected = quality == 0, onClick = { viewModel.setQuality(0) }, label = "عالية")
                        AppFilterChip(selected = quality == 1, onClick = { viewModel.setQuality(1) }, label = "متوسطة")
                        AppFilterChip(selected = quality == 2, onClick = { viewModel.setQuality(2) }, label = "منخفضة")
                    }
                }
            }
        }
    }
}
