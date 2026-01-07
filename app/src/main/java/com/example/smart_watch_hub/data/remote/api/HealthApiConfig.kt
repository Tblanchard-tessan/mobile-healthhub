package com.example.smart_watch_hub.data.remote.api

object HealthApiConfig {
    const val BASE_URL = "https://health-data-hub-835353171414.europe-west1.run.app/"
    const val ENDPOINT_UPLOAD = "uploadHealthMetrics"

    // Sync configuration
    const val MAX_RETRIES = 5
    const val INITIAL_BACKOFF_MS = 30_000L  // 30 seconds
    const val MAX_BACKOFF_MS = 3_600_000L   // 1 hour
    const val BATCH_SIZE_WIFI = 200
    const val SYNC_INTERVAL_MINUTES = 30L
}
