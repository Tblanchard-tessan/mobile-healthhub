package com.example.smart_watch_hub.ui.screens.history.components

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.animation.Easing

/**
 * Chart styling and animation utilities for health metrics visualization.
 */
object ChartUtils {

    // Animation durations
    const val CHART_ANIMATION_DURATION = 800
    const val CHART_FADE_IN_DURATION = 600

    // Chart colors (health-themed)
    object ChartColors {
        val heartRateColor = AndroidColor.rgb(229, 57, 53)        // Red
        val heartRateGradientStart = AndroidColor.argb(180, 229, 57, 53)
        val heartRateGradientEnd = AndroidColor.argb(30, 229, 57, 53)

        val bpSystolicColor = AndroidColor.rgb(25, 118, 210)      // Blue
        val bpSystolicGradientStart = AndroidColor.argb(180, 25, 118, 210)
        val bpSystolicGradientEnd = AndroidColor.argb(30, 25, 118, 210)

        val bpDiastolicColor = AndroidColor.rgb(56, 142, 60)      // Green
        val bpDiastolicGradientStart = AndroidColor.argb(180, 56, 142, 60)
        val bpDiastolicGradientEnd = AndroidColor.argb(30, 56, 142, 60)

        val stepsColor = AndroidColor.rgb(67, 160, 71)            // Green
        val stepsGradientStart = AndroidColor.argb(200, 67, 160, 71)
        val stepsGradientEnd = AndroidColor.argb(40, 67, 160, 71)

        val caloriesColor = AndroidColor.rgb(255, 111, 0)         // Orange
        val caloriesGradientStart = AndroidColor.argb(200, 255, 111, 0)
        val caloriesGradientEnd = AndroidColor.argb(40, 255, 111, 0)

        val gridColor = AndroidColor.argb(50, 255, 255, 255)
        val axisTextColor = AndroidColor.rgb(200, 200, 200)
        val legendTextColor = AndroidColor.WHITE
    }

    /**
     * Apply common styling to line charts.
     */
    fun styleLineChart(
        chart: LineChart,
        enableGrid: Boolean = true,
        enableLegend: Boolean = true
    ) {
        chart.apply {
            // Description
            description.isEnabled = false

            // Legend
            legend.apply {
                isEnabled = enableLegend
                textColor = ChartColors.legendTextColor
                textSize = 12f
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
                formSize = 12f
            }

            // Interaction
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            isDoubleTapToZoomEnabled = false

            // Grid & Borders
            setDrawGridBackground(false)
            setDrawBorders(false)

            // Axis styling
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = ChartColors.axisTextColor
                textSize = 10f
                setDrawGridLines(enableGrid)
                gridColor = ChartColors.gridColor
                gridLineWidth = 0.5f
                setLabelCount(6, false)
                granularity = 1f
                setAvoidFirstLastClipping(true)
            }

            axisLeft.apply {
                textColor = ChartColors.axisTextColor
                textSize = 10f
                setDrawGridLines(enableGrid)
                gridColor = ChartColors.gridColor
                gridLineWidth = 0.5f
                setDrawAxisLine(false)
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            }

            axisRight.isEnabled = false

            // Margins
            setExtraOffsets(8f, 8f, 8f, 8f)
        }
    }

    /**
     * Apply common styling to bar charts.
     */
    fun styleBarChart(
        chart: BarChart,
        enableGrid: Boolean = true,
        enableLegend: Boolean = false
    ) {
        chart.apply {
            // Description
            description.isEnabled = false

            // Legend
            legend.isEnabled = enableLegend

            // Interaction
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false

            // Grid & Borders
            setDrawGridBackground(false)
            setDrawBorders(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(false)

            // Axis styling
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = ChartColors.axisTextColor
                textSize = 10f
                setDrawGridLines(false)
                setLabelCount(6, false)
                granularity = 1f
                setAvoidFirstLastClipping(true)
            }

            axisLeft.apply {
                textColor = ChartColors.axisTextColor
                textSize = 10f
                setDrawGridLines(enableGrid)
                gridColor = ChartColors.gridColor
                gridLineWidth = 0.5f
                setDrawAxisLine(false)
                axisMinimum = 0f
            }

            axisRight.isEnabled = false

            // Margins
            setExtraOffsets(8f, 8f, 8f, 8f)
        }
    }

    /**
     * Animate chart with smooth easing.
     */
    fun animateChart(chart: LineChart) {
        chart.animateXY(
            CHART_ANIMATION_DURATION,
            CHART_ANIMATION_DURATION,
            Easing.EaseInOutCubic
        )
    }

    fun animateChart(chart: BarChart) {
        chart.animateY(
            CHART_ANIMATION_DURATION,
            Easing.EaseInOutCubic
        )
    }

    /**
     * Create gradient drawable for line chart fill.
     */
    fun createGradientDrawable(startColor: Int, endColor: Int): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, endColor)
        )
    }
}
