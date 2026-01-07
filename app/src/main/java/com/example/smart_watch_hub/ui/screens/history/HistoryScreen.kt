package com.example.smart_watch_hub.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smart_watch_hub.ui.components.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import kotlin.OptIn
import com.example.smart_watch_hub.domain.sync.workers.HealthSyncWorker
import com.example.smart_watch_hub.ui.screens.history.components.BloodPressureChart
import com.example.smart_watch_hub.ui.screens.history.components.CaloriesChart
import com.example.smart_watch_hub.ui.screens.history.components.HeartRateChart
import com.example.smart_watch_hub.ui.screens.history.components.StepsChart
import com.example.smart_watch_hub.ui.screens.history.components.TimeRangeSelector
import com.example.smart_watch_hub.ui.theme.Spacing
import com.example.smart_watch_hub.ui.theme.CustomShapes
import com.example.smart_watch_hub.ui.animations.SlideUpFadeIn

/**
 * History Screen for viewing historical health data and charts.
 *
 * Features:
 * - Time range selector (6h, 1d, 1w)
 * - Heart rate chart with trends
 * - Blood pressure chart (systolic/diastolic)
 * - Steps chart with daily totals
 * - Calories chart
 * - Overall statistics (avg HR, total steps, etc)
 * - Data from both local database and Health Connect (merged)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel()
) {
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val historicalMetrics by viewModel.historicalMetrics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val heartRateData by viewModel.heartRateData.collectAsState()
    val stepsData by viewModel.stepsData.collectAsState()
    val bloodPressureData by viewModel.bloodPressureData.collectAsState()
    val caloriesData by viewModel.caloriesData.collectAsState()
    val statistics by viewModel.statistics.collectAsState()

    val context = LocalContext.current
    var isSyncing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health History", color = Color.White) },
                actions = {
                    // Temporary sync button
                    IconButton(
                        onClick = {
                            isSyncing = true
                            HealthSyncWorker.syncNow(context)
                        },
                        enabled = !isSyncing
                    ) {
                        Icon(
                            Icons.Filled.CloudUpload,
                            contentDescription = "Sync to Backend",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            LoadingIndicator(message = "Loading historical data...")
        } else if (historicalMetrics.isEmpty() && heartRateData.isEmpty() && stepsData.isEmpty() && bloodPressureData.isEmpty()) {
            EmptyStateHistoryScreen()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.mediumLarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.mediumLarge)
            ) {
                // Time Range Selector
                TimeRangeSelector(
                    selectedRange = selectedTimeRange,
                    onRangeSelected = { viewModel.setTimeRange(it) }
                )

                // Statistics Overview
                if (statistics != null) {
                    SlideUpFadeIn(
                        visible = true,
                        content = {
                            StatisticsCard(statistics!!)
                        }
                    )
                }

                // Error Message
                if (errorMessage != null) {
                    ErrorMessageCard(
                        message = errorMessage!!,
                        onDismiss = { viewModel.clearError() }
                    )
                }

                // Charts - show for all time ranges with raw data
                // Heart Rate Chart
                if (heartRateData.isNotEmpty()) {
                    HeartRateChart(data = heartRateData)
                }

                // Blood Pressure Chart
                if (bloodPressureData.isNotEmpty()) {
                    BloodPressureChart(data = bloodPressureData)
                }

                // Steps Chart
                if (stepsData.isNotEmpty()) {
                    StepsChart(data = stepsData)
                }

                // Calories Chart
                if (caloriesData.isNotEmpty()) {
                    CaloriesChart(data = caloriesData)
                }
            }
        }
    }
}

/**
 * Statistics card showing aggregate metrics.
 */
@Composable
private fun StatisticsCard(
    stats: HistoryViewModel.HealthStatistics
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Statistics",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            StatisticRow("Avg Heart Rate", "${stats.avgHeartRate} bpm")
            StatisticRow("Total Steps", "${stats.totalSteps}")
            StatisticRow("Total Calories", "${stats.totalCalories} kcal")
            StatisticRow("Total Distance", "${stats.totalDistance / 1000} km")
            StatisticRow("Avg Blood Oxygen", "${stats.avgBloodOxygen}%")
            StatisticRow("Data Points", "${stats.dataPointCount}")
        }
    }
}

/**
 * Single statistic row.
 */
@Composable
private fun StatisticRow(
    label: String,
    value: String
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

/**
 * Error message card.
 */
@Composable
private fun ErrorMessageCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            androidx.compose.material3.Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Dismiss")
            }
        }
    }
}

/**
 * Empty state for when no data is available.
 */
@Composable
private fun EmptyStateHistoryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "No Data Available",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = Color.White
        )
        Text(
            "Connect your watch and collect data to see charts.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
