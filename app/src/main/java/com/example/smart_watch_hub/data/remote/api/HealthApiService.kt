package com.example.smart_watch_hub.data.remote.api

import com.example.smart_watch_hub.data.remote.dto.BatchUploadRequest
import com.example.smart_watch_hub.data.remote.dto.UploadResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface HealthApiService {
    @POST("uploadHealthMetrics")
    suspend fun uploadHealthMetrics(
        @Header("X-Correlation-ID") correlationId: String,
        @Header("X-App-Version") appVersion: String,
        @Body request: BatchUploadRequest
    ): Response<UploadResponse>
}
