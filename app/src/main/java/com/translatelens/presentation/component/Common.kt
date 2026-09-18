package com.translatelens.presentation.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translatelens.domain.repository.SettingsRepository
import com.translatelens.presentation.theme.AppGradients
import com.translatelens.presentation.theme.AppSpec
import com.translatelens.presentation.theme.isAppDark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.Unspecified,
    actions: @Composable () -> Unit = {}
) {
    val tint = if (contentColor == Color.Unspecified) titleColor() else contentColor
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = tint
                )
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = tint,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

@Composable
fun LoadingOverlay(progress: Float?, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (progress != null) {
                CircularProgressIndicator(progress = { progress })
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = bodyColor()
                )
            } else {
                CircularProgressIndicator()
            }
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("إعادة المحاولة")
            }
        }
        if (secondaryActionText != null && onSecondaryAction != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSecondaryAction) {
                Text(secondaryActionText)
            }
        }
    }
}

@Composable
fun titleColor(): Color {
    return if (isAppDark()) Color(0xFFFFFFFF) else Color(0xFF111827)
}

@Composable
fun bodyColor(): Color {
    return if (isAppDark()) Color(0xFFE5E7EB) else Color(0xFF374151)
}

@Composable
fun secondaryColor(): Color {
    return if (isAppDark()) Color(0xFF9E9BA8) else Color(0xFF6B7280)
}

@Composable
fun inactiveTextColor(): Color {
    return if (isAppDark()) Color(0xFFC4C2CE) else Color(0xFF4A4D5E)
}

@Composable
fun SectionTitle(title: String) {    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun InnerTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رجوع",
                tint = titleColor()
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = titleColor(),
            modifier = Modifier.weight(1f)
        )
        action()
    }
}

@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val dark = isAppDark()
    val idleBg = if (dark) AppSpec.InactiveDarkBg else AppSpec.InactiveLightBg
    val idleFg = if (dark) AppSpec.InactiveDarkFg else AppSpec.InactiveLightFg
    val idleBorder = if (dark) AppSpec.InactiveDarkBorder else AppSpec.InactiveLightBorder
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = idleBg,
            labelColor = idleFg,
            selectedContainerColor = AppSpec.Active,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = idleBorder,
            selectedBorderColor = AppSpec.Active,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        )
    )
}

@Composable
fun DeleteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val dark = isAppDark()
    val bg = if (dark) AppSpec.DeleteDarkBg else AppSpec.DeleteLightBg
    val fg = if (dark) AppSpec.DeleteDarkFg else AppSpec.DeleteLightFg
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
        }
        Text(text)
    }
}

@Composable
fun DownloadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppSpec.Download,
            contentColor = Color.White
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
        }
        Text(text)
    }
}

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val dark = isAppDark()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val pink = if (dark) Color(0xFFEC4899).copy(alpha = 0.14f) else Color(0xFFF472B6).copy(alpha = 0.10f)
            val purple = if (dark) Color(0xFF7C3AED).copy(alpha = 0.20f) else Color(0xFF8B5CF6).copy(alpha = 0.08f)
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(pink, Color.Transparent),
                        center = Offset(wPx * 0.12f, hPx * 0.06f),
                        radius = maxOf(wPx, hPx) * 0.65f
                    )
                )
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(purple, Color.Transparent),
                        center = Offset(wPx * 0.9f, hPx * 0.95f),
                        radius = maxOf(wPx, hPx) * 0.7f
                    )
                )
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isAppDark()
    val container = if (dark) Color(0xFF1A1821) else Color(0xFFFFFFFF)
    val border = if (dark) Color(0xFF2A2635) else Color(0xFFE2E4ED)
    val shape = RoundedCornerShape(20.dp)
    val shadowColor = if (dark) Color.Black else Color(0xFF8B5CF6).copy(alpha = 0.22f)
    val cardModifier = modifier.shadow(8.dp, shape, spotColor = shadowColor)
    val colors = CardDefaults.cardColors(containerColor = container)
    val borderStroke = BorderStroke(1.dp, border)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = borderStroke
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = borderStroke
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(18.dp)
    Button(
        onClick = onClick,
        modifier = modifier
            .shadow(10.dp, shape, spotColor = Color(0xFF8B5CF6).copy(alpha = 0.4f))
            .height(56.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) AppGradients.Button
                    else Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f))),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

@Composable
fun GradientTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    align: TextAlign = TextAlign.Center
) {
    Text(
        text,
        modifier = modifier,
        style = style.copy(brush = AppGradients.Title),
        textAlign = align
    )
}

@HiltViewModel
class ThemeToggleViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {
    val themeMode = settings.themeMode

    fun setDark(dark: Boolean) = viewModelScope.launch {
        settings.setThemeMode(if (dark) 2 else 1)
    }
}

@Composable
fun ThemeToggle(
    modifier: Modifier = Modifier,
    viewModel: ThemeToggleViewModel = hiltViewModel()
) {
    val mode by viewModel.themeMode.collectAsState(initial = 0)
    val dark = isAppDark()
    ThemeSwitch(
        dark = dark,
        onToggle = { viewModel.setDark(it) },
        modifier = modifier
    )
}

@Composable
fun ThemeSwitch(
    dark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val target = if (dark != isRtl) 30.dp else 2.dp
    val thumbOffset by animateDpAsState(targetValue = target, animationSpec = tween(300), label = "theme_thumb")
    val track = if (dark) Color.White.copy(alpha = 0.14f) else Color(0xFFE3D6F8)
    Box(
        modifier = modifier
            .size(width = 62.dp, height = 34.dp)
            .clip(CircleShape)
            .background(track)
            .clickable { onToggle(!dark) }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LightMode,
                contentDescription = "فاتح",
                tint = if (!dark) Color(0xFFF59E0B) else Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Icon(
                Icons.Filled.DarkMode,
                contentDescription = "داكن",
                tint = if (dark) Color(0xFFC4B5FD) else Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(26.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (dark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = null,
                tint = if (dark) Color(0xFF7C3AED) else Color(0xFFF59E0B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
