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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import com.translatelens.presentation.component.AppBackground
import com.translatelens.presentation.component.AppTopBar
import com.translatelens.presentation.component.ThemeToggle
import com.translatelens.presentation.component.GlassCard
import com.translatelens.presentation.component.GradientButton
import com.translatelens.presentation.component.bodyColor
import com.translatelens.presentation.component.titleColor
import com.translatelens.presentation.util.languageDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTranslateScreen(
    onBack: () -> Unit,
    viewModel: TextTranslateViewModel = hiltViewModel()
) {
    val state by viewModel.ui.collectAsState()
    AppBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            AppTopBar(title = "ترجمة نص", onBack = onBack, actions = { ThemeToggle() })
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LangDrop("من", state.source, viewModel.sources, { viewModel.setSource(it) }, Modifier.weight(1f))
                            LangDrop("إلى", state.target, viewModel.targets, { viewModel.setTarget(it) }, Modifier.weight(1f))
                        }
                        OutlinedTextField(
                            value = state.input,
                            onValueChange = { viewModel.setInput(it) },
                            label = { Text("اكتب النص هنا") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = bodyColor())
                        )
                    }
                }
                GradientButton(
                    text = if (state.loading) "جاري الترجمة…" else "ترجم",
                    onClick = { viewModel.translate() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading && state.input.isNotBlank()
                )
                if (state.error != null) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
                if (state.output.isNotEmpty()) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "الترجمة",
                                style = MaterialTheme.typography.titleMedium,
                                color = titleColor()
                            )
                            Text(
                                state.output,
                                style = MaterialTheme.typography.bodyLarge,
                                color = bodyColor()
                            )
                            GradientButton(
                                text = "نسخ الترجمة",
                                onClick = { viewModel.copy() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
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
