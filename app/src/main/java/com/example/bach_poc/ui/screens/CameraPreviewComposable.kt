package com.example.bach_poc.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService

@Composable
fun CameraPreviewComposable(
    cameraExecutor: ExecutorService,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    modifier: Modifier = Modifier,
    onPreviewError: ((Throwable) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context)
    }

    val previewViewAltered = remember {
        PreviewView(context)
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        AndroidView(
            modifier = modifier.fillMaxWidth()
                .aspectRatio(1f),
            factory = { previewView }
        ) { view ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (exc: Exception) {
                    onPreviewError?.invoke(exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

}