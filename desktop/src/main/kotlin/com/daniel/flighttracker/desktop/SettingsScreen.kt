package com.daniel.flighttracker.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.daniel.flighttracker.data.AltitudeUnit
import com.daniel.flighttracker.data.AircraftIcon
import com.daniel.flighttracker.data.AppColor
import com.daniel.flighttracker.data.CardElement
import com.daniel.flighttracker.data.CardPosition
import com.daniel.flighttracker.data.MapStyle
import com.daniel.flighttracker.data.OpenSkyClient
import com.daniel.flighttracker.data.Settings
import com.daniel.flighttracker.data.SpeedUnit
import com.daniel.flighttracker.data.TextColorMode
import com.daniel.flighttracker.data.ThemeMode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(min = 360.dp, max = 600.dp)
                .fillMaxHeight(0.75f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { }
                ),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
            SettingsGroup("General") {
                SectionHeader("Altitude units")
                AltitudeUnit.entries.forEach { unit ->
                    SettingRow(
                        label = unit.label,
                        selected = settings.altitudeUnit == unit
                    ) {
                        onSettingsChange(settings.copy(altitudeUnit = unit))
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Speed units")
                SpeedUnit.entries.forEach { unit ->
                    SettingRow(
                        label = unit.label,
                        selected = settings.speedUnit == unit
                    ) {
                        onSettingsChange(settings.copy(speedUnit = unit))
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Theme")
                ThemeMode.entries.forEach { mode ->
                    SettingRow(
                        label = mode.label,
                        selected = settings.themeMode == mode
                    ) {
                        onSettingsChange(settings.copy(themeMode = mode))
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Map style")
                MapStyle.entries.forEach { style ->
                    SettingRow(
                        label = style.label,
                        selected = settings.mapStyle == style
                    ) {
                        onSettingsChange(settings.copy(mapStyle = style))
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Max aircraft on map")
                Text(
                    text = "${settings.maxFlights} aircraft",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Slider(
                    value = settings.maxFlights.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(maxFlights = it.roundToInt())) },
                    valueRange = 20f..300f,
                    steps = 278
                )
                Text(
                    text = "Only the fastest airborne flights are shown when the limit is reached.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            SettingsGroup("Map & navigation") {
                SwitchRow("Latitude / longitude grid", settings.showGrid) {
                    onSettingsChange(settings.copy(showGrid = it))
                }
                SwitchRow("Cluster nearby aircraft", settings.showClusters) {
                    onSettingsChange(settings.copy(showClusters = it))
                }
                SwitchRow("Show aircraft on the ground", settings.showGround) {
                    onSettingsChange(settings.copy(showGround = it))
                }
                SwitchRow("Scroll wheel zooms", settings.scrollZoom) {
                    onSettingsChange(settings.copy(scrollZoom = it))
                }
                SwitchRow(
                    "Invert scroll direction",
                    settings.invertScrollZoom,
                    subtitle = "On if your mouse uses natural scrolling"
                ) {
                    onSettingsChange(settings.copy(invertScrollZoom = it))
                }
                SwitchRow("Double-click zooms in", settings.doubleTapZoom) {
                    onSettingsChange(settings.copy(doubleTapZoom = it))
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Refresh interval")
                Slider(
                    value = settings.pollIntervalSec.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(pollIntervalSec = it.roundToInt())) },
                    valueRange = 2f..30f,
                    steps = 27
                )
                Text(
                    text = "Fetch aircraft every ${settings.pollIntervalSec}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                SectionHeader("Startup view")
                Text(
                    text = "The map opens here and the Home key returns to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = settings.startLat.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(startLat = it.toDouble())) },
                    valueRange = -85f..85f,
                    steps = 33
                )
                Text(
                    text = "Latitude: ${settings.startLat.roundToInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = settings.startLon.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(startLon = it.toDouble())) },
                    valueRange = -180f..180f,
                    steps = 35
                )
                Text(
                    text = "Longitude: ${settings.startLon.roundToInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = settings.startZoom.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(startZoom = it.toDouble())) },
                    valueRange = 3f..15f,
                    steps = 11
                )
                Text(
                    text = "Zoom: ${settings.startZoom.roundToInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsGroup("Customization") {
                SectionHeader("App color")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppColor.entries.forEach { color ->
                        ColorSwatch(
                            color = if (color == AppColor.CUSTOM) Color(settings.customAppColor)
                            else tonesFor(color, false).primary,
                            label = color.label,
                            selected = settings.appColor == color,
                            onClick = { onSettingsChange(settings.copy(appColor = color)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (settings.appColor == AppColor.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    HsvColorPicker(
                        color = Color(settings.customAppColor),
                        label = "Accent color",
                        onColorChange = { newColor ->
                            onSettingsChange(settings.copy(customAppColor = newColor.toArgb()))
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Text color")
                TextColorMode.entries.forEach { mode ->
                    SettingRow(
                        label = mode.label,
                        selected = settings.textColorMode == mode
                    ) {
                        onSettingsChange(settings.copy(textColorMode = mode))
                    }
                }
                if (settings.textColorMode == TextColorMode.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    HsvColorPicker(
                        color = Color(settings.customTextColor),
                        label = "Text color",
                        onColorChange = { newColor ->
                            onSettingsChange(settings.copy(customTextColor = newColor.toArgb()))
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("App tint")
                Text(
                    text = "Color the whole app (backgrounds, bars, cards) with your accent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Slider(
                    value = settings.surfaceTint,
                    onValueChange = { onSettingsChange(settings.copy(surfaceTint = it)) },
                    valueRange = 0f..0.5f,
                    steps = 9
                )
                Text(
                    text = "${(settings.surfaceTint * 100).roundToInt()}% tinted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                SectionHeader("Aircraft icon")
                Text(
                    text = "Pick the marker shape used for each plane.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AircraftIcon.entries.forEach { icon ->
                        IconOptionCard(
                            icon = icon,
                            selected = settings.aircraftIcon == icon,
                            onClick = { onSettingsChange(settings.copy(aircraftIcon = icon)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Marker size")
                Slider(
                    value = settings.markerScale,
                    onValueChange = { onSettingsChange(settings.copy(markerScale = it)) },
                    valueRange = 0.6f..1.6f,
                    steps = 9
                )
                Text(
                    text = "${settings.markerScale}×",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                SectionHeader("Marker colors")
                SwitchRow("Custom marker colors", settings.customMarkerColors) {
                    onSettingsChange(settings.copy(customMarkerColors = it))
                }
                if (settings.customMarkerColors) {
                    Spacer(Modifier.height(8.dp))
                    HsvColorPicker(
                        color = Color(settings.airborneColor),
                        label = "Airborne color",
                        onColorChange = { newColor ->
                            onSettingsChange(settings.copy(airborneColor = newColor.toArgb()))
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    HsvColorPicker(
                        color = Color(settings.groundColor),
                        label = "Ground color",
                        onColorChange = { newColor ->
                            onSettingsChange(settings.copy(groundColor = newColor.toArgb()))
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Interface")
                SwitchRow("Search bar", settings.showSearchBar) {
                    onSettingsChange(settings.copy(showSearchBar = it))
                }
                SwitchRow("Status pill", settings.showStatusBar) {
                    onSettingsChange(settings.copy(showStatusBar = it))
                }
                SwitchRow("Aircraft markers", settings.showAircraftMarkers) {
                    onSettingsChange(settings.copy(showAircraftMarkers = it))
                }
                SwitchRow("Aircraft labels", settings.showAircraftLabels) {
                    onSettingsChange(settings.copy(showAircraftLabels = it))
                }
                SwitchRow("Flight card", settings.showFlightCard) {
                    onSettingsChange(settings.copy(showFlightCard = it))
                }
                SwitchRow("Location button", settings.showLocationButton) {
                    onSettingsChange(settings.copy(showLocationButton = it))
                }
                SwitchRow("No-results message", settings.showNoResultsChip) {
                    onSettingsChange(settings.copy(showNoResultsChip = it))
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Flight card")
                Text(
                    text = "Toggle what the plane card shows. Hold and drag the handle to reorder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                var draggingIndex by remember { mutableStateOf<Int?>(null) }
                var dragOffsetY by remember { mutableStateOf(0f) }
                val density = LocalDensity.current
                val rowPitch = with(density) { (56.dp + 6.dp).toPx() }
                val dropTarget = draggingIndex?.let { from ->
                    (from + (dragOffsetY / rowPitch).roundToInt())
                        .coerceIn(0, settings.cardOrder.lastIndex)
                }

                settings.cardOrder.forEachIndexed { index, element ->
                    val enabled = element !in settings.hiddenCardElements
                    val currentIndex by rememberUpdatedState(index)
                    val currentOrder by rememberUpdatedState(settings.cardOrder)
                    val currentSettings by rememberUpdatedState(settings)
                    val isDragging = draggingIndex == index

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .then(
                                if (isDragging)
                                    Modifier
                                        .zIndex(10f)
                                        .graphicsLayer { translationY = dragOffsetY }
                                else
                                    Modifier
                            ),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isDragging)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.surface,
                        shadowElevation = if (isDragging) 8.dp else 0.dp
                    ) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { on ->
                                        onSettingsChange(
                                            currentSettings.copy(
                                                hiddenCardElements = if (on)
                                                    currentSettings.hiddenCardElements - element
                                                else
                                                    currentSettings.hiddenCardElements + element
                                            )
                                        )
                                    },
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = element.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (enabled)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp, end = 4.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (enabled)
                                                Modifier.pointerInput(Unit) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            draggingIndex = currentIndex
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragEnd = {
                                                            val from = draggingIndex
                                                            if (from != null) {
                                                                val target = (from +
                                                                    (dragOffsetY / rowPitch).roundToInt())
                                                                    .coerceIn(0, currentOrder.lastIndex)
                                                                if (target != from) {
                                                                    val order = currentOrder.toMutableList()
                                                                    val item = order.removeAt(from)
                                                                    order.add(target, item)
                                                                    onSettingsChange(
                                                                        currentSettings.copy(cardOrder = order)
                                                                    )
                                                                }
                                                            }
                                                            draggingIndex = null
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragCancel = {
                                                            draggingIndex = null
                                                            dragOffsetY = 0f
                                                        },
                                                        onDrag = { change, amount ->
                                                            change.consume()
                                                            dragOffsetY += amount.y
                                                        }
                                                    )
                                                }
                                            else
                                                Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DragHandleDots(
                                        tint = if (enabled)
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else
                                            MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                            if (!isDragging && index == dropTarget) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .height(3.dp)
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Card position")
                CardPosition.entries.forEach { pos ->
                    SettingRow(
                        label = pos.label,
                        selected = settings.cardPosition == pos
                    ) {
                        onSettingsChange(settings.copy(cardPosition = pos))
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("Card width")
                Slider(
                    value = settings.cardWidth.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(cardWidth = it.roundToInt())) },
                    valueRange = 280f..560f,
                    steps = 27
                )
                Text(
                    text = "${settings.cardWidth} px",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            SettingsGroup("OpenSky account") {
                SectionHeader("Free OpenSky account (optional)")
                Text(
                    text = "Paste the Client ID and Secret from your credentials.json " +
                        "(Account → API clients on opensky-network.org) to get 10x more daily updates. " +
                        "Leave empty to stay anonymous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = settings.clientId,
                    onValueChange = { onSettingsChange(settings.copy(clientId = it.trim())) },
                    label = { Text("Client ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.clientSecret,
                    onValueChange = { onSettingsChange(settings.copy(clientSecret = it.trim())) },
                    label = { Text("Client Secret") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                TestConnectionSection(settings)
            }

            Spacer(Modifier.height(24.dp))

            SettingsGroup("Reset") {
                Surface(
                    onClick = { onSettingsChange(Settings()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Restore all default settings",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = "Aircraft data: OpenSky Network + airplanes.live",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun TestConnectionSection(settings: Settings) {
    val scope = rememberCoroutineScope()
    val testApi = remember(settings.clientId, settings.clientSecret) {
        OpenSkyClient(settings.clientId, settings.clientSecret)
    }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    OutlinedButton(
        onClick = {
            testing = true
            result = null
            scope.launch {
                result = testApi.testConnection()
                testing = false
            }
        },
        enabled = !testing,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (testing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text("Testing…")
        } else {
            Text("Test connection")
        }
    }

    result?.let {
        val success = it.startsWith("Connected")
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = if (success) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun HsvColorPicker(
    color: Color,
    label: String,
    onColorChange: (Color) -> Unit
) {
    val initial = remember { color.toHsv() }
    var hue by remember { mutableStateOf(initial.h) }
    var sat by remember { mutableStateOf(initial.s) }
    var value by remember { mutableStateOf(initial.v) }

    val current = hsvToColor(hue, sat, value)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = current,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(48.dp)
                ) {}
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#%06X".format(0xFFFFFF and current.toArgb()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            HueBar(
                hue = hue,
                onHueChange = { hue = it },
                onCommit = { onColorChange(hsvToColor(hue, sat, value)) }
            )

            Spacer(Modifier.height(12.dp))

            SvSquare(
                hue = hue,
                saturation = sat,
                value = value,
                onSvChange = { s, v ->
                    sat = s
                    value = v
                },
                onCommit = { onColorChange(hsvToColor(hue, sat, value)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            Text(
                text = "Tap or drag in the square to pick your color",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onHueChange: (Float) -> Unit,
    onCommit: () -> Unit
) {
    val brush = remember {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
            )
        )
    }
    var barWidth by remember { mutableStateOf(0f) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
            .onSizeChanged { barWidth = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onHueChange((offset.x / barWidth).coerceIn(0f, 1f) * 360f)
                    onCommit()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onCommit() },
                    onDragCancel = { onCommit() },
                    onDrag = { change, _ ->
                        change.consume()
                        onHueChange((change.position.x / barWidth).coerceIn(0f, 1f) * 360f)
                    }
                )
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val x = (hue / 360f).coerceIn(0f, 1f) * size.width
            val center = Offset(x, size.height / 2f)
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = size.height * 0.52f,
                center = center,
                style = Stroke(size.height * 0.20f)
            )
            drawCircle(
                color = Color.White,
                radius = size.height * 0.52f,
                center = center,
                style = Stroke(size.height * 0.14f)
            )
        }
    }
}

@Composable
private fun SvSquare(
    hue: Float,
    saturation: Float,
    value: Float,
    onSvChange: (Float, Float) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColor = remember(hue) { hsvToColor(hue, 1f, 1f) }
    var w by remember { mutableStateOf(0f) }
    var h by remember { mutableStateOf(0f) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            .onSizeChanged {
                w = it.width.toFloat()
                h = it.height.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSvChange(
                        (offset.x / w).coerceIn(0f, 1f),
                        1f - (offset.y / h).coerceIn(0f, 1f)
                    )
                    onCommit()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onCommit() },
                    onDragCancel = { onCommit() },
                    onDrag = { change, _ ->
                        change.consume()
                        onSvChange(
                            (change.position.x / w).coerceIn(0f, 1f),
                            1f - (change.position.y / h).coerceIn(0f, 1f)
                        )
                    }
                )
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        }
        Canvas(Modifier.matchParentSize()) {
            val x = saturation * size.width
            val y = (1f - value) * size.height
            val radius = size.height * 0.08f
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = radius,
                center = Offset(x, y),
                style = Stroke(3.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(x, y),
                style = Stroke(2.dp.toPx())
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = color,
            border = BorderStroke(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.size(36.dp)
        ) {
            if (selected) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun MiniMapBackground(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFFEAE5D8))

        drawRoundRect(
            color = Color(0xFFC6DDAE),
            topLeft = Offset(w * 0.08f, h * 0.56f),
            size = Size(w * 0.32f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.04f)
        )
        drawCircle(
            color = Color(0xFFAFD7E8),
            radius = w * 0.11f,
            center = Offset(w * 0.80f, h * 0.18f)
        )

        val main = w * 0.05f
        val minor = w * 0.03f
        drawLine(Color(0xFFC9C1AF), Offset(0f, h * 0.40f), Offset(w, h * 0.46f), strokeWidth = main + w * 0.02f)
        drawLine(Color(0xFFFFFFFF), Offset(0f, h * 0.40f), Offset(w, h * 0.46f), strokeWidth = main)
        drawLine(Color(0xFFC9C1AF), Offset(0f, h * 0.82f), Offset(w, h * 0.74f), strokeWidth = main + w * 0.02f)
        drawLine(Color(0xFFFFFFFF), Offset(0f, h * 0.82f), Offset(w, h * 0.74f), strokeWidth = main)
        drawLine(Color(0xFFC9C1AF), Offset(w * 0.24f, 0f), Offset(w * 0.20f, h), strokeWidth = main + w * 0.02f)
        drawLine(Color(0xFFFFFFFF), Offset(w * 0.24f, 0f), Offset(w * 0.20f, h), strokeWidth = main)
        drawLine(Color(0xFFC9C1AF), Offset(w * 0.66f, 0f), Offset(w * 0.70f, h), strokeWidth = main + w * 0.02f)
        drawLine(Color(0xFFFFFFFF), Offset(w * 0.66f, 0f), Offset(w * 0.70f, h), strokeWidth = main)
        drawLine(Color(0xFFFFFFFF), Offset(0f, h * 0.10f), Offset(w * 0.55f, h * 0.06f), strokeWidth = minor)
        drawLine(Color(0xFFFFFFFF), Offset(w * 0.92f, h * 0.60f), Offset(w, h * 0.68f), strokeWidth = minor)
    }
}

@Composable
private fun IconOptionCard(
    icon: AircraftIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            ) {
                MiniMapBackground(Modifier.fillMaxSize())
                PlaneIcon(
                    heading = 0f,
                    onGround = false,
                    icon = icon,
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                )
            }
            Text(
                text = icon.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    subtitle: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun DragHandleDots(tint: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val dotRadius = size.minDimension * 0.09f
        val spacing = size.width / 3f
        for (i in 0..2) {
            drawCircle(
                color = tint,
                radius = dotRadius,
                center = Offset(spacing * (i + 0.5f), size.height / 2f)
            )
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
