package com.daniel.flighttracker.data

import java.io.File
import java.util.Properties

class SettingsRepository() {
    private val file = File(System.getProperty("user.home"), ".flight_tracker_desktop.properties")
    private val props = Properties().apply {
        if (file.exists()) file.inputStream().use { load(it) }
    }

    fun load(): Settings = Settings(
        altitudeUnit = AltitudeUnit.valueOf(
            props.getProperty(KEY_ALT, AltitudeUnit.FEET.name)
        ),
        speedUnit = SpeedUnit.valueOf(
            props.getProperty(KEY_SPEED, SpeedUnit.KNOTS.name)
        ),
        themeMode = ThemeMode.valueOf(
            props.getProperty(KEY_THEME, ThemeMode.SYSTEM.name)
        ),
        mapStyle = MapStyle.valueOf(
            props.getProperty(KEY_MAP_STYLE, MapStyle.STREET.name)
        ),
        clientId = props.getProperty(KEY_CLIENT_ID, ""),
        clientSecret = props.getProperty(KEY_CLIENT_SECRET, ""),
        maxFlights = props.getProperty(KEY_MAX_FLIGHTS, "100").toIntOrNull() ?: 100,
        aircraftIcon = AircraftIcon.valueOf(
            props.getProperty(KEY_AIRCRAFT_ICON, AircraftIcon.ARROW.name)
        ),
        appColor = AppColor.valueOf(
            props.getProperty(KEY_APP_COLOR, AppColor.BLUE.name)
        ),
        customAppColor = props.getProperty(KEY_CUSTOM_APP_COLOR, "0xFF4FA3FF").let { hex ->
            runCatching { (hex.removePrefix("0x").removePrefix("#").toLong(16)).toInt() }
                .getOrDefault(0xFF4FA3FF.toInt())
        },
        textColorMode = TextColorMode.valueOf(
            props.getProperty(KEY_TEXT_COLOR_MODE, TextColorMode.AUTO.name)
        ),
        customTextColor = props.getProperty(KEY_CUSTOM_TEXT_COLOR, "0xFFE2E7F5").let { hex ->
            runCatching { (hex.removePrefix("0x").removePrefix("#").toLong(16)).toInt() }
                .getOrDefault(0xFFE2E7F5.toInt())
        },
        showSearchBar = props.getProperty(KEY_SHOW_SEARCH, "true").toBoolean(),
        showStatusBar = props.getProperty(KEY_SHOW_STATUS, "true").toBoolean(),
        showLocationButton = props.getProperty(KEY_SHOW_LOCATION, "true").toBoolean(),
        showFlightCard = props.getProperty(KEY_SHOW_FLIGHT_CARD, "true").toBoolean(),
        showAircraftMarkers = props.getProperty(KEY_SHOW_MARKERS, "true").toBoolean(),
        showAircraftLabels = props.getProperty(KEY_SHOW_LABELS, "false").toBoolean(),
        markerScale = props.getProperty(KEY_MARKER_SCALE, "1").toFloatOrNull()?.coerceIn(0.5f, 2f) ?: 1f,
        showNoResultsChip = props.getProperty(KEY_SHOW_NO_RESULTS, "true").toBoolean(),
        surfaceTint = props.getProperty(KEY_SURFACE_TINT, "0").toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f,
        showGrid = props.getProperty(KEY_SHOW_GRID, "true").toBoolean(),
        showClusters = props.getProperty(KEY_SHOW_CLUSTERS, "true").toBoolean(),
        showGround = props.getProperty(KEY_SHOW_GROUND, "true").toBoolean(),
        scrollZoom = props.getProperty(KEY_SCROLL_ZOOM, "true").toBoolean(),
        invertScrollZoom = props.getProperty(KEY_INVERT_SCROLL_ZOOM, "false").toBoolean(),
        doubleTapZoom = props.getProperty(KEY_DOUBLE_TAP_ZOOM, "true").toBoolean(),
        pollIntervalSec = props.getProperty(KEY_POLL_INTERVAL, "5").toIntOrNull()?.coerceIn(2, 30) ?: 5,
        startLat = props.getProperty(KEY_START_LAT, "50").toDoubleOrNull()?.coerceIn(-85.0, 85.0) ?: 50.0,
        startLon = props.getProperty(KEY_START_LON, "10").toDoubleOrNull()?.coerceIn(-180.0, 180.0) ?: 10.0,
        startZoom = props.getProperty(KEY_START_ZOOM, "5").toDoubleOrNull()?.coerceIn(3.0, 19.0) ?: 5.0,
        customMarkerColors = props.getProperty(KEY_CUSTOM_MARKER_COLORS, "false").toBoolean(),
        airborneColor = props.getProperty(KEY_AIRBORNE_COLOR, "4278196479").toIntOrNull() ?: 0xFF4FA3FF.toInt(),
        groundColor = props.getProperty(KEY_GROUND_COLOR, "4278166964").toIntOrNull() ?: 0xFF96A0B4.toInt(),
        cardPosition = runCatching {
            CardPosition.valueOf(props.getProperty(KEY_CARD_POSITION, "BOTTOM_LEFT"))
        }.getOrDefault(CardPosition.BOTTOM_LEFT),
        cardWidth = props.getProperty(KEY_CARD_WIDTH, "360").toIntOrNull()?.coerceIn(280, 560) ?: 360,
        cardOrder = runCatching {
            (props.getProperty(KEY_CARD_ORDER, null) ?: "").split(",")
                .filter { it.isNotBlank() }
                .map { CardElement.valueOf(it) }
        }.getOrDefault(CardElement.defaultOrder)
            .takeIf { it.size == CardElement.entries.size } ?: CardElement.defaultOrder,
        hiddenCardElements = runCatching {
            (props.getProperty(KEY_HIDDEN_CARD, null) ?: "").split(",")
                .filter { it.isNotBlank() }
                .map { CardElement.valueOf(it) }
                .toSet()
        }.getOrDefault(emptySet())
    )

    fun save(settings: Settings) {
        props.setProperty(KEY_ALT, settings.altitudeUnit.name)
        props.setProperty(KEY_SPEED, settings.speedUnit.name)
        props.setProperty(KEY_THEME, settings.themeMode.name)
        props.setProperty(KEY_MAP_STYLE, settings.mapStyle.name)
        props.setProperty(KEY_CLIENT_ID, settings.clientId)
        props.setProperty(KEY_CLIENT_SECRET, settings.clientSecret)
        props.setProperty(KEY_MAX_FLIGHTS, settings.maxFlights.toString())
        props.setProperty(KEY_AIRCRAFT_ICON, settings.aircraftIcon.name)
        props.setProperty(KEY_APP_COLOR, settings.appColor.name)
        props.setProperty(KEY_CUSTOM_APP_COLOR, "0x${settings.customAppColor.toUInt().toString(16)}")
        props.setProperty(KEY_TEXT_COLOR_MODE, settings.textColorMode.name)
        props.setProperty(KEY_CUSTOM_TEXT_COLOR, "0x${settings.customTextColor.toUInt().toString(16)}")
        props.setProperty(KEY_SHOW_SEARCH, settings.showSearchBar.toString())
        props.setProperty(KEY_SHOW_STATUS, settings.showStatusBar.toString())
        props.setProperty(KEY_SHOW_LOCATION, settings.showLocationButton.toString())
        props.setProperty(KEY_SHOW_FLIGHT_CARD, settings.showFlightCard.toString())
        props.setProperty(KEY_SHOW_MARKERS, settings.showAircraftMarkers.toString())
        props.setProperty(KEY_SHOW_LABELS, settings.showAircraftLabels.toString())
        props.setProperty(KEY_MARKER_SCALE, settings.markerScale.toString())
        props.setProperty(KEY_SHOW_NO_RESULTS, settings.showNoResultsChip.toString())
        props.setProperty(KEY_SURFACE_TINT, settings.surfaceTint.toString())
        props.setProperty(KEY_SHOW_GRID, settings.showGrid.toString())
        props.setProperty(KEY_SHOW_CLUSTERS, settings.showClusters.toString())
        props.setProperty(KEY_SHOW_GROUND, settings.showGround.toString())
        props.setProperty(KEY_SCROLL_ZOOM, settings.scrollZoom.toString())
        props.setProperty(KEY_INVERT_SCROLL_ZOOM, settings.invertScrollZoom.toString())
        props.setProperty(KEY_DOUBLE_TAP_ZOOM, settings.doubleTapZoom.toString())
        props.setProperty(KEY_POLL_INTERVAL, settings.pollIntervalSec.toString())
        props.setProperty(KEY_START_LAT, settings.startLat.toString())
        props.setProperty(KEY_START_LON, settings.startLon.toString())
        props.setProperty(KEY_START_ZOOM, settings.startZoom.toString())
        props.setProperty(KEY_CUSTOM_MARKER_COLORS, settings.customMarkerColors.toString())
        props.setProperty(KEY_AIRBORNE_COLOR, settings.airborneColor.toString())
        props.setProperty(KEY_GROUND_COLOR, settings.groundColor.toString())
        props.setProperty(KEY_CARD_POSITION, settings.cardPosition.name)
        props.setProperty(KEY_CARD_WIDTH, settings.cardWidth.toString())
        props.setProperty(KEY_CARD_ORDER, settings.cardOrder.joinToString(",") { it.name })
        props.setProperty(KEY_HIDDEN_CARD, settings.hiddenCardElements.joinToString(",") { it.name })
        file.parentFile?.mkdirs()
        file.outputStream().use { props.store(it, "FlightTracker Desktop settings") }
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
        const val KEY_SHOW_FLIGHT_CARD = "show_flight_card"
        const val KEY_SHOW_MARKERS = "show_aircraft_markers"
        const val KEY_SHOW_LABELS = "show_aircraft_labels"
        const val KEY_MARKER_SCALE = "marker_scale"
        const val KEY_SHOW_NO_RESULTS = "show_no_results_chip"
        const val KEY_SURFACE_TINT = "surface_tint"
        const val KEY_SHOW_GRID = "show_grid"
        const val KEY_SHOW_CLUSTERS = "show_clusters"
        const val KEY_SHOW_GROUND = "show_ground"
        const val KEY_SCROLL_ZOOM = "scroll_zoom"
        const val KEY_INVERT_SCROLL_ZOOM = "invert_scroll_zoom"
        const val KEY_DOUBLE_TAP_ZOOM = "double_tap_zoom"
        const val KEY_POLL_INTERVAL = "poll_interval_sec"
        const val KEY_START_LAT = "start_lat"
        const val KEY_START_LON = "start_lon"
        const val KEY_START_ZOOM = "start_zoom"
        const val KEY_CUSTOM_MARKER_COLORS = "custom_marker_colors"
        const val KEY_AIRBORNE_COLOR = "airborne_color"
        const val KEY_GROUND_COLOR = "ground_color"
        const val KEY_CARD_POSITION = "card_position"
        const val KEY_CARD_WIDTH = "card_width"
        const val KEY_CARD_ORDER = "card_order"
        const val KEY_HIDDEN_CARD = "hidden_card_elements"
    }
}
