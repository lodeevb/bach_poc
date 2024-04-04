package com.example.bach_poc.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bach_poc.classes.FaceDetectorHelper
import com.example.bach_poc.databinding.FragmentCameraBinding
import com.example.bach_poc.viewModels.MainViewModel
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.lang.IllegalStateException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment(), FaceDetectorHelper.DetectorListener {

    private val TAG = "FaceDetection"
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!


    private lateinit var faceDetectorHelper: FaceDetectorHelper
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private lateinit var backgroundExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater,container,false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Init backgroundExecutor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        backgroundExecutor.execute {
            faceDetectorHelper = FaceDetectorHelper(
                context = requireContext(),
                threshold = 0.5F,
                currentDelegate = 0,
                faceDetectorListener = this,
                runningMode = RunningMode.LIVE_STREAM
            )
        }
        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }

    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        // CameraProvider
        val cameraProvider = cameraProvider?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        // Preview
        val screenSize = Size(640, 480)
        val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(screenSize,
                ResolutionStrategy.FALLBACK_RULE_NONE)
        ).build()

        preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()

        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(
                    backgroundExecutor,
                    faceDetectorHelper::detectLivestreamFrame
                )
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(resultBundle: FaceDetectorHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {

                // Pass necessary information to OverlayView for drawing on the canvas
                val detectionResult = resultBundle.results[0]
                if (isAdded) {
                    fragmentCameraBinding.overlay.setResults(
                        detectionResult,
                        resultBundle.inputImageHeight,
                        resultBundle.inputImageWidth
                    )
                }

                // Force a redraw
                fragmentCameraBinding.overlay.invalidate()
            }
        }
    }

}