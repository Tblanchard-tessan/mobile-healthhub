package com.example.smart_watch_hub.domain.sync.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smart_watch_hub.domain.sync.HealthConnectSyncManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for periodic Health Connect synchronization.
 *
 * Runs every 15 minutes to:
 * 1. Push unsynced watch metrics to Health Connect
 * 2. Pull recent records from Health Connect (future enhancement)
 * 3. Log sync status for debugging
 *
 * Constraints:
 * - Network connectivity required
 * - Battery optimization: uses exponential backoff on failure
 * - Respects device Doze mode
 */
class HealthConnectSyncWorker(
    context: Context,
    params: androidx.work.WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val syncManager: HealthConnectSyncManager by inject()

    override suspend fun doWork(): Result {
        return try {
            // Execute bidirectional sync
            val syncResult = syncManager.executeBidirectionalSync()

            // Check if both operations were successful
            if (syncResult.isFullySuccessful) {
                Result.success()
            } else {
                // Retry on failure with exponential backoff
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "health_connect_sync"
        private const val SYNC_INTERVAL_MINUTES = 15L

        /**
         * Schedule periodic Health Connect synchronization.
         * Should be called once during app initialization (in SmartWatchHubApplication).
         *
         * @param context Application context
         */
        fun schedulePeriodicSync(context: Context) {
            val syncConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(
                SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setConstraints(syncConstraints)
                .addTag(SYNC_WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                syncWorkRequest
            )
        }

        /**
         * Cancel periodic Health Connect synchronization.
         *
         * @param context Application context
         */
        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        }

        /**
         * Trigger an immediate one-time sync (for testing or user-initiated).
         *
         * @param context Application context
         */
        fun syncNow(context: Context) {
            val syncConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<HealthConnectSyncWorker>()
                .setConstraints(syncConstraints)
                .addTag("${SYNC_WORK_NAME}_manual")
                .build()

            WorkManager.getInstance(context).enqueue(syncWorkRequest)
        }
    }
}
