package com.example.woundscanner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.woundscanner.R
import com.example.woundscanner.data.WoundEntity
import com.example.woundscanner.ui.components.*
import com.example.woundscanner.ui.state.WoundFilter
import com.example.woundscanner.ui.state.WoundSortType
import com.example.woundscanner.viewmodels.HistoryStatistics
import com.example.woundscanner.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateUp: () -> Unit,
    onWoundSelected: (String) -> Unit,
    viewModel: HistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = HistoryViewModel.provideFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val statistics by viewModel.getStatistics().collectAsState(initial = HistoryStatistics())
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<WoundEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            WoundScannerTopBar(
                title = stringResource(R.string.wound_history),
                onNavigateUp = onNavigateUp,
                actions = {
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = stringResource(R.string.sort_wounds)
                        )
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.filter_wounds)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    viewModel.handleEvent(com.example.woundscanner.ui.state.UiEvent.SearchWounds(it))
                },
                placeholder = stringResource(R.string.search_wounds),
                modifier = Modifier.padding(16.dp)
            )

            // Statistics Cards
            StatisticsSection(statistics)

            // Wounds List
            if (uiState.wounds.isEmpty()) {
                EmptyStateMessage(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.no_history),
                    message = stringResource(R.string.start_scanning),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.wounds) { wound ->
                        WoundHistoryItem(
                            wound = wound,
                            onWoundClick = { onWoundSelected(wound.imageUri) },
                            onDeleteClick = { showDeleteDialog = wound }
                        )
                    }
                }
            }
        }

        // Filter Dialog
        if (showFilterDialog) {
            FilterDialog(
                currentFilter = uiState.currentFilter,
                onFilterApplied = { filter ->
                    viewModel.handleEvent(com.example.woundscanner.ui.state.UiEvent.FilterWounds(filter))
                    showFilterDialog = false
                },
                onDismiss = { showFilterDialog = false }
            )
        }

        // Sort Dialog
        if (showSortDialog) {
            SortDialog(
                currentSortType = uiState.currentSortType,
                onSortSelected = { sortType ->
                    viewModel.handleEvent(com.example.woundscanner.ui.state.UiEvent.SortWounds(sortType))
                    showSortDialog = false
                },
                onDismiss = { showSortDialog = false }
            )
        }

        // Delete Confirmation Dialog
        showDeleteDialog?.let { wound ->
            ConfirmationDialog(
                title = stringResource(R.string.dialog_delete_title),
                message = stringResource(R.string.dialog_delete_message),
                onConfirm = {
                    viewModel.handleEvent(com.example.woundscanner.ui.state.UiEvent.DeleteWound(wound))
                    showDeleteDialog = null
                },
                onDismiss = { showDeleteDialog = null },
                confirmText = stringResource(R.string.action_delete),
                confirmColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WoundHistoryItem(
    wound: WoundEntity,
    onWoundClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        onClick = onWoundClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = wound.type,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(wound.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WoundSeverityChip(severity = wound.severity)
                Text(
                    text = wound.size,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HealingProgressBar(progress = wound.healingProgress)
        }
    }
}

@Composable
private fun StatisticsSection(statistics: HistoryStatistics) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticCard(
                    title = stringResource(R.string.stats_total_wounds),
                    value = statistics.totalCount.toString(),
                    icon = Icons.Default.Assignment,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatisticCard(
                    title = stringResource(R.string.stats_avg_healing_time),
                    value = "${statistics.averageHealingTime.toInt()} days",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FilterDialog(
    currentFilter: WoundFilter,
    onFilterApplied: (WoundFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf(currentFilter) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isStartDate by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_wounds)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date Range
                Text(stringResource(R.string.filter_date_range))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            isStartDate = true
                            showDatePicker = true
                        }
                    ) {
                        Text(filter.startDate?.toString() ?: "Start Date")
                    }
                    TextButton(
                        onClick = {
                            isStartDate = false
                            showDatePicker = true
                        }
                    ) {
                        Text(filter.endDate?.toString() ?: "End Date")
                    }
                }

                // Other filters can be added here
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onFilterApplied(filter) }
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                filter = if (isStartDate) {
                    filter.copy(startDate = date)
                } else {
                    filter.copy(endDate = date)
                }
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun SortDialog(
    currentSortType: WoundSortType,
    onSortSelected: (WoundSortType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_wounds)) },
        text = {
            Column {
                WoundSortType.values().forEach { sortType ->
                    ListItem(
                        headlineContent = { Text(sortType.toDisplayString()) },
                        leadingContent = {
                            RadioButton(
                                selected = sortType == currentSortType,
                                onClick = { onSortSelected(sortType) }
                            )
                        },
                        modifier = Modifier.clickable { onSortSelected(sortType) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
