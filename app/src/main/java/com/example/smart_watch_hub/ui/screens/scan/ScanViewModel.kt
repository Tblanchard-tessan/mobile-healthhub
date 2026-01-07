package com.example.smart_watch_hub.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clj.fastble.data.BleDevice
import com.example.smart_watch_hub.data.models.ConnectionState
import com.example.smart_watch_hub.data.repository.BleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for BLE device scanning and selection.
 *
 * Manages:
 * - Device discovery via BLE scanning
 * - Device selection and connection
 * - Scan state (idle, scanning, connecting)
 * - Error handling for connection failures
 */
class ScanViewModel : ViewModel(), KoinComponent {
    private val bleRepository: BleRepository by inject()

    // UI State
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    init {
        // Observe BLE repository state
        viewModelScope.launch {
            bleRepository.connectionState.collect { state ->
                when (state) {
                    ConnectionState.DISCONNECTED -> {
                        if (_scanState.value != ScanState.Scanning) {
                            _scanState.value = ScanState.Idle
                        }
                    }
                    ConnectionState.CONNECTING -> {
                        _scanState.value = ScanState.Connecting
                        _connectionError.value = null
                    }
                    ConnectionState.CONNECTED -> {
                        _scanState.value = ScanState.Connected
                        _connectionError.value = null
                        // Request initial health data from watch
                        bleRepository.requestHealthData()
                    }
                    ConnectionState.ERROR -> {
                        _scanState.value = ScanState.Error("Connection failed")
                        _connectionError.value = "Connection failed. Please try again."
                    }
                    ConnectionState.DISCONNECTING -> {
                        _scanState.value = ScanState.Idle
                    }
                }
            }
        }

        // Observe scanned devices
        viewModelScope.launch {
            bleRepository.scannedDevices.collect { devices ->
                _scannedDevices.value = devices
            }
        }
    }

    /**
     * Start BLE device scanning.
     */
    fun startScan() {
        _scanState.value = ScanState.Scanning
        _connectionError.value = null
        bleRepository.startScan()
    }

    /**
     * Stop BLE device scanning.
     */
    fun stopScan() {
        bleRepository.stopScan()
        _scanState.value = ScanState.Idle
    }

    /**
     * Connect to a discovered device.
     *
     * @param device BleDevice to connect to
     */
    fun connectToDevice(device: BleDevice) {
        viewModelScope.launch {
            try {
                stopScan()
                bleRepository.connectToDevice(device)
            } catch (e: Exception) {
                _connectionError.value = e.message ?: "Unknown error"
                _scanState.value = ScanState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Retry connection to last selected device.
     */
    fun retryConnection() {
        _connectionError.value = null
        startScan()
    }

    /**
     * Clear connection error message.
     */
    fun clearError() {
        _connectionError.value = null
    }

    sealed class ScanState {
        object Idle : ScanState()
        object Scanning : ScanState()
        object Connecting : ScanState()
        object Connected : ScanState()
        data class Error(val message: String) : ScanState()
    }
}
