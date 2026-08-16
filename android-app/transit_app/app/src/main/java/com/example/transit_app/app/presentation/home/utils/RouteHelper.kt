package com.example.transit_app.app.presentation.home.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.URL

suspend fun fetchRouteFromOSRM(start: GeoPoint, dest: GeoPoint): List<GeoPoint> {
    return withContext(Dispatchers.IO) {
        try {
            // OSRM expects coordinates in {longitude},{latitude} format
            val urlString = "https://router.project-osrm.org/route/v1/driving/" +
                    "${start.longitude},${start.latitude};" +
                    "${dest.longitude},${dest.latitude}" +
                    "?overview=full&geometries=geojson"

            val connection = URL(urlString).openConnection()
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val response = connection.getInputStream().bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(response)
            val routes = jsonObject.optJSONArray("routes")

            if (routes != null && routes.length() > 0) {
                val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                val pathPoints = mutableListOf<GeoPoint>()

                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(i)
                    val lon = coord.getDouble(0)
                    val lat = coord.getDouble(1)
                    pathPoints.add(GeoPoint(lat, lon))
                }
                pathPoints
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}