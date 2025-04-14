package com.example.woundscanner.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import com.example.woundscanner.R
import com.example.woundscanner.ui.theme.WoundScannerTheme
import com.example.woundscanner.viewmodels.CameraViewModel

class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasRequiredPermissions()) {
            requestPermissions()
        }

        setContent {
            WoundScannerTheme {
                CameraScreen(
                    onImageCaptured = { uri ->
                        setResult(RESULT_OK, intent.apply {
                            data = uri
                        })
                        finish()
                    },
                    onClose = {
                        finish()
                    }
                )
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}

@Composable
fun CameraScreen(
    onImageCaptured: (android.net.Uri) -> Unit,
    onClose: () -> Unit,
    viewModel: CameraViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = CameraViewModel.provideFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraState by viewModel.uiState.collectAsState()
    val torchEnabled by viewModel.torchEnabled.collectAsState()

    LaunchedEffect(Unit) {
        if (cameraState.capturedImageUri != null) {
            onImageCaptured(android.net.Uri.parse(cameraState.capturedImageUri))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView ->
                    viewModel.startCamera(lifecycleOwner, previewView)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Camera Controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(
                        onClick = { viewModel.toggleFlash() },
                        enabled = cameraState.isCameraReady,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) {
                                Icons.Default.FlashOn
                            } else {
                                Icons.Default.FlashOff
                            },
                            contentDescription = stringResource(R.string.toggle_flash),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.switchCamera() },
                        enabled = cameraState.isCameraReady,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCamera,
                            contentDescription = stringResource(R.string.switch_camera),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Capture Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = { viewModel.captureImage() },
                    modifier = Modifier.size(72.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = stringResource(R.string.capture_image),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Loading Indicator
        if (cameraState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AndroidView(
    factory: (android.content.Context) -> android.view.View,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = remember { factory(context) }

    androidx.compose.runtime.DisposableEffect(view) {
        onDispose {
            // Clean up view if needed
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { view },
        modifier = modifier
    )
}
