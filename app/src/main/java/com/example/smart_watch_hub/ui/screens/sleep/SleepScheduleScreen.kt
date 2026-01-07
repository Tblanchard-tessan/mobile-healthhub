package com.example.smart_watch_hub.ui.screens.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.OptIn
import com.example.smart_watch_hub.ui.theme.Spacing
import com.example.smart_watch_hub.ui.theme.CustomShapes
import com.example.smart_watch_hub.ui.theme.SleepDeepPurple
import com.example.smart_watch_hub.ui.theme.SleepLightPurple
import com.example.smart_watch_hub.ui.animations.SlideUpFadeIn
import com.example.smart_watch_hub.ui.animations.StaggeredCardAnimation

/**
 * Sleep Schedule Screen for viewing and managing sleep settings.
 *
 * Features:
 * - Display current sleep duration and quality
 * - Sleep score indicator
 * - Deep sleep vs light sleep breakdown
 * - Sleep schedule settings (bed time and wake time)
 * - Enable/disable sleep monitoring
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScheduleScreen(
    viewModel: SleepScheduleViewModel = viewModel()
) {
    val sleepDuration by viewModel.sleepDuration.collectAsState()
    val deepSleep by viewModel.deepSleep.collectAsState()
    val lightSleep by viewModel.lightSleep.collectAsState()
    val sleepScore by viewModel.sleepScore.collectAsState()
    val bedTime by viewModel.bedTime.collectAsState()
    val wakeTime by viewModel.wakeTime.collectAsState()
    val isScheduleEnabled by viewModel.isScheduleEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sleep Schedule") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sleep Score Card
            SleepScoreCard(
                score = sleepScore,
                duration = viewModel.getFormattedSleepDuration()
            )

            // Sleep Duration Breakdown
            SleepBreakdownCard(
                totalDuration = sleepDuration,
                deepSleep = deepSleep,
                lightSleep = lightSleep
            )

            // Sleep Schedule Settings
            SleepScheduleCard(
                bedTime = viewModel.getFormattedBedTime(),
                wakeTime = viewModel.getFormattedWakeTime(),
                isEnabled = isScheduleEnabled,
                onToggle = { viewModel.toggleSchedule() },
                onBedTimeClick = {
                    // TODO: Show time picker dialog
                },
                onWakeTimeClick = {
                    // TODO: Show time picker dialog
                }
            )

            // Sleep Tips
            SleepTipsCard()
        }
    }
}

/**
 * Card displaying sleep score and quality indicator.
 */
@Composable
private fun SleepScoreCard(
    score: Int,
    duration: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Sleep",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "Sleep Score",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$score/100",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Duration: $duration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val qualityText = when {
                score >= 80 -> "Excellent sleep"
                score >= 60 -> "Good sleep"
                score >= 40 -> "Fair sleep"
                else -> "Poor sleep"
            }
            Text(
                text = qualityText,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    score >= 80 -> MaterialTheme.colorScheme.primary
                    score >= 60 -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

/**
 * Card showing deep sleep vs light sleep breakdown.
 */
@Composable
private fun SleepBreakdownCard(
    totalDuration: Int,
    deepSleep: Int,
    lightSleep: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sleep Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Deep Sleep
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Deep Sleep",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "$deepSleep min",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            LinearProgressIndicator(
                progress = if (totalDuration > 0) deepSleep.toFloat() / totalDuration else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )

            // Light Sleep
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Light Sleep",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "$lightSleep min",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            LinearProgressIndicator(
                progress = if (totalDuration > 0) lightSleep.toFloat() / totalDuration else 0f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * Card for managing sleep schedule settings.
 */
@Composable
private fun SleepScheduleCard(
    bedTime: String,
    wakeTime: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    onBedTimeClick: () -> Unit,
    onWakeTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sleep Monitoring",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }

            if (isEnabled) {
                // Bed Time
                Button(
                    onClick = onBedTimeClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bed Time: $bedTime")
                }

                // Wake Time
                Button(
                    onClick = onWakeTimeClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Wake Time: $wakeTime")
                }

                Text(
                    text = "Adjust sleep times to optimize monitoring accuracy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Sleep monitoring is disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Card with sleep improvement tips.
 */
@Composable
private fun SleepTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💡 Sleep Tips",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            val tips = listOf(
                "Maintain consistent sleep schedule",
                "Avoid screens 1 hour before bed",
                "Keep bedroom cool and dark",
                "Aim for 7-9 hours of sleep",
                "Exercise regularly but not before bed"
            )

            tips.forEach { tip ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
