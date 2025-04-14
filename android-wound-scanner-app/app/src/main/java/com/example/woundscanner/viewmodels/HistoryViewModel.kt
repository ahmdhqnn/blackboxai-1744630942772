package com.example.woundscanner.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.woundscanner.data.WoundDatabase
import com.example.woundscanner.data.WoundEntity
import com.example.woundscanner.ui.state.HistoryScreenState
import com.example.woundscanner.ui.state.UiEvent
import com.example.woundscanner.ui.state.WoundFilter
import com.example.woundscanner.ui.state.WoundSortType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class HistoryViewModel(
    application: Application,
    private val database: WoundDatabase = WoundDatabase.getDatabase(application)
) : BaseViewModel<HistoryScreenState, UiEvent>() {

    private val woundDao = database.woundDao()
    private val searchQuery = MutableStateFlow("")
    private val currentFilter = MutableStateFlow(WoundFilter())
    private val currentSortType = MutableStateFlow(WoundSortType.DateDescending)

    init {
        observeWounds()
    }

    override fun createInitialState() = HistoryScreenState()

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.FilterWounds -> updateFilter(event.filter)
            is UiEvent.SortWounds -> updateSortType(event.sortType)
            is UiEvent.SearchWounds -> updateSearchQuery(event.query)
            is UiEvent.DeleteWound -> deleteWound(event.wound)
            is UiEvent.RefreshData -> refreshData()
            else -> {}
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeWounds() {
        combine(
            searchQuery,
            currentFilter,
            currentSortType
        ) { query, filter, sortType ->
            Triple(query, filter, sortType)
        }.flatMapLatest { (query, filter, sortType) ->
            woundDao.getAllWounds()
                .map { wounds ->
                    wounds
                        .filter { wound -> filterWound(wound, query, filter) }
                        .sortedWith(getSortComparator(sortType))
                }
        }.onEach { wounds ->
            setState {
                copy(
                    wounds = wounds,
                    currentFilter = currentFilter.value,
                    currentSortType = currentSortType.value
                )
            }
        }.catch { e ->
            handleError(e)
        }.launchIn(viewModelScope)
    }

    private fun filterWound(wound: WoundEntity, query: String, filter: WoundFilter): Boolean {
        return wound.matchesSearchQuery(query) && wound.matchesFilter(filter)
    }

    private fun WoundEntity.matchesSearchQuery(query: String): Boolean {
        if (query.isEmpty()) return true
        return type.contains(query, ignoreCase = true) ||
                severity.contains(query, ignoreCase = true) ||
                notes?.contains(query, ignoreCase = true) == true
    }

    private fun WoundEntity.matchesFilter(filter: WoundFilter): Boolean {
        return (filter.startDate == null || date >= filter.startDate) &&
                (filter.endDate == null || date <= filter.endDate) &&
                (filter.type == null || type == filter.type) &&
                (filter.severity == null || severity == filter.severity) &&
                (filter.minConfidence == null || confidence >= filter.minConfidence)
    }

    private fun getSortComparator(sortType: WoundSortType): Comparator<WoundEntity> {
        return when (sortType) {
            WoundSortType.DateAscending -> compareBy { it.date }
            WoundSortType.DateDescending -> compareByDescending { it.date }
            WoundSortType.SeverityHighToLow -> compareByDescending { getSeverityWeight(it.severity) }
            WoundSortType.SeverityLowToHigh -> compareBy { getSeverityWeight(it.severity) }
            WoundSortType.TypeAZ -> compareBy { it.type }
            WoundSortType.TypeZA -> compareByDescending { it.type }
        }
    }

    private fun getSeverityWeight(severity: String): Int {
        return when (severity.lowercase()) {
            "severe" -> 3
            "moderate" -> 2
            "mild" -> 1
            else -> 0
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    private fun updateFilter(filter: WoundFilter) {
        currentFilter.value = filter
    }

    private fun updateSortType(sortType: WoundSortType) {
        currentSortType.value = sortType
    }

    private fun deleteWound(wound: WoundEntity) {
        viewModelScope.launch {
            try {
                woundDao.deleteWound(wound)
                showSnackbar("Wound record deleted successfully")
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            try {
                setLoading(true)
                // Refresh data if needed
                setLoading(false)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun getStatistics(): Flow<HistoryStatistics> = flow {
        try {
            combine(
                woundDao.getTotalWoundCount(),
                woundDao.getWoundTypeDistribution(),
                woundDao.getWoundSeverityDistribution(),
                woundDao.getAverageHealingTime()
            ) { total, typeDistribution, severityDistribution, avgHealingTime ->
                HistoryStatistics(
                    totalCount = total,
                    typeDistribution = typeDistribution,
                    severityDistribution = severityDistribution,
                    averageHealingTime = avgHealingTime
                )
            }.collect { stats ->
                emit(stats)
            }
        } catch (e: Exception) {
            handleError(e)
            emit(HistoryStatistics())
        }
    }

    companion object {
        fun provideFactory(
            application: Application,
            database: WoundDatabase = WoundDatabase.getDatabase(application)
        ): androidx.lifecycle.ViewModelProvider.Factory {
            return object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return HistoryViewModel(application, database) as T
                }
            }
        }
    }
}

data class HistoryStatistics(
    val totalCount: Int = 0,
    val typeDistribution: Map<String, Int> = emptyMap(),
    val severityDistribution: Map<String, Int> = emptyMap(),
    val averageHealingTime: Float = 0f
)
