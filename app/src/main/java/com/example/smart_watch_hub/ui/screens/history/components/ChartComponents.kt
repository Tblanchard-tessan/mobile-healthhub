package com.example.smart_watch_hub.ui.screens.history.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.example.smart_watch_hub.ui.theme.Spacing
import com.example.smart_watch_hub.ui.theme.CustomShapes

/**
 * Heart Rate Chart using MPAndroidChart.
 * Displays heart rate trends as a line chart with one point per hour.
 */
@Composable
fun HeartRateChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    ChartCard(
        title = "Heart Rate",
        modifier = modifier
    ) {
        AndroidView(
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = true
                    legend.textColor = android.graphics.Color.WHITE
                    setTouchEnabled(true)
                    isDragEnabled = true
                    setScaleEnabled(true)
                }
            },
            update = { chart ->
                updateHeartRateChart(chart, data)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

/**
 * Blood Pressure Chart using MPAndroidChart.
 * Displays systolic and diastolic pressure as dual-line chart.
 */
@Composable
fun BloodPressureChart(
    data: List<com.example.smart_watch_hub.ui.screens.history.HistoryViewModel.BloodPressurePoint>,
    modifier: Modifier = Modifier
) {
    ChartCard(
        title = "Blood Pressure",
        modifier = modifier
    ) {
        AndroidView(
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = true
                    legend.textColor = android.graphics.Color.WHITE
                    setTouchEnabled(true)
                    isDragEnabled = true
                    setScaleEnabled(true)
                }
            },
            update = { chart ->
                updateBloodPressureChart(chart, data)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

/**
 * Steps Chart using MPAndroidChart.
 * Displays steps as a bar chart.
 */
@Composable
fun StepsChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    ChartCard(
        title = "Steps",
        modifier = modifier
    ) {
        AndroidView(
            factory = { context ->
                BarChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = true
                    legend.textColor = android.graphics.Color.WHITE
                    setTouchEnabled(true)
                    isDragEnabled = true
                    setScaleEnabled(true)
                }
            },
            update = { chart ->
                updateStepsChart(chart, data)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

/**
 * Calories Chart using MPAndroidChart.
 * Displays calories as a bar chart.
 */
@Composable
fun CaloriesChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    ChartCard(
        title = "Calories",
        modifier = modifier
    ) {
        AndroidView(
            factory = { context ->
                BarChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = true
                    legend.textColor = android.graphics.Color.WHITE
                    setTouchEnabled(true)
                    isDragEnabled = true
                    setScaleEnabled(true)
                }
            },
            update = { chart ->
                updateCaloriesChart(chart, data)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

/**
 * Base card for all charts.
 */
@Composable
private fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = CustomShapes.ChartCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

/**
 * Update heart rate chart with simple data mapping.
 * One data point per hour from watch.
 */
private fun updateHeartRateChart(
    chart: LineChart,
    data: List<Pair<String, Double>>
) {
    if (data.isEmpty()) {
        android.util.Log.d("ChartComponents", "Heart rate data is empty")
        return
    }

    android.util.Log.d("ChartComponents", "Updating HR chart with ${data.size} points")

    val entries = data.mapIndexed { index, (_, value) ->
        Entry(index.toFloat(), value.toFloat())
    }

    val dataSet = LineDataSet(entries, "Heart Rate (bpm)").apply {
        color = android.graphics.Color.parseColor("#E53935")
        lineWidth = 2f
        setCircleColor(android.graphics.Color.parseColor("#E53935"))
        circleRadius = 4f
        setDrawValues(false)
    }

    chart.data = LineData(dataSet)
    chart.legend.apply {
        textColor = android.graphics.Color.WHITE
        textSize = 12f
    }
    chart.xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        textColor = android.graphics.Color.WHITE
        valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value.toInt() < data.size) {
                    data[value.toInt()].first
                } else ""
            }
        }
    }

    chart.notifyDataSetChanged()
    chart.invalidate()
    android.util.Log.d("ChartComponents", "HR chart invalidated")
}

/**
 * Update blood pressure chart with systolic and diastolic data.
 */
private fun updateBloodPressureChart(
    chart: LineChart,
    data: List<com.example.smart_watch_hub.ui.screens.history.HistoryViewModel.BloodPressurePoint>
) {
    if (data.isEmpty()) return

    val systolicEntries = data.mapIndexed { index, point ->
        Entry(index.toFloat(), point.systolic.toFloat())
    }

    val diastolicEntries = data.mapIndexed { index, point ->
        Entry(index.toFloat(), point.diastolic.toFloat())
    }

    val systolicSet = LineDataSet(systolicEntries, "Systolic").apply {
        color = android.graphics.Color.parseColor("#1976D2")
        lineWidth = 2f
        setCircleColor(android.graphics.Color.parseColor("#1976D2"))
        circleRadius = 4f
        setDrawValues(false)
    }

    val diastolicSet = LineDataSet(diastolicEntries, "Diastolic").apply {
        color = android.graphics.Color.parseColor("#43A047")
        lineWidth = 2f
        setCircleColor(android.graphics.Color.parseColor("#43A047"))
        circleRadius = 4f
        setDrawValues(false)
    }

    chart.data = LineData(systolicSet, diastolicSet)
    chart.legend.apply {
        textColor = android.graphics.Color.WHITE
        textSize = 12f
    }
    chart.xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        textColor = android.graphics.Color.WHITE
        valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value.toInt() < data.size) {
                    data[value.toInt()].label
                } else ""
            }
        }
    }

    chart.invalidate()
}

/**
 * Update steps chart with simple bar data.
 */
private fun updateStepsChart(
    chart: BarChart,
    data: List<Pair<String, Int>>
) {
    if (data.isEmpty()) return

    val entries = data.mapIndexed { index, (_, value) ->
        BarEntry(index.toFloat(), value.toFloat())
    }

    val dataSet = BarDataSet(entries, "Steps").apply {
        color = android.graphics.Color.parseColor("#43A047")
        setDrawValues(false)
    }

    chart.data = BarData(dataSet).apply {
        barWidth = 0.9f
    }

    chart.legend.apply {
        textColor = android.graphics.Color.WHITE
        textSize = 12f
    }
    chart.xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        textColor = android.graphics.Color.WHITE
        valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value.toInt() < data.size) {
                    data[value.toInt()].first
                } else ""
            }
        }
    }

    chart.invalidate()
}

/**
 * Update calories chart with simple bar data.
 */
private fun updateCaloriesChart(
    chart: BarChart,
    data: List<Pair<String, Int>>
) {
    if (data.isEmpty()) return

    val entries = data.mapIndexed { index, (_, value) ->
        BarEntry(index.toFloat(), value.toFloat())
    }

    val dataSet = BarDataSet(entries, "Calories").apply {
        color = android.graphics.Color.parseColor("#FF6F00")
        setDrawValues(false)
    }

    chart.data = BarData(dataSet).apply {
        barWidth = 0.9f
    }

    chart.legend.apply {
        textColor = android.graphics.Color.WHITE
        textSize = 12f
    }
    chart.xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        textColor = android.graphics.Color.WHITE
        valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value.toInt() < data.size) {
                    data[value.toInt()].first
                } else ""
            }
        }
    }

    chart.invalidate()
}
