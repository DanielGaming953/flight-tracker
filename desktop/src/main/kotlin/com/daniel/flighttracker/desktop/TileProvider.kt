package com.daniel.flighttracker.desktop

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.daniel.flighttracker.data.MapStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class TileProvider(private val style: () -> MapStyle) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private data class TileKey(val style: MapStyle, val z: Int, val x: Int, val y: Int)

    private val cache = mutableStateMapOf<TileKey, ImageBitmap>()
    private val inFlight = ConcurrentHashMap<TileKey, Boolean>()
    private val failedAt = ConcurrentHashMap<TileKey, Long>()
    private val downloadLimit = Semaphore(4)

    private fun key(z: Int, x: Int, y: Int) = TileKey(style(), z, x, y)

    fun cached(z: Int, x: Int, y: Int): ImageBitmap? = cache[key(z, x, y)]

    fun cacheSize(): Int = cache.size

    fun needsLoad(z: Int, x: Int, y: Int): Boolean {
        val k = key(z, x, y)
        if (cache.containsKey(k)) return false
        if (inFlight.containsKey(k)) return false
        val failed = failedAt[k]
        if (failed != null && System.currentTimeMillis() - failed < RETRY_DELAY_MS) return false
        return true
    }

    suspend fun load(z: Int, x: Int, y: Int) {
        val k = key(z, x, y)
        if (!needsLoad(z, x, y)) return
        if (inFlight.putIfAbsent(k, true) != null) return
        try {
            downloadLimit.withPermit {
                val bitmap = withContext(Dispatchers.IO) { fetch(k) }
                if (bitmap != null) {
                    cache[k] = bitmap
                } else {
                    failedAt[k] = System.currentTimeMillis()
                }
            }
        } finally {
            inFlight.remove(k)
        }
    }

    private fun fetch(k: TileKey): ImageBitmap? {
        val url = when (k.style) {
            MapStyle.STREET -> "https://tile.openstreetmap.org/${k.z}/${k.x}/${k.y}.png"
            MapStyle.SATELLITE -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/${k.z}/${k.y}/${k.x}"
        }
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://github.com/DanielGaming953/flight-tracker")
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    println("DBG tile fail $k code=${resp.code} url=$url")
                    return null
                }
                val bytes = resp.body?.bytes() ?: return null
                val buffered = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
                buffered.toComposeImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clear() {
        cache.clear()
        inFlight.clear()
        failedAt.clear()
    }

    private companion object {
        const val RETRY_DELAY_MS = 30_000L
        const val USER_AGENT = "FlightTrackerDesktop/1.0 (flight tracker hobby app; https://github.com/DanielGaming953/flight-tracker)"
    }
}
