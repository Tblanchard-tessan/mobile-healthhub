package com.example.smart_watch_hub.ui.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.OptIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smart_watch_hub.ui.theme.Smart_Watch_HUBTheme

/**
 * Activity shown to explain Health Connect permissions to the user.
 * Used on Android 12-13 for Health Connect permission rationale.
 */
@OptIn(ExperimentalMaterial3Api::class)
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Smart_Watch_HUBTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Health Connect Permissions") },
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Health Connect Access",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "This app uses Health Connect to securely access your health data. " +
                            "You can manage these permissions in your phone's settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { finish() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}
