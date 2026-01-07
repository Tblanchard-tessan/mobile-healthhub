package com.example.smart_watch_hub.ui.screens.scan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clj.fastble.data.BleDevice
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.smart_watch_hub.ui.theme.Spacing
import com.example.smart_watch_hub.ui.theme.CustomShapes
import com.example.smart_watch_hub.ui.animations.SlideUpFadeIn
import com.example.smart_watch_hub.ui.animations.StaggeredCardAnimation

/**
 * Scan Screen for BLE device discovery.
 *
 * Features:
 * - Start/stop device scanning
 * - Display discovered devices with RSSI filtering
 * - Connect to selected device
 * - Show connection status and errors
 * - Auto-start scanning on screen load
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    onDeviceConnected: () -> Unit = {},
    viewModel: ScanViewModel = viewModel()
) {
    val scanState by viewModel.scanState.collectAsState()
    val devices by viewModel.scannedDevices.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    val showPermissionDialog = remember { mutableStateOf(false) }

    // Request Bluetooth permissions
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // Auto-request permissions and start scanning when screen appears (only if not already connected)
    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            showPermissionDialog.value = true
            permissionState.launchMultiplePermissionRequest()
        } else if (scanState != ScanViewModel.ScanState.Connected) {
            viewModel.startScan()
        }
    }

    // Auto-start scanning when permissions are granted (only if not already connected)
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted &&
            scanState == ScanViewModel.ScanState.Idle &&
            scanState != ScanViewModel.ScanState.Connected) {
            viewModel.startScan()
        }
    }

    // Show permission dialog if permissions are denied
    if (showPermissionDialog.value && !permissionState.allPermissionsGranted) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog.value = false },
            title = { Text("Bluetooth Permissions Required") },
            text = {
                Text(
                    "This app needs Bluetooth scan, connect, and location permissions to " +
                    "discover and connect to your Starmax watch. Please grant these permissions to continue."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("Grant Permissions")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog.value = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan for Watch") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    when (scanState) {
                        is ScanViewModel.ScanState.Scanning -> {
                            IconButton(onClick = { viewModel.stopScan() }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Stop scanning",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        else -> {
                            IconButton(
                                onClick = {
                                    if (permissionState.allPermissionsGranted) {
                                        viewModel.startScan()
                                    } else {
                                        showPermissionDialog.value = true
                                        permissionState.launchMultiplePermissionRequest()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Start scanning",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.mediumLarge)
        ) {
            // Status indicator
            SlideUpFadeIn(
                visible = true,
                content = {
                    ScanStatusCard(scanState)
                }
            )

            // Error message
            if (connectionError != null) {
                ErrorCard(
                    message = connectionError!!,
                    onDismiss = { viewModel.clearError() },
                    onRetry = { viewModel.retryConnection() }
                )
            }

            // Device list or empty state
            if (devices.isEmpty() && scanState == ScanViewModel.ScanState.Scanning) {
                ScanningPlaceholder()
            } else if (devices.isEmpty()) {
                EmptyStateCard(
                    onStartScan = {
                        if (permissionState.allPermissionsGranted) {
                            viewModel.startScan()
                        } else {
                            showPermissionDialog.value = true
                            permissionState.launchMultiplePermissionRequest()
                        }
                    }
                )
            } else {
                DeviceListHeader(deviceCount = devices.size)
                DeviceList(
                    devices = devices,
                    onDeviceClick = { device ->
                        viewModel.connectToDevice(device)
                        onDeviceConnected()
                    }
                )
            }
        }
    }
}

/**
 * Card showing scan status.
 */
@Composable
private fun ScanStatusCard(scanState: ScanViewModel.ScanState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = CustomShapes.StatusCard,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.mediumLarge),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (scanState) {
                is ScanViewModel.ScanState.Idle -> {
                    Text("Ready to scan", style = MaterialTheme.typography.bodyMedium)
                }
                is ScanViewModel.ScanState.Scanning -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.CenterVertically)
                    )
                    Text("Scanning for devices...", style = MaterialTheme.typography.bodyMedium)
                }
                is ScanViewModel.ScanState.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.CenterVertically)
                    )
                    Text("Connecting to device...", style = MaterialTheme.typography.bodyMedium)
                }
                is ScanViewModel.ScanState.Connected -> {
                    Text(
                        "✓ Connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is ScanViewModel.ScanState.Error -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        scanState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Card showing error message with retry/dismiss options.
 */
@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = CustomShapes.StatusCard,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retry")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}

/**
 * Placeholder shown while scanning.
 */
@Composable
private fun ScanningPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            "Searching for Starmax watches...",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Empty state card shown when no devices found.
 */
@Composable
private fun EmptyStateCard(onStartScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "No watches found",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            "Make sure your watch is powered on and nearby.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onStartScan) {
            Text("Scan Again")
        }
    }
}

/**
 * Header showing device count.
 */
@Composable
private fun DeviceListHeader(deviceCount: Int) {
    Text(
        "Found $deviceCount device${if (deviceCount != 1) "s" else ""}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * List of discovered devices.
 */
@Composable
private fun DeviceList(
    devices: List<BleDevice>,
    onDeviceClick: (BleDevice) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        items(devices.size) { index ->
            StaggeredCardAnimation(
                index = index,
                visible = true,
                content = {
                    DeviceCard(device = devices[index], onClick = { onDeviceClick(devices[index]) })
                }
            )
        }
    }
}

/**
 * Card showing device information.
 */
@Composable
private fun DeviceCard(device: BleDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = CustomShapes.DeviceCard,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Text(
                text = device.name ?: "Unknown Device",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                Text(
                    text = "MAC: ${device.mac}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "RSSI: ${device.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
