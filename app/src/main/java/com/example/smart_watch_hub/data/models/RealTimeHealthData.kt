package com.example.smart_watch_hub.data.models

/**
 * Real-time health data received from Starmax SDK streams.
 * This represents instantaneous measurements from the watch.
 */
data class RealTimeHealthData(
    val timestamp: Long = System.currentTimeMillis(),
    val heartRate: Int = 0,
    val steps: Int = 0,
    val calories: Int = 0,
    val distance: Int = 0,
    val bloodPressureSystolic: Int = 0,
    val bloodPressureDiastolic: Int = 0,
    val bloodOxygen: Int = 0,
    val temperature: Float = 0f,
    val bloodSugar: Float = 0f,
    val stress: Int = 0,
    val met: Float = 0f,
    val mai: Int = 0,
    val isWearing: Boolean = false,
    val sleepDuration: Int = 0,
    val deviceMac: String? = null
)
