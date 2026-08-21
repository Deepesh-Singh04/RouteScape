package com.example.transit_app.data.models

import com.google.gson.annotations.SerializedName

data class LocalDataResponse(
    val transit_options: List<PlaceDto> = emptyList(),
    val heritage_sites: List<PlaceDto> = emptyList()
)

data class PlaceDto(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val type: String? = null,
    val status: String? = null,
    @SerializedName("distance_meters")
    val distanceMeters: Int? = null
)