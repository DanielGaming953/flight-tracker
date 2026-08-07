package com.daniel.flighttracker.data

data class AircraftMetadata(
    val icao24: String,
    val registration: String?,
    val typeCode: String?,
    val model: String?,
    val operator: String?
) {
    val modelLabel: String
        get() = listOfNotNull(model?.trim(), typeCode?.trim())
            .firstOrNull { it.isNotBlank() } ?: "Unknown aircraft"
}
