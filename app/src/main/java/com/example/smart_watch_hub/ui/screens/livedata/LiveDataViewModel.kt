package com.example.smart_watch_hub.ui.screens.livedata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.smart_watch_hub.data.models.ConnectionState
import com.example.smart_watch_hub.data.models.DeviceInfo
import com.example.smart_watch_hub.data.models.RealTimeHealthData
import com.example.smart_watch_hub.data.repository.BleRepository
import com.example.smart_watch_hub.data.repository.HealthDataRepository
import com.example.smart_watch_hub.domain.sync.HealthSyncManager
import com.starmax.bluetoothsdk.StarmaxBleClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for Live Data screen.
 *
 * Responsibilities:
 * - Subscribe to BLE real-time data streams
 * - Display current connection state and live metrics
 * - Show Azure sync status and data
 * - Track last synced metrics
 *
 * Data Flow:
 * SDK Streams → RealTimeData → Display + DB Save → Azure Sync
 */
class LiveDataViewModel : ViewModel(), KoinComponent {
    private val bleRepository: BleRepository by inject()
    private val healthDataRepository: HealthDataRepository by inject()
    private val healthSyncManager: HealthSyncManager by inject()

    // UI State Flows
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DeviceInfo?>(null)
    val connectedDevice: StateFlow<DeviceInfo?> = _connectedDevice.asStateFlow()

    private val _currentMetrics = MutableStateFlow<RealTimeHealthData?>(null)
    val currentMetrics: StateFlow<RealTimeHealthData?> = _currentMetrics.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _azureSyncData = MutableStateFlow<AzureSyncData?>(null)
    val azureSyncData: StateFlow<AzureSyncData?> = _azureSyncData.asStateFlow()

    init {
        observeConnectionState()
        observeRealTimeData()
        startPeriodicRefresh()
    }

    /**
     * Observe BLE connection state from repository.
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            bleRepository.connectionState.collect { state ->
                _connectionState.value = state
                if (state == ConnectionState.DISCONNECTED) {
                    _currentMetrics.value = null
                }
            }
        }

        viewModelScope.launch {
            bleRepository.connectedDevice.collect { device ->
                _connectedDevice.value = device
            }
        }
    }

    /**
     * Observe real-time data from BLE repository and display immediately.
     */
    private fun observeRealTimeData() {
        viewModelScope.launch {
            bleRepository.realTimeData.collect { data ->
                if (data != null) {
                    _currentMetrics.value = data
                    // Update Azure sync preview data
                    updateAzureSyncData(data)
                }
            }
        }
    }

    /**
     * Periodically request fresh health data every 15 seconds when connected.
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(15000)  // 15 seconds
                // Only request data if connected
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    requestHealthData()
                }
            }
        }
    }

    /**
     * Update the preview of data that will be sent to Azure.
     */
    private fun updateAzureSyncData(data: RealTimeHealthData) {
        val deviceMac = _connectedDevice.value?.macAddress ?: "unknown"
        _azureSyncData.value = AzureSyncData(
            deviceMac = deviceMac,
            timestamp = data.timestamp,
            heartRate = if (data.heartRate > 0) data.heartRate else null,
            steps = if (data.steps >= 0) data.steps else null,
            bloodPressure = if (data.bloodPressureSystolic > 0)
                "${data.bloodPressureSystolic}/${data.bloodPressureDiastolic}" else null,
            bloodOxygen = if (data.bloodOxygen > 0) data.bloodOxygen else null,
            temperature = if (data.temperature > 0) String.format("%.1f", data.temperature) else null,
            calories = if (data.calories >= 0) data.calories else null,
            distance = if (data.distance >= 0) (data.distance / 1000f).toInt() else null,
            isWearing = data.isWearing
        )
    }

    /**
     * Request device version info.
     */
    fun requestDeviceVersion() {
        try {
            Log.d("LiveDataViewModel", "Requesting device version...")
            StarmaxBleClient.instance.getVersion()
                .subscribe(
                    { version ->
                        Log.d("LiveDataViewModel", "✓ Device: ${version.model}, Firmware: ${version.version}")
                    },
                    { error ->
                        Log.e("LiveDataViewModel", "Version request error: ${error.message}")
                    }
                )
        } catch (e: Exception) {
            Log.e("LiveDataViewModel", "Failed to request version: ${e.message}")
        }
    }

    /**
     * Request health data from watch.
     */
    fun requestHealthData() {
        bleRepository.requestHealthData()
    }

    /**
     * Trigger manual sync to Azure backend.
     */
    fun triggerAzureSync() {
        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.Syncing
                val result = healthSyncManager.manualSync()

                if (result.isSuccess) {
                    _syncStatus.value = SyncStatus.Success
                    _lastSyncTime.value = System.currentTimeMillis()
                    Log.d("LiveDataViewModel", "Azure sync successful: ${result.syncedCount} records")
                } else {
                    _syncStatus.value = SyncStatus.Failed(result.errorMessage ?: "Unknown error")
                    Log.w("LiveDataViewModel", "Azure sync failed: ${result.errorMessage}")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Failed(e.message ?: "Unexpected error")
                Log.e("LiveDataViewModel", "Azure sync error: ${e.message}", e)
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear sync status.
     */
    fun clearSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    /**
     * Sync status enum.
     */
    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        object Success : SyncStatus()
        data class Failed(val error: String) : SyncStatus()
    }
}

/**
 * Data being sent to Azure.
 */
data class AzureSyncData(
    val deviceMac: String,
    val timestamp: Long,
    val heartRate: Int?,
    val steps: Int?,
    val bloodPressure: String?,
    val bloodOxygen: Int?,
    val temperature: String?,
    val calories: Int?,
    val distance: Int?,
    val isWearing: Boolean
)
