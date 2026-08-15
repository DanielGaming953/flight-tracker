package com.daniel.flighttracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.location.LocationManager
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import com.daniel.flighttracker.data.AircraftIcon
import com.daniel.flighttracker.data.AircraftMetadataStore
import com.daniel.flighttracker.data.AircraftInfoClient
import com.daniel.flighttracker.data.BoundingBox
import com.daniel.flighttracker.data.CardElement
import com.daniel.flighttracker.data.isStat
import com.daniel.flighttracker.data.Flight
import com.daniel.flighttracker.data.MapStyle
import com.daniel.flighttracker.data.OpenSkyClient
import com.daniel.flighttracker.data.RateLimitedException
import com.daniel.flighttracker.data.Settings
import com.daniel.flighttracker.data.formatAltitude
import com.daniel.flighttracker.data.formatSpeed
import kotlinx.coroutines.delay
import kotlin.math.log2
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

private fun adaptivePollInterval(remaining: Int?, credits: Int): Long {
    val base = POLL_INTERVAL_MS * credits
    return when {
        remaining == null -> base
        remaining < 5 -> 60_000L
        remaining < 15 -> 30_000L
        remaining < 40 -> 15_000L
        else -> base
    }
}

private class EsriTileSource : XYTileSource(
    "Esri_World_Imagery_v2",
    0, 19, 256, ".png",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    "Esri, Maxar, Earthstar Geographics"
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return getBaseUrl() + z + "/" + y + "/" + x + mImageFilenameEnding
    }
}

private val satelliteTileSource: ITileSource = EsriTileSource()

@Composable
fun FlightMapScreen(
    settings: Settings,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val api = remember(settings.clientId, settings.clientSecret) {
        OpenSkyClient(settings.clientId, settings.clientSecret)
    }
    val metadataStore = remember { AircraftMetadataStore(AircraftInfoClient()) }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var flights by remember { mutableStateOf<List<Flight>>(emptyList()) }
    var displayedCount by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Flight?>(null) }
    var remainingCredits by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    val managedOverlays = remember { mutableListOf<Overlay>() }
    var lastCellDeg by remember { mutableStateOf(-1.0) }

    fun addFlightMarker(m: MapView, flight: Flight) {
        val marker = Marker(m).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            infoWindow = null
            position = GeoPoint(flight.latitude ?: 0.0, flight.longitude ?: 0.0)
            icon = BitmapDrawable(
                context.resources,
                planeBitmap(context, (flight.heading ?: 0.0).toFloat(), flight.onGround, settings.aircraftIcon)
            )
            relatedObject = flight
            setOnMarkerClickListener { clicked, _ ->
                selected = clicked.relatedObject as? Flight
                true
            }
        }
        m.overlays.add(marker)
        managedOverlays.add(marker)
    }

    fun addClusterMarker(m: MapView, builder: ClusterBuilder) {
        val marker = Marker(m).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            infoWindow = null
            position = GeoPoint(builder.centerLat, builder.centerLon)
            icon = BitmapDrawable(context.resources, clusterBitmap(context, builder.count))
            setOnMarkerClickListener { clicked, _ ->
                m.controller.animateTo(clicked.position, m.zoomLevelDouble + 1, null)
                true
            }
        }
        m.overlays.add(marker)
        managedOverlays.add(marker)
    }

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

    fun rebuildMarkers(m: MapView) {
        m.overlays.removeAll(managedOverlays)
        managedOverlays.clear()

        val zoom = m.zoomLevelDouble
        val filtered = flights.filter { f ->
            val matchesQuery = matchesQuery(f)
            val hasPosition = f.latitude != null && f.longitude != null
            val airborne = zoom >= 6.0 || query.isNotBlank() || !f.onGround
            matchesQuery && hasPosition && airborne
        }
        if (filtered.isEmpty()) {
            displayedCount = 0
            m.invalidate()
            return
        }

        val limited = if (filtered.size > settings.maxFlights) {
            filtered.sortedWith(
                compareByDescending<Flight> { it.onGround }
                    .thenByDescending { it.velocity ?: 0.0 }
            ).take(settings.maxFlights)
        } else {
            filtered
        }
        displayedCount = limited.size

        val cellDeg = cellDegrees(zoom)
        lastCellDeg = cellDeg

        val clusterMap = LinkedHashMap<String, ClusterBuilder>()
        for (f in limited) {
            val lat = f.latitude ?: continue
            val lon = f.longitude ?: continue
            val key = "${(lat / cellDeg).roundToLong()},${(lon / cellDeg).roundToLong()}"
            clusterMap.getOrPut(key) { ClusterBuilder() }.add(f)
        }

        for (builder in clusterMap.values) {
            if (builder.count == 1) {
                addFlightMarker(m, builder.flights.first())
            } else {
                addClusterMarker(m, builder)
            }
        }
        m.invalidate()
    }

    LaunchedEffect(Unit) {
        var pollInterval = POLL_INTERVAL_MS
        while (true) {
            val m = mapView
            if (m != null && m.isAttachedToWindow) {
                try {
                    val bb = m.boundingBox
                    val bbox = BoundingBox(bb.latNorth, bb.latSouth, bb.lonEast, bb.lonWest)
                    val result = api.fetchFlights(bbox)
                    flights = result.flights
                    rebuildMarkers(m)
                    remainingCredits = result.remainingCredits
                    error = null
                    pollInterval = adaptivePollInterval(result.remainingCredits, creditsPerPoll(bbox))
                } catch (e: RateLimitedException) {
                    error = "OpenSky rate limit hit — slowing down"
                    pollInterval = ((e.retryAfterSeconds ?: 60) * 1000L).coerceAtLeast(60_000L)
                } catch (e: Exception) {
                    error = e.message ?: "Could not fetch flights"
                    pollInterval = POLL_INTERVAL_MS
                }
            }
            delay(pollInterval)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val m = mapView
            val zoomedIn = m != null && m.zoomLevelDouble >= 7.0
            val toPrefetch = if (zoomedIn) {
                flights.filter { it.latitude != null && !it.onGround }.take(120)
            } else {
                emptyList()
            }
            metadataStore.prefetch(toPrefetch)
            delay(4000)
        }
    }

    LaunchedEffect(query) {
        mapView?.let { m ->
            rebuildMarkers(m)
            if (query.isNotBlank()) {
                val matches = filteredForQuery()
                if (matches.isNotEmpty()) {
                    val lat = matches.map { it.latitude!! }.average()
                    val lon = matches.map { it.longitude!! }.average()
                    m.controller.animateTo(GeoPoint(lat, lon), max(m.zoomLevelDouble, 6.5), null)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OSMView(
            modifier = Modifier.fillMaxSize(),
            mapStyle = settings.mapStyle,
            onReady = { mapView = it },
            onZoomChanged = { m ->
                if (lastCellDeg != cellDegrees(m.zoomLevelDouble)) rebuildMarkers(m)
            },
            onMapTap = { selected = null }
        )

        val locationLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> if (granted) centerOnUser(context, mapView) }

        if (settings.showSearchBar) {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 56.dp, top = 4.dp, bottom = 4.dp)
            )
        }

        val noResults = query.isNotBlank() && filteredForQuery().isEmpty()
        if (noResults) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp),
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
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp)
        )

        if (settings.showStatusBar) {
            StatusLine(
                count = displayedCount,
                error = error,
                remainingCredits = remainingCredits,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 72.dp, bottom = 16.dp)
            )
        }

        if (settings.showLocationButton) {
            LocationButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) centerOnUser(context, mapView)
                else locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

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
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
            )
        }
    }
}

private fun centerOnUser(context: Context, mapView: MapView?) {
    val map = mapView ?: return
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
    val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    if (loc != null) {
        map.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
        map.controller.setZoom(10.0)
    }
}

@Composable
private fun OSMView(
    modifier: Modifier = Modifier,
    mapStyle: MapStyle,
    onReady: (MapView) -> Unit,
    onZoomChanged: (MapView) -> Unit,
    onMapTap: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val minZoom = remember {
        val metrics = context.resources.displayMetrics
        val largestPixels = max(metrics.widthPixels, metrics.heightPixels).toDouble()
        (log2((largestPixels / 256.0).coerceAtLeast(1.0)) + 0.1)
            .coerceIn(2.0, 4.0)
    }
    var lastCenter by remember { mutableStateOf(GeoPoint(49.0, 8.0)) }
    var lastZoom by remember { mutableStateOf(5.0) }

    val mapView = remember(mapStyle) {
        buildMapView(
            context,
            minZoom,
            if (mapStyle == MapStyle.SATELLITE) satelliteTileSource else TileSourceFactory.MAPNIK,
            lastCenter,
            lastZoom,
            onMapTap
        )
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                lastCenter = GeoPoint(mapView.mapCenter.latitude, mapView.mapCenter.longitude)
                return false
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                lastCenter = GeoPoint(mapView.mapCenter.latitude, mapView.mapCenter.longitude)
                lastZoom = mapView.zoomLevelDouble
                onZoomChanged(mapView)
                return false
            }
        })
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(mapStyle) {
        onReady(mapView)
        onZoomChanged(mapView)
    }

    key(mapStyle) {
        AndroidView(
            factory = { mapView },
            modifier = modifier,
            onRelease = { it.onDetach() }
        )
    }
}

private fun buildMapView(
    context: Context,
    minZoom: Double,
    tileSource: ITileSource,
    center: GeoPoint,
    zoom: Double,
    onMapTap: () -> Unit
): MapView {
    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    return MapView(context).apply {
        setTileSource(tileSource)
        setMultiTouchControls(true)
        setBuiltInZoomControls(false)
        setHorizontalMapRepetitionEnabled(false)
        setVerticalMapRepetitionEnabled(false)
        minZoomLevel = minZoom
        maxZoomLevel = 19.0
        setScrollableAreaLimitLatitude(85.0, -85.0, 0)
        setScrollableAreaLimitLongitude(-180.0, 180.0, 0)
        controller.setZoom(zoom)
        controller.setCenter(center)
        overlays.add(
            0,
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    onMapTap()
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            })
        )
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
                    .fillMaxHeight(),
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
    val credits = remainingCredits?.let { " • $it credits left" } ?: ""
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (error != null)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Text(
            text = when {
                error != null -> error
                count == 0 -> "No aircraft in view$credits"
                else -> "$count aircraft tracked$credits"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (error != null)
                MaterialTheme.colorScheme.onErrorContainer
            else
                MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun LocationButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = "Center on my location",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(8.dp)
        )
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
        color = MaterialTheme.colorScheme.surface
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
                                        formatAltitude(flight.baroAltitude ?: flight.geoAltitude, settings.altitudeUnit),
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
                        Text(
                            text = if (flight.onGround) "On the ground" else "Airborne",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (flight.onGround) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary
                        )
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
    val flights = mutableListOf<Flight>()

    fun add(flight: Flight) {
        count++
        latSum += flight.latitude ?: 0.0
        lonSum += flight.longitude ?: 0.0
        flights.add(flight)
    }

    val centerLat: Double get() = latSum / count
    val centerLon: Double get() = lonSum / count
}

private val planeBitmapCache = mutableMapOf<String, Bitmap>()

fun planeBitmap(context: Context, heading: Float, onGround: Boolean, icon: AircraftIcon): Bitmap {
    val bucket = (((heading + 360) % 360).roundToInt() / 15 * 15).toString()
    val key = "$bucket-${if (onGround) "g" else "a"}-${icon.name}"
    return planeBitmapCache.getOrPut(key) {
        val density = context.resources.displayMetrics.density
        val size = (26 * density).roundToInt()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        canvas.rotate(bucket.toFloat(), cx, cy)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (onGround) AndroidColor.rgb(150, 160, 180) else AndroidColor.rgb(79, 163, 255)
            style = Paint.Style.FILL
        }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2 * density
            strokeJoin = Paint.Join.ROUND
        }

        when (icon) {
            AircraftIcon.ARROW -> {
                val wingSpan = size * 0.38f
                val nose = cy - size * 0.36f
                val tail = cy + size * 0.36f
                val path = Path().apply {
                    moveTo(cx, nose)
                    lineTo(cx - wingSpan, tail)
                    lineTo(cx, tail - size * 0.12f)
                    lineTo(cx + wingSpan, tail)
                    close()
                }
                canvas.drawPath(path, fill)
                canvas.drawPath(path, outline)
            }

            AircraftIcon.AIRCRAFT -> {
                val s = size.toFloat()
                val silhouette = Path().apply {
                    moveTo(cx + 0.1602f * s, cy + 0.4990f * s)
                    lineTo(cx + 0.1445f * s, cy + 0.4951f * s)
                    lineTo(cx + 0.1348f * s, cy + 0.4951f * s)
                    lineTo(cx + 0.1328f * s, cy + 0.4932f * s)
                    lineTo(cx + 0.1230f * s, cy + 0.4932f * s)
                    lineTo(cx + 0.1113f * s, cy + 0.4893f * s)
                    lineTo(cx + 0.0781f * s, cy + 0.4854f * s)
                    lineTo(cx + 0.0664f * s, cy + 0.4814f * s)
                    lineTo(cx + 0.0449f * s, cy + 0.4795f * s)
                    lineTo(cx + 0.0332f * s, cy + 0.4756f * s)
                    lineTo(cx + 0.0117f * s, cy + 0.4736f * s)
                    lineTo(cx + 0.0098f * s, cy + 0.4717f * s)
                    lineTo(cx + -0.0117f * s, cy + 0.4717f * s)
                    lineTo(cx + -0.0234f * s, cy + 0.4756f * s)
                    lineTo(cx + -0.1016f * s, cy + 0.4873f * s)
                    lineTo(cx + -0.1035f * s, cy + 0.4893f * s)
                    lineTo(cx + -0.1348f * s, cy + 0.4932f * s)
                    lineTo(cx + -0.1465f * s, cy + 0.4971f * s)
                    lineTo(cx + -0.1631f * s, cy + 0.4980f * s)
                    lineTo(cx + -0.1631f * s, cy + 0.4453f * s)
                    lineTo(cx + -0.0537f * s, cy + 0.3848f * s)
                    lineTo(cx + -0.0576f * s, cy + 0.2676f * s)
                    lineTo(cx + -0.0596f * s, cy + 0.2656f * s)
                    lineTo(cx + -0.0596f * s, cy + 0.2227f * s)
                    lineTo(cx + -0.0615f * s, cy + 0.2207f * s)
                    lineTo(cx + -0.0615f * s, cy + 0.1777f * s)
                    lineTo(cx + -0.0635f * s, cy + 0.1758f * s)
                    lineTo(cx + -0.0635f * s, cy + 0.1328f * s)
                    lineTo(cx + -0.0654f * s, cy + 0.1309f * s)
                    lineTo(cx + -0.0654f * s, cy + 0.0879f * s)
                    lineTo(cx + -0.0674f * s, cy + 0.0859f * s)
                    lineTo(cx + -0.0684f * s, cy + 0.0557f * s)
                    lineTo(cx + -0.2148f * s, cy + 0.1143f * s)
                    lineTo(cx + -0.2246f * s, cy + 0.1201f * s)
                    lineTo(cx + -0.2480f * s, cy + 0.1279f * s)
                    lineTo(cx + -0.2578f * s, cy + 0.1338f * s)
                    lineTo(cx + -0.2812f * s, cy + 0.1416f * s)
                    lineTo(cx + -0.2910f * s, cy + 0.1475f * s)
                    lineTo(cx + -0.3145f * s, cy + 0.1553f * s)
                    lineTo(cx + -0.3242f * s, cy + 0.1611f * s)
                    lineTo(cx + -0.4043f * s, cy + 0.1924f * s)
                    lineTo(cx + -0.4141f * s, cy + 0.1982f * s)
                    lineTo(cx + -0.4238f * s, cy + 0.2021f * s)
                    lineTo(cx + -0.4268f * s, cy + 0.2012f * s)
                    lineTo(cx + -0.4268f * s, cy + 0.1211f * s)
                    lineTo(cx + -0.2705f * s, cy + 0.0059f * s)
                    lineTo(cx + -0.2686f * s, cy + 0.0020f * s)
                    lineTo(cx + -0.2676f * s, cy + -0.0869f * s)
                    lineTo(cx + -0.2051f * s, cy + -0.0869f * s)
                    lineTo(cx + -0.2041f * s, cy + -0.0469f * s)
                    lineTo(cx + -0.2012f * s, cy + -0.0459f * s)
                    lineTo(cx + -0.0732f * s, cy + -0.1406f * s)
                    lineTo(cx + -0.0732f * s, cy + -0.3496f * s)
                    lineTo(cx + -0.0713f * s, cy + -0.3516f * s)
                    lineTo(cx + -0.0713f * s, cy + -0.3730f * s)
                    lineTo(cx + -0.0693f * s, cy + -0.3750f * s)
                    lineTo(cx + -0.0674f * s, cy + -0.3984f * s)
                    lineTo(cx + -0.0576f * s, cy + -0.4355f * s)
                    lineTo(cx + -0.0439f * s, cy + -0.4668f * s)
                    lineTo(cx + -0.0361f * s, cy + -0.4785f * s)
                    lineTo(cx + -0.0195f * s, cy + -0.4951f * s)
                    lineTo(cx + -0.0078f * s, cy + -0.5010f * s)
                    lineTo(cx + 0.0059f * s, cy + -0.5010f * s)
                    lineTo(cx + 0.0176f * s, cy + -0.4951f * s)
                    lineTo(cx + 0.0342f * s, cy + -0.4785f * s)
                    lineTo(cx + 0.0479f * s, cy + -0.4551f * s)
                    lineTo(cx + 0.0635f * s, cy + -0.4082f * s)
                    lineTo(cx + 0.0674f * s, cy + -0.3750f * s)
                    lineTo(cx + 0.0693f * s, cy + -0.3730f * s)
                    lineTo(cx + 0.0693f * s, cy + -0.3516f * s)
                    lineTo(cx + 0.0713f * s, cy + -0.3496f * s)
                    lineTo(cx + 0.0713f * s, cy + -0.1406f * s)
                    lineTo(cx + 0.1992f * s, cy + -0.0459f * s)
                    lineTo(cx + 0.2021f * s, cy + -0.0469f * s)
                    lineTo(cx + 0.2031f * s, cy + -0.0869f * s)
                    lineTo(cx + 0.2656f * s, cy + -0.0869f * s)
                    lineTo(cx + 0.2666f * s, cy + 0.0020f * s)
                    lineTo(cx + 0.2686f * s, cy + 0.0059f * s)
                    lineTo(cx + 0.4248f * s, cy + 0.1211f * s)
                    lineTo(cx + 0.4238f * s, cy + 0.2021f * s)
                    lineTo(cx + 0.3652f * s, cy + 0.1787f * s)
                    lineTo(cx + 0.3555f * s, cy + 0.1729f * s)
                    lineTo(cx + 0.3320f * s, cy + 0.1650f * s)
                    lineTo(cx + 0.3223f * s, cy + 0.1592f * s)
                    lineTo(cx + 0.2988f * s, cy + 0.1514f * s)
                    lineTo(cx + 0.2891f * s, cy + 0.1455f * s)
                    lineTo(cx + 0.2656f * s, cy + 0.1377f * s)
                    lineTo(cx + 0.2559f * s, cy + 0.1318f * s)
                    lineTo(cx + 0.2324f * s, cy + 0.1240f * s)
                    lineTo(cx + 0.2227f * s, cy + 0.1182f * s)
                    lineTo(cx + 0.1992f * s, cy + 0.1104f * s)
                    lineTo(cx + 0.1895f * s, cy + 0.1045f * s)
                    lineTo(cx + 0.1660f * s, cy + 0.0967f * s)
                    lineTo(cx + 0.1562f * s, cy + 0.0908f * s)
                    lineTo(cx + 0.1328f * s, cy + 0.0830f * s)
                    lineTo(cx + 0.1230f * s, cy + 0.0771f * s)
                    lineTo(cx + 0.0996f * s, cy + 0.0693f * s)
                    lineTo(cx + 0.0898f * s, cy + 0.0635f * s)
                    lineTo(cx + 0.0703f * s, cy + 0.0557f * s)
                    lineTo(cx + 0.0654f * s, cy + 0.0566f * s)
                    lineTo(cx + 0.0518f * s, cy + 0.3848f * s)
                    lineTo(cx + 0.1611f * s, cy + 0.4453f * s)
                    lineTo(cx + 0.1602f * s, cy + 0.4990f * s)
                    close()
                }
                canvas.save()
                canvas.scale(0.72f, 0.72f, cx, cy)
                canvas.drawPath(silhouette, fill)
                canvas.drawPath(silhouette, outline)
                canvas.restore()
            }
        }
        bmp
    }
}

private fun clusterBitmap(context: Context, count: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (46 * density).roundToInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f
    val radius = size * 0.42f

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(15, 76, 146)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, radius, fill)

    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
    }
    canvas.drawCircle(cx, cy, radius, stroke)

    val text = if (count > 999) "999+" else count.toString()
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 15 * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val baseline = cy - (textPaint.ascent() + textPaint.descent()) / 2
    canvas.drawText(text, cx, baseline, textPaint)
    return bmp
}
