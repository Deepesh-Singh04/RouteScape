package com.example.transit_app.app.presentation.home.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.example.transit_app.app.presentation.home.utils.fetchRouteFromOSRM
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun FullscreenMapView(
    selectedLocation: GeoPoint?,
    triggerUserCentering: Boolean,
    onCenteringComplete: () -> Unit,
    onRouteError: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    Configuration.getInstance().load(context, sharedPreferences)
    Configuration.getInstance().userAgentValue = "RouteScapeMVP/1.0"

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    var startLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var destinationLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var pendingLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var showSelectionDialog by remember { mutableStateOf(false) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    val defaultAnchor = remember { GeoPoint(28.60882, 77.03588) }

    LaunchedEffect(selectedLocation) {
        if (selectedLocation != null) {
            destinationLocation = selectedLocation
            if ((startLocation == null) && (mapViewRef != null)) {
                val gpsOverlay = mapViewRef!!.overlays.asSequence().filterIsInstance<MyLocationNewOverlay>().firstOrNull()
                startLocation = gpsOverlay?.myLocation ?: defaultAnchor
            }
        }
    }

    LaunchedEffect(startLocation, destinationLocation) {
        val start = startLocation
        val dest = destinationLocation
        if (start != null && dest != null) {
            val result = fetchRouteFromOSRM(start, dest)
            if (result.isNotEmpty()) {
                routePoints = result
            } else {
                routePoints = listOf(start, dest)
                onRouteError("Unable to fetch street route. Showing straight line.")
            }
        } else {
            routePoints = emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    val cartoDbSource = XYTileSource(
                        "CartoDB",
                        1, 20, 256, ".png",
                        arrayOf("https://a.basemaps.cartocdn.com/light_all/")
                    )
                    setTileSource(cartoDbSource)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                    val mReceive = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                if (startLocation == null) {
                                    startLocation = p
                                } else {
                                    pendingLocation = p
                                    showSelectionDialog = true
                                }
                            }
                            return true
                        }
                    }
                    overlays.add(MapEventsOverlay(mReceive))

                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)

                    controller.setZoom(15.0)
                    controller.setCenter(defaultAnchor)

                    mapViewRef = this
                }
            },
            update = { mapView ->
                if (triggerUserCentering) {
                    val gpsOverlay = mapView.overlays.filterIsInstance<MyLocationNewOverlay>().firstOrNull()
                    gpsOverlay?.myLocation?.let { liveLocation ->
                        mapView.controller.animateTo(liveLocation)
                    }
                    onCenteringComplete()
                }

                mapView.overlays.removeAll {
                    it is Polyline || (it is Marker && (it.id == "START_MARKER" || it.id == "DEST_MARKER"))
                }

                startLocation?.let { startPoint ->
                    val startMarker = Marker(mapView).apply {
                        id = "START_MARKER"
                        position = startPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Starting Point"
                        snippet = "Lat: ${"%.4f".format(startPoint.latitude)}, Lng: ${"%.4f".format(startPoint.longitude)}"
                    }
                    mapView.overlays.add(startMarker)
                }

                destinationLocation?.let { destPoint ->
                    val destMarker = Marker(mapView).apply {
                        id = "DEST_MARKER"
                        position = destPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Destination"
                        snippet = "Lat: ${"%.4f".format(destPoint.latitude)}, Lng: ${"%.4f".format(destPoint.longitude)}"
                    }
                    mapView.overlays.add(destMarker)
                }

                if (routePoints.isNotEmpty()) {
                    val routeLine = Polyline(mapView).apply {
                        outlinePaint.color = "#1976D2".toColorInt()
                        outlinePaint.strokeWidth = 12f
                        setPoints(routePoints)
                    }
                    mapView.overlays.add(routeLine)

                    try {
                        val boundingBox = BoundingBox.fromGeoPoints(routePoints)
                        mapView.zoomToBoundingBox(boundingBox, true, 140)
                    } catch (e: Exception) {
                        destinationLocation?.let { mapView.controller.animateTo(it) }
                    }
                }

                mapView.invalidate()
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 380.dp)
        ) {
            FloatingActionButton(
                onClick = { mapViewRef?.controller?.zoomIn() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            Spacer(modifier = Modifier.height(8.dp))

            FloatingActionButton(
                onClick = { mapViewRef?.controller?.zoomOut() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }

        if (showSelectionDialog && pendingLocation != null) {
            AlertDialog(
                onDismissRequest = {
                    showSelectionDialog = false
                    pendingLocation = null
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text(text = "Selected Point Action") },
                text = { Text("Choose how you want to use this pinned location:") },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                startLocation = pendingLocation
                                showSelectionDialog = false
                                pendingLocation = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.TripOrigin, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Starting Point")
                        }

                        Button(
                            onClick = {
                                destinationLocation = pendingLocation
                                showSelectionDialog = false
                                pendingLocation = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set as Destination")
                        }

                        TextButton(
                            onClick = {
                                showSelectionDialog = false
                                pendingLocation = null
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}