package com.daniel.flighttracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class RateLimitedException(val retryAfterSeconds: Int?) : IOException("OpenSky rate limited")

data class PollResult(
    val flights: List<Flight>,
    val remainingCredits: Int?,
    val retryAfterSeconds: Int?
)

class OpenSkyClient(
    private val clientId: String = "",
    private val clientSecret: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private var accessToken: String? = null
    private var tokenExpiryMillis: Long = 0L

    private suspend fun ensureToken(): String? {
        if (clientId.isEmpty() || clientSecret.isEmpty()) return null
        val now = System.currentTimeMillis()
        if (accessToken != null && now < tokenExpiryMillis - 60_000L) return accessToken

        val formBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .build()
        val request = Request.Builder()
            .url(AUTH_URL)
            .post(formBody)
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("OpenSky login failed: HTTP ${response.code}")
        }
        val body = response.body?.string() ?: throw IOException("Empty auth response")
        val root = json.parseToJsonElement(body).jsonObject
        accessToken = root["access_token"]?.jsonPrimitive?.content
        val expiresIn = root["expires_in"]?.jsonPrimitive?.intOrNull ?: 1800
        tokenExpiryMillis = now + expiresIn * 1000L
        return accessToken
    }

    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            return@withContext "Enter both Client ID and Client Secret first."
        }
        try {
            ensureToken() ?: return@withContext "Login failed: no token received."
            val result = fetchFlights(BoundingBox(52.51, 52.49, 13.41, 13.39))
            val credits = result.remainingCredits?.toString() ?: "?"
            "Connected — $credits credits left today"
        } catch (e: RateLimitedException) {
            "Connected, but currently rate limited (wait ${e.retryAfterSeconds ?: "a bit"}s)"
        } catch (e: Exception) {
            "Login failed: ${e.message ?: "unknown error"}"
        }
    }

    suspend fun fetchFlights(bbox: BoundingBox? = null): PollResult = withContext(Dispatchers.IO) {
        val url = buildString {
            append(BASE_URL)
            append("/states/all")
            if (bbox != null) {
                append("?lamin=").append(bbox.latMin)
                append("&lomin=").append(bbox.lonMin)
                append("&lamax=").append(bbox.latMax)
                append("&lomax=").append(bbox.lonMax)
            }
        }
        val requestBuilder = Request.Builder().url(url).get()
        val token = ensureToken()
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (response.code == 429) {
            val retryAfter = response.header("X-Rate-Limit-Retry-After-Seconds")?.toIntOrNull()
            throw RateLimitedException(retryAfter)
        }
        if (!response.isSuccessful) {
            throw IOException("OpenSky HTTP ${response.code}")
        }
        val remaining = response.header("X-Rate-Limit-Remaining")?.toIntOrNull()

        val body = response.body?.string() ?: throw IOException("Empty response")
        try {
            val root = json.parseToJsonElement(body).jsonObject
            val states = root["states"] as? JsonArray ?: return@withContext PollResult(emptyList(), remaining, null)
            val flights = states.mapNotNull { raw ->
                val row = raw as? JsonArray ?: return@mapNotNull null
                parseRow(row)
            }
            PollResult(flights, remaining, null)
        } catch (e: Exception) {
            throw IOException("Failed to parse OpenSky response", e)
        }
    }

    private fun parseRow(row: JsonArray): Flight? {
        val lat = row.getOrNull(6)?.jsonPrimitive?.doubleOrNull ?: return null
        val lon = row.getOrNull(5)?.jsonPrimitive?.doubleOrNull ?: return null
        if (lat.isNaN() || lon.isNaN()) return null

        val icao = row.getOrNull(0)?.jsonPrimitive?.content ?: return null
        return Flight(
            icao24 = icao,
            callsign = row.getOrNull(1)?.stringOrNull(),
            originCountry = row.getOrNull(2)?.stringOrNull(),
            longitude = lon,
            latitude = lat,
            baroAltitude = row.getOrNull(7)?.jsonPrimitive?.doubleOrNull,
            onGround = row.getOrNull(8)?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
            velocity = row.getOrNull(9)?.jsonPrimitive?.doubleOrNull,
            heading = row.getOrNull(10)?.jsonPrimitive?.doubleOrNull,
            verticalRate = row.getOrNull(11)?.jsonPrimitive?.doubleOrNull,
            geoAltitude = row.getOrNull(13)?.jsonPrimitive?.doubleOrNull
        )
    }

    private fun kotlinx.serialization.json.JsonElement?.stringOrNull(): String? =
        if (this == null || this is JsonNull) null else this.jsonPrimitive.content
            .takeIf { it.isNotBlank() }

    private companion object {
        const val BASE_URL = "https://opensky-network.org/api"
        const val AUTH_URL = "https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token"
    }
}
