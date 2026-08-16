package com.example.transit_app.app.presentation.home.models

import androidx.compose.ui.graphics.vector.ImageVector
import org.osmdroid.util.GeoPoint

data class DisplayCardItem(val title: String, val subtitle: String, val icon: ImageVector, val geoPoint: GeoPoint)
