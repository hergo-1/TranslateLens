package com.translatelens.presentation.screen.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.translatelens.presentation.util.languageDisplayName

@Composable
fun OfflineLanguagesScreen(
    onBack: () -> Unit,
    viewModel: OfflineViewModel = hiltViewModel()
) {
    val state by viewModel.ui.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("رجوع") }
        Text("اللغات بدون إنترنت", style = MaterialTheme.typography.headlineSmall)
        Text("English → العربية يعمل بدون إنترنت بعد تنزيل النموذج. النماذج تُنزّل عند الطلب.", style = MaterialTheme.typography.bodySmall)
        if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.languages, key = { it.code }) { lang ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${languageDisplayName(lang.code)} (${lang.code})")
                            Text(lang.displaySize, style = MaterialTheme.typography.labelSmall)
                            if (lang.isDownloaded) Text("تم التنزيل", style = MaterialTheme.typography.labelSmall)
                        }
                        if (state.downloading == lang.code) {
                            CircularProgressIndicator()
                        } else if (lang.isDownloaded && lang.code != "en") {
                            OutlinedButton(onClick = { viewModel.delete(lang.code) }) { Text("حذف") }
                        } else if (!lang.isDownloaded) {
                            Button(onClick = { viewModel.download(lang) }) { Text("تنزيل") }
                        }
                    }
                }
            }
        }
    }
}
