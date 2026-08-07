package com.daniel.flighttracker.data

data class BoundingBox(
    val latMax: Double,
    val latMin: Double,
    val lonMax: Double,
    val lonMin: Double
)

data class Flight(
    val icao24: String,
    val callsign: String?,
    val originCountry: String?,
    val longitude: Double?,
    val latitude: Double?,
    val baroAltitude: Double?,
    val onGround: Boolean,
    val velocity: Double?,
    val heading: Double?,
    val verticalRate: Double?,
    val geoAltitude: Double?
)
