package com.example.woundscanner.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.woundscanner.data.WoundDatabase
import com.example.woundscanner.data.WoundEntity
import com.example.woundscanner.ui.state.HomeScreenState
import com.example.woundscanner.ui.state.UiEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(
    application: Application,
    private val database: WoundDatabase = WoundDatabase.getDatabase(application)
) : BaseViewModel<HomeScreenState, UiEvent>() {

    private val woundDao = database.woundDao()

    init {
        loadRecentScans()
    }

    override fun createInitialState() = HomeScreenState()

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.RefreshData -> loadRecentScans()
            else -> {}
        }
    }

    private fun loadRecentScans() {
        viewModelScope.launch {
            try {
                setLoading(true)
                val startDate = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1) // Last month
                }.time

                woundDao.getRecentWounds(startDate, RECENT_SCANS_LIMIT)
                    .collect { wounds ->
                        setState {
                            copy(
                                isLoading = false,
                                recentScans = wounds,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun getStatistics(): Flow<HomeStatistics> = flow {
        try {
            combine(
                woundDao.getTotalWoundCount(),
                woundDao.getHealedWoundCount(),
                woundDao.getHighPriorityWoundCount(),
                woundDao.getOverdueFollowUpsCount(Date())
            ) { total, healed, highPriority, overdue ->
                HomeStatistics(
                    totalScans = total,
                    healedWounds = healed,
                    activeWounds = total - healed,
                    highPriorityWounds = highPriority,
                    overdueFollowUps = overdue
                )
            }.collect { stats ->
                emit(stats)
            }
        } catch (e: Exception) {
            handleError(e)
            emit(HomeStatistics())
        }
    }

    fun getHealingProgress(): Flow<List<HealingProgressItem>> = flow {
        try {
            woundDao.getWoundTypeDistribution()
                .map { distribution ->
                    distribution.map { (type, count) ->
                        val healedCount = woundDao.getHealedWoundCount().first()
                        HealingProgressItem(
                            type = type,
                            totalCount = count,
                            healedCount = healedCount,
                            healedPercentage = healedCount.toFloat() / count,
                            averageProgress = calculateAverageProgress(type)
                        )
                    }
                }
                .collect { items ->
                    emit(items)
                }
        } catch (e: Exception) {
            handleError(e)
            emit(emptyList())
        }
    }

    private suspend fun calculateAverageProgress(type: String): Float {
        return try {
            woundDao.getAllWounds()
                .map { wounds ->
                    wounds.filter { it.type == type }
                        .map { it.healingProgress }
                        .average()
                        .toFloat()
                }
                .first()
        } catch (e: Exception) {
            0f
        }
    }

    fun getUpcomingFollowUps(): Flow<List<WoundEntity>> = flow {
        try {
            woundDao.getUpcomingFollowUps(Date())
                .collect { followUps ->
                    emit(followUps)
                }
        } catch (e: Exception) {
            handleError(e)
            emit(emptyList())
        }
    }

    companion object {
        private const val RECENT_SCANS_LIMIT = 5

        fun provideFactory(
            application: Application,
            database: WoundDatabase = WoundDatabase.getDatabase(application)
        ): androidx.lifecycle.ViewModelProvider.Factory {
            return object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return HomeViewModel(application, database) as T
                }
            }
        }
    }
}

data class HomeStatistics(
    val totalScans: Int = 0,
    val activeWounds: Int = 0,
    val healedWounds: Int = 0,
    val highPriorityWounds: Int = 0,
    val overdueFollowUps: Int = 0
)

data class HealingProgressItem(
    val type: String,
    val totalCount: Int,
    val healedCount: Int,
    val healedPercentage: Float,
    val averageProgress: Float
)
