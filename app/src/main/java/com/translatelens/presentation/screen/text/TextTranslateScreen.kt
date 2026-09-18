package com.translatelens.presentation.screen.text

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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.translatelens.presentation.component.AppTopBar
import com.translatelens.presentation.util.languageDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTranslateScreen(
    onBack: () -> Unit,
    viewModel: TextTranslateViewModel = hiltViewModel()
) {
    val state by viewModel.ui.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppTopBar(title = "ترجمة نص", onBack = onBack)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LangDrop("من", state.source, viewModel.sources, { viewModel.setSource(it) }, Modifier.weight(1f))
            LangDrop("إلى", state.target, viewModel.targets, { viewModel.setTarget(it) }, Modifier.weight(1f))
        }
        OutlinedTextField(
            value = state.input,
            onValueChange = { viewModel.setInput(it) },
            label = { Text("النص") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Button(onClick = { viewModel.translate() }, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) {
            Text(if (state.loading) "جاري الترجمة…" else "ترجم")
        }
        if (state.error != null) Text(state.error!!)
        if (state.output.isNotEmpty()) {
            OutlinedTextField(
                value = state.output,
                onValueChange = {},
                readOnly = true,
                label = { Text("الترجمة") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Button(onClick = { viewModel.copy() }) { Text("نسخ الترجمة") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LangDrop(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = languageDisplayName(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { code ->
                DropdownMenuItem(
                    text = { Text(languageDisplayName(code)) },
                    onClick = { onSelect(code); expanded = false }
                )
            }
        }
    }
}
