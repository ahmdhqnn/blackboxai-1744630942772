package com.example.woundscanner.viewmodels

import android.app.Application
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.woundscanner.ui.state.CameraScreenState
import com.example.woundscanner.ui.state.UiEvent
import com.example.woundscanner.utils.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraViewModel(
    private val application: Application
) : BaseViewModel<CameraScreenState, UiEvent>() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled = _torchEnabled.asStateFlow()

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector = _cameraSelector.asStateFlow()

    override fun createInitialState() = CameraScreenState()

    override fun handleEvent(event: UiEvent) {
        // Handle specific camera events if needed
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(application)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                setState { copy(isCameraReady = true) }
            } catch (e: Exception) {
                handleError(e)
            }
        }, ContextCompat.getMainExecutor(application))
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProvider = cameraProvider ?: return

        // Preview use case
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // ImageCapture use case
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            // Unbind previous use cases
            cameraProvider.unbindAll()

            // Bind new use cases
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector.value,
                preview,
                imageCapture
            )

            // Initialize torch state
            _torchEnabled.value = camera?.cameraInfo?.hasFlashUnit() == true
        } catch (e: Exception) {
            handleError(e)
        }
    }

    fun captureImage() {
        val imageCapture = imageCapture ?: return
        setState { copy(isLoading = true) }

        try {
            // Create temporary file
            val photoFile = ImageUtils.createImageFile(application)

            // Create output options
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // Take the picture
            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val uri = Uri.fromFile(photoFile)
                        setState {
                            copy(
                                isLoading = false,
                                capturedImageUri = uri.toString()
                            )
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        handleError(exception)
                        setState { copy(isLoading = false) }
                    }
                }
            )
        } catch (e: Exception) {
            handleError(e)
            setState { copy(isLoading = false) }
        }
    }

    fun toggleFlash() {
        camera?.let { camera ->
            if (camera.cameraInfo.hasFlashUnit()) {
                _torchEnabled.value = !_torchEnabled.value
                camera.cameraControl.enableTorch(_torchEnabled.value)
            }
        }
    }

    fun switchCamera() {
        _cameraSelector.value = if (_cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Rebind use cases with new camera selector
        cameraProvider?.let { provider ->
            try {
                provider.unbindAll()
                camera = null
                imageCapture = null
                preview = null
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
    }

    companion object {
        fun provideFactory(
            application: Application
        ): androidx.lifecycle.ViewModelProvider.Factory {
            return object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return CameraViewModel(application) as T
                }
            }
        }
    }
}
