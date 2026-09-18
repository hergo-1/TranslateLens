package com.translatelens.presentation.screen.result

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.translatelens.presentation.component.ErrorView
import com.translatelens.presentation.component.LoadingOverlay
import com.translatelens.presentation.util.rememberImageBitmap

@Composable
fun ResultScreen(
    onBack: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val state by viewModel.ui.collectAsState()
    val snack = remember { SnackbarHostState() }
    var showEdit by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snack.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) { Text("رجوع") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !state.showOriginal,
                        onClick = { viewModel.toggleOriginal(false) },
                        label = { Text("المترجمة") }
                    )
                    FilterChip(
                        selected = state.showOriginal,
                        onClick = { viewModel.toggleOriginal(true) },
                        label = { Text("الأصلية") }
                    )
                }
            }
        },
        bottomBar = {
            if (state.result != null) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.save() }, modifier = Modifier.weight(1f)) { Text("حفظ") }
                        Button(onClick = { viewModel.share() }, modifier = Modifier.weight(1f)) { Text("مشاركة") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showEdit = true }, modifier = Modifier.weight(1f)) { Text("تعديل الترجمة") }
                        OutlinedButton(onClick = { viewModel.retry() }, modifier = Modifier.weight(1f)) { Text("إعادة") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.copyOriginal() }, modifier = Modifier.weight(1f)) { Text("نسخ الأصلي") }
                        OutlinedButton(onClick = { viewModel.copyTranslated() }, modifier = Modifier.weight(1f)) { Text("نسخ الترجمة") }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp)
        ) {
            when {
                state.isLoading -> LoadingOverlay(state.progress, "جاري الترجمة…")
                state.error != null -> ErrorView(state.error!!, onRetry = { viewModel.retry() })
                state.result != null -> {
                    val r = state.result!!
                    val path = if (state.showOriginal) r.originalImagePath else r.translatedImagePath
                    val bmp = rememberImageBitmap(path)
                    if (bmp != null) {
                        Image(bmp, contentDescription = null, modifier = Modifier.fillMaxWidth().height(380.dp), contentScale = ContentScale.Fit)
                    } else {
                        Text("تعذر عرض الصورة")
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("النص الأصلي:")
                    Text(r.ocrResult.fullText)
                    Spacer(Modifier.height(8.dp))
                    Text("الترجمة:")
                    val edited = state.editedTranslations
                    if (edited != null) Text(edited.joinToString("\n"))
                    else Text(r.translations.joinToString("\n") { it.translatedText })
                }
            }
        }
    }

    if (showEdit) {
        val r = state.result
        if (r != null) {
            var texts by remember(r) {
                mutableStateOf(
                    (state.editedTranslations ?: r.translations.map { it.translatedText })
                )
            }
            AlertDialog(
                onDismissRequest = { showEdit = false },
                title = { Text("تعديل الترجمة") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        texts.forEachIndexed { i, t ->
                            OutlinedTextField(
                                value = t,
                                onValueChange = { v ->
                                    texts = texts.toMutableList().also { it[i] = v }
                                },
                                label = { Text("سطر ${i + 1}") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showEdit = false
                        viewModel.applyEdits(texts)
                    }) { Text("تطبيق") }
                },
                dismissButton = {
                    TextButton(onClick = { showEdit = false }) { Text("إلغاء") }
                }
            )
        }
    }
}
