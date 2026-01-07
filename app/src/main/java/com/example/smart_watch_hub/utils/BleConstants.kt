package com.example.smart_watch_hub.utils

/**
 * Bluetooth and BLE constants for Starmax watch communication.
 */
object BleConstants {
    // Starmax GATT Service and Characteristics UUIDs (String format for FastBLE)
    const val STARMAX_SERVICE_UUID_STR = "6e400001-b5a3-f393-e0a9-e50e24dcca9d"
    const val STARMAX_WRITE_UUID_STR = "6e400002-b5a3-f393-e0a9-e50e24dcca9d"
    const val STARMAX_NOTIFY_UUID_STR = "6e400003-b5a3-f393-e0a9-e50e24dcca9d"

    // Standard GATT Descriptors
    const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

    // BLE Connection Parameters
    const val CONNECTION_TIMEOUT_MS = 10000
    const val OPERATION_TIMEOUT_MS = 5000
    const val NOTIFY_DELAY_MS = 3000 // CRITICAL: 3-second delay before opening notify

    // Auto-reconnect Parameters
    const val MAX_RECONNECT_ATTEMPTS = 3
    const val RECONNECT_DELAY_MS = 2000
    const val RSSI_THRESHOLD = -90 // Only accept devices with RSSI > -90 dBm

    // SDK Polling Intervals
    const val HEALTH_DETAIL_POLL_INTERVAL_MS = 5000 // Poll getHealthDetail() every 5s
    const val AGGREGATION_WINDOW_MS = 300000 // 5-minute aggregation window

    // ========== HEALTH METRIC VALIDATION RANGES ==========
    // Heart Rate (valid range: 30-220 bpm)
    const val MIN_HEART_RATE = 30
    const val MAX_HEART_RATE = 220

    // Blood Pressure Systolic (valid range: 60-280 mmHg)
    const val MIN_BP_SYSTOLIC = 60
    const val MAX_BP_SYSTOLIC = 280

    // Blood Pressure Diastolic (valid range: 30-150 mmHg)
    const val MIN_BP_DIASTOLIC = 30
    const val MAX_BP_DIASTOLIC = 150

    // Blood Oxygen (SpO2, valid range: 70-100%)
    const val MIN_SPO2 = 70
    const val MAX_SPO2 = 100

    // Body Temperature (valid range: 35.0-41.0°C)
    const val MIN_TEMPERATURE = 35.0f
    const val MAX_TEMPERATURE = 41.0f

    // Blood Sugar (valid range: 40-400 mg/dL)
    const val MIN_BLOOD_SUGAR = 40.0f
    const val MAX_BLOOD_SUGAR = 400.0f

    // Stress (valid range: 0-100)
    const val MIN_STRESS = 0
    const val MAX_STRESS = 100
}
