package com.example.transit_app.data.models

data class LocalDataResponse(
    val anchor_location: AnchorLocation,
    val radius_km: Double,
    val transit_options: List<TransitOption>,
    val heritage_sites: List<HeritageSite>
)

data class AnchorLocation(
    val name: String,
    val coordinates: Coordinates
)

data class Coordinates(
    val lat: Double,
    val lng: Double
)

data class TransitOption(
    val id: String,
    val type: String,
    val name: String,
    val distance_meters: Int,
    val eta_mins: Int,
    val status: String,
    val coordinates: Coordinates
)

data class HeritageSite(
    val id: String,
    val name: String,
    val category: String,
    val distance_meters: Int,
    val entry_fee_inr: Int,
    val coordinates: Coordinates
)