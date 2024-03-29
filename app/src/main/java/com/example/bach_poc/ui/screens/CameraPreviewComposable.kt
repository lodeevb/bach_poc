package com.example.bach_poc.ui.screens

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bach_poc.classes.FaceDetectorHelper
import com.example.bach_poc.viewModels.MainViewModel
import com.example.bach_poc.views.OverlayView
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@Composable
fun CameraPreviewComposable(
    cameraExecutor: ExecutorService,
    modifier: Modifier = Modifier,
    onPreviewError: ((Throwable) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context)
    }
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        CameraScreen(context, lifecycleOwner,cameraExecutor,previewView)
    }
}
