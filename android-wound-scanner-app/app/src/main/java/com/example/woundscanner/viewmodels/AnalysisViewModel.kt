package com.example.woundscanner.viewmodels

import android.app.Application
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.woundscanner.ai.WoundAnalysis
import com.example.woundscanner.data.WoundDatabase
import com.example.woundscanner.data.WoundEntity
import com.example.woundscanner.ui.state.*
import com.example.woundscanner.utils.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class AnalysisViewModel(
    private val application: Application,
    private val database: WoundDatabase = WoundDatabase.getDatabase(application),
    private val woundAnalysis: WoundAnalysis = WoundAnalysis()
) : BaseViewModel<AnalysisScreenState, UiEvent>() {

    private val _analysisProgress = MutableStateFlow<AnalysisProgress?>(null)
    val analysisProgress = _analysisProgress.asStateFlow()

    override fun createInitialState() = AnalysisScreenState()

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.AnalyzeImage -> analyzeImage(event.imageUri)
            is UiEvent.SaveAnalysis -> saveAnalysis(event.notes)
            is UiEvent.RetryAnalysis -> retryAnalysis()
            is UiEvent.DiscardAnalysis -> discardAnalysis()
            else -> {}
        }
    }

    private fun analyzeImage(imageUri: String) {
        viewModelScope.launch {
            try {
                setState { copy(isAnalyzing = true, imageUri = imageUri) }

                // Load and preprocess image
                updateProgress(AnalysisStage.PREPROCESSING, 0f, "Preprocessing image...")
                val bitmap = ImageUtils.loadBitmapFromUri(application, Uri.parse(imageUri))
                    ?: throw IllegalStateException("Failed to load image")
                val processedBitmap = woundAnalysis.preprocessImage(bitmap)
                updateProgress(AnalysisStage.PREPROCESSING, 1f, "Preprocessing complete")

                // Detect wound region
                updateProgress(AnalysisStage.DETECTION, 0f, "Detecting wound region...")
                val boundingBox = woundAnalysis.detectWound(processedBitmap)
                updateProgress(AnalysisStage.DETECTION, 1f, "Wound region detected")

                // Classify wound
                updateProgress(AnalysisStage.CLASSIFICATION, 0f, "Analyzing wound...")
                val classification = woundAnalysis.classifyWound(processedBitmap, boundingBox)
                updateProgress(AnalysisStage.CLASSIFICATION, 1f, "Analysis complete")

                // Generate recommendations
                updateProgress(AnalysisStage.RECOMMENDATION, 0f, "Generating recommendations...")
                val recommendations = woundAnalysis.generateRecommendations(classification)
                updateProgress(AnalysisStage.RECOMMENDATION, 1f, "Recommendations ready")

                // Update state with analysis result
                setState {
                    copy(
                        isAnalyzing = false,
                        analysisResult = AnalysisResult(
                            woundType = classification.type,
                            size = classification.size,
                            severity = classification.severity,
                            confidence = classification.confidence,
                            recommendations = recommendations,
                            boundingBox = boundingBox
                        )
                    )
                }
            } catch (e: Exception) {
                handleError(e)
                setState { copy(isAnalyzing = false) }
            } finally {
                _analysisProgress.value = null
            }
        }
    }

    private fun saveAnalysis(notes: String?) {
        viewModelScope.launch {
            try {
                val result = currentState().analysisResult
                    ?: throw IllegalStateException("No analysis result to save")

                val wound = WoundEntity(
                    imageUri = currentState().imageUri ?: "",
                    type = result.woundType,
                    size = result.size,
                    severity = result.severity,
                    confidence = result.confidence,
                    recommendations = result.recommendations,
                    notes = notes,
                    date = Date(),
                    healingProgress = 0f,
                    requiresFollowUp = result.severity.lowercase() != "mild",
                    followUpDate = if (result.severity.lowercase() != "mild") {
                        Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 7)
                        }.time
                    } else null,
                    isHighPriority = result.severity.lowercase() == "severe"
                )

                database.woundDao().insertWound(wound)
                showSnackbar("Analysis saved successfully")
                setEvent(UiEvent.NavigateBack)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun retryAnalysis() {
        currentState().imageUri?.let { uri ->
            analyzeImage(uri)
        }
    }

    private fun discardAnalysis() {
        viewModelScope.launch {
            try {
                currentState().imageUri?.let { uri ->
                    ImageUtils.deleteImage(application, Uri.parse(uri))
                }
                setEvent(UiEvent.NavigateBack)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun updateProgress(
        stage: AnalysisStage,
        progress: Float,
        message: String
    ) {
        _analysisProgress.value = AnalysisProgress(
            stage = stage,
            progress = progress,
            message = message
        )
    }

    companion object {
        fun provideFactory(
            application: Application,
            database: WoundDatabase = WoundDatabase.getDatabase(application),
            woundAnalysis: WoundAnalysis = WoundAnalysis()
        ): androidx.lifecycle.ViewModelProvider.Factory {
            return object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return AnalysisViewModel(application, database, woundAnalysis) as T
                }
            }
        }
    }
}
