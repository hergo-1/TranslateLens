package com.translatelens.presentation.screen.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.translatelens.presentation.util.rememberImageBitmap

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val items = viewModel.paging.collectAsLazyPagingItems()
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("رجوع") }
            TextButton(onClick = { confirmClear = true }) { Text("مسح السجل") }
        }
        Text("السجل", style = MaterialTheme.typography.headlineSmall)
        if (items.itemCount == 0) {
            Text("لا يوجد سجل ترجمة")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items.itemCount) { index ->
                val h = items[index] ?: return@items
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val bmp = rememberImageBitmap(h.translatedImagePath)
                        if (bmp != null) {
                            Image(bmp, null, Modifier.size(72.dp), contentScale = ContentScale.Crop)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(h.originalText.take(120), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            Text(h.translatedText.take(120), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            Text("${h.sourceLanguage} → ${h.targetLanguage}", style = MaterialTheme.typography.labelSmall)
                        }
                        Column {
                            IconButton(onClick = { viewModel.toggleFavorite(h) }) {
                                Text(if (h.isFavorite) "★" else "☆")
                            }
                            IconButton(onClick = { viewModel.delete(h.id) }) {
                                Text("✕")
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.showFavoritesOnly(true) }, modifier = Modifier.weight(1f)) { Text("المفضلة") }
            OutlinedButton(onClick = { viewModel.showFavoritesOnly(false) }, modifier = Modifier.weight(1f)) { Text("الكل") }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("مسح السجل") },
            text = { Text("هل أنت متأكد من مسح كل السجل؟") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; viewModel.clearAll() }) { Text("مسح") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("إلغاء") }
            }
        )
    }
}
