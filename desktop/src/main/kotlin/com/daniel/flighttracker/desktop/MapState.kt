package com.daniel.flighttracker.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.daniel.flighttracker.data.BoundingBox
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan

class MapState(
    initialLat: Double = 50.0,
    initialLon: Double = 10.0,
    initialZoom: Double = 5.0
) {
    var centerLat by mutableStateOf(initialLat)
    var centerLon by mutableStateOf(initialLon)
    var zoom by mutableStateOf(initialZoom)
    var viewportWidth by mutableStateOf(800.0)
    var viewportHeight by mutableStateOf(600.0)

    val minZoom: Double get() = 3.0

    val worldSize: Double get() = 256.0 * 2.0.pow(zoom)

    fun lonToWorldX(lon: Double): Double = (lon + 180.0) / 360.0 * worldSize

    fun latToWorldY(lat: Double): Double {
        val rad = lat * PI / 180.0
        return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * worldSize
    }

    fun worldXToLon(x: Double): Double = x / worldSize * 360.0 - 180.0

    fun worldYToLat(y: Double): Double {
        val n = PI * (1.0 - 2.0 * y / worldSize)
        return Math.toDegrees(Math.atan(sinh(n)))
    }

    val centerWorldX: Double get() = lonToWorldX(centerLon)
    val centerWorldY: Double get() = latToWorldY(centerLat)

    val viewLeft: Double get() = centerWorldX - viewportWidth / 2.0
    val viewTop: Double get() = centerWorldY - viewportHeight / 2.0

    fun pan(dx: Double, dy: Double) {
        val nx = centerWorldX + dx
        val ny = centerWorldY + dy
        centerLon = worldXToLon(clampCenterWorldX(nx))
        centerLat = worldYToLat(clampCenterWorldY(ny))
    }

    fun zoomAt(factor: Double, screenX: Double, screenY: Double) {
        val minZ = minZoom.roundToInt().toDouble()
        val newZoom = (zoom + factor).roundToInt().toDouble().coerceIn(minZ, 19.0)
        if (newZoom == zoom) return
        val anchorX = viewLeft + screenX
        val anchorY = viewTop + screenY
        val anchorLon = worldXToLon(anchorX).coerceIn(-180.0, 180.0)
        val anchorLat = worldYToLat(anchorY).coerceIn(-85.0, 85.0)
        zoom = newZoom
        val ax = lonToWorldX(anchorLon)
        val ay = latToWorldY(anchorLat)
        centerLon = worldXToLon(
            clampCenterWorldX(ax - (screenX - viewportWidth / 2.0))
        )
        centerLat = worldYToLat(
            clampCenterWorldY(ay - (screenY - viewportHeight / 2.0))
        )
    }

    private fun clampCenterWorldX(nx: Double): Double {
        val halfView = viewportWidth / 2.0
        val minX = halfView
        val maxX = worldSize - halfView
        return if (maxX > minX) nx.coerceIn(minX, maxX) else worldSize / 2.0
    }

    private fun clampCenterWorldY(ny: Double): Double {
        val halfView = viewportHeight / 2.0
        val minY = halfView
        val maxY = worldSize - halfView
        return if (maxY > minY) ny.coerceIn(minY, maxY) else worldSize / 2.0
    }

    fun tileRange(minZoom: Int = 2): List<Triple<Int, Int, Int>> {
        val z = floor(zoom).toInt().coerceIn(maxOf(minZoom, floor(this.minZoom).toInt()), 19)
        val scale = 2.0.pow(z - zoom)
        val left = (viewLeft * scale / 256.0).let { floor(it).toInt() }
        val top = (viewTop * scale / 256.0).let { floor(it).toInt() }
        val right = ((viewLeft + viewportWidth) * scale / 256.0).let { floor(it).toInt() }
        val bottom = ((viewTop + viewportHeight) * scale / 256.0).let { floor(it).toInt() }
        val max = 2.0.pow(z).toInt()
        return buildList {
            for (tx in left.coerceAtLeast(0)..right.coerceAtMost(max - 1)) {
                for (ty in top.coerceAtLeast(0)..bottom.coerceAtMost(max - 1)) {
                    add(Triple(z, tx, ty))
                }
            }
        }
    }

    fun flightToScreen(lat: Double, lon: Double): Pair<Double, Double> {
        val z = floor(zoom).toInt().coerceIn(2, 19)
        val scale = 2.0.pow(z - zoom)
        val x = (lonToWorldX(lon) - viewLeft) * scale
        val y = (latToWorldY(lat) - viewTop) * scale
        return Pair(x, y)
    }

    fun screenToLatLon(screenX: Double, screenY: Double): Pair<Double, Double> {
        val worldX = viewLeft + screenX
        val worldY = viewTop + screenY
        return Pair(worldYToLat(worldY), worldXToLon(worldX))
    }

    fun visibleBoundingBox(): BoundingBox {
        val (lat1, lon1) = screenToLatLon(0.0, 0.0)
        val (lat2, lon2) = screenToLatLon(viewportWidth, viewportHeight)
        return BoundingBox(
            latMax = maxOf(lat1, lat2),
            latMin = minOf(lat1, lat2),
            lonMax = maxOf(lon1, lon2),
            lonMin = minOf(lon1, lon2)
        )
    }
}
