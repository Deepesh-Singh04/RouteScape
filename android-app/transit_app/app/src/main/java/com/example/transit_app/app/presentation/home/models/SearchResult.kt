package com.example.transit_app.app.presentation.home.models

import org.osmdroid.util.GeoPoint

data class SearchResult(
    val title: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double
) {
    val geoPoint: GeoPoint
        get() = GeoPoint(latitude, longitude)
}