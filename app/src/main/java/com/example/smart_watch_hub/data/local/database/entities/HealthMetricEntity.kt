package com.example.smart_watch_hub.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_metrics",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["azure_sync_status", "azure_retry_count"]),
        Index(value = ["azure_sync_status", "azure_last_attempt"])
    ]
)
data class HealthMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timestamp: Long,
    val heartRate: Int,
    val steps: Int,
    val calories: Int,
    val distance: Int,
    val bloodPressureSystolic: Int,
    val bloodPressureDiastolic: Int,
    val bloodOxygen: Int,
    val temperature: Float,
    val bloodSugar: Float,
    val totalSleep: Int,
    val deepSleep: Int,
    val lightSleep: Int,
    val stress: Int,
    val met: Float,
    val mai: Int,
    val isWearing: Boolean,

    // Sync tracking
    val syncedToHealthConnect: Boolean = false,
    val syncTimestamp: Long? = null,

    // Azure sync tracking
    @ColumnInfo(name = "azure_sync_status")
    val azureSyncStatus: String = "PENDING",

    @ColumnInfo(name = "azure_sync_timestamp")
    val azureSyncTimestamp: Long? = null,

    @ColumnInfo(name = "azure_retry_count")
    val azureRetryCount: Int = 0,

    @ColumnInfo(name = "azure_last_error")
    val azureLastError: String? = null,

    @ColumnInfo(name = "azure_last_attempt")
    val azureLastAttempt: Long? = null,

    @ColumnInfo(name = "device_mac")
    val deviceMac: String? = null
)
