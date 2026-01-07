package com.example.smart_watch_hub.data.models

/**
 * Watch history data models - parsed directly from Starmax SDK responses.
 * These are displayed in charts without going through the database.
 */

data class StepHistoryPoint(
    val timestamp: Long,      // Unix timestamp in ms
    val steps: Int,
    val calories: Int,        // Already converted from SDK units (/1000)
    val distance: Int         // Already converted from SDK units (/100)
)

data class HeartRateHistoryPoint(
    val timestamp: Long,      // Unix timestamp in ms
    val heartRate: Int        // BPM
)

data class BloodPressureHistoryPoint(
    val timestamp: Long,      // Unix timestamp in ms
    val systolic: Int,        // mmHg
    val diastolic: Int        // mmHg
)

/**
 * Container for all watch history data.
 */
data class WatchHistoryData(
    val steps: List<StepHistoryPoint> = emptyList(),
    val heartRate: List<HeartRateHistoryPoint> = emptyList(),
    val bloodPressure: List<BloodPressureHistoryPoint> = emptyList()
) {
    fun isEmpty() = steps.isEmpty() && heartRate.isEmpty() && bloodPressure.isEmpty()
}
