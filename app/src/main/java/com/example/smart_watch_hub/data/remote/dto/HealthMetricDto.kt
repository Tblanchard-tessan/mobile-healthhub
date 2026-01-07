package com.example.smart_watch_hub.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HealthMetricDto(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("deviceId")
    val deviceId: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("heartRate")
    val heartRate: Int?,

    @SerializedName("bpSystolic")
    val bpSystolic: Int?,

    @SerializedName("bpDiastolic")
    val bpDiastolic: Int?,

    @SerializedName("spO2")
    val spO2: Int?,

    @SerializedName("steps")
    val steps: Int?,

    @SerializedName("calories")
    val calories: Int?,

    @SerializedName("distance")
    val distance: Int?,

    @SerializedName("temperature")
    val temperature: Float?,

    @SerializedName("bloodGlucose")
    val bloodGlucose: Float?,

    @SerializedName("totalSleep")
    val totalSleep: Int?,

    @SerializedName("deepSleep")
    val deepSleep: Int?,

    @SerializedName("lightSleep")
    val lightSleep: Int?,

    @SerializedName("stress")
    val stress: Int?,

    @SerializedName("met")
    val met: Float?,

    @SerializedName("mai")
    val mai: Int?,

    @SerializedName("isWearing")
    val isWearing: Boolean,

    @SerializedName("recordHash")
    val recordHash: String
)

data class BatchUploadRequest(
    @SerializedName("metrics")
    val metrics: List<HealthMetricDto>,

    @SerializedName("correlationId")
    val correlationId: String
)

data class UploadResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("syncedCount")
    val syncedCount: Int,

    @SerializedName("failedCount")
    val failedCount: Int,

    @SerializedName("durationMs")
    val durationMs: Int,

    @SerializedName("correlationId")
    val correlationId: String,

    @SerializedName("errors")
    val errors: List<String>? = null
)
