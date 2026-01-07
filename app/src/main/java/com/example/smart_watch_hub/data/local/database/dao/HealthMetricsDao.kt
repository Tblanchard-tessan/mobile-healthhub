package com.example.smart_watch_hub.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smart_watch_hub.data.local.database.entities.HealthMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricsDao {
    @Query("SELECT * FROM health_metrics WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getMetricsByTimeRange(startTime: Long, endTime: Long): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics WHERE syncedToHealthConnect = 0 AND timestamp >= :startTime ORDER BY timestamp ASC")
    suspend fun getUnsyncedMetrics(startTime: Long): List<HealthMetricEntity>

    @Query("SELECT * FROM health_metrics ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestMetrics(limit: Int): Flow<List<HealthMetricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: HealthMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metrics: List<HealthMetricEntity>)

    @Update
    suspend fun update(metric: HealthMetricEntity)

    @Query("UPDATE health_metrics SET syncedToHealthConnect = 1, syncTimestamp = :syncTime WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>, syncTime: Long)

    @Query("DELETE FROM health_metrics WHERE timestamp < :cutoffTime")
    suspend fun deleteOldMetrics(cutoffTime: Long)

    @Query("SELECT AVG(heartRate) FROM health_metrics WHERE timestamp >= :startTime AND timestamp <= :endTime AND heartRate > 0")
    suspend fun getAverageHeartRate(startTime: Long, endTime: Long): Double?

    // Azure sync methods
    @Query("""
        SELECT * FROM health_metrics
        WHERE azure_sync_status IN ('PENDING', 'FAILED')
        AND (azure_last_attempt IS NULL OR azure_last_attempt < :backoffCutoff)
        AND azure_retry_count < :maxRetries
        AND device_mac IS NOT NULL
        ORDER BY timestamp DESC
        LIMIT :batchSize
    """)
    suspend fun getAzureUnsyncedMetrics(
        batchSize: Int,
        maxRetries: Int,
        backoffCutoff: Long
    ): List<HealthMetricEntity>

    @Query("UPDATE health_metrics SET azure_sync_status = 'SYNCING', azure_last_attempt = :attemptTime WHERE id IN (:ids)")
    suspend fun markAsAzureSyncing(ids: List<Long>, attemptTime: Long)

    @Query("UPDATE health_metrics SET azure_sync_status = 'SYNCED', azure_sync_timestamp = :syncTime, azure_last_error = NULL WHERE id IN (:ids)")
    suspend fun markAsAzureSynced(ids: List<Long>, syncTime: Long)

    @Query("""
        UPDATE health_metrics
        SET azure_sync_status = CASE
                WHEN azure_retry_count + 1 >= :maxRetries THEN 'DLQ'
                ELSE 'FAILED'
            END,
            azure_retry_count = azure_retry_count + 1,
            azure_last_error = :errorMessage,
            azure_last_attempt = :attemptTime
        WHERE id IN (:ids)
    """)
    suspend fun markAsAzureFailed(
        ids: List<Long>,
        errorMessage: String,
        attemptTime: Long,
        maxRetries: Int
    )

    @Query("SELECT azure_sync_status as status, COUNT(*) as count FROM health_metrics GROUP BY azure_sync_status")
    suspend fun getAzureSyncStats(): List<SyncStat>
}

data class SyncStat(
    val status: String,
    val count: Int
)
