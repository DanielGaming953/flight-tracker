package com.daniel.flighttracker.desktop

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daniel.flighttracker.data.AircraftIcon
import com.daniel.flighttracker.data.AircraftInfoClient
import com.daniel.flighttracker.data.AircraftMetadataStore
import com.daniel.flighttracker.data.BoundingBox
import com.daniel.flighttracker.data.CardElement
import com.daniel.flighttracker.data.CardPosition
import com.daniel.flighttracker.data.Flight
import com.daniel.flighttracker.data.OpenSkyClient
import com.daniel.flighttracker.data.RateLimitedException
import com.daniel.flighttracker.data.Settings
import com.daniel.flighttracker.data.formatAltitude
import com.daniel.flighttracker.data.formatSpeed
import com.daniel.flighttracker.data.isStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val POLL_INTERVAL_MS = 5_000L

private fun creditsPerPoll(bbox: BoundingBox): Int {
    val area = (bbox.latMax - bbox.latMin).let { max(it, -it) } *
        (bbox.lonMax - bbox.lonMin).let { max(it, -it) }
    return when {
        area <= 25 -> 1
        area <= 100 -> 2
        area <= 400 -> 3
        else -> 4
    }
}

private fun adaptivePollInterval(remaining: Int?, credits: Int, baseMs: Long): Long {
    val base = baseMs * credits
    return when {
        remaining == null -> base
        remaining < 5 -> 60_000L
        remaining < 15 -> 30_000L
        remaining < 40 -> 15_000L
        else -> base
    }
}

private fun cellDegrees(zoom: Double): Double {
    val px = when {
        zoom <= 3.5 -> 140.0
        zoom <= 6.0 -> 90.0
        else -> 64.0
    }
    return (px * 360.0) / (256.0 * 2.0.pow(zoom))
}

private class ClusterBuilder {
    var count = 0
        private set
    private var latSum = 0.0
    private var lonSum = 0.0

    fun add(flight: Flight) {
        count++
        latSum += flight.latitude ?: 0.0
        lonSum += flight.longitude ?: 0.0
        flights.add(flight)
    }

    val flights = mutableListOf<Flight>()
    val centerLat: Double get() = latSum / count
    val centerLon: Double get() = lonSum / count
}

private data class MapMarker(
    val flight: Flight,
    val lat: Double,
    val lon: Double,
    val x: Float,
    val y: Float
)

private data class ClusterMarker(
    val lat: Double,
    val lon: Double,
    val count: Int,
    val x: Float,
    val y: Float
)

@Composable
fun FlightMapScreen(
    settings: Settings,
    onOpenSettings: () -> Unit
) {
    val api = remember(settings.clientId, settings.clientSecret) {
        OpenSkyClient(settings.clientId, settings.clientSecret)
    }
    val metadataStore = remember { AircraftMetadataStore(AircraftInfoClient()) }
    val mapState = remember {
        MapState(
            initialLat = settings.startLat,
            initialLon = settings.startLon,
            initialZoom = settings.startZoom
        )
    }
    val currentSettings by rememberUpdatedState(settings)
    val tileProvider = remember { TileProvider { currentSettings.mapStyle } }

    LaunchedEffect(settings.mapStyle) {
        tileProvider.clear()
    }

    var flights by remember { mutableStateOf<List<Flight>>(emptyList()) }
    var displayedCount by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Flight?>(null) }
    var remainingCredits by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    val currentMapState by rememberUpdatedState(mapState)
    val currentTileProvider by rememberUpdatedState(tileProvider)
    val currentMetadataStore by rememberUpdatedState(metadataStore)

    fun matchesQuery(f: Flight): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        val md = metadataStore.get(f.icao24)
        val haystack = listOfNotNull(
            f.callsign,
            f.icao24,
            md?.registration,
            md?.model,
            md?.operator
        ).joinToString(" ")
        return haystack.contains(q, ignoreCase = true)
    }

    fun filteredForQuery(): List<Flight> {
        if (query.isBlank()) return emptyList()
        return flights.filter { f ->
            matchesQuery(f) && f.latitude != null && f.longitude != null
        }
    }

    LaunchedEffect(api, settings.pollIntervalSec) {
        var pollInterval = (settings.pollIntervalSec * 1000L).coerceAtLeast(2000L)
        while (true) {
            val bbox = mapState.visibleBoundingBox()
            try {
                val result = api.fetchFlights(bbox)
                flights = result.flights
                remainingCredits = result.remainingCredits
                error = null
                pollInterval = adaptivePollInterval(
                    result.remainingCredits,
                    creditsPerPoll(bbox),
                    settings.pollIntervalSec * 1000L
                )
            } catch (e: RateLimitedException) {
                error = "OpenSky rate limit hit — slowing down"
                pollInterval = ((e.retryAfterSeconds ?: 60) * 1000L).coerceAtLeast(60_000L)
            } catch (e: Exception) {
                error = e.message ?: "Could not fetch flights"
                pollInterval = (settings.pollIntervalSec * 1000L).coerceAtLeast(2000L)
            }
            delay(pollInterval)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val tiles = mapState.tileRange()
            tiles.forEach { (z, tx, ty) ->
                if (tileProvider.needsLoad(z, tx, ty)) {
                    launch { tileProvider.load(z, tx, ty) }
                }
            }
            delay(400)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            println(
                "DBG tiles loaded=${tileProvider.cacheSize()} style=${settings.mapStyle} " +
                    "zoom=${mapState.zoom} center=${mapState.centerLat},${mapState.centerLon} " +
                    "flights=${flights.size} error=${error ?: "none"} credits=${remainingCredits}"
            )
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val zoomedIn = mapState.zoom >= 7.0
            val toPrefetch = if (zoomedIn) {
                flights.filter { it.latitude != null && !it.onGround }.take(120)
            } else {
                emptyList()
            }
            withContext(Dispatchers.IO) {
                metadataStore.prefetch(toPrefetch)
            }
            delay(4000)
        }
    }

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun panBy(fractionX: Double, fractionY: Double) {
        mapState.pan(
            -mapState.viewportWidth * fractionX,
            -mapState.viewportHeight * fractionY
        )
    }

    fun resetView() {
        mapState.centerLat = settings.startLat
        mapState.centerLon = settings.startLon
        mapState.zoom = settings.startZoom
    }

    fun handleMapKey(e: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (e.type != KeyEventType.KeyDown) return false
        return when (e.key) {
            Key.DirectionUp, Key.W -> { panBy(0.0, -0.2); true }
            Key.DirectionDown, Key.S -> { panBy(0.0, 0.2); true }
            Key.DirectionLeft, Key.A -> { panBy(-0.2, 0.0); true }
            Key.DirectionRight, Key.D -> { panBy(0.2, 0.0); true }
            Key.Equals -> {
                mapState.zoomAt(1.0, mapState.viewportWidth / 2, mapState.viewportHeight / 2)
                true
            }
            Key.Minus -> {
                mapState.zoomAt(-1.0, mapState.viewportWidth / 2, mapState.viewportHeight / 2)
                true
            }
            Key.Home -> { resetView(); true }
            else -> false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { handleMapKey(it) }
    ) {
        MapCanvas(
            mapState = currentMapState,
            tileProvider = currentTileProvider,
            settings = currentSettings,
            metadataStore = currentMetadataStore,
            flights = flights,
            query = query,
            selectedIcao = selected?.icao24,
            onSelect = { selected = it },
            onMapTap = { selected = null },
            onCountChange = { displayedCount = it },
            focusRequester = focusRequester,
            modifier = Modifier.fillMaxSize()
        )

        if (settings.showSearchBar) {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 560.dp)
                    .padding(start = 16.dp, end = 64.dp, top = 8.dp)
            )
        }

        val noResults = query.isNotBlank() && filteredForQuery().isEmpty()
        if (noResults && settings.showNoResultsChip) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "No flights found for \"${query.trim()}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        SettingsButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 8.dp)
        )

        if (settings.showStatusBar) {
            StatusLine(
                count = displayedCount,
                error = error,
                remainingCredits = remainingCredits,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 520.dp)
                    .padding(start = 16.dp, end = 16.dp, bottom = 40.dp)
            )
        }

        if (settings.showFlightCard) {
            selected?.let { flight ->
                LaunchedEffect(flight.icao24) {
                    metadataStore.prefetch(listOf(flight), maxPerCycle = 1)
                }
                FlightDetailsCard(
                    flight = flight,
                    model = metadataStore.get(flight.icao24)?.modelLabel,
                    registration = metadataStore.get(flight.icao24)?.registration,
                    operator = metadataStore.get(flight.icao24)?.operator,
                    settings = settings,
                    onClose = { selected = null },
                    modifier = Modifier
                        .align(
                            if (settings.cardPosition == CardPosition.BOTTOM_RIGHT)
                                Alignment.BottomEnd
                            else
                                Alignment.BottomStart
                        )
                        .padding(16.dp)
                        .width(settings.cardWidth.dp)
                )
            }
        } else {
            selected = null
        }
    }
}

@Composable
private fun MapCanvas(
    mapState: MapState,
    tileProvider: TileProvider,
    settings: Settings,
    metadataStore: AircraftMetadataStore,
    flights: List<Flight>,
    query: String,
    selectedIcao: String?,
    onSelect: (Flight) -> Unit,
    onMapTap: () -> Unit,
    onCountChange: (Int) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val placeholderColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        Color(0xFF1E222A)
    } else {
        Color(0xFFDDE1E7)
    }

    val latestFlights by rememberUpdatedState(flights)
    val latestQuery by rememberUpdatedState(query)
    val latestSettings by rememberUpdatedState(settings)
    val latestMetadata by rememberUpdatedState(metadataStore)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestOnMapTap by rememberUpdatedState(onMapTap)
    val latestOnCountChange by rememberUpdatedState(onCountChange)

    fun computeMarkers(): List<Any> {
        val zoom = mapState.zoom
        val q = latestQuery.trim()
        val filtered = latestFlights.filter { f ->
            val matchesQuery = q.isEmpty() || listOfNotNull(
                f.callsign,
                f.icao24,
                latestMetadata.get(f.icao24)?.registration,
                latestMetadata.get(f.icao24)?.model,
                latestMetadata.get(f.icao24)?.operator
            ).joinToString(" ").contains(q, ignoreCase = true)
            val hasPosition = f.latitude != null && f.longitude != null
            val airborne = !f.onGround ||
                (latestSettings.showGround && (zoom >= 6.0 || q.isNotEmpty()))
            matchesQuery && hasPosition && airborne
        }
        if (filtered.isEmpty()) {
            latestOnCountChange(0)
            return emptyList()
        }

        val limited = if (filtered.size > latestSettings.maxFlights) {
            filtered.sortedWith(
                compareByDescending<Flight> { it.onGround }
                    .thenByDescending { it.velocity ?: 0.0 }
            ).take(latestSettings.maxFlights)
        } else {
            filtered
        }
        latestOnCountChange(limited.size)

        if (!latestSettings.showAircraftMarkers) return emptyList()

        if (!latestSettings.showClusters) {
            return limited.mapNotNull { f ->
                val lat = f.latitude ?: return@mapNotNull null
                val lon = f.longitude ?: return@mapNotNull null
                val (x, y) = mapState.flightToScreen(lat, lon)
                MapMarker(f, lat, lon, x.toFloat(), y.toFloat())
            }
        }

        val cellDeg = cellDegrees(zoom)
        val clusterMap = LinkedHashMap<String, ClusterBuilder>()
        for (f in limited) {
            val lat = f.latitude ?: continue
            val lon = f.longitude ?: continue
            val key = "${(lat / cellDeg).roundToLong()},${(lon / cellDeg).roundToLong()}"
            clusterMap.getOrPut(key) { ClusterBuilder() }.add(f)
        }

        val result = mutableListOf<Any>()
        for (builder in clusterMap.values) {
            if (builder.count == 1) {
                val f = builder.flights.first()
                val (lat, lon) = f.latitude!! to f.longitude!!
                val (x, y) = mapState.flightToScreen(lat, lon)
                result.add(MapMarker(f, lat, lon, x.toFloat(), y.toFloat()))
            } else {
                val (x, y) = mapState.flightToScreen(builder.centerLat, builder.centerLon)
                result.add(
                    ClusterMarker(
                        builder.centerLat,
                        builder.centerLon,
                        builder.count,
                        x.toFloat(),
                        y.toFloat()
                    )
                )
            }
        }
        return result
    }

    fun handleTap(lat: Double, lon: Double, x: Double, y: Double) {
        val markers = computeMarkers()
        val nearest = markers.filterIsInstance<MapMarker>()
            .minByOrNull { m ->
                val dlat = m.lat - lat
                val dlon = m.lon - lon
                dlat * dlat + dlon * dlon
            }
        if (nearest != null) {
            val dlat = nearest.lat - lat
            val dlon = nearest.lon - lon
            if (dlat * dlat + dlon * dlon <= 0.25) {
                latestOnSelect(nearest.flight)
                return
            }
        }
        val cluster = markers.filterIsInstance<ClusterMarker>()
            .minByOrNull { m ->
                val dlat = m.lat - lat
                val dlon = m.lon - lon
                dlat * dlat + dlon * dlon
            }
        if (cluster != null) {
            val dlat = cluster.lat - lat
            val dlon = cluster.lon - lon
            if (dlat * dlat + dlon * dlon <= 0.25) {
                mapState.zoomAt(1.0, x, y)
                return
            }
        }
        latestOnMapTap()
    }

    val canvasScheme = MaterialTheme.colorScheme

    Canvas(
        modifier = modifier
            .onSizeChanged {
                size = it
                mapState.viewportWidth = it.width.toDouble()
                mapState.viewportHeight = it.height.toDouble()
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var pressX = 0f
                    var pressY = 0f
                    var lastX = 0f
                    var lastY = 0f
                    var pressed = false
                    var lastTapAt = 0L
                    var lastTapX = 0f
                    var lastTapY = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        when (event.type) {
                            PointerEventType.Press -> {
                                focusRequester.requestFocus()
                                pressed = true
                                pressX = change.position.x
                                pressY = change.position.y
                                lastX = pressX
                                lastY = pressY
                            }
                            PointerEventType.Move -> {
                                if (pressed) {
                                    mapState.pan(
                                        -(change.position.x - lastX).toDouble(),
                                        -(change.position.y - lastY).toDouble()
                                    )
                                    lastX = change.position.x
                                    lastY = change.position.y
                                }
                            }
                            PointerEventType.Release -> {
                                if (pressed) {
                                    val dist = hypot(
                                        change.position.x - pressX,
                                        change.position.y - pressY
                                    )
                                    if (dist < 10f) {
                                        val now = System.currentTimeMillis()
                                        val sinceLast = now - lastTapAt
                                        val doubleDist = hypot(
                                            pressX - lastTapX,
                                            pressY - lastTapY
                                        )
                                        if (sinceLast in 1..400 && doubleDist < 16f) {
                                            if (latestSettings.doubleTapZoom) {
                                                mapState.zoomAt(
                                                    1.0,
                                                    pressX.toDouble(),
                                                    pressY.toDouble()
                                                )
                                                lastTapAt = 0L
                                            } else {
                                                lastTapAt = now
                                                val (lat, lon) = mapState.screenToLatLon(
                                                    pressX.toDouble(),
                                                    pressY.toDouble()
                                                )
                                                handleTap(
                                                    lat,
                                                    lon,
                                                    pressX.toDouble(),
                                                    pressY.toDouble()
                                                )
                                            }
                                        } else {
                                            lastTapAt = now
                                            lastTapX = pressX
                                            lastTapY = pressY
                                            val (lat, lon) = mapState.screenToLatLon(
                                                pressX.toDouble(),
                                                pressY.toDouble()
                                            )
                                            handleTap(lat, lon, pressX.toDouble(), pressY.toDouble())
                                        }
                                    }
                                    pressed = false
                                }
                            }
                            PointerEventType.Scroll -> {
                                if (latestSettings.scrollZoom) {
                                    val invert = if (latestSettings.invertScrollZoom) -1.0 else 1.0
                                    val delta = change.scrollDelta
                                    if (abs(delta.x) > abs(delta.y)) {
                                        mapState.pan(
                                            -delta.x * invert * mapState.viewportWidth * 0.03,
                                            0.0
                                        )
                                    } else {
                                        mapState.zoomAt(
                                            invert * if (delta.y > 0) 1.0 else -1.0,
                                            change.position.x.toDouble(),
                                            change.position.y.toDouble()
                                        )
                                    }
                                }
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        mapState.viewportWidth = size.width.toDouble()
        mapState.viewportHeight = size.height.toDouble()

        val z = mapState.zoom.toInt().coerceIn(2, 19)
        val scale = Math.pow(2.0, (z - mapState.zoom).toDouble())
        val viewLeftZ = mapState.viewLeft * scale
        val viewTopZ = mapState.viewTop * scale
        val tileSize = (256.0 * scale).toFloat()

        val viewportW = mapState.viewportWidth.toFloat()
        val viewportH = mapState.viewportHeight.toFloat()

        mapState.tileRange().forEach { (tz, tx, ty) ->
            val screenX = (tx * 256.0 - viewLeftZ).toFloat()
            val screenY = (ty * 256.0 - viewTopZ).toFloat()

            val bitmap = tileProvider.cached(tz, tx, ty)
            if (bitmap != null) {
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(screenX.roundToInt(), screenY.roundToInt()),
                    dstSize = IntSize(tileSize.roundToInt() + 1, tileSize.roundToInt() + 1)
                )
            } else {
                var cz = tz
                var cx = tx
                var cy = ty
                var fallback: ImageBitmap? = null
                while (cz > 2) {
                    cz--
                    cx /= 2
                    cy /= 2
                    fallback = tileProvider.cached(cz, cx, cy)
                    if (fallback != null) break
                }
                if (fallback != null) {
                    val zoomDiff = tz - cz
                    val ancWorldSize = 256.0 * 2.0.pow(zoomDiff)
                    drawImage(
                        image = fallback,
                        dstOffset = IntOffset(
                            (cx * ancWorldSize * scale - viewLeftZ).roundToInt(),
                            (cy * ancWorldSize * scale - viewTopZ).roundToInt()
                        ),
                        dstSize = IntSize(
                            (ancWorldSize * scale).roundToInt() + 1,
                            (ancWorldSize * scale).roundToInt() + 1
                        )
                    )
                } else {
                    drawRect(
                        color = placeholderColor,
                        topLeft = Offset(screenX, screenY),
                        size = Size(tileSize, tileSize)
                    )
                }
            }
        }

        val markers = computeMarkers()

        if (latestSettings.showGrid) {
            val step = when {
                z >= 10 -> 1.0
                z >= 8 -> 2.0
                z >= 6 -> 5.0
                z >= 4 -> 10.0
                else -> 20.0
            }
            val gridColor = canvasScheme.primary.copy(alpha = 0.16f)
            val lonStart = mapState.worldXToLon(mapState.viewLeft)
            val lonEnd = mapState.worldXToLon(mapState.viewLeft + mapState.viewportWidth)
            var lon = Math.floor(lonStart / step) * step
            while (lon <= lonEnd) {
                val (x, _) = mapState.flightToScreen(0.0, lon)
                drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), viewportH), 1f)
                lon += step
            }
            val latTop = mapState.worldYToLat(mapState.viewTop)
            val latBottom = mapState.worldYToLat(mapState.viewTop + mapState.viewportHeight)
            var lat = Math.floor(latBottom / step) * step
            while (lat <= latTop) {
                val (_, y) = mapState.flightToScreen(lat, 0.0)
                drawLine(gridColor, Offset(0f, y.toFloat()), Offset(viewportW, y.toFloat()), 1f)
                lat += step
            }
        }

        val selectedMarker = markers.filterIsInstance<MapMarker>().firstOrNull {
            it.flight.icao24 == selectedIcao
        }

        markers.forEach { m ->
            when (m) {
                is MapMarker -> {
                    if (m.x < -40 || m.x > viewportW + 40) return@forEach
                    if (m.y < -40 || m.y > viewportH + 40) return@forEach
                    val flight = m.flight
                    val isSelected = flight.icao24 == selectedIcao
                    val scale = latestSettings.markerScale.coerceIn(0.5f, 2f)
                    val fill = when {
                        latestSettings.customMarkerColors && !flight.onGround ->
                            Color(latestSettings.airborneColor)
                        latestSettings.customMarkerColors ->
                            Color(latestSettings.groundColor)
                        flight.onGround -> canvasScheme.onSurfaceVariant
                        else -> canvasScheme.primary
                    }
                    val outline = Color.Black.copy(alpha = 0.6f)
                    val s = if (isSelected) 30f * scale else 22f * scale

                    if (isSelected) {
                        drawCircle(
                            color = fill.copy(alpha = 0.25f),
                            radius = 18f * scale,
                            center = Offset(m.x, m.y)
                        )
                        drawCircle(
                            color = fill,
                            radius = 18f * scale,
                            center = Offset(m.x, m.y),
                            style = Stroke(2f)
                        )
                    }

                    rotate(
                        degrees = (flight.heading ?: 0.0).toFloat(),
                        pivot = Offset(m.x, m.y)
                    ) {
                        when (latestSettings.aircraftIcon) {
                            AircraftIcon.ARROW ->
                                drawArrowPath(m.x, m.y, s, fill, outline)
                            AircraftIcon.AIRCRAFT ->
                                drawSilhouettePath(m.x, m.y, s, fill, outline)
                        }
                    }
                }
                is ClusterMarker -> {
                    val scale = latestSettings.markerScale.coerceIn(0.5f, 2f)
                    val radius = 17f * scale
                    drawCircle(
                        color = canvasScheme.primary,
                        radius = radius,
                        center = Offset(m.x, m.y)
                    )
                    drawCircle(
                        color = canvasScheme.onPrimary,
                        radius = radius,
                        center = Offset(m.x, m.y),
                        style = Stroke(2f)
                    )
                    val label = if (m.count > 999) "999+" else m.count.toString()
                    val style = TextStyle(
                        color = canvasScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val layout = textMeasurer.measure(label, style)
                    drawText(
                        textLayoutResult = layout,
                        color = canvasScheme.onPrimary,
                        topLeft = Offset(
                            m.x - layout.size.width / 2f,
                            m.y - layout.size.height / 2f
                        )
                    )
                }
            }
        }

        if (latestSettings.showAircraftLabels && z >= 7) {
            val labelAlpha = ((z - 7) / 2f).coerceIn(0f, 1f)
            var drawn = 0
            for (m in markers) {
                if (m !is MapMarker) continue
                if (drawn >= 120) break
                val flight = m.flight
                val callsign = flight.callsign?.trim()
                if (callsign.isNullOrBlank() || flight.onGround) continue
                if (m.x < -60 || m.x > viewportW + 60) continue
                if (m.y < -20 || m.y > viewportH + 60) continue
                val style = TextStyle(
                    color = Color.White.copy(alpha = labelAlpha),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                val layout = textMeasurer.measure(callsign, style)
                val bw = layout.size.width + 12
                val bh = layout.size.height + 6
                val tx = m.x - bw / 2f
                val ty = m.y + 20f
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.55f * labelAlpha),
                    topLeft = Offset(tx, ty),
                    size = Size(bw.toFloat(), bh.toFloat()),
                    cornerRadius = CornerRadius(7f, 7f)
                )
                drawText(
                    textLayoutResult = layout,
                    color = Color.White.copy(alpha = labelAlpha),
                    topLeft = Offset(tx + 6f, ty + 3f)
                )
                drawn++
            }
        }

        if (selectedMarker != null) {
            val flight = selectedMarker.flight
            val label = flight.callsign?.takeIf { it.isNotBlank() }
                ?: flight.icao24.uppercase()
            val style = TextStyle(
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            val layout = textMeasurer.measure(label, style)
            val bw = layout.size.width + 16
            val bh = layout.size.height + 10
            drawRoundRect(
                color = Color(0xCC000000),
                topLeft = Offset(selectedMarker.x + 12f, selectedMarker.y - 14f),
                size = Size(bw.toFloat(), bh.toFloat()),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawText(
                textLayoutResult = layout,
                color = Color.White,
                topLeft = Offset(selectedMarker.x + 20f, selectedMarker.y - 10f)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 14.dp, end = 6.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search callsign, flight…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(7.dp)
        )
    }
}

@Composable
private fun StatusLine(
    count: Int,
    error: String?,
    remainingCredits: Int?,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val time = java.time.Instant.ofEpochMilli(now)
        .atZone(java.time.ZoneOffset.UTC)
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    val credits = remainingCredits?.let { " • $it credits" } ?: ""
    val live = error == null && count > 0

    val pulse = rememberInfiniteTransition(label = "live")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )
    val dotColor = if (error != null) MaterialTheme.colorScheme.error
    else if (live) Color(0xFF22C55E)
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (error != null)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = if (live || error != null) dotAlpha else 0.6f))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    error != null -> error
                    count == 0 -> "No aircraft in view • $time UTC"
                    else -> "$count aircraft tracked • $time UTC$credits"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (error != null)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FlightDetailsCard(
    flight: Flight,
    model: String?,
    registration: String?,
    operator: String?,
    settings: Settings,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = flight.callsign?.trim()?.ifBlank { "Unknown flight" }
                        ?: "Unknown flight",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            val visible = settings.cardOrder.filter { it !in settings.hiddenCardElements }
            var first = true
            var i = 0
            while (i < visible.size) {
                val item = visible[i]
                if (!first) Spacer(Modifier.height(10.dp))
                when {
                    item.isStat -> {
                        val stats = mutableListOf<CardElement>()
                        while (i < visible.size && visible[i].isStat) {
                            stats.add(visible[i])
                            i++
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stats.forEach { stat ->
                                val statModifier = Modifier.weight(1f)
                                when (stat) {
                                    CardElement.ALT -> DetailStat(
                                        "ALT",
                                        formatAltitude(
                                            flight.baroAltitude ?: flight.geoAltitude,
                                            settings.altitudeUnit
                                        ),
                                        statModifier
                                    )
                                    CardElement.SPD -> DetailStat(
                                        "SPD",
                                        formatSpeed(flight.velocity, settings.speedUnit),
                                        statModifier
                                    )
                                    CardElement.HDG -> DetailStat(
                                        "HDG",
                                        flight.heading?.let { "${it.roundToInt()}°" } ?: "—",
                                        statModifier
                                    )
                                    CardElement.VS -> DetailStat(
                                        "VS",
                                        flight.verticalRate?.let { "${it.roundToInt()} m/s" } ?: "—",
                                        statModifier
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                    CardElement.INFO == item -> {
                        Text(
                            text = if (model != null) model else "Loading aircraft model…",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (model == null)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = buildString {
                                registration?.trim()?.takeIf { it.isNotBlank() }?.let {
                                    append(it)
                                    append("  •  ")
                                }
                                append(flight.originCountry ?: "Unknown origin")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (operator != null) {
                            Text(
                                text = "Operated by $operator",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        i++
                    }
                    CardElement.STATUS == item -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (flight.onGround) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (flight.onGround) "On the ground" else "Airborne",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (flight.onGround) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                        i++
                    }
                    else -> i++
                }
                first = false
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
