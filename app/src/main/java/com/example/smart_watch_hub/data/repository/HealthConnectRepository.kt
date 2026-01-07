package com.example.smart_watch_hub.data.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.smart_watch_hub.data.local.database.entities.SyncLogEntity
import com.example.smart_watch_hub.data.models.HealthMetric
import com.example.smart_watch_hub.utils.HealthConnectDataMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Repository for Health Connect integration.
 *
 * Handles:
 * - Bidirectional data synchronization (watch ↔ Health Connect)
 * - Permission management
 * - Reading records from Health Connect
 * - Writing records to Health Connect
 * - Conflict resolution and deduplication
 */
class HealthConnectRepository(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    // Required permissions for Health Connect integration
    private val permissions = setOf(
        // Read permissions
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.BloodPressureRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.DistanceRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.SleepSessionRecord::class),

        // Write permissions
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(androidx.health.connect.client.records.BloodPressureRecord::class),
        HealthPermission.getWritePermission(androidx.health.connect.client.records.OxygenSaturationRecord::class),
        HealthPermission.getWritePermission(androidx.health.connect.client.records.BodyTemperatureRecord::class),
        HealthPermission.getWritePermission(androidx.health.connect.client.records.DistanceRecord::class),
        HealthPermission.getWritePermission(androidx.health.connect.client.records.TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(androidx.health.connect.client.records.SleepSessionRecord::class)
    )

    /**
     * Check if Health Connect is available on this device.
     */
    fun isHealthConnectAvailable(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get the list of Health Connect permissions required.
     */
    fun getRequiredPermissions(): Set<String> = permissions

    /**
     * Check if all required permissions are granted.
     *
     * @return True if all permissions granted, false otherwise
     */
    suspend fun hasAllPermissions(): Boolean {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            grantedPermissions.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Write health metrics to Health Connect.
     * Called after successful 5-minute aggregation.
     *
     * @param metrics List of health metrics to write
     * @return True if successful, false otherwise
     */
    suspend fun writeHealthMetrics(metrics: List<HealthMetric>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasAllPermissions()) {
                    return@withContext false
                }

                val records = mutableListOf<Record>()
                for (metric in metrics) {
                    records.addAll(HealthConnectDataMapper.toHealthConnectRecords(metric))
                }

                if (records.isEmpty()) {
                    return@withContext false
                }

                healthConnectClient.insertRecords(records)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Read heart rate records from Health Connect for a time range.
     *
     * @param startTime Start of time range (milliseconds)
     * @param endTime End of time range (milliseconds)
     * @return List of heart rate records from Health Connect
     */
    suspend fun readHeartRateRecords(startTime: Long, endTime: Long): List<HeartRateRecord> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasAllPermissions()) {
                    return@withContext emptyList()
                }

                val request = ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.ofEpochMilli(startTime),
                        Instant.ofEpochMilli(endTime)
                    ),
                    ascendingOrder = false
                )

                val response = healthConnectClient.readRecords(request)
                response.records as List<HeartRateRecord>
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Read steps records from Health Connect for a time range.
     *
     * @param startTime Start of time range (milliseconds)
     * @param endTime End of time range (milliseconds)
     * @return List of steps records from Health Connect
     */
    suspend fun readStepsRecords(startTime: Long, endTime: Long): List<StepsRecord> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasAllPermissions()) {
                    return@withContext emptyList()
                }

                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.ofEpochMilli(startTime),
                        Instant.ofEpochMilli(endTime)
                    ),
                    ascendingOrder = false
                )

                val response = healthConnectClient.readRecords(request)
                response.records as List<StepsRecord>
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Read all records from Health Connect for a time range.
     * Used to sync Health Connect data back to local database.
     *
     * @param startTime Start of time range (milliseconds)
     * @param endTime End of time range (milliseconds)
     * @return List of all available records from Health Connect
     */
    suspend fun readAllRecords(startTime: Long, endTime: Long): List<Record> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasAllPermissions()) {
                    return@withContext emptyList()
                }

                val allRecords = mutableListOf<Record>()
                val timeRange = TimeRangeFilter.between(
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(endTime)
                )

                // Read each record type
                val recordTypes = listOf(
                    HeartRateRecord::class,
                    StepsRecord::class,
                    androidx.health.connect.client.records.BloodPressureRecord::class,
                    androidx.health.connect.client.records.OxygenSaturationRecord::class,
                    androidx.health.connect.client.records.BodyTemperatureRecord::class,
                    androidx.health.connect.client.records.DistanceRecord::class,
                    androidx.health.connect.client.records.TotalCaloriesBurnedRecord::class,
                    androidx.health.connect.client.records.SleepSessionRecord::class
                )

                for (recordType in recordTypes) {
                    try {
                        val request = ReadRecordsRequest(
                            recordType = recordType as KClass<Record>,
                            timeRangeFilter = timeRange,
                            ascendingOrder = false
                        )
                        val response = healthConnectClient.readRecords(request)
                        allRecords.addAll(response.records)
                    } catch (e: Exception) {
                        // Continue if one record type fails
                    }
                }

                allRecords
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Create sync log entry for Health Connect operation.
     *
     * @param direction "TO_HC" (watch to Health Connect) or "FROM_HC" (Health Connect to app)
     * @param recordCount Number of records synced
     * @param success True if sync successful
     * @param errorMessage Error message if sync failed
     */
    fun createSyncLog(
        direction: String,
        recordCount: Int,
        success: Boolean,
        errorMessage: String? = null
    ): SyncLogEntity {
        return SyncLogEntity(
            timestamp = System.currentTimeMillis(),
            direction = direction,
            recordCount = recordCount,
            success = success,
            errorMessage = errorMessage
        )
    }

    /**
     * Request Health Connect permissions (for UI integration).
     * This should be called by PermissionHandler component.
     */
    fun getPermissionIntentForHealthConnect(): android.content.Intent? {
        return try {
            // This would be handled by PermissionsRationaleActivity
            // Returns the intent to launch Health Connect permission request
            null
        } catch (e: Exception) {
            null
        }
    }
}
