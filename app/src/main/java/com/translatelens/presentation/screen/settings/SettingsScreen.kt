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
import androidx.compose.material3.FilterChip
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
import com.translatelens.presentation.component.AppTopBar
import com.translatelens.presentation.component.GlassCard
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
    val theme by viewModel.themeMode.collectAsState(initial = 0)
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
        ) {
            AppTopBar(
                title = "الإعدادات",
                onBack = onBack,
                actions = { ThemeToggle() }
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionTitle("المظهر")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = theme == 0, onClick = { viewModel.setTheme(0) }, label = { Text("افتراضي النظام") })
                            FilterChip(selected = theme == 1, onClick = { viewModel.setTheme(1) }, label = { Text("فاتح") })
                            FilterChip(selected = theme == 2, onClick = { viewModel.setTheme(2) }, label = { Text("داكن") })
                        }
                    }
                }
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("اللغات")
                        Text("لغة المصدر", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("en", "fr", "de", "es", "ar").forEach { code ->
                                FilterChip(
                                    selected = src == code,
                                    onClick = { viewModel.setSource(code) },
                                    label = { Text(languageDisplayName(code)) }
                                )
                            }
                        }
                        Text("لغة الهدف", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ar", "en").forEach { code ->
                                FilterChip(
                                    selected = dst == code,
                                    onClick = { viewModel.setTarget(code) },
                                    label = { Text(languageDisplayName(code)) }
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
                    }
                }
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionTitle("السجل")
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("حفظ السجل", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = saveH, onCheckedChange = { viewModel.setSaveHistory(it) })
                        }
                        OutlinedButton(onClick = { viewModel.clearHistory() }, modifier = Modifier.fillMaxWidth()) {
                            Text("حذف السجل")
                        }
                    }
                }
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionTitle("جودة الصور")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = quality == 0, onClick = { viewModel.setQuality(0) }, label = { Text("عالية") })
                            FilterChip(selected = quality == 1, onClick = { viewModel.setQuality(1) }, label = { Text("متوسطة") })
                            FilterChip(selected = quality == 2, onClick = { viewModel.setQuality(2) }, label = { Text("منخفضة") })
                        }
                    }
                }
            }
        }
    }
}
