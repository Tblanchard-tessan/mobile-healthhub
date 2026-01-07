package com.example.smart_watch_hub.data.repository

import android.util.Log
import com.example.smart_watch_hub.data.remote.api.HealthApiConfig
import com.example.smart_watch_hub.data.remote.api.HealthApiService
import com.example.smart_watch_hub.data.remote.dto.BatchUploadRequest
import com.example.smart_watch_hub.data.remote.dto.HealthMetricDto
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class HealthApiRepository {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "SmartWatchHub-Android/1.0")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(HealthApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(HealthApiService::class.java)

    suspend fun uploadHealthMetrics(
        metrics: List<HealthMetricDto>,
        correlationId: String
    ): UploadResult {
        return try {
            val request = BatchUploadRequest(
                metrics = metrics,
                correlationId = correlationId
            )

            val response = apiService.uploadHealthMetrics(
                correlationId = correlationId,
                appVersion = "1.0",
                request = request
            )

            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null && body.success) {
                        UploadResult.Success(body.syncedCount)
                    } else {
                        UploadResult.Error(
                            message = body?.errors?.firstOrNull() ?: "Unknown error",
                            isRetryable = false
                        )
                    }
                }
                response.code() in 500..599 -> {
                    // Server error - retryable
                    UploadResult.Error(
                        message = "Server error: ${response.code()}",
                        isRetryable = true
                    )
                }
                else -> {
                    // Client error - non-retryable
                    UploadResult.Error(
                        message = "Client error: ${response.code()}",
                        isRetryable = false
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Network timeout", e)
            UploadResult.Error("Network timeout", isRetryable = true)
        } catch (e: UnknownHostException) {
            Log.w(TAG, "No internet connection", e)
            UploadResult.Error("No internet connection", isRetryable = true)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            UploadResult.Error(
                message = e.message ?: "Unknown error",
                isRetryable = true
            )
        }
    }

    companion object {
        private const val TAG = "HealthApiRepository"
    }
}

sealed class UploadResult {
    data class Success(val syncedCount: Int) : UploadResult()
    data class Error(val message: String, val isRetryable: Boolean) : UploadResult()
}
