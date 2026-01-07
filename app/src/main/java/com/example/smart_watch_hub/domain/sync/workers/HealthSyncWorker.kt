package com.example.smart_watch_hub.domain.sync.workers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.*
import com.example.smart_watch_hub.data.remote.api.HealthApiConfig
import com.example.smart_watch_hub.domain.sync.HealthSyncManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class HealthSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val syncManager: HealthSyncManager by inject()

    override suspend fun doWork(): Result {
        val correlationId = "${System.currentTimeMillis()}-${hashCode()}"

        Log.d(TAG, "[$correlationId] Starting sync...")

        // Check WiFi connection
        if (!isOnWiFi()) {
            Log.w(TAG, "[$correlationId] Not on WiFi, skipping sync")
            return Result.retry()
        }

        // Sync with batch size of 200
        val syncResult = syncManager.syncToBackend(
            batchSize = HealthApiConfig.BATCH_SIZE_WIFI,
            correlationId = correlationId
        )

        return when {
            syncResult.isSuccess -> {
                Log.d(TAG, "[$correlationId] Sync successful: ${syncResult.syncedCount} records")
                Result.success(workDataOf(
                    "syncedCount" to syncResult.syncedCount,
                    "correlationId" to correlationId
                ))
            }
            syncResult.isRetryable -> {
                Log.w(TAG, "[$correlationId] Sync failed (retryable): ${syncResult.errorMessage}")
                Result.retry()
            }
            else -> {
                Log.e(TAG, "[$correlationId] Sync failed (non-retryable): ${syncResult.errorMessage}")
                Result.failure(workDataOf(
                    "error" to syncResult.errorMessage
                ))
            }
        }
    }

    private fun isOnWiFi(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object {
        private const val TAG = "HealthSyncWorker"
        private const val WORK_NAME = "health_sync_periodic"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)  // WiFi only
                .setRequiresBatteryNotLow(true)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
                HealthApiConfig.SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
                10,  // Flex window: 10 minutes
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    HealthApiConfig.INITIAL_BACKOFF_MS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )

            Log.d(TAG, "Periodic sync scheduled (every ${HealthApiConfig.SYNC_INTERVAL_MINUTES} minutes)")
        }

        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<HealthSyncWorker>()
                .setConstraints(constraints)
                .addTag("${WORK_NAME}_manual")
                .build()

            WorkManager.getInstance(context).enqueue(syncWorkRequest)
            Log.d(TAG, "Manual sync triggered")
        }
    }
}
