package com.example.woundscanner.ui.state

import android.graphics.RectF
import com.example.woundscanner.data.WoundEntity
import java.util.*

// Base interface for all UI states
interface UiState {
    val isLoading: Boolean
    val error: String?
}

// Base interface for all UI events
interface UiEvent

// Message class for displaying snackbars, toasts, etc.
data class UiMessage(
    val message: String,
    val actionLabel: String? = null,
    val onActionClick: (() -> Unit)? = null,
    val duration: Long = 3000L
)

// Home screen state
data class HomeScreenState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val recentScans: List<WoundEntity> = emptyList(),
    val isCameraPermissionGranted: Boolean = false
) : UiState

// History screen state
data class HistoryScreenState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val wounds: List<WoundEntity> = emptyList(),
    val currentFilter: WoundFilter = WoundFilter(),
    val currentSortType: WoundSortType = WoundSortType.DateDescending
) : UiState

// Camera screen state
data class CameraScreenState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val isCameraReady: Boolean = false,
    val capturedImageUri: String? = null
) : UiState

// Analysis screen state
data class AnalysisScreenState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val isAnalyzing: Boolean = false,
    val imageUri: String? = null,
    val analysisResult: AnalysisResult? = null
) : UiState

// Analysis result
data class AnalysisResult(
    val woundType: String,
    val size: String,
    val severity: String,
    val confidence: Float,
    val recommendations: String,
    val boundingBox: RectF? = null
)

// Analysis progress
data class AnalysisProgress(
    val stage: AnalysisStage,
    val progress: Float,
    val message: String
)

// Analysis stages
enum class AnalysisStage {
    PREPROCESSING,
    DETECTION,
    CLASSIFICATION,
    RECOMMENDATION
}

// Wound filter
data class WoundFilter(
    val startDate: Date? = null,
    val endDate: Date? = null,
    val type: String? = null,
    val severity: String? = null,
    val minConfidence: Float? = null
)

// Wound sort types
enum class WoundSortType {
    DateAscending,
    DateDescending,
    SeverityHighToLow,
    SeverityLowToHigh,
    TypeAZ,
    TypeZA;

    fun toDisplayString(): String {
        return when (this) {
            DateAscending -> "Date (Oldest First)"
            DateDescending -> "Date (Newest First)"
            SeverityHighToLow -> "Severity (High to Low)"
            SeverityLowToHigh -> "Severity (Low to High)"
            TypeAZ -> "Type (A-Z)"
            TypeZA -> "Type (Z-A)"
        }
    }
}

// UI Events
sealed class UiEvent {
    // Navigation events
    object NavigateBack : UiEvent()
    data class NavigateToAnalysis(val imageUri: String) : UiEvent()
    object NavigateToCamera : UiEvent()
    object NavigateToHistory : UiEvent()

    // Action events
    data class AnalyzeImage(val imageUri: String) : UiEvent()
    data class SaveAnalysis(val notes: String?) : UiEvent()
    object RetryAnalysis : UiEvent()
    object DiscardAnalysis : UiEvent()
    object RefreshData : UiEvent()

    // Filter and sort events
    data class FilterWounds(val filter: WoundFilter) : UiEvent()
    data class SortWounds(val sortType: WoundSortType) : UiEvent()
    data class SearchWounds(val query: String) : UiEvent()

    // Wound management events
    data class DeleteWound(val wound: WoundEntity) : UiEvent()
    data class UpdateWound(val wound: WoundEntity) : UiEvent()

    // Permission events
    data class PermissionResult(
        val permission: String,
        val isGranted: Boolean
    ) : UiEvent()
}

// UI Effects
sealed class UiEffect {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : UiEffect()

    data class ShowDialog(
        val title: String,
        val message: String,
        val confirmText: String = "OK",
        val dismissText: String = "Cancel",
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit
    ) : UiEffect()

    object NavigateUp : UiEffect()
    data class NavigateTo(val route: String) : UiEffect()
}

// Loading state
sealed class LoadingState {
    object Idle : LoadingState()
    object Loading : LoadingState()
    data class Success<T>(val data: T) : LoadingState()
    data class Error(val message: String) : LoadingState()
}

// Resource state
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}

// UI Dimensions
object UiDimensions {
    const val CARD_ELEVATION = 4
    const val SPACING_SMALL = 4
    const val SPACING_MEDIUM = 8
    const val SPACING_LARGE = 16
    const val SPACING_EXTRA_LARGE = 24
    const val CORNER_RADIUS = 8
    const val BORDER_WIDTH = 1
    const val ICON_SIZE = 24
    const val BUTTON_HEIGHT = 48
    const val THUMBNAIL_SIZE = 64
}
