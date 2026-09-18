package com.translatelens.presentation.screen.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.translatelens.presentation.component.AppTopBar
import java.io.File

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onCaptured: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    var lens by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var capture: ImageCapture? by remember { mutableStateOf(null) }
    var error by remember { mutableStateOf<String?>(null) }

    if (!hasPermission) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp)
        ) {
            Text("يحتاج التطبيق إذن الكاميرا")
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("منح الإذن") }
            TextButton(onClick = onBack) { Text("رجوع") }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    post {
                        bindCamera(ctx, lifecycle, this, lens) { capture = it }
                    }
                }
            },
            update = { view ->
                bindCamera(context, lifecycle, view, lens) { capture = it }
            },
            modifier = Modifier.fillMaxSize()
        )
        AppTopBar(
            title = "ترجمة بالكاميرا",
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
            contentColor = androidx.compose.ui.graphics.Color.White
        )
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            if (error != null) Text(error!!, color = androidx.compose.ui.graphics.Color.White)
            Row(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) { Text("رجوع") }
                Button(
                    onClick = {
                        val c = capture
                        if (c != null) {
                            takePhoto(context, c, onCaptured) { error = it }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("التقاط") }
                TextButton(
                    onClick = {
                        lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
                        else CameraSelector.LENS_FACING_BACK
                    },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) { Text("تبديل") }
            }
            Text(
                "وجّه الكاميرا نحو النص",
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

private fun bindCamera(
    context: Context,
    lifecycle: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    lensFacing: Int,
    onCapture: (ImageCapture) -> Unit
) {
    try {
        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cap = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycle,
            CameraSelector.Builder().requireLensFacing(lensFacing).build(),
            preview,
            cap
        )
        onCapture(cap)
    } catch (_: Exception) {
    }
}

private fun takePhoto(
    context: Context,
    capture: ImageCapture,
    onCaptured: (String) -> Unit,
    onError: (String) -> Unit
) {
    val dir = File(context.filesDir, "images").apply { mkdirs() }
    val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(file).build()
    capture.takePicture(
        output,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(o: ImageCapture.OutputFileResults) {
                onCaptured(file.absolutePath)
            }

            override fun onError(e: ImageCaptureException) {
                onError(e.message ?: "فشل الالتقاط")
            }
        }
    )
}
