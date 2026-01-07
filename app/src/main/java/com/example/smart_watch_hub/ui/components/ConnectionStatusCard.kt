package com.example.smart_watch_hub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smart_watch_hub.data.models.ConnectionState

/**
 * Reusable connection status card component.
 *
 * Displays:
 * - Current connection state (Connected, Connecting, Disconnected, Error)
 * - Device name and MAC address (if connected)
 * - Status icon and color indication
 *
 * Usage:
 * ConnectionStatusCard(
 *     state = ConnectionState.CONNECTED,
 *     deviceName = "MyWatch",
 *     deviceMac = "AA:BB:CC:DD:EE:FF"
 * )
 */
@Composable
fun ConnectionStatusCard(
    state: ConnectionState,
    deviceName: String? = null,
    deviceMac: String? = null,
    modifier: Modifier = Modifier
) {
    val statusInfo = when (state) {
        ConnectionState.DISCONNECTED -> ConnectionStatusInfo(
            label = "Disconnected",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Default.Close,
            showProgress = false
        )
        ConnectionState.CONNECTING -> ConnectionStatusInfo(
            label = "Connecting...",
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Sync,
            showProgress = true
        )
        ConnectionState.CONNECTED -> ConnectionStatusInfo(
            label = "Connected",
            color = Color(0xFF4CAF50), // Green
            icon = Icons.Default.Check,
            showProgress = false
        )
        ConnectionState.DISCONNECTING -> ConnectionStatusInfo(
            label = "Disconnecting...",
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Sync,
            showProgress = true
        )
        ConnectionState.ERROR -> ConnectionStatusInfo(
            label = "Connection Error",
            color = MaterialTheme.colorScheme.error,
            icon = Icons.Default.Error,
            showProgress = false
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (statusInfo.showProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = statusInfo.color
                    )
                } else {
                    Icon(
                        imageVector = statusInfo.icon,
                        contentDescription = statusInfo.label,
                        tint = statusInfo.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = statusInfo.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = statusInfo.color
                )
            }

            // Device details (if connected)
            if (state == ConnectionState.CONNECTED && deviceName != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp), // Align with text above
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Device: $deviceName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (deviceMac != null) {
                        Text(
                            text = "MAC: $deviceMac",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Internal data class for connection status information.
 */
internal data class ConnectionStatusInfo(
    val label: String,
    val color: Color,
    val icon: ImageVector,
    val showProgress: Boolean
)
