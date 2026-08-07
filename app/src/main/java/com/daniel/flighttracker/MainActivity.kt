package com.daniel.flighttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.daniel.flighttracker.data.SettingsRepository
import com.daniel.flighttracker.data.ThemeMode
import com.daniel.flighttracker.ui.FlightMapScreen
import com.daniel.flighttracker.ui.SettingsScreen
import com.daniel.flighttracker.ui.theme.FlightTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlightTrackerRoot()
        }
    }
}

@Composable
private fun FlightTrackerRoot() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    var settings by remember { mutableStateOf(repo.load()) }

    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    FlightTrackerTheme(
        darkTheme = darkTheme,
        appColor = settings.appColor,
        customAppColor = settings.customAppColor,
        textColorMode = settings.textColorMode,
        customTextColor = settings.customTextColor
    ) {
        var showSettings by remember { mutableStateOf(false) }

        BackHandler(enabled = showSettings) {
            showSettings = false
        }

        if (showSettings) {
            SettingsScreen(
                settings = settings,
                onSettingsChange = { new ->
                    settings = new
                    repo.save(new)
                },
                onBack = { showSettings = false }
            )
        } else {
            FlightMapScreen(
                settings = settings,
                onOpenSettings = { showSettings = true }
            )
        }
    }
}
