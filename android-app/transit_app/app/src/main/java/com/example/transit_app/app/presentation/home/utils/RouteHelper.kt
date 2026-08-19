package com.example.transit_app.app.presentation.home.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

// Added an errorMessage parameter to safely pass offline alerts to the UI
data class RouteResult(
    val points: List<GeoPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val durationSeconds: Double = 0.0,
    val errorMessage: String? = null
)

suspend fun fetchRouteFromOSRM(start: GeoPoint, dest: GeoPoint): RouteResult {
    return withContext(Dispatchers.IO) {
        try {
            val urlString = "https://router.project-osrm.org/route/v1/driving/" +
                    "${start.longitude},${start.latitude};" +
                    "${dest.longitude},${dest.latitude}" +
                    "?overview=full&geometries=geojson"

            Log.d("OSRM_API", "Requesting Route: $urlString")

            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; RouteScape/1.0)")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val routes = jsonObject.optJSONArray("routes")

                if (routes != null && routes.length() > 0) {
                    val route = routes.getJSONObject(0)

                    val distance = route.optDouble("distance", 0.0)
                    val duration = route.optDouble("duration", 0.0)

                    val geometry = route.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    val pathPoints = mutableListOf<GeoPoint>()

                    for (i in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(i)
                        val lon = coord.getDouble(0)
                        val lat = coord.getDouble(1)
                        pathPoints.add(GeoPoint(lat, lon))
                    }

                    RouteResult(pathPoints, distance, duration, null)
                } else {
                    RouteResult(errorMessage = "No valid roads found between these locations.")
                }
            } else {
                RouteResult(errorMessage = "Routing server is currently down. Please try again later.")
            }

            // 1. Catches "No DNS / Offline" errors
        } catch (e: UnknownHostException) {
            Log.e("OSRM_API", "Offline Error: ${e.localizedMessage}")
            RouteResult(errorMessage = "You are offline. Please check your internet connection.")

            // 2. Catches "Network Disabled / Timeout" errors
        } catch (e: ConnectException) {
            Log.e("OSRM_API", "Connection Error: ${e.localizedMessage}")
            RouteResult(errorMessage = "You are offline. Please check your internet connection.")

            // 3. Catches any other crashes
        } catch (e: Exception) {
            Log.e("OSRM_API", "Unknown Exception: ${e.localizedMessage}")
            RouteResult(errorMessage = "Unable to fetch street route. Showing straight line.")
        }
    }
}