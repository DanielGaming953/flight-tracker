package com.daniel.flighttracker.data

import android.content.Context
import kotlin.math.roundToInt

enum class AltitudeUnit(val label: String) {
    FEET("Feet (ft)"),
    METERS("Meters (m)")
}

enum class SpeedUnit(val label: String) {
    KNOTS("Knots (kt)"),
    KMH("Kilometers per hour (km/h)"),
    MPH("Miles per hour (mph)")
}

enum class ThemeMode(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class MapStyle(val label: String) {
    STREET("Street map"),
    SATELLITE("Satellite")
}

enum class AircraftIcon(val label: String) {
    ARROW("Arrow"),
    AIRCRAFT("Aircraft")
}

enum class AppColor(val label: String) {
    BLUE("Blue"),
    GREEN("Green"),
    TEAL("Teal"),
    PURPLE("Purple"),
    ORANGE("Orange"),
    RED("Red"),
    CUSTOM("Custom")
}

enum class TextColorMode(val label: String) {
    AUTO("Automatic"),
    CUSTOM("Custom")
}

enum class CardElement(val label: String) {
    INFO("Aircraft info"),
    ALT("Altitude"),
    SPD("Speed"),
    HDG("Heading"),
    VS("Vertical speed"),
    STATUS("On ground / Airborne");

    companion object {
        val defaultOrder = listOf(INFO, ALT, SPD, HDG, VS, STATUS)
    }
}

val CardElement.isStat: Boolean
    get() = this == CardElement.ALT || this == CardElement.SPD ||
        this == CardElement.HDG || this == CardElement.VS

data class Settings(
    val altitudeUnit: AltitudeUnit = AltitudeUnit.FEET,
    val speedUnit: SpeedUnit = SpeedUnit.KNOTS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val mapStyle: MapStyle = MapStyle.STREET,
    val clientId: String = "",
    val clientSecret: String = "",
    val maxFlights: Int = 100,
    val aircraftIcon: AircraftIcon = AircraftIcon.ARROW,
    val appColor: AppColor = AppColor.BLUE,
    val customAppColor: Int = 0xFF4FA3FF.toInt(),
    val textColorMode: TextColorMode = TextColorMode.AUTO,
    val customTextColor: Int = 0xFFE2E7F5.toInt(),
    val showSearchBar: Boolean = true,
    val showStatusBar: Boolean = true,
    val showLocationButton: Boolean = true,
    val cardOrder: List<CardElement> = CardElement.defaultOrder,
    val hiddenCardElements: Set<CardElement> = emptySet()
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("flight_tracker_settings", Context.MODE_PRIVATE)

    fun load(): Settings = Settings(
        altitudeUnit = AltitudeUnit.valueOf(
            prefs.getString(KEY_ALT, AltitudeUnit.FEET.name) ?: AltitudeUnit.FEET.name
        ),
        speedUnit = SpeedUnit.valueOf(
            prefs.getString(KEY_SPEED, SpeedUnit.KNOTS.name) ?: SpeedUnit.KNOTS.name
        ),
        themeMode = ThemeMode.valueOf(
            prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        ),
        mapStyle = MapStyle.valueOf(
            prefs.getString(KEY_MAP_STYLE, MapStyle.STREET.name) ?: MapStyle.STREET.name
        ),
        clientId = prefs.getString(KEY_CLIENT_ID, "") ?: "",
        clientSecret = prefs.getString(KEY_CLIENT_SECRET, "") ?: "",
        maxFlights = prefs.getInt(KEY_MAX_FLIGHTS, 100),
        aircraftIcon = AircraftIcon.valueOf(
            prefs.getString(KEY_AIRCRAFT_ICON, AircraftIcon.ARROW.name) ?: AircraftIcon.ARROW.name
        ),
        appColor = AppColor.valueOf(
            prefs.getString(KEY_APP_COLOR, AppColor.BLUE.name) ?: AppColor.BLUE.name
        ),
        customAppColor = prefs.getInt(KEY_CUSTOM_APP_COLOR, 0xFF4FA3FF.toInt()),
        textColorMode = TextColorMode.valueOf(
            prefs.getString(KEY_TEXT_COLOR_MODE, TextColorMode.AUTO.name) ?: TextColorMode.AUTO.name
        ),
        customTextColor = prefs.getInt(KEY_CUSTOM_TEXT_COLOR, 0xFFE2E7F5.toInt()),
        showSearchBar = prefs.getBoolean(KEY_SHOW_SEARCH, true),
        showStatusBar = prefs.getBoolean(KEY_SHOW_STATUS, true),
        showLocationButton = prefs.getBoolean(KEY_SHOW_LOCATION, true),
        cardOrder = runCatching {
            (prefs.getString(KEY_CARD_ORDER, null) ?: "").split(",")
                .filter { it.isNotBlank() }
                .map { CardElement.valueOf(it) }
        }.getOrDefault(CardElement.defaultOrder)
            .takeIf { it.size == CardElement.entries.size } ?: CardElement.defaultOrder,
        hiddenCardElements = runCatching {
            (prefs.getString(KEY_HIDDEN_CARD, null) ?: "").split(",")
                .filter { it.isNotBlank() }
                .map { CardElement.valueOf(it) }
                .toSet()
        }.getOrDefault(emptySet())
    )

    fun save(settings: Settings) {
        prefs.edit()
            .putString(KEY_ALT, settings.altitudeUnit.name)
            .putString(KEY_SPEED, settings.speedUnit.name)
            .putString(KEY_THEME, settings.themeMode.name)
            .putString(KEY_MAP_STYLE, settings.mapStyle.name)
            .putString(KEY_CLIENT_ID, settings.clientId)
            .putString(KEY_CLIENT_SECRET, settings.clientSecret)
            .putInt(KEY_MAX_FLIGHTS, settings.maxFlights)
            .putString(KEY_AIRCRAFT_ICON, settings.aircraftIcon.name)
            .putString(KEY_APP_COLOR, settings.appColor.name)
            .putInt(KEY_CUSTOM_APP_COLOR, settings.customAppColor)
            .putString(KEY_TEXT_COLOR_MODE, settings.textColorMode.name)
            .putInt(KEY_CUSTOM_TEXT_COLOR, settings.customTextColor)
            .putBoolean(KEY_SHOW_SEARCH, settings.showSearchBar)
            .putBoolean(KEY_SHOW_STATUS, settings.showStatusBar)
            .putBoolean(KEY_SHOW_LOCATION, settings.showLocationButton)
            .putString(KEY_CARD_ORDER, settings.cardOrder.joinToString(",") { it.name })
            .putString(KEY_HIDDEN_CARD, settings.hiddenCardElements.joinToString(",") { it.name })
            .apply()
    }

    private companion object {
        const val KEY_ALT = "altitude_unit"
        const val KEY_SPEED = "speed_unit"
        const val KEY_THEME = "theme_mode"
        const val KEY_MAP_STYLE = "map_style"
        const val KEY_CLIENT_ID = "opensky_client_id"
        const val KEY_CLIENT_SECRET = "opensky_client_secret"
        const val KEY_MAX_FLIGHTS = "max_flights"
        const val KEY_AIRCRAFT_ICON = "aircraft_icon"
        const val KEY_APP_COLOR = "app_color"
        const val KEY_CUSTOM_APP_COLOR = "custom_app_color"
        const val KEY_TEXT_COLOR_MODE = "text_color_mode"
        const val KEY_CUSTOM_TEXT_COLOR = "custom_text_color"
        const val KEY_SHOW_SEARCH = "show_search_bar"
        const val KEY_SHOW_STATUS = "show_status_bar"
        const val KEY_SHOW_LOCATION = "show_location_button"
        const val KEY_CARD_ORDER = "card_order"
        const val KEY_HIDDEN_CARD = "hidden_card_elements"
    }
}

fun formatAltitude(meters: Double?, unit: AltitudeUnit): String {
    if (meters == null || meters.isNaN()) return "—"
    return when (unit) {
        AltitudeUnit.FEET -> "${(meters * 3.28084).roundToInt()} ft"
        AltitudeUnit.METERS -> "${meters.roundToInt()} m"
    }
}

fun formatSpeed(metersPerSecond: Double?, unit: SpeedUnit): String {
    if (metersPerSecond == null || metersPerSecond.isNaN()) return "—"
    return when (unit) {
        SpeedUnit.KNOTS -> "${(metersPerSecond * 1.94384).roundToInt()} kt"
        SpeedUnit.KMH -> "${(metersPerSecond * 3.6).roundToInt()} km/h"
        SpeedUnit.MPH -> "${(metersPerSecond * 2.23694).roundToInt()} mph"
    }
}
