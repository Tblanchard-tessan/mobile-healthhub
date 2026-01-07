package com.example.smart_watch_hub.domain.sync

import com.example.smart_watch_hub.data.models.HealthMetric
import com.example.smart_watch_hub.data.models.RealTimeHealthData
import com.example.smart_watch_hub.utils.BleConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.LinkedList
import java.util.Queue

/**
 * Data aggregator for health metrics.
 *
 * Implements 5-minute aggregation window pattern:
 * 1. Receive real-time data from 3 SDK streams
 * 2. Add to in-memory aggregation buffer
 * 3. Every 5 minutes:
 *    - Calculate averages (HR, BP, SpO2, temperature, stress, MET, MAI)
 *    - Calculate totals (steps, distance, calories)
 *    - Save aggregated record to Room database
 *    - Sync to Health Connect (batch write)
 *    - Clear buffer
 *
 * This pattern:
 * - Reduces noise in health data
 * - Saves battery by batching database writes
 * - Prevents Health Connect throttling (avoid writing too frequently)
 * - Aligns with industry standard health app patterns (Google Fit, Apple Health)
 *
 * Buffer management:
 * - Max size: 1000 entries per 5-minute window
 * - At ~3-4 Hz data rate, limits memory to ~50-100KB per window
 * - Prevents unbounded growth from SDK stream bugs
 */
class DataAggregator {
    private val buffer: Queue<RealTimeHealthData> = LinkedList()
    private var aggregationWindowStart = System.currentTimeMillis()

    companion object {
        // Maximum data points per aggregation window to prevent unbounded memory growth
        // At 3-4 Hz data rate, typical window has ~900-1200 points, so 1000 is safe margin
        private const val MAX_BUFFER_SIZE = 1000
    }

    private val _aggregatedMetrics = MutableSharedFlow<HealthMetric>(replay = 0)
    val aggregatedMetrics: Flow<HealthMetric> = _aggregatedMetrics.asSharedFlow()

    /**
     * Add real-time data to aggregation buffer.
     * Called when receiving data from SDK streams.
     *
     * Enforces MAX_BUFFER_SIZE limit to prevent unbounded memory growth.
     * If buffer exceeds max size, oldest entries are discarded.
     */
    fun addRealTimeData(data: RealTimeHealthData) {
        buffer.offer(data)

        // Prevent unbounded buffer growth by removing oldest entries if limit exceeded
        while (buffer.size > MAX_BUFFER_SIZE) {
            buffer.poll()
        }
    }

    /**
     * Check if aggregation window has elapsed (5 minutes).
     *
     * @return True if 5 minutes have passed since last aggregation
     */
    fun isAggregationWindowComplete(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - aggregationWindowStart) >= BleConstants.AGGREGATION_WINDOW_MS
    }

    /**
     * Get and clear aggregated metric for the completed window.
     * Should be called when isAggregationWindowComplete() returns true.
     *
     * @return Aggregated HealthMetric or null if no data in window
     */
    fun getAndClearAggregation(): HealthMetric? {
        if (buffer.isEmpty()) {
            resetWindow()
            return null
        }

        val metric = aggregateBuffer()
        resetWindow()
        return metric
    }

    /**
     * Manually trigger aggregation (for testing or forced sync).
     */
    fun forceAggregation(): HealthMetric? {
        return getAndClearAggregation()
    }

    /**
     * Get current buffer size (for monitoring).
     */
    fun getBufferSize(): Int = buffer.size

    /**
     * Clear all buffered data (for disconnection cleanup).
     */
    fun clearBuffer() {
        buffer.clear()
        resetWindow()
    }

    /**
     * Aggregate all data in buffer into a single HealthMetric.
     */
    private fun aggregateBuffer(): HealthMetric {
        val dataPoints = buffer.toList()

        // Calculate averages (ignore zero values)
        val heartRateValues = dataPoints.map { it.heartRate }.filter { it > 0 }
        val avgHeartRate = if (heartRateValues.isNotEmpty()) {
            heartRateValues.average().toInt()
        } else 0

        val bpSystolicValues = dataPoints.map { it.bloodPressureSystolic }.filter { it > 0 }
        val avgBPSystolic = if (bpSystolicValues.isNotEmpty()) {
            bpSystolicValues.average().toInt()
        } else 0

        val bpDiastolicValues = dataPoints.map { it.bloodPressureDiastolic }.filter { it > 0 }
        val avgBPDiastolic = if (bpDiastolicValues.isNotEmpty()) {
            bpDiastolicValues.average().toInt()
        } else 0

        val spO2Values = dataPoints.map { it.bloodOxygen }.filter { it > 0 }
        val avgSpO2 = if (spO2Values.isNotEmpty()) {
            spO2Values.average().toInt()
        } else 0

        val tempValues = dataPoints.map { it.temperature }.filter { it > 0f }
        val avgTemp = if (tempValues.isNotEmpty()) {
            tempValues.average().toFloat()
        } else 0f

        val stressValues = dataPoints.map { it.stress }.filter { it > 0 }
        val avgStress = if (stressValues.isNotEmpty()) {
            stressValues.average().toInt()
        } else 0

        val metValues = dataPoints.map { it.met }.filter { it > 0f }
        val avgMET = if (metValues.isNotEmpty()) {
            metValues.average().toFloat()
        } else 0f

        val maiValues = dataPoints.map { it.mai }.filter { it > 0 }
        val avgMAI = if (maiValues.isNotEmpty()) {
            maiValues.average().toInt()
        } else 0

        // Calculate totals (sum up values)
        val totalSteps = dataPoints.sumOf { it.steps }
        val totalCalories = dataPoints.sumOf { it.calories }
        val totalDistance = dataPoints.sumOf { it.distance }
        val totalSleep = dataPoints.sumOf { it.sleepDuration }

        // Blood sugar (take last non-zero value)
        val bloodSugar = dataPoints.lastOrNull { it.bloodSugar > 0 }?.bloodSugar ?: 0f

        // Sleep stages (use values from latest entry)
        val latestData = dataPoints.lastOrNull()

        // Device MAC (use from any data point, preferring non-null value)
        val deviceMac = dataPoints.firstOrNull { it.deviceMac != null }?.deviceMac

        return HealthMetric(
            timestamp = aggregationWindowStart, // Use window start time
            heartRate = avgHeartRate,
            steps = totalSteps,
            calories = totalCalories,
            distance = totalDistance,
            bloodPressureSystolic = avgBPSystolic,
            bloodPressureDiastolic = avgBPDiastolic,
            bloodOxygen = avgSpO2,
            temperature = avgTemp,
            bloodSugar = bloodSugar,
            totalSleep = totalSleep,
            deepSleep = latestData?.sleepDuration ?: 0, // TODO: Get from SDK
            lightSleep = 0, // TODO: Get from SDK
            stress = avgStress,
            met = avgMET,
            mai = avgMAI,
            isWearing = dataPoints.lastOrNull()?.isWearing ?: false,
            deviceMac = deviceMac,
            syncedToHealthConnect = false,
            syncTimestamp = null
        )
    }

    /**
     * Reset aggregation window for next cycle.
     */
    private fun resetWindow() {
        aggregationWindowStart = System.currentTimeMillis()
        buffer.clear()
    }

    /**
     * Get aggregation progress as percentage (0-100).
     */
    fun getAggregationProgress(): Int {
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - aggregationWindowStart
        return ((elapsed.toFloat() / BleConstants.AGGREGATION_WINDOW_MS.toFloat()) * 100).toInt()
            .coerceIn(0, 100)
    }
}
