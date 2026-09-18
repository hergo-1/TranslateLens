package com.translatelens.presentation.screen.result

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.translatelens.data.model.TranslatedRegion
import com.translatelens.presentation.component.AppBackground
import com.translatelens.presentation.component.AppTopBar
import com.translatelens.presentation.component.ErrorView
import com.translatelens.presentation.component.LoadingOverlay
import com.translatelens.presentation.util.languageDisplayName
import com.translatelens.presentation.util.rememberImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onBack: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val state by viewModel.ui.collectAsState()
    val snack = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.message) {
        state.message?.let {
            snack.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    AppBackground {
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                AppTopBar(title = "ترجمة الصورة", onBack = onBack)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("من:", style = MaterialTheme.typography.labelLarge)
                    Text(
                        languageDisplayName(state.sourceLang),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("إلى:", style = MaterialTheme.typography.labelLarge)
                    Text(
                        languageDisplayName(state.targetLang),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        bottomBar = {
            if (state.result != null && !state.isLoading) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.save() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Save, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("حفظ")
                        }
                        Button(onClick = { viewModel.share() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("مشاركة")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.openEditor() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Edit, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("تعديل الترجمة")
                        }
                        OutlinedButton(onClick = { viewModel.retry() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("إعادة الترجمة")
                        }
                    }
                    OutlinedButton(onClick = { viewModel.copyTranslated() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("نسخ الترجمة")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                state.isLoading -> LoadingOverlay(state.progress, "جاري الترجمة…")
                state.error != null -> ErrorView(state.error!!, onRetry = { viewModel.retry() })
                state.result != null -> {
                    val r = state.result!!
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
                    val path = if (state.showOriginal) r.originalImagePath else r.translatedImagePath
                    ZoomableImage(path)
                    Text("النص الأصلي:", style = MaterialTheme.typography.titleMedium)
                    Text(r.ocrResult.fullText, style = MaterialTheme.typography.bodyMedium)
                    Text("الترجمة:", style = MaterialTheme.typography.titleMedium)
                    val regions = r.regions.ifEmpty { null }
                    if (regions != null) {
                        Text(
                            regions.joinToString("\n") { it.translatedText },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            r.translations.joinToString("\n") { it.translatedText },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        }
    }

    if (state.editingRegions != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeEditor() },
            sheetState = sheetState,
            windowInsets = WindowInsets.safeDrawing
        ) {
            EditRegionsSheet(
                regions = state.editingRegions!!,
                retranslatingIndex = state.retranslatingIndex,
                onTextChange = { i, t -> viewModel.editRegionText(i, t) },
                onFontScale = { i, s -> viewModel.editRegionFontScale(i, s) },
                onAlignment = { i, a -> viewModel.editRegionAlignment(i, a) },
                onRetranslate = { viewModel.retranslateRegion(it) },
                onSave = { viewModel.saveEdits() },
                onCancel = { viewModel.closeEditor() }
            )
        }
    }
}

@Composable
private fun ZoomableImage(path: String) {
    val bmp = rememberImageBitmap(path)
    var scale by remember(path) { mutableFloatStateOf(1f) }
    var offset by remember(path) { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale <= 1f) Offset.Zero else offset + panChange
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp, max = 420.dp)
                .transformable(transform),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(
                    bmp,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("تعذر عرض الصورة", modifier = Modifier.padding(32.dp))
            }
        }
    }
    Text(
        "قرّب بإصبعين للتكبير والتحريك",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EditRegionsSheet(
    regions: List<TranslatedRegion>,
    retranslatingIndex: Int?,
    onTextChange: (Int, String) -> Unit,
    onFontScale: (Int, Float) -> Unit,
    onAlignment: (Int, Int) -> Unit,
    onRetranslate: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("تعديل الترجمة (${regions.size} مناطق)", style = MaterialTheme.typography.titleLarge)
        regions.forEach { region ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الأصل: ${region.originalText}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = region.translatedText,
                        onValueChange = { onTextChange(region.index, it) },
                        label = { Text("الترجمة — منطقة ${region.index + 1}") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onRetranslate(region.index) },
                            enabled = retranslatingIndex == null
                        ) {
                            if (retranslatingIndex == region.index) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                            }
                            Spacer(Modifier.size(4.dp))
                            Text("إعادة ترجمة")
                        }
                    }
                    Text("حجم الخط: ${(region.fontScale * 100).toInt()}%")
                    Slider(
                        value = region.fontScale,
                        onValueChange = { onFontScale(region.index, it) },
                        valueRange = 0.6f..1.6f,
                        steps = 9
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = region.alignment == TranslatedRegion.ALIGN_START,
                            onClick = { onAlignment(region.index, TranslatedRegion.ALIGN_START) },
                            label = { Text("بداية") }
                        )
                        FilterChip(
                            selected = region.alignment == TranslatedRegion.ALIGN_CENTER,
                            onClick = { onAlignment(region.index, TranslatedRegion.ALIGN_CENTER) },
                            label = { Text("وسط") }
                        )
                        FilterChip(
                            selected = region.alignment == TranslatedRegion.ALIGN_END,
                            onClick = { onAlignment(region.index, TranslatedRegion.ALIGN_END) },
                            label = { Text("نهاية") }
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("حفظ النتيجة") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("إلغاء") }
        }
    }
}
