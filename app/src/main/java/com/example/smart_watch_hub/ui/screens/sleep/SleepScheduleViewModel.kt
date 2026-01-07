package com.example.smart_watch_hub.ui.screens.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smart_watch_hub.data.models.RealTimeHealthData
import com.example.smart_watch_hub.data.repository.BleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalTime

/**
 * ViewModel for Sleep Schedule screen.
 *
 * Responsibilities:
 * - Display current sleep data
 * - Show sleep schedule settings
 * - Allow configuration of sleep time and wake time
 * - Display sleep duration and quality metrics
 */
class SleepScheduleViewModel : ViewModel(), KoinComponent {
    private val bleRepository: BleRepository by inject()

    // Sleep metrics
    private val _sleepDuration = MutableStateFlow(0) // minutes
    val sleepDuration: StateFlow<Int> = _sleepDuration.asStateFlow()

    private val _deepSleep = MutableStateFlow(0) // minutes
    val deepSleep: StateFlow<Int> = _deepSleep.asStateFlow()

    private val _lightSleep = MutableStateFlow(0) // minutes
    val lightSleep: StateFlow<Int> = _lightSleep.asStateFlow()

    private val _sleepScore = MutableStateFlow(0) // 0-100
    val sleepScore: StateFlow<Int> = _sleepScore.asStateFlow()

    // Sleep schedule settings
    private val _bedTime = MutableStateFlow(LocalTime.of(22, 30))
    val bedTime: StateFlow<LocalTime> = _bedTime.asStateFlow()

    private val _wakeTime = MutableStateFlow(LocalTime.of(7, 0))
    val wakeTime: StateFlow<LocalTime> = _wakeTime.asStateFlow()

    private val _isScheduleEnabled = MutableStateFlow(true)
    val isScheduleEnabled: StateFlow<Boolean> = _isScheduleEnabled.asStateFlow()

    init {
        observeHealthData()
    }

    /**
     * Observe real-time health data to update sleep metrics.
     */
    private fun observeHealthData() {
        viewModelScope.launch {
            bleRepository.realTimeData.collect { data ->
                if (data != null) {
                    updateSleepMetrics(data)
                }
            }
        }
    }

    /**
     * Update sleep metrics from health data.
     */
    private fun updateSleepMetrics(data: RealTimeHealthData) {
        _sleepDuration.value = data.sleepDuration

        // Calculate deep/light sleep based on typical 30/70 ratio
        val deepSleepPercent = (_sleepDuration.value * 0.3).toInt()
        val lightSleepPercent = (_sleepDuration.value * 0.7).toInt()

        _deepSleep.value = deepSleepPercent
        _lightSleep.value = lightSleepPercent

        // Calculate sleep score (0-100) based on duration
        _sleepScore.value = when {
            _sleepDuration.value >= 480 -> 100  // 8+ hours = perfect
            _sleepDuration.value >= 420 -> 90   // 7+ hours = very good
            _sleepDuration.value >= 360 -> 80   // 6+ hours = good
            _sleepDuration.value >= 300 -> 60   // 5+ hours = fair
            else -> 40                          // Less than 5 hours = poor
        }
    }

    /**
     * Update bed time.
     */
    fun setBedTime(hour: Int, minute: Int) {
        _bedTime.value = LocalTime.of(hour, minute)
        syncSleepScheduleToWatch()
    }

    /**
     * Update wake time.
     */
    fun setWakeTime(hour: Int, minute: Int) {
        _wakeTime.value = LocalTime.of(hour, minute)
        syncSleepScheduleToWatch()
    }

    /**
     * Toggle sleep schedule.
     */
    fun toggleSchedule() {
        _isScheduleEnabled.value = !_isScheduleEnabled.value
        syncSleepScheduleToWatch()
    }

    /**
     * Sync sleep schedule to watch via BLE SDK.
     */
    private fun syncSleepScheduleToWatch() {
        // TODO: Implement SDK call to set sleep schedule on watch
        // StarmaxBleClient.instance.setSleepSchedule(...)
    }

    /**
     * Get formatted bed time string.
     */
    fun getFormattedBedTime(): String {
        val time = _bedTime.value
        return String.format("%02d:%02d", time.hour, time.minute)
    }

    /**
     * Get formatted wake time string.
     */
    fun getFormattedWakeTime(): String {
        val time = _wakeTime.value
        return String.format("%02d:%02d", time.hour, time.minute)
    }

    /**
     * Get sleep duration formatted as "H hours M minutes".
     */
    fun getFormattedSleepDuration(): String {
        val hours = _sleepDuration.value / 60
        val minutes = _sleepDuration.value % 60
        return when {
            hours > 0 -> "$hours h ${minutes} m"
            else -> "$minutes m"
        }
    }
}
