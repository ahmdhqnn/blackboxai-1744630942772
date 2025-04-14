package com.example.woundscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.woundscanner.navigation.NavigationHandler
import com.example.woundscanner.navigation.WoundScannerNavigation
import com.example.woundscanner.ui.theme.WoundScannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WoundScannerApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WoundScannerApp() {
    WoundScannerTheme {
        val navController = rememberNavController()
        val navigationHandler = remember { NavigationHandler(navController) }
        var snackbarHostState = remember { SnackbarHostState() }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    WoundScannerNavigation(navController)
                }
            }
        }

        // Handle system back press
        BackHandler(enabled = navController.previousBackStackEntry != null) {
            navController.navigateUp()
        }
    }
}

@Composable
private fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    val currentOnBack by rememberUpdatedState(onBack)
    
    androidx.activity.compose.BackHandler(enabled = enabled) {
        currentOnBack()
    }
}

// Permission handling
object Permissions {
    const val CAMERA = android.Manifest.permission.CAMERA
    const val WRITE_EXTERNAL_STORAGE = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    const val READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE

    val REQUIRED_PERMISSIONS = arrayOf(
        CAMERA,
        WRITE_EXTERNAL_STORAGE,
        READ_EXTERNAL_STORAGE
    )
}

// Extension function to check if all required permissions are granted
fun ComponentActivity.checkRequiredPermissions(): Boolean {
    return Permissions.REQUIRED_PERMISSIONS.all { permission ->
        checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

// Extension function to request required permissions
fun ComponentActivity.requestRequiredPermissions() {
    requestPermissions(
        Permissions.REQUIRED_PERMISSIONS,
        REQUEST_CODE_PERMISSIONS
    )
}

private const val REQUEST_CODE_PERMISSIONS = 10

// Extension function to handle permission results
fun ComponentActivity.handlePermissionResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray,
    onAllGranted: () -> Unit,
    onSomeNotGranted: () -> Unit
) {
    when (requestCode) {
        REQUEST_CODE_PERMISSIONS -> {
            if (grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                onAllGranted()
            } else {
                onSomeNotGranted()
            }
        }
    }
}

// Lifecycle observer for handling app state
class AppLifecycleObserver(private val onBackground: () -> Unit) : 
    androidx.lifecycle.DefaultLifecycleObserver {
    
    override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
        super.onStop(owner)
        onBackground()
    }
}

// Extension function to observe app lifecycle
fun ComponentActivity.observeAppLifecycle(onBackground: () -> Unit) {
    lifecycle.addObserver(AppLifecycleObserver(onBackground))
}

// Extension function to show error dialog
@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// Extension function to show confirmation dialog
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}

// Extension function to show loading dialog
@Composable
fun LoadingDialog(
    message: String
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Please Wait") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(MaterialTheme.spacing.large)
                )
                Text(message)
            }
        },
        confirmButton = { }
    )
}

// Material3 spacing values
val MaterialTheme.spacing: Spacing
    @Composable
    get() = LocalSpacing.current

data class Spacing(
    val default: androidx.compose.ui.unit.Dp = 16.dp,
    val extraSmall: androidx.compose.ui.unit.Dp = 4.dp,
    val small: androidx.compose.ui.unit.Dp = 8.dp,
    val medium: androidx.compose.ui.unit.Dp = 16.dp,
    val large: androidx.compose.ui.unit.Dp = 24.dp,
    val extraLarge: androidx.compose.ui.unit.Dp = 32.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
