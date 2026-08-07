package com.daniel.flighttracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor
import com.daniel.flighttracker.data.AppColor
import com.daniel.flighttracker.data.TextColorMode

data class AppTones(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color
)

fun tonesFor(color: AppColor, dark: Boolean): AppTones = when (color) {
    AppColor.BLUE -> if (dark) {
        AppTones(
            Color(0xFF4FA3FF), Color(0xFF00264D), Color(0xFF123C6E), Color(0xFFCFE5FF),
            Color(0xFF8FB7E8), Color(0xFF234061), Color(0xFFCFE5FF), Color(0xFFB6E0C0)
        )
    } else {
        AppTones(
            Color(0xFF0A66C2), Color.White, Color(0xFFD3E4FF), Color(0xFF001C3D),
            Color(0xFF00658A), Color(0xFFC9E6FF), Color(0xFF001E2D), Color(0xFF1E6B47)
        )
    }

    AppColor.GREEN -> if (dark) {
        AppTones(
            Color(0xFF4CD17D), Color(0xFF00391D), Color(0xFF0B5B30), Color(0xFFA9F0C3),
            Color(0xFF8FD6A5), Color(0xFF24532F), Color(0xFFCFE9D1), Color(0xFFC9E88F)
        )
    } else {
        AppTones(
            Color(0xFF0E7A3D), Color.White, Color(0xFFA9F0C3), Color(0xFF00230F),
            Color(0xFF006B46), Color(0xFF9EE8BF), Color(0xFF002017), Color(0xFF557C00)
        )
    }

    AppColor.TEAL -> if (dark) {
        AppTones(
            Color(0xFF5EC8C0), Color(0xFF003C38), Color(0xFF155A55), Color(0xFFA9EEE8),
            Color(0xFF8FD0C8), Color(0xFF245F5A), Color(0xFFCFEBE6), Color(0xFFA9E8D9)
        )
    } else {
        AppTones(
            Color(0xFF006B64), Color.White, Color(0xFFA9EEE8), Color(0xFF003C38),
            Color(0xFF4B6D68), Color(0xFFCFEBE6), Color(0xFF06342F), Color(0xFF3D6D5D)
        )
    }

    AppColor.PURPLE -> if (dark) {
        AppTones(
            Color(0xFFB98AFF), Color(0xFF2A005E), Color(0xFF54239B), Color(0xFFE8D7FF),
            Color(0xFFCFB2FF), Color(0xFF4B2E73), Color(0xFFE8D7FF), Color(0xFFE2B6FF)
        )
    } else {
        AppTones(
            Color(0xFF7C4DCC), Color.White, Color(0xFFE8D7FF), Color(0xFF2A005E),
            Color(0xFF6E51A8), Color(0xFFE8D7FF), Color(0xFF260E4D), Color(0xFF8A5BB8)
        )
    }

    AppColor.ORANGE -> if (dark) {
        AppTones(
            Color(0xFFFFA75C), Color(0xFF452500), Color(0xFF7A3D00), Color(0xFFFFDDBB),
            Color(0xFFFFC48D), Color(0xFF5C3A17), Color(0xFFFFE3C7), Color(0xFFFFB380)
        )
    } else {
        AppTones(
            Color(0xFF9A4A00), Color.White, Color(0xFFFFDDBB), Color(0xFF452500),
            Color(0xFF8A5400), Color(0xFFFFE2BC), Color(0xFF2B1600), Color(0xFF9A4A00)
        )
    }

    AppColor.RED -> if (dark) {
        AppTones(
            Color(0xFFFF8A80), Color(0xFF600000), Color(0xFF932323), Color(0xFFFFD9D2),
            Color(0xFFFFB0A8), Color(0xFF6D3A37), Color(0xFFFFD9D2), Color(0xFFFFC9C1)
        )
    } else {
        AppTones(
            Color(0xFFB3261E), Color.White, Color(0xFFFFDAD4), Color(0xFF3F0200),
            Color(0xFF8F4B44), Color(0xFFFFDAD4), Color(0xFF380905), Color(0xFF9C4A45)
        )
    }

    AppColor.CUSTOM -> deriveTones(Color(0xFF4FA3FF), dark)
}

fun deriveTones(primary: Color, dark: Boolean): AppTones {
    val onPrimary = if (primary.luminance() > 0.55f) Color(0xFF171A20) else Color.White
    val containerBase = if (dark) DarkSurface else Color.White
    val container = lerp(primary, containerBase, 0.72f)
    val onContainer = if (dark) lerp(primary, Color.White, 0.6f) else lerp(primary, Color.Black, 0.45f)
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(primary.toArgb(), hsv)
    val secondaryH = (hsv[0] + 30f) % 360f
    val tertiaryH = (hsv[0] - 30f + 360f) % 360f
    val s2 = hsv[1] * 0.55f
    val v2 = if (dark) 0.92f else 0.45f
    val secondary = Color(AndroidColor.HSVToColor(floatArrayOf(secondaryH, s2, v2)))
    val tertiary = Color(AndroidColor.HSVToColor(floatArrayOf(tertiaryH, s2 * 0.8f, v2)))
    val secondaryContainer = lerp(secondary, containerBase, 0.78f)
    val onContainerText = if (dark) Color(0xFFE2E7F5) else Color(0xFF171A20)
    return AppTones(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = secondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onContainerText,
        tertiary = tertiary
    )
}

private val DarkBackground = Color(0xFF0B1026)
private val DarkOnBackground = Color(0xFFE2E7F5)
private val DarkSurface = Color(0xFF141B33)
private val DarkOnSurface = Color(0xFFE2E7F5)
private val DarkSurfaceVariant = Color(0xFF232B45)
private val DarkOnSurfaceVariant = Color(0xFFB7C2DD)
private val DarkError = Color(0xFFFFB4AB)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)

private val LightBackground = Color(0xFFF7F9FE)
private val LightOnBackground = Color(0xFF171A20)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF171A20)
private val LightSurfaceVariant = Color(0xFFE0E4EC)
private val LightOnSurfaceVariant = Color(0xFF44474E)
private val LightError = Color(0xFFBA1A1A)
private val LightErrorContainer = Color(0xFFFFDAD6)
private val LightOnErrorContainer = Color(0xFF410002)

@Composable
fun FlightTrackerTheme(
    darkTheme: Boolean = true,
    appColor: AppColor = AppColor.BLUE,
    customAppColor: Int = 0xFF4FA3FF.toInt(),
    textColorMode: TextColorMode = TextColorMode.AUTO,
    customTextColor: Int = 0xFFE2E7F5.toInt(),
    content: @Composable () -> Unit
) {
    val tones = if (appColor == AppColor.CUSTOM) {
        deriveTones(Color(customAppColor), darkTheme)
    } else {
        tonesFor(appColor, darkTheme)
    }
    val baseScheme = if (darkTheme) {
        darkColorScheme(
            primary = tones.primary,
            onPrimary = tones.onPrimary,
            primaryContainer = tones.primaryContainer,
            onPrimaryContainer = tones.onPrimaryContainer,
            secondary = tones.secondary,
            secondaryContainer = tones.secondaryContainer,
            onSecondaryContainer = tones.onSecondaryContainer,
            tertiary = tones.tertiary,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            error = DarkError,
            errorContainer = DarkErrorContainer,
            onErrorContainer = DarkOnErrorContainer
        )
    } else {
        lightColorScheme(
            primary = tones.primary,
            onPrimary = tones.onPrimary,
            primaryContainer = tones.primaryContainer,
            onPrimaryContainer = tones.onPrimaryContainer,
            secondary = tones.secondary,
            secondaryContainer = tones.secondaryContainer,
            onSecondaryContainer = tones.onSecondaryContainer,
            tertiary = tones.tertiary,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            error = LightError,
            errorContainer = LightErrorContainer,
            onErrorContainer = LightOnErrorContainer
        )
    }
    val scheme = if (textColorMode == TextColorMode.CUSTOM) {
        baseScheme.copy(
            onSurface = Color(customTextColor),
            onBackground = Color(customTextColor)
        )
    } else {
        baseScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
