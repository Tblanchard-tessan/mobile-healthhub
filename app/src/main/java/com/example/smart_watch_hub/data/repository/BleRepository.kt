package com.example.smart_watch_hub.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleWriteCallback
import com.clj.fastble.data.BleDevice
import com.example.smart_watch_hub.data.local.database.dao.DeviceDao
import com.example.smart_watch_hub.data.models.ConnectionState
import com.example.smart_watch_hub.data.models.DeviceInfo
import com.example.smart_watch_hub.data.models.RealTimeHealthData
import com.example.smart_watch_hub.data.models.StepHistoryPoint
import com.example.smart_watch_hub.data.models.HeartRateHistoryPoint
import com.example.smart_watch_hub.data.models.BloodPressureHistoryPoint
import com.example.smart_watch_hub.data.models.WatchHistoryData
import com.example.smart_watch_hub.utils.BleConstants
import com.starmax.bluetoothsdk.StarmaxBleClient
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Repository for managing BLE connections to Starmax watches.
 *
 * Handles:
 * - Device scanning and filtering
 * - BLE connection with 3-second notify delay (CRITICAL)
 * - Auto-reconnect logic (max 3 attempts, 2s delay)
 * - SDK initialization and stream subscription
 * - Device memory via Room database
 *
 * CRITICAL: Do NOT open notify until 3 seconds after connection success.
 * This is required for proper Starmax SDK initialization.
 */
class BleRepository(private val context: Context, private val applicationScope: kotlinx.coroutines.CoroutineScope) : KoinComponent {
    private val deviceDao: DeviceDao by inject()
    private val healthDataRepository: HealthDataRepository by inject()
    private val healthSyncManager: com.example.smart_watch_hub.domain.sync.HealthSyncManager by inject()
    private val bleManager = BleManager.getInstance()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DeviceInfo?>(null)
    val connectedDevice: StateFlow<DeviceInfo?> = _connectedDevice.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    private val _realTimeData = MutableStateFlow<RealTimeHealthData?>(RealTimeHealthData())
    val realTimeData: StateFlow<RealTimeHealthData?> = _realTimeData.asStateFlow()

    // Watch history data - exposed for direct display in charts
    private val _watchHistoryData = MutableStateFlow(WatchHistoryData())
    val watchHistoryData: StateFlow<WatchHistoryData> = _watchHistoryData.asStateFlow()

    private var reconnectAttempt = 0
    private var isUserInitiatedDisconnect = false
    private var currentBleDevice: BleDevice? = null
    private var connectedDeviceMac: String? = null  // Store device MAC reliably
    // Disposables for SDK streams - kept alive across ViewModel lifecycle
    private val disposables = CompositeDisposable()
    // Tracks if SDK streams are initialized to prevent duplicate subscriptions
    private var sdkStreamsInitialized = false

    init {
        loadLastConnectedDevice()

        // Set SDK write handler for sending commands to watch
        try {
            StarmaxBleClient.instance.setWrite { byteArray ->
                sendBleCommand(byteArray)
            }
        } catch (e: Exception) {
            Log.e("BleRepository", "Failed to set SDK write handler: ${e.message}")
        }
    }

    /**
     * Validate heart rate is within physiological range.
     * @return True if valid, false otherwise
     */
    private fun isValidHeartRate(hr: Int): Boolean {
        return hr in BleConstants.MIN_HEART_RATE..BleConstants.MAX_HEART_RATE
    }

    /**
     * Validate blood pressure values are within physiological range.
     * @return True if both systolic and diastolic are valid
     */
    private fun isValidBloodPressure(systolic: Int, diastolic: Int): Boolean {
        val systolicValid = systolic in BleConstants.MIN_BP_SYSTOLIC..BleConstants.MAX_BP_SYSTOLIC
        val diastolicValid = diastolic in BleConstants.MIN_BP_DIASTOLIC..BleConstants.MAX_BP_DIASTOLIC
        return systolicValid && diastolicValid
    }

    /**
     * Validate blood oxygen (SpO2) is within physiological range.
     * @return True if valid, false otherwise
     */
    private fun isValidSpO2(spO2: Int): Boolean {
        return spO2 in BleConstants.MIN_SPO2..BleConstants.MAX_SPO2
    }

    /**
     * Validate body temperature is within physiological range.
     * @return True if valid, false otherwise
     */
    private fun isValidTemperature(temp: Float): Boolean {
        return temp in BleConstants.MIN_TEMPERATURE..BleConstants.MAX_TEMPERATURE
    }

    /**
     * Validate blood sugar is within physiological range.
     * @return True if valid (or zero/missing), false if out of range
     */
    private fun isValidBloodSugar(sugar: Float): Boolean {
        return sugar == 0f || sugar in BleConstants.MIN_BLOOD_SUGAR..BleConstants.MAX_BLOOD_SUGAR
    }

    /**
     * Validate stress level is within range.
     * @return True if valid, false otherwise
     */
    private fun isValidStress(stress: Int): Boolean {
        return stress in BleConstants.MIN_STRESS..BleConstants.MAX_STRESS
    }

    /**
     * Send BLE command to watch via FastBLE.
     */
    @SuppressLint("MissingPermission")
    private fun sendBleCommand(commandBytes: ByteArray) {
        try {
            currentBleDevice?.let { device ->
                bleManager.write(
                    device,
                    BleConstants.STARMAX_SERVICE_UUID_STR,
                    BleConstants.STARMAX_WRITE_UUID_STR,
                    commandBytes,
                    object : BleWriteCallback() {
                        override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray) {
                            Log.d("BleRepository", "SDK command sent successfully: ${current}/${total}")
                        }

                        override fun onWriteFailure(exception: com.clj.fastble.exception.BleException) {
                            Log.e("BleRepository", "SDK command send failed: ${exception.toString()}")
                        }
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("BleRepository", "Error sending SDK command: ${e.message}")
        }
    }

    /**
     * Start BLE device scanning with RSSI filtering.
     * Only devices with RSSI > -90 dBm are included.
     */
    @SuppressLint("MissingPermission")
    fun startScan() {
        _connectionState.value = ConnectionState.CONNECTING
        _scannedDevices.value = emptyList()

        bleManager.scan(object : com.clj.fastble.callback.BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                if (!success) {
                    _connectionState.value = ConnectionState.ERROR
                }
            }

            override fun onScanning(bleDevice: BleDevice) {
                // Device found during scan - optional callback
                // Will be aggregated in onScanFinished
            }

            override fun onScanFinished(scanResultList: MutableList<BleDevice>) {
                // Filter by RSSI strength and exclude devices without names
                val filtered = scanResultList.filter {
                    it.rssi > -90 && !it.name.isNullOrEmpty()
                }
                _scannedDevices.value = filtered
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        })
    }

    /**
     * Stop BLE scanning.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        bleManager.cancelScan()
    }

    /**
     * Connect to a discovered BLE device.
     *
     * Connection sequence:
     * 1. BleManager.connect() initiates connection
     * 2. onConnectSuccess() callback fires
     * 3. ⚠️ WAIT 3 SECONDS (Handler.postDelayed) - CRITICAL
     * 4. Open notify characteristic (bleManager.notify())
     * 5. onNotifySuccess() callback fires
     * 6. Initialize SDK streams
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BleDevice) {
        stopScan()
        currentBleDevice = device
        isUserInitiatedDisconnect = false
        reconnectAttempt = 0
        _connectionState.value = ConnectionState.CONNECTING

        bleManager.connect(device, object : BleGattCallback() {
            override fun onStartConnect() {
                _connectionState.value = ConnectionState.CONNECTING
            }

            override fun onConnectSuccess(bleDevice: BleDevice, gatt: android.bluetooth.BluetoothGatt, status: Int) {
                // Step 2: Connection successful, but DO NOT open notify yet!
                // Step 3: CRITICAL 3-second delay before opening notify
                Handler(Looper.getMainLooper()).postDelayed({
                    openNotifyCharacteristic(bleDevice)
                }, 3000) // 3000ms = 3 seconds
            }

            override fun onConnectFail(bleDevice: BleDevice, exception: com.clj.fastble.exception.BleException) {
                handleConnectionFailure()
            }

            override fun onDisConnected(
                isActiveDisconnect: Boolean,
                device: BleDevice,
                gatt: android.bluetooth.BluetoothGatt,
                status: Int
            ) {
                if (!isUserInitiatedDisconnect && reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
                    // Auto-reconnect: max 3 attempts with 2s delay
                    reconnectAttempt++
                    // Reset SDK streams flag for reconnection
                    sdkStreamsInitialized = false
                    disposables.clear()
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (currentBleDevice != null) {
                            connectToDevice(currentBleDevice!!)
                        }
                    }, 2000) // 2 second delay between attempts
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    sdkStreamsInitialized = false
                    disposables.clear()
                }
            }
        })
    }

    /**
     * Open notify characteristic after 3-second delay.
     * This is Step 4 in the connection sequence.
     */
    @SuppressLint("MissingPermission")
    private fun openNotifyCharacteristic(device: BleDevice) {
        bleManager.notify(
            device,
            BleConstants.STARMAX_SERVICE_UUID_STR,
            BleConstants.STARMAX_NOTIFY_UUID_STR,
            object : BleNotifyCallback() {
                override fun onNotifySuccess() {
                    // Step 5: Notify characteristic opened successfully
                    _connectionState.value = ConnectionState.CONNECTED
                    connectedDeviceMac = device.mac  // Store device MAC reliably
                    val deviceInfo = DeviceInfo(
                        macAddress = device.mac,
                        name = device.name ?: "Starmax Watch",
                        lastConnected = System.currentTimeMillis()
                    )
                    _connectedDevice.value = deviceInfo

                    Log.d("BleRepository", "Device connected: ${device.name} (${device.mac})")

                    // Save device to Room database for auto-reconnect
                    saveDeviceAsync(deviceInfo)

                    // Step 6: Initialize SDK streams for real-time data
                    initializeSdkStreams()
                }

                override fun onNotifyFailure(exception: com.clj.fastble.exception.BleException) {
                    handleConnectionFailure()
                }

                override fun onCharacteristicChanged(data: ByteArray) {
                    // Handle incoming notifications from watch
                    processSdkData(data)
                }
            }
        )
    }


    /**
     * Save connected device to Room database asynchronously.
     * Enables auto-reconnect to last connected device on app restart.
     * Uses application scope for proper lifecycle management.
     */
    private fun saveDeviceAsync(device: DeviceInfo) {
        // Launch coroutine without blocking main thread
        try {
            applicationScope.launch(Dispatchers.IO) {
                try {
                    deviceDao.insert(DeviceInfo.toEntity(device))
                } catch (e: Exception) {
                    Log.e("BleRepository", "Failed to save device: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("BleRepository", "Error launching save device coroutine: ${e.message}")
        }
    }

    /**
     * Initialize Starmax SDK streams for real-time data.
     * Called after successful notify characteristic connection.
     * Streams are kept alive across ViewModel/screen navigation.
     *
     * Subscribes to:
     * - realTimeDataStream(): Live metrics (heart rate, steps, blood pressure, oxygen, etc)
     * - healthMeasureStream(): On-demand health measurements
     * - getHealthDetail(): Polled health summary every 5 seconds
     *
     * IMPORTANT: These streams must persist across page navigation.
     * Only initialize once to prevent duplicate subscriptions.
     */
    private fun initializeSdkStreams() {
        try {
            // Prevent duplicate initialization
            if (sdkStreamsInitialized) {
                Log.d("BleRepository", "SDK streams already initialized, skipping")
                return
            }
            sdkStreamsInitialized = true

            // Subscribe to real-time data stream
            val realTimeSubscription = StarmaxBleClient.instance.realTimeDataStream()
                .subscribe(
                    { realTimeData ->
                        try {
                            // Extract real-time health metrics from watch with validation
                            val hr = realTimeData.heartRate
                            val bpSys = realTimeData.bloodPressureSs.toInt()
                            val bpDia = realTimeData.bloodPressureFz.toInt()
                            val spO2 = realTimeData.bloodOxygen.toInt()
                            val temp = realTimeData.temp.toFloat()
                            val sugar = realTimeData.bloodSugar.toFloat()

                            // Validate critical metrics before updating
                            if ((hr > 0 && !isValidHeartRate(hr)) ||
                                (bpSys > 0 && bpDia > 0 && !isValidBloodPressure(bpSys, bpDia)) ||
                                (spO2 > 0 && !isValidSpO2(spO2)) ||
                                (temp > 0 && !isValidTemperature(temp)) ||
                                (sugar > 0 && !isValidBloodSugar(sugar))
                            ) {
                                Log.w("BleRepository", "Received invalid health metric: HR=$hr BP=$bpSys/$bpDia SpO2=$spO2 T=$temp Sugar=$sugar")
                                // Skip this data point if invalid
                                return@subscribe
                            }

                            val newData = RealTimeHealthData(
                                heartRate = hr,
                                steps = realTimeData.steps.toInt(),
                                distance = realTimeData.distance.toInt(),
                                calories = realTimeData.calore.toInt(),
                                bloodPressureSystolic = bpSys,
                                bloodPressureDiastolic = bpDia,
                                bloodOxygen = spO2,
                                temperature = temp,
                                bloodSugar = sugar,
                                timestamp = System.currentTimeMillis(),
                                deviceMac = connectedDeviceMac  // Use stored device MAC
                            )
                            _realTimeData.value = newData
                            Log.d("BleRepository", "Real-time data: HR=$hr, Steps=${realTimeData.steps}")
                        } catch (e: Exception) {
                            Log.e("BleRepository", "Error processing real-time data: ${e.message}")
                        }
                    },
                    { error ->
                        Log.e("BleRepository", "Real-time data stream error: ${error.message}")
                    }
                )
            disposables.add(realTimeSubscription)

            // Subscribe to health measure stream (on-demand measurements)
            val healthMeasureSubscription = StarmaxBleClient.instance.healthMeasureStream()
                .subscribe(
                    { measurement ->
                        try {
                            when (measurement.type.toInt()) {
                                0x63 -> {
                                    // Heart rate measurement
                                    val hrValue = measurement.dataList.getOrNull(0)?.toInt() ?: 0
                                    if (hrValue > 0 && isValidHeartRate(hrValue)) {
                                        val updated = _realTimeData.value?.copy(heartRate = hrValue)
                                        _realTimeData.value = updated
                                        Log.d("BleRepository", "Heart rate measurement: $hrValue bpm")
                                    } else if (hrValue > 0) {
                                        Log.w("BleRepository", "Invalid heart rate measurement: $hrValue bpm")
                                    }
                                }
                                0x66 -> {
                                    // Blood pressure measurement
                                    if (measurement.dataList.size >= 2) {
                                        val sysBp = measurement.dataList[0].toInt()
                                        val diaBp = measurement.dataList[1].toInt()
                                        if (isValidBloodPressure(sysBp, diaBp)) {
                                            val updated = _realTimeData.value?.copy(
                                                bloodPressureSystolic = sysBp,
                                                bloodPressureDiastolic = diaBp
                                            )
                                            _realTimeData.value = updated
                                            Log.d("BleRepository", "BP: $sysBp/$diaBp mmHg")
                                        } else {
                                            Log.w("BleRepository", "Invalid blood pressure: $sysBp/$diaBp mmHg")
                                        }
                                    }
                                }
                                else -> {
                                    Log.d("BleRepository", "Other measurement type: ${measurement.type}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("BleRepository", "Error processing health measure: ${e.message}")
                        }
                    },
                    { error ->
                        Log.e("BleRepository", "Health measure stream error: ${error.message}")
                    }
                )
            disposables.add(healthMeasureSubscription)

            // Subscribe to health detail snapshot (5-minute polling)
            val healthDetailSubscription = StarmaxBleClient.instance.getHealthDetail()
                .subscribe(
                    { detail ->
                        try {
                            // Validate critical metrics before updating
                            val hr = detail.currentHeartRate
                            val bpSys = detail.currentSs.toInt()
                            val bpDia = detail.currentFz.toInt()
                            val spO2 = detail.currentBloodOxygen.toInt()
                            val temp = detail.currentTemp.toFloat()

                            // Only update with valid values
                            val updatedData = _realTimeData.value?.copy(
                                heartRate = if (hr > 0 && isValidHeartRate(hr)) hr else _realTimeData.value?.heartRate ?: 0,
                                steps = detail.totalSteps,
                                distance = detail.totalDistance,
                                calories = detail.totalHeat.toInt(),
                                bloodPressureSystolic = if (bpSys > 0 && bpDia > 0 && isValidBloodPressure(bpSys, bpDia)) bpSys else _realTimeData.value?.bloodPressureSystolic ?: 0,
                                bloodPressureDiastolic = if (bpSys > 0 && bpDia > 0 && isValidBloodPressure(bpSys, bpDia)) bpDia else _realTimeData.value?.bloodPressureDiastolic ?: 0,
                                bloodOxygen = if (spO2 > 0 && isValidSpO2(spO2)) spO2 else _realTimeData.value?.bloodOxygen ?: 0,
                                temperature = if (temp > 0 && isValidTemperature(temp)) temp else _realTimeData.value?.temperature ?: 0f
                            )
                            _realTimeData.value = updatedData

                            // Log invalid metrics for debugging
                            if ((hr > 0 && !isValidHeartRate(hr)) ||
                                (bpSys > 0 && bpDia > 0 && !isValidBloodPressure(bpSys, bpDia)) ||
                                (spO2 > 0 && !isValidSpO2(spO2)) ||
                                (temp > 0 && !isValidTemperature(temp))
                            ) {
                                Log.w("BleRepository", "Health detail had invalid values: HR=$hr BP=$bpSys/$bpDia SpO2=$spO2 T=$temp")
                            }

                            Log.d("BleRepository", "Health detail: Steps=${detail.totalSteps}, Sleep=${detail.totalSleep}min")
                        } catch (e: Exception) {
                            Log.e("BleRepository", "Error processing health detail: ${e.message}")
                        }
                    },
                    { error ->
                        Log.e("BleRepository", "Health detail error: ${error.message}")
                    }
                )
            disposables.add(healthDetailSubscription)

            Log.d("BleRepository", "SDK streams initialized successfully")
        } catch (e: Exception) {
            Log.e("BleRepository", "Failed to initialize SDK streams: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }


    private fun processSdkData(data: ByteArray) {
        try {
            StarmaxBleClient.instance.notify(data)
        } catch (e: Exception) {
            Log.e("BleRepository", "Error processing notification: ${e.message}")
        }
    }

    /**
     * Disconnect from the currently connected device.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        isUserInitiatedDisconnect = true
        reconnectAttempt = 0

        // Clean up SDK stream subscriptions
        disposables.clear()
        sdkStreamsInitialized = false
        _realTimeData.value = null

        currentBleDevice?.let { device ->
            bleManager.disconnect(device)
        }
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDevice.value = null
        connectedDeviceMac = null  // Clear device MAC on disconnect
    }

    /**
     * Manually request health detail from watch.
     * Can be called to trigger a data update.
     */
    fun requestHealthData() {
        try {
            Log.d("BleRepository", "Requesting health data from watch...")
            StarmaxBleClient.instance.getHealthDetail()
                .subscribe(
                    { detail ->
                        try {
                            Log.d("BleRepository", "✓ Health data received: Steps=${detail.totalSteps}, HR=${detail.currentHeartRate}")
                            val updated = _realTimeData.value?.copy(
                                heartRate = detail.currentHeartRate,
                                steps = detail.totalSteps,
                                distance = detail.totalDistance,
                                calories = detail.totalHeat.toInt(),
                                bloodPressureSystolic = detail.currentSs.toInt(),
                                bloodPressureDiastolic = detail.currentFz.toInt(),
                                bloodOxygen = detail.currentBloodOxygen.toInt(),
                                temperature = detail.currentTemp.toFloat(),
                                deviceMac = connectedDeviceMac  // Ensure device MAC is set
                            )
                            _realTimeData.value = updated
                            // Save to database directly (no aggregation)
                            if (updated != null) {
                                applicationScope.launch {
                                    healthDataRepository.insertHealthMetric(
                                        com.example.smart_watch_hub.data.models.HealthMetric(
                                            timestamp = updated.timestamp,
                                            heartRate = updated.heartRate,
                                            steps = updated.steps,
                                            calories = updated.calories,
                                            distance = updated.distance,
                                            bloodPressureSystolic = updated.bloodPressureSystolic,
                                            bloodPressureDiastolic = updated.bloodPressureDiastolic,
                                            bloodOxygen = updated.bloodOxygen,
                                            temperature = updated.temperature,
                                            bloodSugar = updated.bloodSugar,
                                            totalSleep = updated.sleepDuration,
                                            deepSleep = 0,
                                            lightSleep = 0,
                                            stress = updated.stress,
                                            met = updated.met,
                                            mai = updated.mai,
                                            isWearing = updated.isWearing,
                                            deviceMac = updated.deviceMac
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("BleRepository", "Error processing manual health detail: ${e.message}", e)
                        }
                    },
                    { error ->
                        Log.e("BleRepository", "Manual health detail error: ${error.message}", error)
                    }
                ).let { disposables.add(it) }
        } catch (e: Exception) {
            Log.e("BleRepository", "Failed to request health data: ${e.message}", e)
        }
    }

    /**
     * Fetch step history from watch and expose via StateFlow for immediate display in charts.
     * Also saves to database asynchronously for persistence.
     */
    fun fetchStepHistory(timestamp: Long) {
        try {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = timestamp
            }
            Log.d("BleRepository", "Requesting step history for date: ${calendar.time}")

            StarmaxBleClient.instance.getStepHistory(calendar)
                .subscribe(
                    { response ->
                        try {
                            if (response.status == 0) {
                                Log.d("BleRepository", "✓ Step history received: ${response.dataLength} data points")

                                // IMMEDIATE: Parse and expose via StateFlow for UI display
                                val stepData = parseStepHistoryResponse(response)
                                _watchHistoryData.value = _watchHistoryData.value.copy(
                                    steps = _watchHistoryData.value.steps + stepData
                                )
                                Log.d("BleRepository", "Watch history cache updated: ${stepData.size} step points added")

                                // BACKGROUND: Save to database for persistence/sync
                                saveStepHistoryToDatabase(response)
                            } else {
                                Log.w("BleRepository", "Step history error status: ${response.status}")
                            }
                        } catch (e: Exception) {
                            Log.e("BleRepository", "Error processing step history: ${e.message}", e)
                        }
                    },
                    { error ->
                        Log.e("BleRepository", "Step history error: ${error.message}", error)
                    }
                ).let { disposables.add(it) }
        } catch (e: Exception) {
            Log.e("BleRepository", "Failed to request step history: ${e.message}", e)
        }
    }

    /**
     * Fetch heart rate history from watch and expose via StateFlow for immediate display.
     * Also saves to database asynchronously for persistence.
     */
    fun fetchHeartRateHistory(timestamp: Long) {
        try {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = timestamp
            }
            Log.d("BleRepository", "Requesting heart rate history for date: ${calendar.time}")

            StarmaxBleClient.instance.getHeartRateHistory(calendar)
                .subscribe(
                    { response ->
                        try {
                            if (response.status == 0) {
                                Log.d("BleRepository", "✓ Heart rate history received: ${response.dataLength} data points")

                                // IMMEDIATE: Parse and expose via StateFlow
                                val hrData = parseHeartRateHistoryResponse(response)
                                _watchHistoryData.value = _watchHistoryData.value.copy(
                                    heartRate = _watchHistoryData.value.heartRate + hrData
                                )
                                Log.d("BleRepository", "Watch history cache updated: ${hrData.size} HR points added")

                                // BACKGROUND: Save to database
                                saveHeartRateHistoryToDatabase(response)
                            } else {
                                Log.w("BleRepository", "Heart rate history error status: ${response.status}")
                            }
                        } catch (e: Exception) {
                            Log.e("BleRepository", "Error processing heart rate history: ${e.message}", e)
                        }
                    },
                    { error ->
                        Log.e("BleRepository", "Heart rate history error: ${error.message}", error)
                    }
                ).let { disposables.add(it) }
        } catch (e: Exception) {
            Log.e("BleRepository", "Failed to request heart rate history: ${e.message}", e)
        }
    }

    /**
     * Fetch blood pressure history from watch and expose via StateFlow for immediate display.
     * Also saves to database asynchronously for persistence.
     */
    fun fetchBloodPressureHistory(timestamp: Long) {
        try {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = timestamp
            }
            Log.d("BleRepository", "Requesting blood pressure history for date: ${calendar.time}")

            StarmaxBleClient.instance.getBloodPressureHistory(calendar)
                .subscribe(
                    { response ->
                        try {
                            if (response.status == 0) {
                                Log.d("BleRepository", "✓ Blood pressure history received: ${response.dataLength} data points")

                                // IMMEDIATE: Parse and expose via StateFlow
                                val bpData = parseBloodPressureHistoryResponse(response)
                                _watchHistoryData.value = _watchHistoryData.value.copy(
                                    bloodPressure = _watchHistoryData.value.bloodPressure + bpData
                                )
                                Log.d("BleRepository", "Watch history cache updated: ${bpData.size} BP points added")

                                // BACKGROUND: Save to database
                                saveBloodPressureHistoryToDatabase(response)
                            } else {
                                Log.w("BleRepository", "Blood pressure history error status: ${response.status}")
                            }
                        } catch (e: Exception) {
                            Log.e("BleRepository", "Error processing blood pressure history: ${e.message}", e)
                        }
                    },
                    { error ->
                        Log.e("BleRepository", "Blood pressure history error: ${error.message}", error)
                    }
                ).let { disposables.add(it) }
        } catch (e: Exception) {
            Log.e("BleRepository", "Failed to request blood pressure history: ${e.message}", e)
        }
    }

    /**
     * Parse step history response into clean data models.
     */
    private fun parseStepHistoryResponse(response: Any): List<StepHistoryPoint> {
        return try {
            val getStepsList = response.javaClass.getMethod("getStepsList")
            val stepsList = getStepsList.invoke(response) as? List<*> ?: return emptyList()

            val year = response.javaClass.getMethod("getYear").invoke(response) as Int
            val month = response.javaClass.getMethod("getMonth").invoke(response) as Int
            val day = response.javaClass.getMethod("getDay").invoke(response) as Int

            stepsList.mapNotNull { stepData ->
                try {
                    val hour = stepData?.javaClass?.getMethod("getHour")?.invoke(stepData) as? Int ?: 0
                    val minute = stepData?.javaClass?.getMethod("getMinute")?.invoke(stepData) as? Int ?: 0
                    val steps = stepData?.javaClass?.getMethod("getSteps")?.invoke(stepData) as? Int ?: 0
                    val calorie = (stepData?.javaClass?.getMethod("getCalorie")?.invoke(stepData) as? Number)?.toInt() ?: 0
                    val distance = (stepData?.javaClass?.getMethod("getDistance")?.invoke(stepData) as? Number)?.toInt() ?: 0

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    StepHistoryPoint(
                        timestamp = calendar.timeInMillis,
                        steps = steps,
                        calories = calorie / 1000,
                        distance = distance / 100
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("BleRepository", "Error parsing step history", e)
            emptyList()
        }
    }

    /**
     * Parse heart rate history response into clean data models.
     */
    private fun parseHeartRateHistoryResponse(response: Any): List<HeartRateHistoryPoint> {
        return try {
            val getDataList = response.javaClass.getMethod("getDataList")
            val dataList = getDataList.invoke(response) as? List<*> ?: return emptyList()

            val year = response.javaClass.getMethod("getYear").invoke(response) as Int
            val month = response.javaClass.getMethod("getMonth").invoke(response) as Int
            val day = response.javaClass.getMethod("getDay").invoke(response) as Int

            dataList.mapNotNull { hrData ->
                try {
                    val hour = hrData?.javaClass?.getMethod("getHour")?.invoke(hrData) as? Int ?: 0
                    val minute = hrData?.javaClass?.getMethod("getMinute")?.invoke(hrData) as? Int ?: 0
                    val value = hrData?.javaClass?.getMethod("getValue")?.invoke(hrData) as? Int ?: 0

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    HeartRateHistoryPoint(
                        timestamp = calendar.timeInMillis,
                        heartRate = value
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("BleRepository", "Error parsing heart rate history", e)
            emptyList()
        }
    }

    /**
     * Parse blood pressure history response into clean data models.
     */
    private fun parseBloodPressureHistoryResponse(response: Any): List<BloodPressureHistoryPoint> {
        return try {
            val getDataList = response.javaClass.getMethod("getDataList")
            val dataList = getDataList.invoke(response) as? List<*> ?: return emptyList()

            val year = response.javaClass.getMethod("getYear").invoke(response) as Int
            val month = response.javaClass.getMethod("getMonth").invoke(response) as Int
            val day = response.javaClass.getMethod("getDay").invoke(response) as Int

            dataList.mapNotNull { bpData ->
                try {
                    val hour = bpData?.javaClass?.getMethod("getHour")?.invoke(bpData) as? Int ?: 0
                    val minute = bpData?.javaClass?.getMethod("getMinute")?.invoke(bpData) as? Int ?: 0
                    val ss = bpData?.javaClass?.getMethod("getSs")?.invoke(bpData) as? Int ?: 0
                    val fz = bpData?.javaClass?.getMethod("getFz")?.invoke(bpData) as? Int ?: 0

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    BloodPressureHistoryPoint(
                        timestamp = calendar.timeInMillis,
                        systolic = ss,
                        diastolic = fz
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("BleRepository", "Error parsing blood pressure history", e)
            emptyList()
        }
    }

    /**
     * Clear watch history data cache.
     */
    fun clearWatchHistoryData() {
        _watchHistoryData.value = WatchHistoryData()
    }

    /**
     * Save step history data to database.
     * Converts each hourly step entry to a HealthMetric.
     */
    private fun saveStepHistoryToDatabase(response: Any) {
        try {
            applicationScope.launch(Dispatchers.IO) {
                try {
                    val stepsList = response::class.java.getMethod("getStepsList").invoke(response) as List<*>
                    val year = response::class.java.getMethod("getYear").invoke(response) as Int
                    val month = response::class.java.getMethod("getMonth").invoke(response) as Int
                    val day = response::class.java.getMethod("getDay").invoke(response) as Int

                    Log.d("BleRepository", "Saving ${stepsList.size} step history records")

                    stepsList.forEach { stepData ->
                        val hour = stepData!!::class.java.getMethod("getHour").invoke(stepData) as Int
                        val minute = stepData!!::class.java.getMethod("getMinute").invoke(stepData) as Int
                        val steps = stepData!!::class.java.getMethod("getSteps").invoke(stepData) as Int
                        val calorie = (stepData!!::class.java.getMethod("getCalorie").invoke(stepData) as Number).toInt() / 1000
                        val distance = (stepData!!::class.java.getMethod("getDistance").invoke(stepData) as Number).toInt() / 100

                        val calendar = Calendar.getInstance().apply {
                            set(year, month - 1, day, hour, minute, 0)
                        }

                        val metric = com.example.smart_watch_hub.data.models.HealthMetric(
                            timestamp = calendar.timeInMillis,
                            heartRate = 0,
                            steps = steps,
                            calories = calorie,
                            distance = distance,
                            bloodPressureSystolic = 0,
                            bloodPressureDiastolic = 0,
                            bloodOxygen = 0,
                            temperature = 0f,
                            bloodSugar = 0f,
                            totalSleep = 0,
                            deepSleep = 0,
                            lightSleep = 0,
                            stress = 0,
                            met = 0f,
                            mai = 0,
                            isWearing = true
                        )
                        healthDataRepository.insertHealthMetric(metric)
                    }
                    Log.d("BleRepository", "✓ Step history saved to database")
                } catch (e: Exception) {
                    Log.e("BleRepository", "Error saving step history: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("BleRepository", "Failed to save step history: ${e.message}", e)
        }
    }

    /**
     * Save heart rate history data to database.
     * Converts each hourly heart rate entry to a HealthMetric.
     */
    private fun saveHeartRateHistoryToDatabase(response: Any) {
        try {
            applicationScope.launch(Dispatchers.IO) {
                try {
                    val dataList = response::class.java.getMethod("getDataList").invoke(response) as List<*>
                    val year = response::class.java.getMethod("getYear").invoke(response) as Int
                    val month = response::class.java.getMethod("getMonth").invoke(response) as Int
                    val day = response::class.java.getMethod("getDay").invoke(response) as Int

                    Log.d("BleRepository", "Saving ${dataList.size} heart rate history records")

                    dataList.forEach { hrData ->
                        val hour = hrData!!::class.java.getMethod("getHour").invoke(hrData) as Int
                        val minute = hrData!!::class.java.getMethod("getMinute").invoke(hrData) as Int
                        val heartRate = hrData!!::class.java.getMethod("getValue").invoke(hrData) as Int

                        val calendar = Calendar.getInstance().apply {
                            set(year, month - 1, day, hour, minute, 0)
                        }

                        val metric = com.example.smart_watch_hub.data.models.HealthMetric(
                            timestamp = calendar.timeInMillis,
                            heartRate = heartRate,
                            steps = 0,
                            calories = 0,
                            distance = 0,
                            bloodPressureSystolic = 0,
                            bloodPressureDiastolic = 0,
                            bloodOxygen = 0,
                            temperature = 0f,
                            bloodSugar = 0f,
                            totalSleep = 0,
                            deepSleep = 0,
                            lightSleep = 0,
                            stress = 0,
                            met = 0f,
                            mai = 0,
                            isWearing = true
                        )
                        healthDataRepository.insertHealthMetric(metric)
                    }
                    Log.d("BleRepository", "✓ Heart rate history saved to database")
                } catch (e: Exception) {
                    Log.e("BleRepository", "Error saving heart rate history: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("BleRepository", "Failed to save heart rate history: ${e.message}", e)
        }
    }

    /**
     * Save blood pressure history data to database.
     * Converts each hourly blood pressure entry to a HealthMetric.
     */
    private fun saveBloodPressureHistoryToDatabase(response: Any) {
        try {
            applicationScope.launch(Dispatchers.IO) {
                try {
                    val dataList = response::class.java.getMethod("getDataList").invoke(response) as List<*>
                    val year = response::class.java.getMethod("getYear").invoke(response) as Int
                    val month = response::class.java.getMethod("getMonth").invoke(response) as Int
                    val day = response::class.java.getMethod("getDay").invoke(response) as Int

                    Log.d("BleRepository", "Saving ${dataList.size} blood pressure history records")

                    dataList.forEach { bpData ->
                        val hour = bpData!!::class.java.getMethod("getHour").invoke(bpData) as Int
                        val minute = bpData!!::class.java.getMethod("getMinute").invoke(bpData) as Int
                        val systolic = bpData!!::class.java.getMethod("getSs").invoke(bpData) as Int
                        val diastolic = bpData!!::class.java.getMethod("getFz").invoke(bpData) as Int

                        val calendar = Calendar.getInstance().apply {
                            set(year, month - 1, day, hour, minute, 0)
                        }

                        val metric = com.example.smart_watch_hub.data.models.HealthMetric(
                            timestamp = calendar.timeInMillis,
                            heartRate = 0,
                            steps = 0,
                            calories = 0,
                            distance = 0,
                            bloodPressureSystolic = systolic,
                            bloodPressureDiastolic = diastolic,
                            bloodOxygen = 0,
                            temperature = 0f,
                            bloodSugar = 0f,
                            totalSleep = 0,
                            deepSleep = 0,
                            lightSleep = 0,
                            stress = 0,
                            met = 0f,
                            mai = 0,
                            isWearing = true
                        )
                        healthDataRepository.insertHealthMetric(metric)
                    }
                    Log.d("BleRepository", "✓ Blood pressure history saved to database")
                } catch (e: Exception) {
                    Log.e("BleRepository", "Error saving blood pressure history: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("BleRepository", "Failed to save blood pressure history: ${e.message}", e)
        }
    }

    /**
     * Handle connection failure - set state to ERROR.
     */
    private fun handleConnectionFailure() {
        _connectionState.value = ConnectionState.ERROR
    }

    /**
     * Load last connected device from Room database for auto-connect.
     * Called during initialization to enable auto-reconnect.
     * Uses application scope for proper lifecycle management.
     */
    private fun loadLastConnectedDevice() {
        try {
            applicationScope.launch(Dispatchers.IO) {
                try {
                    val device = deviceDao.getLastConnectedDevice()
                    if (device != null) {
                        withContext(Dispatchers.Main) {
                            _connectedDevice.value = DeviceInfo.fromEntity(device)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BleRepository", "Failed to load last connected device: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("BleRepository", "Error launching load device coroutine: ${e.message}")
        }
    }


    /**
     * Start batch push of all pending metrics to Azure backend.
     * Automatically splits data into batches and syncs with 5-minute intervals.
     *
     * Example: 20,000 records will be pushed in ~100 batches of 200 records,
     * with 5-minute delays between batches (total ~8 hours)
     *
     * Progress is logged with batch numbers and sync counts.
     */
    fun startBatchPushAllPendingMetrics() {
        applicationScope.launch {
            try {
                Log.d("BleRepository", "Starting batch push of all pending metrics...")
                val result = healthSyncManager.batchPushAllPendingMetrics()
                Log.d(
                    "BleRepository",
                    "Batch push complete: Synced=${result.totalSynced}, Failed=${result.totalFailed}, Batches=${result.totalBatches}"
                )
            } catch (e: Exception) {
                Log.e("BleRepository", "Error during batch push: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 3
    }
}
