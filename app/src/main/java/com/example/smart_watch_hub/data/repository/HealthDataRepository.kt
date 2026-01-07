package com.example.smart_watch_hub.data.repository

import com.example.smart_watch_hub.data.local.database.dao.DeviceDao
import com.example.smart_watch_hub.data.local.database.dao.HealthMetricsDao
import com.example.smart_watch_hub.data.local.database.dao.SyncLogDao
import com.example.smart_watch_hub.data.local.database.entities.HealthMetricEntity
import com.example.smart_watch_hub.data.local.database.entities.SyncLogEntity
import com.example.smart_watch_hub.data.models.DeviceInfo
import com.example.smart_watch_hub.data.models.HealthMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing local Room database operations.
 *
 * Handles:
 * - Health metrics storage (CRUD)
 * - Device information management
 * - Sync log tracking
 * - Data queries by time range
 */
class HealthDataRepository(
    private val healthMetricsDao: HealthMetricsDao,
    private val deviceDao: DeviceDao,
    private val syncLogDao: SyncLogDao
) {

    // ============ Health Metrics Operations ============

    /**
     * Get health metrics within a time range.
     * Used for historical data display in charts.
     *
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds
     * @return Flow of health metrics ordered by timestamp descending
     */
    fun getMetricsByTimeRange(startTime: Long, endTime: Long): Flow<List<HealthMetric>> {
        return healthMetricsDao.getMetricsByTimeRange(startTime, endTime)
            .map { entities -> entities.map { HealthMetric.fromEntity(it) } }
    }

    /**
     * Get the latest N health metrics for real-time UI display.
     *
     * @param limit Number of records to fetch
     * @return Flow of latest metrics ordered by timestamp descending
     */
    fun getLatestMetrics(limit: Int = 100): Flow<List<HealthMetric>> {
        return healthMetricsDao.getLatestMetrics(limit)
            .map { entities -> entities.map { HealthMetric.fromEntity(it) } }
    }

    /**
     * Insert a new health metric record.
     * Called when 5-minute aggregation window completes.
     *
     * @param metric Health metric to insert
     */
    suspend fun insertHealthMetric(metric: HealthMetric) {
        val entity = HealthMetric.toEntity(metric)
        healthMetricsDao.insert(entity)
    }

    /**
     * Insert multiple health metrics in batch.
     * Used for bulk inserts from aggregation or imports.
     *
     * @param metrics List of health metrics to insert
     */
    suspend fun insertHealthMetrics(metrics: List<HealthMetric>) {
        val entities = metrics.map { HealthMetric.toEntity(it) }
        healthMetricsDao.insertAll(entities)
    }

    /**
     * Update an existing health metric.
     *
     * @param metric Health metric to update
     */
    suspend fun updateHealthMetric(metric: HealthMetric) {
        val entity = HealthMetric.toEntity(metric)
        healthMetricsDao.update(entity)
    }

    /**
     * Get unsynced health metrics for Health Connect synchronization.
     *
     * @param startTime Only get metrics newer than this timestamp
     * @return List of metrics not yet synced to Health Connect
     */
    suspend fun getUnsyncedMetrics(startTime: Long): List<HealthMetric> {
        return healthMetricsDao.getUnsyncedMetrics(startTime)
            .map { HealthMetric.fromEntity(it) }
    }

    /**
     * Mark metrics as synced to Health Connect.
     * Updates syncedToHealthConnect flag and syncTimestamp.
     *
     * @param metricIds List of metric IDs to mark as synced
     * @param syncTime Timestamp of the sync operation
     */
    suspend fun markMetricsAsSynced(metricIds: List<Long>, syncTime: Long) {
        healthMetricsDao.markAsSynced(metricIds, syncTime)
    }

    /**
     * Delete old health metrics to manage database size.
     * Called periodically to clean up old data.
     *
     * @param cutoffTime Delete metrics older than this timestamp
     */
    suspend fun deleteOldMetrics(cutoffTime: Long) {
        healthMetricsDao.deleteOldMetrics(cutoffTime)
    }

    /**
     * Get average heart rate for a time range.
     * Used for analytics and chart display.
     *
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return Average heart rate, or null if no data
     */
    suspend fun getAverageHeartRate(startTime: Long, endTime: Long): Double? {
        return healthMetricsDao.getAverageHeartRate(startTime, endTime)
    }

    // ============ Device Operations ============

    /**
     * Get the last connected device for auto-reconnect.
     *
     * @return DeviceInfo of last connected device, or null if none
     */
    suspend fun getLastConnectedDevice(): DeviceInfo? {
        return deviceDao.getLastConnectedDevice()?.let { DeviceInfo.fromEntity(it) }
    }

    /**
     * Save a device to the database.
     * Called when successfully connecting to a watch.
     *
     * @param device Device information to save
     */
    suspend fun saveDevice(device: DeviceInfo) {
        val entity = DeviceInfo.toEntity(device)
        deviceDao.insert(entity)
    }

    /**
     * Delete a device from the database.
     *
     * @param macAddress MAC address of device to delete
     */
    suspend fun deleteDevice(macAddress: String) {
        deviceDao.delete(macAddress)
    }

    // ============ Sync Log Operations ============

    /**
     * Get recent sync logs for debugging and UI display.
     *
     * @param limit Number of recent logs to fetch
     * @return Flow of recent sync logs
     */
    fun getRecentSyncLogs(limit: Int = 50): Flow<List<SyncLogEntity>> {
        return syncLogDao.getRecentLogs(limit)
    }

    /**
     * Insert a sync log entry.
     * Called after each Health Connect sync operation.
     *
     * @param log Sync log entry to insert
     */
    suspend fun insertSyncLog(log: SyncLogEntity) {
        syncLogDao.insert(log)
    }

    /**
     * Delete old sync logs to manage database size.
     *
     * @param cutoffTime Delete logs older than this timestamp
     */
    suspend fun deleteOldSyncLogs(cutoffTime: Long) {
        syncLogDao.deleteOldLogs(cutoffTime)
    }

    // ============ Data Cleanup ============

    /**
     * Clean up old data from database.
     * Removes metrics and logs older than 30 days.
     * Called periodically by WorkManager.
     */
    suspend fun cleanupOldData() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
        deleteOldMetrics(thirtyDaysAgo)
        deleteOldSyncLogs(thirtyDaysAgo)
    }
}
