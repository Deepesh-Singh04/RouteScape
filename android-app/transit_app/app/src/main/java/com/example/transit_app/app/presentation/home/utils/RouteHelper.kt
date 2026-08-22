package com.example.transit_app.app.presentation.home.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.util.Locale

data class RoutePath(
    val points: List<GeoPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val durationSeconds: Double = 0.0,
    val instructions: List<String> = emptyList()
)

data class RouteResult(
    val routes: List<RoutePath> = emptyList(),
    val errorMessage: String? = null
)

suspend fun fetchRouteFromOSRM(start: GeoPoint, dest: GeoPoint): RouteResult {
    return withContext(Dispatchers.IO) {
        try {
            val urlString = "https://router.project-osrm.org/route/v1/driving/" +
                    "${start.longitude},${start.latitude};" +
                    "${dest.longitude},${dest.latitude}" +
                    "?overview=simplified&geometries=geojson&steps=true&alternatives=true"

            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; RouteScape/1.0)")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val routesArray = jsonObject.optJSONArray("routes")

                if (routesArray != null && routesArray.length() > 0) {
                    val parsedRoutes = List(routesArray.length()) { r ->
                        val route = routesArray.getJSONObject(r)
                        val distance = route.optDouble("distance", 0.0)
                        val duration = route.optDouble("duration", 0.0)

                        val geometry = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        val pathPoints = List(coordinates.length()) { i ->
                            val coord = coordinates.getJSONArray(i)
                            GeoPoint(coord.getDouble(1), coord.getDouble(0))
                        }

                        val instructionsList = mutableListOf<String>()
                        val legs = route.optJSONArray("legs")
                        if (legs != null && legs.length() > 0) {
                            val steps = legs.getJSONObject(0).optJSONArray("steps")
                            if (steps != null) {
                                for (j in 0 until steps.length()) {
                                    val step = steps.getJSONObject(j)
                                    val maneuver = step.optJSONObject("maneuver")
                                    val type = maneuver?.optString("type", "") ?: ""
                                    val modifier = maneuver?.optString("modifier", "") ?: ""
                                    val name = step.optString("name", "")

                                    var instruction = type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                    if (modifier.isNotEmpty() && modifier != "straight") instruction += " $modifier"
                                    if (name.isNotEmpty()) instruction += " onto $name"

                                    instruction = instruction.replace("Depart", "Head")
                                        .replace("Arrive", "Arrive at destination")

                                    instructionsList.add(instruction)
                                }
                            }
                        }
                        RoutePath(pathPoints, distance, duration, instructionsList)
                    }
                    RouteResult(routes = parsedRoutes, errorMessage = null)
                } else {
                    RouteResult(errorMessage = "No valid roads found between these locations.")
                }
            } else {
                RouteResult(errorMessage = "Routing server is currently down. Please try again later.")
            }
        } catch (e: UnknownHostException) {
            RouteResult(errorMessage = "You are offline. Please check your internet connection.")
        } catch (e: ConnectException) {
            RouteResult(errorMessage = "You are offline. Please check your internet connection.")
        } catch (e: Exception) {
            RouteResult(errorMessage = "Unable to fetch street route. Showing straight line.")
        }
    }
}