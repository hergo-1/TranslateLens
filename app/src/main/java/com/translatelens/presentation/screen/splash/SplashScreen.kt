package com.translatelens.presentation.screen.splash

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.translatelens.presentation.component.AppBackground
import com.translatelens.presentation.component.GradientButton
import com.translatelens.presentation.component.GradientTitle
import com.translatelens.presentation.component.secondaryColor
import com.translatelens.presentation.theme.AppGradients

@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    onAutoProceed: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.ui.collectAsState()

    LaunchedEffect(state.loading, state.showGetStarted) {
        if (!state.loading && !state.showGetStarted) {
            onAutoProceed()
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(8.dp)
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF8B5CF6).copy(alpha = 0.55f))
                    .clip(RoundedCornerShape(32.dp))
                    .background(AppGradients.Button),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            GradientTitle(
                text = "TranslateLens",
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "بوابتك لترجمة النصوص داخل الصور بذكاء وسهولة مع الحفاظ على مكانها",
                style = MaterialTheme.typography.bodyLarge,
                color = secondaryColor(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            LoadingDots()
            Spacer(Modifier.weight(1f))
            if (state.showGetStarted) {
                GradientButton(
                    text = "ابدأ الآن",
                    onClick = {
                        viewModel.onGetStarted()
                        onGetStarted()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.navigationBarsPadding())
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LoadingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(AppGradients.Button)
            )
        }
    }
}
