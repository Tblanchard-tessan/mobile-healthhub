package com.example.smart_watch_hub.ui.screens.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smart_watch_hub.data.models.HealthMetric
import com.example.smart_watch_hub.data.models.StepHistoryPoint
import com.example.smart_watch_hub.data.models.HeartRateHistoryPoint
import com.example.smart_watch_hub.data.models.BloodPressureHistoryPoint
import com.example.smart_watch_hub.data.repository.BleRepository
import com.example.smart_watch_hub.data.repository.HealthConnectRepository
import com.example.smart_watch_hub.data.repository.HealthDataRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

/**
 * ViewModel for History screen.
 *
 * Responsibilities:
 * - Load historical health data by time range
 * - Support 3 time ranges: 6 hours, 1 day, 1 week
 * - Fetch data from both Room database and Health Connect
 * - Merge data from both sources (HC data takes precedence if newer)
 * - Provide data for chart display
 * - Handle errors gracefully
 */
class HistoryViewModel : ViewModel(), KoinComponent {
    private val healthDataRepository: HealthDataRepository by inject()
    private val healthConnectRepository: HealthConnectRepository by inject()
    private val bleRepository: BleRepository by inject()

    // UI State Flows
    private val _selectedTimeRange = MutableStateFlow(TimeRange.ONE_DAY)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    private val _historicalMetrics = MutableStateFlow<List<HealthMetric>>(emptyList())
    val historicalMetrics: StateFlow<List<HealthMetric>> = _historicalMetrics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Chart aggregation data
    private val _heartRateData = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val heartRateData: StateFlow<List<Pair<String, Double>>> = _heartRateData.asStateFlow()

    private val _stepsData = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val stepsData: StateFlow<List<Pair<String, Int>>> = _stepsData.asStateFlow()

    private val _bloodPressureData = MutableStateFlow<List<BloodPressurePoint>>(emptyList())
    val bloodPressureData: StateFlow<List<BloodPressurePoint>> = _bloodPressureData.asStateFlow()

    private val _caloriesData = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val caloriesData: StateFlow<List<Pair<String, Int>>> = _caloriesData.asStateFlow()

    // Statistics
    private val _statistics = MutableStateFlow<HealthStatistics?>(null)
    val statistics: StateFlow<HealthStatistics?> = _statistics.asStateFlow()

    init {
        // Observe watch history data from BleRepository
        observeWatchHistoryData()

        // Fetch fresh data from watch on startup
        viewModelScope.launch {
            fetchWatchHistoryData()
        }
    }

    /**
     * Observe watch history data directly from BleRepository.
     * Updates charts immediately as data is fetched from watch.
     */
    private fun observeWatchHistoryData() {
        viewModelScope.launch {
            combine(
                bleRepository.watchHistoryData,
                _selectedTimeRange
            ) { watchData, timeRange ->
                Pair(watchData, timeRange)
            }.collect { (watchData, timeRange) ->
                if (!watchData.isEmpty()) {
                    Log.d("HistoryViewModel", "Watch data received: ${watchData.steps.size} steps, ${watchData.heartRate.size} HR, ${watchData.bloodPressure.size} BP")

                    // Filter by time range
                    val now = System.currentTimeMillis()
                    val startTime = now - timeRange.durationMs

                    val filteredSteps = watchData.steps.filter { it.timestamp >= startTime }
                    val filteredHR = watchData.heartRate.filter { it.timestamp >= startTime }
                    val filteredBP = watchData.bloodPressure.filter { it.timestamp >= startTime }

                    // Prepare chart data directly from watch data
                    prepareChartDataFromWatch(filteredSteps, filteredHR, filteredBP)

                    // Calculate statistics
                    calculateStatisticsFromWatch(filteredSteps, filteredHR, filteredBP)

                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Change time range and reload data.
     * Clears watch cache and fetches fresh data from watch.
     */
    fun setTimeRange(range: TimeRange) {
        viewModelScope.launch {
            _selectedTimeRange.value = range
            Log.d("HistoryViewModel", "Time range changed to: ${range.name}")

            _isLoading.value = true

            // Clear old watch data and fetch new data for this time range
            bleRepository.clearWatchHistoryData()
            fetchWatchHistoryData()
        }
    }


    /**
     * Fetch historical data from connected Starmax watch.
     * Retrieves step, heart rate, and blood pressure history for all days in the selected time range.
     * Data is immediately available via StateFlows without waiting for database saves.
     */
    private fun fetchWatchHistoryData() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val startTime = now - _selectedTimeRange.value.durationMs

                val startCalendar = Calendar.getInstance().apply { timeInMillis = startTime }
                val endCalendar = Calendar.getInstance().apply { timeInMillis = now }

                Log.d("HistoryViewModel", "Fetching watch history from ${startCalendar.time} to ${endCalendar.time}")

                // Fetch data for each day in the range
                var currentCalendar = Calendar.getInstance().apply { timeInMillis = startTime }

                while (currentCalendar.timeInMillis < now) {
                    val dateStr = String.format("%04d-%02d-%02d",
                        currentCalendar.get(Calendar.YEAR),
                        currentCalendar.get(Calendar.MONTH) + 1,
                        currentCalendar.get(Calendar.DAY_OF_MONTH)
                    )
                    Log.d("HistoryViewModel", "Fetching watch data for: $dateStr")

                    // Fetch all three metrics for this day
                    // Data will be immediately available via StateFlows
                    bleRepository.fetchStepHistory(currentCalendar.timeInMillis)
                    bleRepository.fetchHeartRateHistory(currentCalendar.timeInMillis)
                    bleRepository.fetchBloodPressureHistory(currentCalendar.timeInMillis)

                    // Small delay between days to avoid overwhelming the SDK
                    delay(300)

                    // Move to next day
                    currentCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                Log.d("HistoryViewModel", "Watch history fetch initiated for all days")

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error fetching watch history: ${e.message}", e)
                _errorMessage.value = "Failed to fetch watch data: ${e.message}"
                _isLoading.value = false
            }
        }
    }


    /**
     * Prepare chart data directly from watch history data.
     * No database involved - shows data exactly as fetched from watch.
     */
    private fun prepareChartDataFromWatch(
        steps: List<StepHistoryPoint>,
        heartRate: List<HeartRateHistoryPoint>,
        bloodPressure: List<BloodPressureHistoryPoint>
    ) {
        try {
            Log.d("HistoryViewModel", "Preparing charts from watch data: ${steps.size} steps, ${heartRate.size} HR, ${bloodPressure.size} BP")

            when (_selectedTimeRange.value) {
                TimeRange.ONE_DAY -> prepareChartDataForDay(steps, heartRate, bloodPressure)
                TimeRange.ONE_WEEK -> prepareChartDataForWeek(steps, heartRate, bloodPressure)
            }

            Log.d("HistoryViewModel", "Charts prepared: HR=${_heartRateData.value.size}, Steps=${_stepsData.value.size}, BP=${_bloodPressureData.value.size}")
        } catch (e: Exception) {
            Log.e("HistoryViewModel", "Error preparing charts from watch", e)
            _errorMessage.value = "Error displaying watch data: ${e.message}"
        }
    }

    /**
     * Prepare chart data for ONE_DAY mode from watch data.
     */
    private fun prepareChartDataForDay(
        steps: List<StepHistoryPoint>,
        heartRate: List<HeartRateHistoryPoint>,
        bloodPressure: List<BloodPressureHistoryPoint>
    ) {
        // Create 24 hourly time slots
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)

        val dayStart = Calendar.getInstance().apply {
            timeInMillis = oneDayAgo
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val hourlySlots = (0..23).map { hour ->
            val slotCalendar = Calendar.getInstance().apply {
                timeInMillis = dayStart.timeInMillis
                add(Calendar.HOUR_OF_DAY, hour)
            }
            val startHour = slotCalendar.timeInMillis
            val endHour = startHour + 60 * 60 * 1000

            val label = String.format("%02d:00", slotCalendar.get(Calendar.HOUR_OF_DAY))
            Triple(label, startHour, endHour)
        }

        // Heart Rate - first reading per hour
        _heartRateData.value = hourlySlots.map { (label, start, end) ->
            val hrPoints = heartRate.filter { it.timestamp in start..<end }
            val value = hrPoints.firstOrNull()?.heartRate?.toDouble() ?: 0.0
            label to value
        }

        // Steps - sum per hour
        _stepsData.value = hourlySlots.map { (label, start, end) ->
            val stepPoints = steps.filter { it.timestamp in start..<end }
            val value = stepPoints.sumOf { it.steps }
            label to value
        }

        // Blood Pressure - first reading per hour
        _bloodPressureData.value = hourlySlots.map { (label, start, end) ->
            val bpPoints = bloodPressure.filter { it.timestamp in start..<end }
            val first = bpPoints.firstOrNull()
            BloodPressurePoint(
                label,
                first?.systolic ?: 0,
                first?.diastolic ?: 0
            )
        }

        // Calories - sum per hour
        _caloriesData.value = hourlySlots.map { (label, start, end) ->
            val stepPoints = steps.filter { it.timestamp in start..<end }
            val value = stepPoints.sumOf { it.calories }
            label to value
        }
    }

    /**
     * Prepare chart data for ONE_WEEK mode from watch data.
     */
    private fun prepareChartDataForWeek(
        steps: List<StepHistoryPoint>,
        heartRate: List<HeartRateHistoryPoint>,
        bloodPressure: List<BloodPressureHistoryPoint>
    ) {
        // Create 168 hourly time slots (7 days × 24 hours)
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000)

        val startOfPeriod = Calendar.getInstance().apply {
            timeInMillis = sevenDaysAgo
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val hourlySlots = (0..167).map { hourOffset ->
            val slotCalendar = Calendar.getInstance().apply {
                timeInMillis = startOfPeriod.timeInMillis
                add(Calendar.HOUR_OF_DAY, hourOffset)
            }
            val startHour = slotCalendar.timeInMillis
            val endHour = startHour + 60 * 60 * 1000

            val dayOfWeek = slotCalendar.getDisplayName(
                Calendar.DAY_OF_WEEK,
                Calendar.SHORT,
                java.util.Locale.US
            ) ?: ""
            val hour = slotCalendar.get(Calendar.HOUR_OF_DAY)
            val label = String.format("%s %02d:00", dayOfWeek, hour)

            Triple(label, startHour, endHour)
        }

        // Heart Rate - first reading per hour
        _heartRateData.value = hourlySlots.map { (label, start, end) ->
            val hrPoints = heartRate.filter { it.timestamp in start..<end }
            val value = hrPoints.firstOrNull()?.heartRate?.toDouble() ?: 0.0
            label to value
        }

        // Steps - sum per hour
        _stepsData.value = hourlySlots.map { (label, start, end) ->
            val stepPoints = steps.filter { it.timestamp in start..<end }
            val value = stepPoints.sumOf { it.steps }
            label to value
        }

        // Blood Pressure - first reading per hour
        _bloodPressureData.value = hourlySlots.map { (label, start, end) ->
            val bpPoints = bloodPressure.filter { it.timestamp in start..<end }
            val first = bpPoints.firstOrNull()
            BloodPressurePoint(
                label,
                first?.systolic ?: 0,
                first?.diastolic ?: 0
            )
        }

        // Calories - sum per hour
        _caloriesData.value = hourlySlots.map { (label, start, end) ->
            val stepPoints = steps.filter { it.timestamp in start..<end }
            val value = stepPoints.sumOf { it.calories }
            label to value
        }
    }

    /**
     * Calculate statistics directly from watch data.
     */
    private fun calculateStatisticsFromWatch(
        steps: List<StepHistoryPoint>,
        heartRate: List<HeartRateHistoryPoint>,
        bloodPressure: List<BloodPressureHistoryPoint>
    ) {
        try {
            if (heartRate.isEmpty() && steps.isEmpty()) {
                _statistics.value = null
                return
            }

            val avgHeartRate = if (heartRate.isNotEmpty()) {
                heartRate.map { it.heartRate }.average().toInt()
            } else 0

            val totalSteps = steps.sumOf { it.steps }
            val totalCalories = steps.sumOf { it.calories }
            val totalDistance = steps.sumOf { it.distance }

            _statistics.value = HealthStatistics(
                avgHeartRate = avgHeartRate,
                totalSteps = totalSteps,
                totalCalories = totalCalories,
                totalDistance = totalDistance,
                avgBloodOxygen = 0,  // Not in watch history
                dataPointCount = heartRate.size + steps.size + bloodPressure.size
            )
        } catch (e: Exception) {
            Log.e("HistoryViewModel", "Error calculating statistics", e)
        }
    }

    /**
     * Format timestamp to appropriate label based on time range.
     * For raw data: shows HH:mm for granular time labels.
     */
    private fun formatTimeLabel(timestamp: Long, range: TimeRange): String {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (range) {
            TimeRange.ONE_DAY -> {
                // Show HH:mm for individual data points
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val minute = calendar.get(java.util.Calendar.MINUTE)
                String.format("%02d:%02d", hour, minute)
            }
            TimeRange.ONE_WEEK -> {
                // Show day and hour
                val dayOfWeek = calendar.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SHORT, java.util.Locale.US)
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                String.format("%s %02d:00", dayOfWeek ?: "", hour)
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
     * Time range options for historical data.
     */
    enum class TimeRange(val durationMs: Long) {
        ONE_DAY(24 * 60 * 60 * 1000),
        ONE_WEEK(7 * 24 * 60 * 60 * 1000)
    }

    /**
     * Blood pressure data point.
     */
    data class BloodPressurePoint(
        val label: String,
        val systolic: Int,
        val diastolic: Int
    )

    /**
     * Overall health statistics for time range.
     */
    data class HealthStatistics(
        val avgHeartRate: Int,
        val totalSteps: Int,
        val totalCalories: Int,
        val totalDistance: Int,
        val avgBloodOxygen: Int,
        val dataPointCount: Int
    )
}
