package com.daniel.flighttracker.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.daniel.flighttracker.data.SettingsRepository
import com.daniel.flighttracker.data.ThemeMode
import kotlinx.coroutines.delay
import java.awt.Dimension
import java.awt.Frame

fun main() = application {
    val screen = java.awt.Toolkit.getDefaultToolkit().screenSize
    val widthPx = screen.width
    val heightPx = screen.height

    Window(
        onCloseRequest = ::exitApplication,
        title = "Flight Tracker",
        state = rememberWindowState(
            size = DpSize(widthPx.dp, heightPx.dp),
            position = WindowPosition(Alignment.Center)
        )
    ) {
        window.minimumSize = Dimension(720, 480)
        val density = LocalDensity.current.density
        LaunchedEffect(Unit) {
            delay(100)
            window.setBounds(0, 0, widthPx, heightPx)
            window.setExtendedState(Frame.MAXIMIZED_BOTH)
            delay(500)
            println(
                "DBG screen=$screen window=${window.bounds} density=$density"
            )
        }
        FlightTrackerRoot()
    }
}

@Composable
fun FlightTrackerRoot() {
    val repo = remember { SettingsRepository() }
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
        customTextColor = settings.customTextColor,
        surfaceTint = settings.surfaceTint
    ) {
        var showSettings by remember { mutableStateOf(false) }

        Box {
            FlightMapScreen(
                settings = settings,
                onOpenSettings = { showSettings = true }
            )

            if (showSettings) {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = { new ->
                        settings = new
                        repo.save(new)
                    },
                    onBack = { showSettings = false }
                )
            }
        }
    }
}
