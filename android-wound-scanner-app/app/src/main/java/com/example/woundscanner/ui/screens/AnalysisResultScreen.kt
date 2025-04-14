package com.example.woundscanner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.woundscanner.R
import com.example.woundscanner.ui.components.*
import com.example.woundscanner.ui.state.AnalysisStage
import com.example.woundscanner.ui.state.UiEvent
import com.example.woundscanner.viewmodels.AnalysisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    imageUri: String,
    onNavigateUp: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: AnalysisViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AnalysisViewModel.provideFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        viewModel.handleEvent(UiEvent.AnalyzeImage(imageUri))
    }

    Scaffold(
        topBar = {
            WoundScannerTopBar(
                title = stringResource(
                    if (uiState.isAnalyzing) R.string.analyzing_image
                    else R.string.analysis_complete
                ),
                onNavigateUp = {
                    if (uiState.analysisResult != null) {
                        showDiscardDialog = true
                    } else {
                        onNavigateUp()
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.error != null -> AnalysisError(
                    error = uiState.error!!,
                    onRetry = { viewModel.handleEvent(UiEvent.RetryAnalysis) }
                )
                uiState.isAnalyzing -> AnalyzingContent(uiState.analysisProgress?.stage)
                uiState.analysisResult != null -> AnalysisContent(
                    imageUri = imageUri,
                    result = uiState.analysisResult!!,
                    onSave = { showNotesDialog = true },
                    onDiscard = { showDiscardDialog = true }
                )
            }
        }

        // Dialogs
        if (showDiscardDialog) {
            ConfirmationDialog(
                title = stringResource(R.string.discard_analysis),
                message = "Are you sure you want to discard this analysis?",
                onConfirm = {
                    viewModel.handleEvent(UiEvent.DiscardAnalysis)
                    showDiscardDialog = false
                    onNavigateUp()
                },
                onDismiss = { showDiscardDialog = false }
            )
        }

        if (showNotesDialog) {
            AddNotesDialog(
                onSave = { notes ->
                    viewModel.handleEvent(UiEvent.SaveAnalysis(notes))
                    showNotesDialog = false
                    onSaveComplete()
                },
                onDismiss = { showNotesDialog = false }
            )
        }
    }
}

@Composable
private fun AnalyzingContent(stage: AnalysisStage?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (stage) {
                AnalysisStage.PREPROCESSING -> "Preprocessing image..."
                AnalysisStage.DETECTION -> "Detecting wound..."
                AnalysisStage.CLASSIFICATION -> "Analyzing wound type..."
                AnalysisStage.RECOMMENDATION -> "Generating recommendations..."
                null -> "Preparing analysis..."
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnalysisContent(
    imageUri: String,
    result: com.example.woundscanner.ui.state.AnalysisResult,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Image Preview
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Analysis Results
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                InfoRow(
                    label = stringResource(R.string.wound_type),
                    value = result.woundType,
                    icon = Icons.Default.LocalHospital
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(
                    label = stringResource(R.string.wound_size),
                    value = result.woundSize,
                    icon = Icons.Default.Straighten
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(
                    label = stringResource(R.string.wound_severity),
                    value = result.severity,
                    icon = Icons.Default.Warning
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(
                    label = stringResource(R.string.confidence_score),
                    value = "${(result.confidence * 100).toInt()}%",
                    icon = Icons.Default.Analytics
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recommendations
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.recommendations),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.recommendations,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.discard_analysis))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save_analysis))
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun AnalysisError(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_retry))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNotesDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_notes)) },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add any additional notes...") },
                minLines = 3
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(notes) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
