package com.translatelens.presentation.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val theme by viewModel.themeMode.collectAsState(initial = 0)
    val src by viewModel.source.collectAsState(initial = "en")
    val dst by viewModel.target.collectAsState(initial = "ar")
    val saveH by viewModel.saveHistory.collectAsState(initial = true)
    val quality by viewModel.quality.collectAsState(initial = 0)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("رجوع") }
        Text("الإعدادات", style = MaterialTheme.typography.headlineSmall)
        Text("السمة")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = theme == 0, onClick = { viewModel.setTheme(0) }, label = { Text("افتراضي النظام") })
            FilterChip(selected = theme == 1, onClick = { viewModel.setTheme(1) }, label = { Text("فاتح") })
            FilterChip(selected = theme == 2, onClick = { viewModel.setTheme(2) }, label = { Text("داكن") })
        }
        Text("المصدر: $src — الهدف: $dst")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = src == "en", onClick = { viewModel.setSource("en") }, label = { Text("English") })
            FilterChip(selected = src == "fr", onClick = { viewModel.setSource("fr") }, label = { Text("Français") })
            FilterChip(selected = src == "de", onClick = { viewModel.setSource("de") }, label = { Text("Deutsch") })
            FilterChip(selected = src == "es", onClick = { viewModel.setSource("es") }, label = { Text("Español") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = dst == "ar", onClick = { viewModel.setTarget("ar") }, label = { Text("العربية") })
            FilterChip(selected = dst == "en", onClick = { viewModel.setTarget("en") }, label = { Text("English") })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("حفظ السجل")
            Switch(checked = saveH, onCheckedChange = { viewModel.setSaveHistory(it) })
        }
        Text("جودة الصورة المحفوظة")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = quality == 0, onClick = { viewModel.setQuality(0) }, label = { Text("عالية") })
            FilterChip(selected = quality == 1, onClick = { viewModel.setQuality(1) }, label = { Text("متوسطة") })
            FilterChip(selected = quality == 2, onClick = { viewModel.setQuality(2) }, label = { Text("منخفضة") })
        }
    }
}
