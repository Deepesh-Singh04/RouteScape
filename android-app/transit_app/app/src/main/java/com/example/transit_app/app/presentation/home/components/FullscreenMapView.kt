package com.example.transit_app.app.presentation.home.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.transit_app.app.presentation.home.utils.FusedLocationProvider
import com.example.transit_app.app.presentation.home.utils.RouteResult
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
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale

@Composable
fun FullscreenMapView(
    selectedLocation: GeoPoint?,
    triggerUserCentering: Boolean,
    onCenteringComplete: () -> Unit,
    onRouteError: (String) -> Unit = {},
    onLiveLocationUpdate: (GeoPoint) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDarkTheme = isSystemInDarkTheme()

    val sharedPreferences = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    Configuration.getInstance().load(context, sharedPreferences)
    Configuration.getInstance().userAgentValue = "RouteScapeMVP/1.0"

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    var liveUserLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var startLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var destinationLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var pendingLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var showSelectionDialog by remember { mutableStateOf(false) }

    var routeData by remember { mutableStateOf(RouteResult()) }
    val defaultAnchor = remember { GeoPoint(28.6692, 77.4538) }

    DisposableEffect(lifecycleOwner, locationOverlayRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> locationOverlayRef?.enableMyLocation()
                Lifecycle.Event.ON_PAUSE -> locationOverlayRef?.disableMyLocation()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            locationOverlayRef?.disableMyLocation()
        }
    }

    LaunchedEffect(selectedLocation) {
        if (selectedLocation != null) {
            destinationLocation = selectedLocation
            if ((startLocation == null) && (mapViewRef != null)) {
                val gpsOverlay = mapViewRef!!.overlays.asSequence().filterIsInstance<MyLocationNewOverlay>().firstOrNull()
                startLocation = gpsOverlay?.myLocation ?: liveUserLocation ?: defaultAnchor
            }
        }
    }

    LaunchedEffect(startLocation, liveUserLocation, destinationLocation) {
        val start = startLocation ?: liveUserLocation ?: defaultAnchor
        val dest = destinationLocation
        if (dest != null) {
            val result = fetchRouteFromOSRM(start, dest)

            if (result.points.isNotEmpty()) {
                routeData = result
            } else {
                routeData = RouteResult(points = listOf(start, dest))
                val errorMsg = result.errorMessage ?: "Unable to calculate route."
                onRouteError(errorMsg)
            }
        } else {
            routeData = RouteResult()
        }
    }

    LaunchedEffect(triggerUserCentering, liveUserLocation) {
        if (triggerUserCentering && liveUserLocation != null) {
            mapViewRef?.controller?.animateTo(liveUserLocation, 18.0, 1000L)
            onCenteringComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    val tileUrl = if (isDarkTheme) "https://a.basemaps.cartocdn.com/dark_all/" else "https://a.basemaps.cartocdn.com/light_all/"
                    val cartoDbSource = XYTileSource(
                        "CartoDB",
                        1, 20, 256, ".png",
                        arrayOf(tileUrl)
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

                    val fusedProvider = FusedLocationProvider(ctx)
                    val locationOverlay = object : MyLocationNewOverlay(fusedProvider, this) {
                        override fun onLocationChanged(location: android.location.Location?, source: IMyLocationProvider?) {
                            super.onLocationChanged(location, source)
                            if (location != null) {
                                val newGeoPoint = GeoPoint(location.latitude, location.longitude)
                                liveUserLocation = newGeoPoint
                                onLiveLocationUpdate(newGeoPoint)
                            }
                        }
                    }

                    locationOverlay.isDrawAccuracyEnabled = true
                    locationOverlay.enableMyLocation()

                    locationOverlay.runOnFirstFix {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            controller.animateTo(locationOverlay.myLocation, 18.0, 1000L)
                            liveUserLocation = locationOverlay.myLocation
                            onCenteringComplete()
                        }
                    }

                    overlays.add(locationOverlay)
                    locationOverlayRef = locationOverlay

                    controller.setZoom(15.0)

                    val initialCenter = liveUserLocation ?: locationOverlay.myLocation ?: defaultAnchor
                    controller.setCenter(initialCenter)

                    mapViewRef = this
                }
            },
            update = { mapView ->
                val tileUrl = if (isDarkTheme) "https://a.basemaps.cartocdn.com/dark_all/" else "https://a.basemaps.cartocdn.com/light_all/"
                mapView.setTileSource(XYTileSource("CartoDB", 1, 20, 256, ".png", arrayOf(tileUrl)))

                if (triggerUserCentering) {
                    val targetLocation = liveUserLocation ?: mapView.overlays.asSequence().filterIsInstance<MyLocationNewOverlay>().firstOrNull()?.myLocation

                    if (targetLocation != null) {
                        mapView.controller.animateTo(targetLocation, 18.0, 1000L)
                        onCenteringComplete()
                    }
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

                if (routeData.points.isNotEmpty()) {
                    val routeColor = if (isDarkTheme) "#38BDF8" else "#2563EB"
                    val routeLine = Polyline(mapView).apply {
                        outlinePaint.color = routeColor.toColorInt()
                        outlinePaint.strokeWidth = 14f
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        setPoints(routeData.points)
                    }

                    val casingLine = Polyline(mapView).apply {
                        outlinePaint.color = if (isDarkTheme) "#1AFFFFFF".toColorInt() else "#1A000000".toColorInt()
                        outlinePaint.strokeWidth = 20f
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        setPoints(routeData.points)
                    }

                    mapView.overlays.add(casingLine)
                    mapView.overlays.add(routeLine)

                    try {
                        val boundingBox = BoundingBox.fromGeoPoints(routeData.points)
                        mapView.zoomToBoundingBox(boundingBox, true, 140)
                    } catch (_: Exception) {
                        destinationLocation?.let { mapView.controller.animateTo(it) }
                    }
                }

                mapView.invalidate()
            }
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { mapViewRef?.controller?.zoomIn() }) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider(modifier = Modifier.width(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
                IconButton(onClick = { mapViewRef?.controller?.zoomOut() }) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider(modifier = Modifier.width(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
                IconButton(
                    onClick = {
                        liveUserLocation?.let { mapViewRef?.controller?.animateTo(it, 18.0, 1000L) }
                    }
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        AnimatedVisibility(
            visible = routeData.distanceMeters > 0,
            enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 130.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Travel Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    val formattedDistance = if (routeData.distanceMeters >= 1000) {
                        String.format(Locale.getDefault(), "%.1f km", routeData.distanceMeters / 1000f)
                    } else {
                        "${routeData.distanceMeters} m"
                    }

                    val minutes = (routeData.durationSeconds / 60).toInt()
                    val formattedTime = if (minutes > 60) "${minutes / 60} hr ${minutes % 60} min" else "$minutes min"

                    Text(
                        text = "$formattedDistance • $formattedTime",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        if (showSelectionDialog && pendingLocation != null) {
            AlertDialog(
                onDismissRequest = {
                    showSelectionDialog = false
                    pendingLocation = null
                },
                containerColor = MaterialTheme.colorScheme.surface,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text(text = "Selected Point Action", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Choose how you want to use this pinned location:", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            )
        }
    }
}