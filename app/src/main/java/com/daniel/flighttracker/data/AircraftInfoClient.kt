package com.daniel.flighttracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class AircraftInfoClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lookup(icao24: String): AircraftMetadata? = withContext(Dispatchers.IO) {
        val url = "https://api.airplanes.live/v2/icao/$icao24"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "OpenSkyTracker/1.0")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Aircraft DB HTTP ${response.code}")
        val body = response.body?.string() ?: return@withContext null

        try {
            val ac = json.parseToJsonElement(body).jsonObject["ac"]?.jsonArray ?: return@withContext null
            val first = ac.firstOrNull()?.jsonObject ?: return@withContext null
            AircraftMetadata(
                icao24 = icao24,
                registration = first["r"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                typeCode = first["t"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                model = first["desc"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                operator = first["op"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            null
        }
    }
}
