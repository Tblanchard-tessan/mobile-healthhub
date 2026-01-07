package com.example.smart_watch_hub.ui.screens.livedata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smart_watch_hub.data.models.ConnectionState
import com.example.smart_watch_hub.data.models.DeviceInfo
import com.example.smart_watch_hub.data.models.RealTimeHealthData
import com.example.smart_watch_hub.ui.screens.livedata.AzureSyncData
import com.example.smart_watch_hub.ui.theme.Spacing
import com.example.smart_watch_hub.ui.theme.CustomShapes
import com.example.smart_watch_hub.ui.theme.HeartRateRed
import com.example.smart_watch_hub.ui.theme.OxygenBlue
import com.example.smart_watch_hub.ui.theme.StepsGreen
import com.example.smart_watch_hub.ui.theme.CaloriesOrange
import com.example.smart_watch_hub.ui.theme.BPSystolicBlue
import com.example.smart_watch_hub.ui.theme.BPDiastolicGreen
import com.example.smart_watch_hub.ui.animations.SlideUpFadeIn
import com.example.smart_watch_hub.ui.animations.AnimatedNumber

/**
 * Live Data Screen for real-time health metrics display.
 *
 * Features:
 * - Connection status indicator
 * - Real-time metrics display (HR, steps, BP, SpO2, etc)
 * - 5-minute aggregation progress bar
 * - Last aggregated metric display
 * - Health Connect sync status
 * - Manual aggregation trigger
 * - Metrics update as data arrives from watch
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDataScreen(
    viewModel: LiveDataViewModel = viewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val currentMetrics by viewModel.currentMetrics.collectAsState()
    val azureSyncData by viewModel.azureSyncData.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Data") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.mediumLarge)
        ) {
            // Connection Status Card
            SlideUpFadeIn(
                visible = true,
                content = {
                    ConnectionStatusCard(
                        connectionState = connectionState,
                        device = connectedDevice
                    )
                }
            )

            // Debug Buttons (for testing data extraction)
            if (connectionState == ConnectionState.CONNECTED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.requestDeviceVersion() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Get Version")
                    }
                    Button(
                        onClick = { viewModel.requestHealthData() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Get Data")
                    }
                }
            }

            // Current Metrics
            if (currentMetrics != null && connectionState == ConnectionState.CONNECTED) {
                MetricsGridCard(
                    metrics = currentMetrics!!
                )
            } else if (connectionState != ConnectionState.CONNECTED) {
                DisconnectedPlaceholder()
            }

            // Azure Sync Data Preview
            if (azureSyncData != null && connectionState == ConnectionState.CONNECTED) {
                AzureSyncDataCard(
                    data = azureSyncData!!,
                    onSyncClick = { viewModel.triggerAzureSync() }
                )
            }

            // Sync Status
            when (syncStatus) {
                is LiveDataViewModel.SyncStatus.Syncing -> {
                    SyncingCard()
                }
                is LiveDataViewModel.SyncStatus.Success -> {
                    SyncSuccessCard(
                        onDismiss = { viewModel.clearSyncStatus() }
                    )
                }
                is LiveDataViewModel.SyncStatus.Failed -> {
                    SyncFailedCard(
                        error = (syncStatus as LiveDataViewModel.SyncStatus.Failed).error,
                        onDismiss = { viewModel.clearSyncStatus() }
                    )
                }
                else -> {}
            }

            // Error Message
            if (errorMessage != null) {
                ErrorCard(
                    message = errorMessage!!,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }
}

/**
 * Card showing connection status and device info.
 */
@Composable
private fun ConnectionStatusCard(
    connectionState: ConnectionState,
    device: DeviceInfo?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                ConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (connectionState) {
                    ConnectionState.CONNECTED -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Connected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Connected",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    ConnectionState.CONNECTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            "Connecting...",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    ConnectionState.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Connection Error",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        Text(
                            "Disconnected",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (device != null && connectionState == ConnectionState.CONNECTED) {
                Text(
                    "Device: ${device.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "MAC: ${device.macAddress}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Grid of metric cards showing current real-time values.
 */
@Composable
private fun MetricsGridCard(metrics: RealTimeHealthData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Real-Time Metrics",
                style = MaterialTheme.typography.titleMedium
            )

            // First row: HR, Steps, Calories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "HR",
                    value = metrics.heartRate.toString(),
                    unit = "bpm",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Steps",
                    value = metrics.steps.toString(),
                    unit = "",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Cal",
                    value = metrics.calories.toString(),
                    unit = "kcal",
                    modifier = Modifier.weight(1f)
                )
            }

            // Second row: BP, SpO2, Temp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "BP",
                    value = "${metrics.bloodPressureSystolic}/${metrics.bloodPressureDiastolic}",
                    unit = "mmHg",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "SpO₂",
                    value = metrics.bloodOxygen.toString(),
                    unit = "%",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Temp",
                    value = String.format("%.1f", metrics.temperature),
                    unit = "°C",
                    modifier = Modifier.weight(1f)
                )
            }

            // Third row: Distance, Stress, Wearing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "Dist",
                    value = (metrics.distance / 1000f).toInt().toString(),
                    unit = "km",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Stress",
                    value = metrics.stress.toString(),
                    unit = "",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Wearing",
                    value = if (metrics.isWearing) "Yes" else "No",
                    unit = "",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Small metric display chip with semantic colors.
 */
@Composable
private fun MetricChip(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    // Determine semantic color based on metric type
    val (backgroundColor, textColor) = when (label) {
        "HR" -> Pair(HeartRateRed.copy(alpha = 0.15f), HeartRateRed)
        "SpO₂" -> Pair(OxygenBlue.copy(alpha = 0.15f), OxygenBlue)
        "Steps" -> Pair(StepsGreen.copy(alpha = 0.15f), StepsGreen)
        "Cal" -> Pair(CaloriesOrange.copy(alpha = 0.15f), CaloriesOrange)
        "BP" -> Pair(BPSystolicBlue.copy(alpha = 0.15f), BPSystolicBlue)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = CustomShapes.MetricCard,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = textColor
            )
            if (unit.isNotEmpty()) {
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Card showing Azure sync data that will be sent.
 */
@Composable
private fun AzureSyncDataCard(
    data: AzureSyncData,
    onSyncClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Azure Sync Data",
                style = MaterialTheme.typography.titleMedium
            )

            // Device and timestamp info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Device:", style = MaterialTheme.typography.labelSmall)
                    Text(
                        data.deviceMac,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Data values
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DataValueRow("HR", data.heartRate?.toString() ?: "—", "bpm")
                DataValueRow("Steps", data.steps?.toString() ?: "—", "")
                DataValueRow("BP", data.bloodPressure ?: "—", "mmHg")
                DataValueRow("SpO₂", data.bloodOxygen?.toString() ?: "—", "%")
                DataValueRow("Temp", data.temperature ?: "—", "°C")
                DataValueRow("Calories", data.calories?.toString() ?: "—", "kcal")
                DataValueRow("Distance", data.distance?.toString() ?: "—", "km")
                DataValueRow("Wearing", if (data.isWearing) "Yes" else "No", "")
            }

            // Sync button
            Button(
                onClick = onSyncClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync to Azure",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Sync Now")
            }
        }
    }
}

/**
 * Single data value row for sync data.
 */
@Composable
private fun DataValueRow(label: String, value: String, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (unit.isNotEmpty()) {
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Syncing indicator.
 */
@Composable
private fun SyncingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            Text("Syncing to Azure...", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Sync success card.
 */
@Composable
private fun SyncSuccessCard(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text(
                "Synced to Azure",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Dismiss",
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

/**
 * Sync failed card.
 */
@Composable
private fun SyncFailedCard(error: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Sync Failed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Dismiss")
            }
        }
    }
}

/**
 * Error card for general errors.
 */
@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Dismiss")
            }
        }
    }
}

/**
 * Placeholder for disconnected state.
 */
@Composable
private fun DisconnectedPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "No Watch Connected",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Connect to a watch to see real-time metrics.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
