package com.daniel.flighttracker.data

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

enum class CardPosition(val label: String) {
    BOTTOM_LEFT("Bottom left"),
    BOTTOM_RIGHT("Bottom right")
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
    val showFlightCard: Boolean = true,
    val showAircraftMarkers: Boolean = true,
    val showAircraftLabels: Boolean = false,
    val markerScale: Float = 1f,
    val showNoResultsChip: Boolean = true,
    val surfaceTint: Float = 0f,
    val showGrid: Boolean = true,
    val showClusters: Boolean = true,
    val showGround: Boolean = true,
    val scrollZoom: Boolean = true,
    val doubleTapZoom: Boolean = true,
    val pollIntervalSec: Int = 5,
    val startLat: Double = 50.0,
    val startLon: Double = 10.0,
    val startZoom: Double = 5.0,
    val customMarkerColors: Boolean = false,
    val airborneColor: Int = 0xFF4FA3FF.toInt(),
    val groundColor: Int = 0xFF96A0B4.toInt(),
    val cardPosition: CardPosition = CardPosition.BOTTOM_LEFT,
    val cardWidth: Int = 360,
    val cardOrder: List<CardElement> = CardElement.defaultOrder,
    val hiddenCardElements: Set<CardElement> = emptySet()
)

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
