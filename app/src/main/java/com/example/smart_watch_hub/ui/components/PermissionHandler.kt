package com.example.smart_watch_hub.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import com.example.smart_watch_hub.data.repository.HealthConnectRepository
import com.example.smart_watch_hub.domain.sync.workers.HealthConnectSyncWorker
import org.koin.compose.koinInject

/**
 * Permission handler component for Health Connect permissions.
 *
 * Displays rationale if permissions not granted and provides UI to request them.
 * Automatically schedules background sync once permissions are granted.
 */
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit = {},
    onPermissionsDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val healthConnectRepository: HealthConnectRepository = koinInject()
    val permissionsState = remember { mutableStateOf<PermissionsState>(PermissionsState.CHECKING) }

    // Check permissions on first composition
    LaunchedEffect(Unit) {
        val hasPermissions = healthConnectRepository.hasAllPermissions()
        val hcAvailable = healthConnectRepository.isHealthConnectAvailable()

        permissionsState.value = when {
            !hcAvailable -> PermissionsState.HC_NOT_AVAILABLE
            hasPermissions -> PermissionsState.GRANTED
            else -> PermissionsState.NOT_GRANTED
        }
    }

    // Launcher for Health Connect permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            permissionsState.value = PermissionsState.GRANTED
            // Schedule background sync
            HealthConnectSyncWorker.schedulePeriodicSync(context)
            onPermissionsGranted()
        } else {
            permissionsState.value = PermissionsState.DENIED_PERMANENTLY
            onPermissionsDenied()
        }
    }

    // Render based on permission state
    when (permissionsState.value) {
        PermissionsState.CHECKING -> {
            // Loading state - don't show anything
        }

        PermissionsState.GRANTED -> {
            // Permissions granted - don't show UI
            LaunchedEffect(Unit) {
                onPermissionsGranted()
            }
        }

        PermissionsState.NOT_GRANTED -> {
            PermissionRationaleCard(
                title = "Health Connect Access",
                description = "To sync your health data, this app needs permission to access Health Connect.",
                onRequestClick = {
                    val permissions = healthConnectRepository.getRequiredPermissions().toTypedArray()
                    permissionLauncher.launch(permissions)
                }
            )
        }

        PermissionsState.DENIED_PERMANENTLY -> {
            PermissionRationaleCard(
                title = "Health Connect Permission Denied",
                description = "Health Connect permissions were denied. You can grant them in app settings.",
                onRequestClick = {
                    // Open app settings
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
                buttonText = "Open Settings"
            )
        }

        PermissionsState.HC_NOT_AVAILABLE -> {
            PermissionRationaleCard(
                title = "Health Connect Not Available",
                description = "Health Connect is not available on this device. Please install it from Google Play Store.",
                onRequestClick = {
                    // Open Google Play Store
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(
                            "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
                        )
                    )
                    context.startActivity(intent)
                },
                buttonText = "Install Health Connect",
                isError = true
            )
        }
    }
}

/**
 * Card component showing permission rationale and request button.
 */
@Composable
private fun PermissionRationaleCard(
    title: String,
    description: String,
    onRequestClick: () -> Unit,
    buttonText: String = "Grant Permission",
    isError: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = onRequestClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text(buttonText)
            }
        }
    }
}

/**
 * Permission state enum.
 */
enum class PermissionsState {
    CHECKING,
    GRANTED,
    NOT_GRANTED,
    DENIED_PERMANENTLY,
    HC_NOT_AVAILABLE
}
