package com.example.bach_poc.ui.screens

import android.content.Context
import android.print.PrintAttributes.Resolution
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
fun CameraScreen(
    context: Context, lifecycleOwner: LifecycleOwner, executor: ExecutorService, preview: PreviewView
) {
    val viewModel: MainViewModel = viewModel()
    var faceDetectorHelper: FaceDetectorHelper by remember { mutableStateOf(FaceDetectorHelper(context = context)) }
    val overlay = remember { OverlayView(context, null) }

    var threshold by remember { mutableStateOf(viewModel.currentThreshold) }
    var delegate by remember { mutableStateOf(viewModel.currentDelegate) }

    DisposableEffect(Unit) {
        onDispose {
            faceDetectorHelper.clearFaceDetector()
            executor.shutdown()
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        }
    }

    fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            val screenSize = Size(640, 480)
            val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(
                ResolutionStrategy(screenSize,
                ResolutionStrategy.FALLBACK_RULE_NONE)
            ).build()

            // Build and bind camera use cases
            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build().also {
                it.setSurfaceProvider(preview.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(executor, faceDetectorHelper::detectLivestreamFrame)
                }

            cameraProvider.unbindAll()
            try {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                android.util.Log.e("CameraFragment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }


    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { preview }
        ) { view ->
            setUpCamera()
        }

        // Overlay view for drawing bounding boxes
        Box(modifier = Modifier.fillMaxSize()) {
            // Draw overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                this.drawIntoCanvas { canvas -> overlay.draw(canvas = canvas.nativeCanvas) }
            }
        }
    }
}