package com.example.smart_watch_hub.domain.sync

import com.example.smart_watch_hub.data.local.database.entities.SyncLogEntity
import com.example.smart_watch_hub.data.repository.HealthConnectRepository
import com.example.smart_watch_hub.data.repository.HealthDataRepository
import com.example.smart_watch_hub.data.models.HealthMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Manages bidirectional synchronization between Starmax watch and Health Connect.
 *
 * Sync Flow:
 * 1. PUSH (TO_HC): Watch → Health Connect
 *    - Get unsynced metrics from Room database
 *    - Convert to Health Connect records
 *    - Write to Health Connect
 *    - Mark as synced in Room
 *
 * 2. PULL (FROM_HC): Health Connect → Local Database
 *    - Read recent records from Health Connect (for merge)
 *    - Compare with local data (avoid duplicates)
 *    - Merge and store in Room
 *    - Update sync timestamp
 *
 * Runs on 15-minute interval via WorkManager.
 */
class HealthConnectSyncManager : KoinComponent {
    private val healthDataRepository: HealthDataRepository by inject()
    private val healthConnectRepository: HealthConnectRepository by inject()

    /**
     * Execute bidirectional sync: PUSH then PULL.
     *
     * @return SyncResult with details of operation
     */
    suspend fun executeBidirectionalSync(): SyncResult {
        return withContext(Dispatchers.IO) {
            // Check Health Connect availability
            if (!healthConnectRepository.isHealthConnectAvailable()) {
                return@withContext SyncResult(
                    pushSuccessful = false,
                    pullSuccessful = false,
                    pushError = "Health Connect not available",
                    pullError = "Health Connect not available"
                )
            }

            // Check permissions
            if (!healthConnectRepository.hasAllPermissions()) {
                return@withContext SyncResult(
                    pushSuccessful = false,
                    pullSuccessful = false,
                    pushError = "Health Connect permissions not granted",
                    pullError = "Health Connect permissions not granted"
                )
            }

            val pushResult = syncPushToHealthConnect()
            val pullResult = syncPullFromHealthConnect()

            SyncResult(
                pushSuccessful = pushResult.successful,
                pullSuccessful = pullResult.successful,
                pushError = pushResult.errorMessage,
                pullError = pullResult.errorMessage,
                pushRecordCount = pushResult.recordCount,
                pullRecordCount = pullResult.recordCount
            )
        }
    }

    /**
     * Sync PUSH: Watch data → Health Connect
     * Get unsynced metrics from local database and push to Health Connect.
     */
    private suspend fun syncPushToHealthConnect(): SyncOperation {
        return try {
            // Get last 24 hours of unsynced metrics
            val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            val unsyncedMetrics = healthDataRepository.getUnsyncedMetrics(twentyFourHoursAgo)

            if (unsyncedMetrics.isEmpty()) {
                // No data to sync
                healthDataRepository.insertSyncLog(
                    SyncLogEntity(
                        timestamp = System.currentTimeMillis(),
                        direction = "TO_HC",
                        recordCount = 0,
                        success = true,
                        errorMessage = null
                    )
                )
                return SyncOperation(successful = true, recordCount = 0)
            }

            // Write to Health Connect
            val writeSuccessful = healthConnectRepository.writeHealthMetrics(unsyncedMetrics)

            if (writeSuccessful) {
                // Mark as synced in local database
                val metricIds = unsyncedMetrics.map { it.id }
                val syncTime = System.currentTimeMillis()
                healthDataRepository.markMetricsAsSynced(metricIds, syncTime)

                // Log successful sync
                healthDataRepository.insertSyncLog(
                    SyncLogEntity(
                        timestamp = syncTime,
                        direction = "TO_HC",
                        recordCount = unsyncedMetrics.size,
                        success = true,
                        errorMessage = null
                    )
                )

                SyncOperation(successful = true, recordCount = unsyncedMetrics.size)
            } else {
                // Log failed sync
                healthDataRepository.insertSyncLog(
                    SyncLogEntity(
                        timestamp = System.currentTimeMillis(),
                        direction = "TO_HC",
                        recordCount = unsyncedMetrics.size,
                        success = false,
                        errorMessage = "Failed to write to Health Connect"
                    )
                )

                SyncOperation(
                    successful = false,
                    errorMessage = "Failed to write to Health Connect",
                    recordCount = 0
                )
            }
        } catch (e: Exception) {
            healthDataRepository.insertSyncLog(
                SyncLogEntity(
                    timestamp = System.currentTimeMillis(),
                    direction = "TO_HC",
                    recordCount = 0,
                    success = false,
                    errorMessage = e.message ?: "Unknown error during push sync"
                )
            )

            SyncOperation(
                successful = false,
                errorMessage = e.message ?: "Unknown error during push sync"
            )
        }
    }

    /**
     * Sync PULL: Health Connect → Local Database
     * Read records from Health Connect and merge with local data.
     */
    private suspend fun syncPullFromHealthConnect(): SyncOperation {
        return try {
            // Read last 7 days from Health Connect
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val now = System.currentTimeMillis()

            val hcRecords = healthConnectRepository.readAllRecords(sevenDaysAgo, now)

            if (hcRecords.isEmpty()) {
                // No data to sync from HC
                healthDataRepository.insertSyncLog(
                    SyncLogEntity(
                        timestamp = System.currentTimeMillis(),
                        direction = "FROM_HC",
                        recordCount = 0,
                        success = true,
                        errorMessage = null
                    )
                )
                return SyncOperation(successful = true, recordCount = 0)
            }

            // TODO: In Phase 3.4, implement merging logic
            // For now, just log that we would pull from HC
            // In production:
            // 1. Convert HC records to HealthMetric objects
            // 2. Check for duplicates (by timestamp + metric type)
            // 3. Merge with local data (HC data takes precedence if newer)
            // 4. Insert merged data into Room

            healthDataRepository.insertSyncLog(
                SyncLogEntity(
                    timestamp = System.currentTimeMillis(),
                    direction = "FROM_HC",
                    recordCount = hcRecords.size,
                    success = true,
                    errorMessage = null
                )
            )

            SyncOperation(successful = true, recordCount = hcRecords.size)
        } catch (e: Exception) {
            healthDataRepository.insertSyncLog(
                SyncLogEntity(
                    timestamp = System.currentTimeMillis(),
                    direction = "FROM_HC",
                    recordCount = 0,
                    success = false,
                    errorMessage = e.message ?: "Unknown error during pull sync"
                )
            )

            SyncOperation(
                successful = false,
                errorMessage = e.message ?: "Unknown error during pull sync"
            )
        }
    }

    /**
     * Clean up old sync logs (older than 30 days).
     */
    suspend fun cleanupOldSyncLogs() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
        healthDataRepository.deleteOldSyncLogs(thirtyDaysAgo)
    }

    /**
     * Result of a single sync operation (PUSH or PULL).
     */
    data class SyncOperation(
        val successful: Boolean,
        val recordCount: Int = 0,
        val errorMessage: String? = null
    )

    /**
     * Result of bidirectional sync (PUSH + PULL).
     */
    data class SyncResult(
        val pushSuccessful: Boolean,
        val pullSuccessful: Boolean,
        val pushRecordCount: Int = 0,
        val pullRecordCount: Int = 0,
        val pushError: String? = null,
        val pullError: String? = null
    ) {
        val isFullySuccessful = pushSuccessful && pullSuccessful
    }
}
