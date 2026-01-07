package com.example.smart_watch_hub.domain.sync

import android.util.Log
import com.example.smart_watch_hub.data.local.database.dao.HealthMetricsDao
import com.example.smart_watch_hub.data.local.database.entities.HealthMetricEntity
import com.example.smart_watch_hub.data.providers.UserIdProvider
import com.example.smart_watch_hub.data.remote.api.HealthApiConfig
import com.example.smart_watch_hub.data.remote.dto.HealthMetricDto
import com.example.smart_watch_hub.data.repository.HealthApiRepository
import com.example.smart_watch_hub.data.repository.UploadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class HealthSyncManager(
    private val healthMetricsDao: HealthMetricsDao,
    private val healthApiRepository: HealthApiRepository,
    private val userIdProvider: UserIdProvider
) {

    suspend fun syncToBackend(batchSize: Int, correlationId: String): SyncResult = withContext(Dispatchers.IO) {
        try {
            var totalSynced = 0
            var totalFailed = 0
            var batchNumber = 0
            val maxBatches = 500  // Safety limit to prevent infinite loops

            // Sync all pending records in one operation (multiple batches if needed)
            while (batchNumber < maxBatches) {
                batchNumber++
                val batchCorrelationId = "$correlationId-batch-$batchNumber"

                // 1. Calculate backoff cutoff (don't retry FAILED records too soon)
                val backoffCutoff = calculateBackoffCutoff()

                // 2. Get unsynced records
                val unsyncedRecords = healthMetricsDao.getAzureUnsyncedMetrics(
                    batchSize = batchSize,
                    maxRetries = HealthApiConfig.MAX_RETRIES,
                    backoffCutoff = backoffCutoff
                )

                if (unsyncedRecords.isEmpty()) {
                    Log.d(TAG, "[$batchCorrelationId] No more records to sync")
                    break
                }

                Log.d(TAG, "[$batchCorrelationId] Syncing batch ${batchNumber}: ${unsyncedRecords.size} records")

                // 3. Filter records with valid MAC ID
                val validRecords = unsyncedRecords.filter { it.deviceMac != null && it.deviceMac.isNotEmpty() }
                val invalidRecords = unsyncedRecords.filter { it.deviceMac == null || it.deviceMac.isEmpty() }

                // Mark invalid records as failed (no MAC ID)
                if (invalidRecords.isNotEmpty()) {
                    val invalidIds = invalidRecords.map { it.id }
                    healthMetricsDao.markAsAzureFailed(
                        ids = invalidIds,
                        errorMessage = "Record skipped: missing device MAC ID",
                        attemptTime = System.currentTimeMillis(),
                        maxRetries = HealthApiConfig.MAX_RETRIES
                    )
                    Log.w(TAG, "[$batchCorrelationId] Skipped ${invalidRecords.size} records without MAC ID")
                    totalFailed += invalidRecords.size
                }

                // If no valid records, continue to next batch
                if (validRecords.isEmpty()) {
                    continue
                }

                // 4. Mark valid records as SYNCING
                val recordIds = validRecords.map { it.id }
                healthMetricsDao.markAsAzureSyncing(recordIds, System.currentTimeMillis())

                // 5. Transform to DTOs (only valid records with MAC ID)
                val userId = userIdProvider.getUserId()
                val dtos = validRecords.map { entity ->
                    entity.toDto(userId)
                }

                // 6. Upload to backend
                val uploadResult = healthApiRepository.uploadHealthMetrics(dtos, batchCorrelationId)

                // 6. Update status
                val currentTime = System.currentTimeMillis()
                when (uploadResult) {
                    is UploadResult.Success -> {
                        healthMetricsDao.markAsAzureSynced(recordIds, currentTime)
                        Log.d(TAG, "[$batchCorrelationId] Successfully synced ${recordIds.size} records")
                        totalSynced += recordIds.size
                    }
                    is UploadResult.Error -> {
                        healthMetricsDao.markAsAzureFailed(
                            ids = recordIds,
                            errorMessage = uploadResult.message.take(500),
                            attemptTime = currentTime,
                            maxRetries = HealthApiConfig.MAX_RETRIES
                        )
                        Log.w(TAG, "[$batchCorrelationId] Sync failed: ${uploadResult.message}")
                        totalFailed += recordIds.size

                        // If upload failed, stop trying further batches
                        if (!uploadResult.isRetryable) {
                            Log.e(TAG, "[$batchCorrelationId] Non-retryable error, stopping batch sync")
                            break
                        }
                    }
                }
            }

            Log.i(TAG, "[$correlationId] Sync complete: $totalSynced synced, $totalFailed failed across $batchNumber batches")

            SyncResult(
                isSuccess = totalFailed == 0,
                isRetryable = totalFailed > 0,
                syncedCount = totalSynced,
                failedCount = totalFailed,
                errorMessage = if (totalFailed > 0) "Failed to sync $totalFailed records across $batchNumber batches" else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "[$correlationId] Unexpected error during sync", e)
            SyncResult(
                isSuccess = false,
                isRetryable = true,
                syncedCount = 0,
                failedCount = 0,
                errorMessage = e.message
            )
        }
    }

    private fun calculateBackoffCutoff(): Long {
        // Don't retry records that failed less than 30 seconds ago
        return System.currentTimeMillis() - HealthApiConfig.INITIAL_BACKOFF_MS
    }

    private fun HealthMetricEntity.toDto(userId: String): HealthMetricDto {
        return HealthMetricDto(
            userId = userId,
            deviceId = deviceMac!!,
            timestamp = timestamp,
            heartRate = heartRate.takeIf { it != 0 },
            bpSystolic = bloodPressureSystolic.takeIf { it != 0 },
            bpDiastolic = bloodPressureDiastolic.takeIf { it != 0 },
            spO2 = bloodOxygen.takeIf { it != 0 },
            steps = steps.takeIf { it != 0 },
            calories = calories.takeIf { it != 0 },
            distance = distance.takeIf { it != 0 },
            temperature = temperature.takeIf { it != 0f },
            bloodGlucose = bloodSugar.takeIf { it != 0f },
            totalSleep = totalSleep.takeIf { it != 0 },
            deepSleep = deepSleep.takeIf { it != 0 },
            lightSleep = lightSleep.takeIf { it != 0 },
            stress = stress.takeIf { it != 0 },
            met = met.takeIf { it != 0f },
            mai = mai.takeIf { it != 0 },
            isWearing = isWearing,
            recordHash = generateRecordHash(this)
        )
    }

    private fun generateRecordHash(entity: HealthMetricEntity): String {
        val data = "${entity.timestamp}${entity.heartRate}${entity.steps}${entity.deviceMac}"
        return MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    /**
     * Manually trigger sync of all pending metrics to Azure backend.
     * Used for manual data push without waiting for scheduled sync.
     */
    suspend fun manualSync(): SyncResult {
        val correlationId = "manual-${System.currentTimeMillis()}"
        Log.d(TAG, "[$correlationId] Manual sync triggered")
        return syncToBackend(
            batchSize = HealthApiConfig.BATCH_SIZE_WIFI,
            correlationId = correlationId
        )
    }

    /**
     * Batch push all pending metrics to Azure backend.
     *
     * This method:
     * 1. Queries all PENDING records from database
     * 2. Splits them into batches (e.g., 200 records per batch)
     * 3. Pushes each batch sequentially without delays
     *
     * Rate limiting is handled via backend HTTP response codes and exponential backoff retry logic.
     *
     * @return BatchPushResult with total stats and per-batch results
     */
    suspend fun batchPushAllPendingMetrics(): BatchPushResult = withContext(Dispatchers.IO) {
        try {
            val correlationId = "batch-push-${System.currentTimeMillis()}"
            Log.d(TAG, "[$correlationId] Starting batch push of all pending metrics")

            // Get count of pending records (this will be called at database layer)
            // For now, we'll continuously sync until no more records are available
            var totalSynced = 0
            var totalFailed = 0
            var batchNumber = 0
            val batchSize = HealthApiConfig.BATCH_SIZE_WIFI
            val maxBatches = 500  // Safety limit to prevent infinite loops

            while (batchNumber < maxBatches) {
                batchNumber++
                val batchCorrelationId = "$correlationId-batch-$batchNumber"

                // Sync one batch
                val result = syncToBackend(
                    batchSize = batchSize,
                    correlationId = batchCorrelationId
                )

                totalSynced += result.syncedCount
                totalFailed += result.failedCount

                Log.d(TAG, "[$batchCorrelationId] Batch complete: Synced=${result.syncedCount}, Failed=${result.failedCount}")

                // If no records were synced, we're done
                if (result.syncedCount == 0 && result.failedCount == 0) {
                    Log.d(TAG, "[$correlationId] All batches complete after $batchNumber batches")
                    break
                }

            }

            BatchPushResult(
                isSuccess = totalFailed == 0,
                totalSynced = totalSynced,
                totalFailed = totalFailed,
                totalBatches = batchNumber,
                errorMessage = if (totalFailed > 0) "Failed to sync $totalFailed records" else null
            )

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during batch push: ${e.message}", e)
            BatchPushResult(
                isSuccess = false,
                totalSynced = 0,
                totalFailed = 0,
                totalBatches = 0,
                errorMessage = e.message
            )
        }
    }

    companion object {
        private const val TAG = "HealthSyncManager"
    }
}

data class SyncResult(
    val isSuccess: Boolean,
    val isRetryable: Boolean,
    val syncedCount: Int,
    val failedCount: Int,
    val errorMessage: String?
)

data class BatchPushResult(
    val isSuccess: Boolean,
    val totalSynced: Int,
    val totalFailed: Int,
    val totalBatches: Int,
    val errorMessage: String?
)
