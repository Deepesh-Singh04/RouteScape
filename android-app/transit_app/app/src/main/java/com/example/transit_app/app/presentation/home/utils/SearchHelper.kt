package com.example.transit_app.app.presentation.home.utils

import android.util.Log
import com.example.transit_app.app.presentation.home.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

suspend fun fetchPlacesFromNominatim(query: String): List<SearchResult> {
    if (query.trim().length < 3) return emptyList()

    return withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=5"

            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "RouteScapeMVP/1.0 (Android)")
            connection.connectTimeout = 6000
            connection.readTimeout = 6000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val results = mutableListOf<SearchResult>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val displayName = item.optString("display_name", "")
                    val parts = displayName.split(",", limit = 2)
                    val name = item.optString("name").ifEmpty { parts.firstOrNull() ?: displayName }
                    val address = parts.getOrNull(1)?.trim() ?: displayName
                    val lat = item.optDouble("lat", 0.0)
                    val lon = item.optDouble("lon", 0.0)

                    results.add(SearchResult(title = name, fullAddress = address, latitude = lat, longitude = lon))
                }
                results
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NOMINATIM_API", "Search exception: ${e.localizedMessage}")
            emptyList()
        }
    }
}