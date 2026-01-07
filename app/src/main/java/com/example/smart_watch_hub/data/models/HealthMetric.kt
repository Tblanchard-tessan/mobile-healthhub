package com.example.smart_watch_hub.data.models

import com.example.smart_watch_hub.data.local.database.entities.HealthMetricEntity

/**
 * Domain model for health metrics.
 * Represents aggregated 5-minute health snapshots.
 */
data class HealthMetric(
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
    val deviceMac: String? = null,
    val syncedToHealthConnect: Boolean = false,
    val syncTimestamp: Long? = null
) {
    companion object {
        fun fromEntity(entity: HealthMetricEntity): HealthMetric {
            return HealthMetric(
                id = entity.id,
                timestamp = entity.timestamp,
                heartRate = entity.heartRate,
                steps = entity.steps,
                calories = entity.calories,
                distance = entity.distance,
                bloodPressureSystolic = entity.bloodPressureSystolic,
                bloodPressureDiastolic = entity.bloodPressureDiastolic,
                bloodOxygen = entity.bloodOxygen,
                temperature = entity.temperature,
                bloodSugar = entity.bloodSugar,
                totalSleep = entity.totalSleep,
                deepSleep = entity.deepSleep,
                lightSleep = entity.lightSleep,
                stress = entity.stress,
                met = entity.met,
                mai = entity.mai,
                isWearing = entity.isWearing,
                deviceMac = entity.deviceMac,
                syncedToHealthConnect = entity.syncedToHealthConnect,
                syncTimestamp = entity.syncTimestamp
            )
        }

        fun toEntity(metric: HealthMetric): HealthMetricEntity {
            return HealthMetricEntity(
                id = metric.id,
                timestamp = metric.timestamp,
                heartRate = metric.heartRate,
                steps = metric.steps,
                calories = metric.calories,
                distance = metric.distance,
                bloodPressureSystolic = metric.bloodPressureSystolic,
                bloodPressureDiastolic = metric.bloodPressureDiastolic,
                bloodOxygen = metric.bloodOxygen,
                temperature = metric.temperature,
                bloodSugar = metric.bloodSugar,
                totalSleep = metric.totalSleep,
                deepSleep = metric.deepSleep,
                lightSleep = metric.lightSleep,
                stress = metric.stress,
                met = metric.met,
                mai = metric.mai,
                isWearing = metric.isWearing,
                deviceMac = metric.deviceMac,
                syncedToHealthConnect = metric.syncedToHealthConnect,
                syncTimestamp = metric.syncTimestamp
            )
        }
    }
}
