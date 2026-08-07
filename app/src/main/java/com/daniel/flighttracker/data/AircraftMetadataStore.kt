package com.daniel.flighttracker.data

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.delay

class AircraftMetadataStore(private val client: AircraftInfoClient) {
    val cache = mutableStateMapOf<String, AircraftMetadata>()
    private val failedAt = mutableMapOf<String, Long>()
    private val cooldownMs = 10 * 60_000L

    fun get(icao24: String): AircraftMetadata? = cache[icao24]

    suspend fun prefetch(flights: List<Flight>, maxPerCycle: Int = 6) {
        val now = System.currentTimeMillis()
        val candidates = flights.asSequence()
            .map { it.icao24 }
            .distinct()
            .filter { it !in cache && now - (failedAt[it] ?: 0L) > cooldownMs }
            .take(maxPerCycle)
            .toList()

        for (icao in candidates) {
            try {
                val meta = client.lookup(icao)
                if (meta != null) cache[icao] = meta else failedAt[icao] = now
            } catch (e: Exception) {
                failedAt[icao] = now
                if (e.message?.contains("429") == true) {
                    candidates.forEach { failedAt.putIfAbsent(it, now) }
                    break
                }
            }
            delay(1500)
        }
    }
}
