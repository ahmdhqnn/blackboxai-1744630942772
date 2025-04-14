package com.example.woundscanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.woundscanner.R
import com.example.woundscanner.data.WoundEntity
import com.example.woundscanner.ui.components.*
import com.example.woundscanner.viewmodels.HomeStatistics
import com.example.woundscanner.viewmodels.HomeViewModel
import com.example.woundscanner.viewmodels.HealingProgressItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalysis: (String) -> Unit,
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = HomeViewModel.provideFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val statistics by viewModel.getStatistics().collectAsState(initial = HomeStatistics())
    val healingProgress by viewModel.getHealingProgress().collectAsState(initial = emptyList())
    val upcomingFollowUps by viewModel.getUpcomingFollowUps().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            WoundScannerTopBar(
                title = stringResource(R.string.app_name)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAnalysis("") }
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = stringResource(R.string.start_scanning)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Welcome Section
            WelcomeSection()

            // Statistics Section
            StatisticsSection(statistics)

            // Recent Scans Section
            RecentScansSection(
                recentScans = uiState.recentScans,
                onScanClick = { scan -> onNavigateToAnalysis(scan.imageUri) },
                onViewAllClick = onNavigateToHistory
            )

            // Healing Progress Section
            HealingProgressSection(healingProgress)

            // Follow-ups Section
            FollowUpsSection(upcomingFollowUps)
        }
    }
}

@Composable
private fun WelcomeSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatisticsSection(statistics: HomeStatistics) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatisticCard(
                title = stringResource(R.string.total_scans),
                value = statistics.totalScans.toString(),
                icon = Icons.Default.PhotoCamera,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatisticCard(
                title = stringResource(R.string.active_wounds),
                value = statistics.activeWounds.toString(),
                icon = Icons.Default.Healing,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatisticCard(
                title = stringResource(R.string.healed_wounds),
                value = statistics.healedWounds.toString(),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RecentScansSection(
    recentScans: List<WoundEntity>,
    onScanClick: (WoundEntity) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_scans),
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onViewAllClick) {
                Text(stringResource(R.string.view_all_history))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (recentScans.isEmpty()) {
            EmptyStateMessage(
                icon = Icons.Default.History,
                title = stringResource(R.string.no_recent_scans),
                message = stringResource(R.string.start_scanning)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentScans) { scan ->
                    RecentScanCard(
                        scan = scan,
                        onClick = { onScanClick(scan) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentScanCard(
    scan: WoundEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = scan.type,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(scan.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            WoundSeverityChip(severity = scan.severity)
            Spacer(modifier = Modifier.height(8.dp))
            HealingProgressBar(progress = scan.healingProgress)
        }
    }
}

@Composable
private fun HealingProgressSection(healingProgress: List<HealingProgressItem>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.healing_progress),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        healingProgress.forEach { item ->
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.type,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${(item.healedPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                HealingProgressBar(progress = item.averageProgress)
            }
        }
    }
}

@Composable
private fun FollowUpsSection(followUps: List<WoundEntity>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.upcoming_followups),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (followUps.isEmpty()) {
            Text(
                text = "No upcoming follow-ups",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            followUps.forEach { wound ->
                ListItem(
                    headlineContent = { Text(wound.type) },
                    supportingContent = {
                        Text(
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(wound.followUpDate ?: wound.date)
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        WoundSeverityChip(severity = wound.severity)
                    }
                )
                Divider()
            }
        }
    }
}
