package com.translatelens.presentation.screen.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.translatelens.presentation.component.AppBackground
import com.translatelens.presentation.component.DeleteButton
import com.translatelens.presentation.component.DownloadButton
import com.translatelens.presentation.component.GlassCard
import com.translatelens.presentation.component.InnerTopBar
import com.translatelens.presentation.component.ThemeToggle
import com.translatelens.presentation.component.bodyColor
import com.translatelens.presentation.component.secondaryColor
import com.translatelens.presentation.component.titleColor
import com.translatelens.presentation.util.directionLabel
import com.translatelens.presentation.util.formatBytes
import com.translatelens.presentation.util.pairLabel

@Composable
fun OfflineLanguagesScreen(
    onBack: () -> Unit,
    viewModel: OfflineViewModel = hiltViewModel()
) {
    val state by viewModel.ui.collectAsState()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.notice) {
        state.error?.let {
            snack.showSnackbar(it)
            viewModel.clearError()
        }
        state.notice?.let {
            snack.showSnackbar(it)
            viewModel.consumeNotice()
        }
    }

    AppBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InnerTopBar(
                title = "اللغات بدون إنترنت",
                onBack = onBack,
                action = {
                    ThemeToggle()
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث الحالة")
                    }
                }
            )
            GlassCard(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "الترجمة تحتاج نموذجَي المصدر والهدف معًا. التنزيل لمرة واحدة بالإنترنت ثم يعمل كل شيء Offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.rows, key = { it.language.code }) { row ->
                                val lang = row.language
                                val busy = state.downloading == lang.code || state.repairing == lang.code
                                val repairing = state.repairing == lang.code
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                pairLabel(lang.code, row.targetLang),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = titleColor()
                                            )
                                            if (row.installed) {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "${directionLabel(lang.code)} ← ${directionLabel(row.targetLang)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = secondaryColor()
                                        )
                                        if (busy) {
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                            Text(
                                                if (repairing) "جارٍ إصلاح النماذج… قد يستغرق دقائق"
                                                else "جارٍ التنزيل… قد يستغرق دقائق",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = bodyColor()
                                            )
                                        } else if (row.installed) {
                                            Text(
                                                "مثبت على الجهاز" + (row.installedBytes?.let { " — ${formatBytes(it)}" } ?: ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text(
                                                "غير مثبت — الحجم: جارٍ حساب الحجم عند بدء التنزيل",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = secondaryColor()
                                            )
                                        }
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (busy) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 3.dp
                                                )
                                            } else if (row.installed) {
                                                DeleteButton(
                                                    text = "حذف النموذج",
                                                    icon = Icons.Filled.Delete,
                                                    onClick = { viewModel.delete(lang.code) }
                                                )
                                                OutlinedButton(onClick = { viewModel.repair(lang.code) }) {
                                                    Icon(Icons.Filled.Build, null, Modifier.size(18.dp))
                                                    Spacer(Modifier.size(6.dp))
                                                    Text("إصلاح")
                                                }
                                            } else {
                                                DownloadButton(
                                                    text = "تنزيل",
                                                    icon = Icons.Filled.Download,
                                                    onClick = { viewModel.download(lang) }
                                                )
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
            }
            SnackbarHost(hostState = snack)
        }
    }
}
