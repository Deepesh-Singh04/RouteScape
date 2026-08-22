package com.example.transit_app.app.presentation.home.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
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
import com.example.transit_app.app.presentation.home.utils.RoutePath
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
import org.osmdroid.views.overlay.milestones.MilestoneBitmapDisplayer
import org.osmdroid.views.overlay.milestones.MilestoneManager
import org.osmdroid.views.overlay.milestones.MilestonePixelDistanceLister
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenMapView(
    selectedLocation: GeoPoint?,
    triggerUserCentering: Boolean,
    onCenteringComplete: () -> Unit,
    onRouteError: (String) -> Unit = {},
    onLiveLocationUpdate: (GeoPoint) -> Unit = {},
    onClearRoute: () -> Unit = {}
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
    var liveBearing by remember { mutableStateOf(0f) }

    var startLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var destinationLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var pendingLocation by remember { mutableStateOf<GeoPoint?>(null) }

    var showSelectionDialog by remember { mutableStateOf(false) }
    var showStepsSheet by remember { mutableStateOf(false) }
    var isDriveMode by remember { mutableStateOf(false) }

    var routeData by remember { mutableStateOf(RouteResult()) }
    var selectedRouteIndex by remember { mutableIntStateOf(0) }
    val defaultAnchor = remember { GeoPoint(28.6692, 77.4538) }

    val activeRoute = routeData.routes.getOrNull(selectedRouteIndex)

    // Optimization: Cache Bitmaps and Drawables
    val startIcon = remember(context) { createDotDrawable(context, "#10B981".toColorInt(), 50) }
    val destIcon = remember(context) { createPinDrawable(context, "#EF4444".toColorInt()) }
    val pendingIcon = remember(context) { createPinDrawable(context, "#8B5CF6".toColorInt()) }
    val chevronBitmap = remember { createChevronBitmap() }
    val personHotspot = remember { 30f }
    val blueDotBitmap = remember(context) {
        (createDotDrawable(context, "#3B82F6".toColorInt(), 60) as BitmapDrawable).bitmap
    }

    val cartoDbSource = remember(isDarkTheme) {
        val tileUrl = if (isDarkTheme) "https://a.basemaps.cartocdn.com/dark_all/" else "https://a.basemaps.cartocdn.com/light_all/"
        XYTileSource("CartoDB", 1, 20, 256, ".png", arrayOf(tileUrl))
    }

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
            if (result.routes.isNotEmpty()) {
                routeData = result
                selectedRouteIndex = 0
            } else {
                routeData = RouteResult(routes = listOf(RoutePath(points = listOf(start, dest))))
                selectedRouteIndex = 0
                val errorMsg = result.errorMessage ?: "Unable to calculate route."
                onRouteError(errorMsg)
            }
        } else {
            routeData = RouteResult()
            selectedRouteIndex = 0
            isDriveMode = false
        }
    }

    LaunchedEffect(triggerUserCentering, liveUserLocation) {
        if (triggerUserCentering && liveUserLocation != null) {
            mapViewRef?.controller?.animateTo(liveUserLocation, 18.0, 1000L)
            onCenteringComplete()
        }
    }

    LaunchedEffect(isDriveMode) {
        if (isDriveMode) {
            locationOverlayRef?.enableFollowLocation()
            mapViewRef?.controller?.animateTo(liveUserLocation, 19.0, 1000L)
        } else {
            locationOverlayRef?.disableFollowLocation()
            mapViewRef?.mapOrientation = 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(cartoDbSource)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                    val mReceive = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                pendingLocation = p
                                showSelectionDialog = true
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
                                if (location.hasBearing()) {
                                    liveBearing = location.bearing
                                }
                                onLiveLocationUpdate(newGeoPoint)
                            }
                        }
                    }

                    locationOverlay.setPersonIcon(blueDotBitmap)
                    locationOverlay.setDirectionArrow(blueDotBitmap, blueDotBitmap)
                    locationOverlay.setPersonHotspot(personHotspot, personHotspot)

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
                if (mapView.tileProvider.tileSource.name() != cartoDbSource.name()) {
                    mapView.setTileSource(cartoDbSource)
                }

                if (triggerUserCentering && !isDriveMode) {
                    val targetLocation = liveUserLocation ?: locationOverlayRef?.myLocation
                    if (targetLocation != null) {
                        mapView.controller.animateTo(targetLocation, 18.0, 1000L)
                        onCenteringComplete()
                    }
                }

                if (isDriveMode) {
                    mapView.mapOrientation = 360f - liveBearing
                }

                // Clear existing route overlays
                mapView.overlays.removeAll {
                    it is Polyline || (it is Marker && (it.id == "START_MARKER" || it.id == "DEST_MARKER" || it.id == "PENDING_MARKER"))
                }

                startLocation?.let { startPoint ->
                    val startMarker = Marker(mapView).apply {
                        id = "START_MARKER"
                        position = startPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Starting Point"
                        icon = startIcon
                    }
                    mapView.overlays.add(startMarker)
                }

                destinationLocation?.let { destPoint ->
                    val destMarker = Marker(mapView).apply {
                        id = "DEST_MARKER"
                        position = destPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Destination"
                        icon = destIcon
                    }
                    mapView.overlays.add(destMarker)
                }

                if (showSelectionDialog && pendingLocation != null) {
                    val pendingMarker = Marker(mapView).apply {
                        id = "PENDING_MARKER"
                        position = pendingLocation
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Selected Point"
                        icon = pendingIcon
                    }
                    mapView.overlays.add(pendingMarker)
                }

                if (routeData.routes.isNotEmpty()) {
                    val routesToDraw = routeData.routes.withIndex().sortedBy { if (it.index == selectedRouteIndex) 1 else 0 }
                    
                    for ((index, route) in routesToDraw) {
                        val isSelected = index == selectedRouteIndex

                        if (isSelected) {
                            val routeColor = if (isDarkTheme) "#38BDF8" else "#2563EB"
                            val routeLine = Polyline(mapView).apply {
                                outlinePaint.color = routeColor.toColorInt()
                                outlinePaint.strokeWidth = 14f
                                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                setPoints(route.points)
                                setOnClickListener { _, _, _ -> true }

                                val milestoneManager = MilestoneManager(
                                    MilestonePixelDistanceLister(400.0, 50.0),
                                    MilestoneBitmapDisplayer(0.0, true, chevronBitmap, chevronBitmap.width / 2, chevronBitmap.height / 2)
                                )
                                setMilestoneManagers(listOf(milestoneManager))
                            }

                            val casingLine = Polyline(mapView).apply {
                                outlinePaint.color = if (isDarkTheme) "#1AFFFFFF".toColorInt() else "#1A000000".toColorInt()
                                outlinePaint.strokeWidth = 20f
                                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                setPoints(route.points)
                            }
                            mapView.overlays.add(casingLine)
                            mapView.overlays.add(routeLine)
                        } else {
                            val greyColor = if (isDarkTheme) "#6B7280" else "#9CA3AF"
                            val altLine = Polyline(mapView).apply {
                                outlinePaint.color = greyColor.toColorInt()
                                outlinePaint.strokeWidth = 10f
                                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                setPoints(route.points)
                                setOnClickListener { _, _, _ ->
                                    selectedRouteIndex = index
                                    mapView.invalidate()
                                    true
                                }
                            }
                            mapView.overlays.add(altLine)
                        }
                    }

                    if (!isDriveMode && activeRoute != null) {
                        try {
                            val boundingBox = BoundingBox.fromGeoPoints(activeRoute.points)
                            mapView.zoomToBoundingBox(boundingBox, true, 140)
                        } catch (_: Exception) {
                            destinationLocation?.let { mapView.controller.animateTo(it) }
                        }
                    }
                }
                mapView.invalidate()
            }
        )

        AnimatedVisibility(
            visible = !isDriveMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 4.dp
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
        }

        AnimatedVisibility(
            visible = activeRoute != null && activeRoute.distanceMeters > 0,
            enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 80.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Travel Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    val dist = activeRoute?.distanceMeters ?: 0.0
                    val formattedDistance = if (dist >= 1000) {
                        String.format(Locale.getDefault(), "%.1f km", dist / 1000f)
                    } else {
                        "$dist m"
                    }

                    val minutes = ((activeRoute?.durationSeconds ?: 0.0) / 60).toInt()
                    val formattedTime = if (minutes > 60) "${minutes / 60} hr ${minutes % 60} min" else "$minutes min"

                    Text(
                        text = "$formattedDistance • $formattedTime",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (!isDriveMode) {
                        Button(
                            onClick = { isDriveMode = true },
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start")
                        }
                    } else {
                        Button(
                            onClick = { isDriveMode = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Stop")
                        }
                    }

                    if (activeRoute != null && activeRoute.instructions.isNotEmpty() && !isDriveMode) {
                        IconButton(onClick = { showStepsSheet = true }) {
                            Icon(Icons.Default.List, contentDescription = "Show Directions", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (!isDriveMode) {
                        IconButton(
                            onClick = {
                                destinationLocation = null
                                showStepsSheet = false
                                isDriveMode = false
                                onClearRoute()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Route", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSelectionDialog && pendingLocation != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 140.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Set Route Point", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                startLocation = pendingLocation
                                showSelectionDialog = false
                                pendingLocation = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.TripOrigin, contentDescription = "Set Start", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Here")
                        }
                        Button(
                            onClick = {
                                destinationLocation = pendingLocation
                                showSelectionDialog = false
                                pendingLocation = null
                            }
                        ) {
                            Icon(Icons.Default.Place, contentDescription = "Set Destination", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Destination")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showSelectionDialog = false; pendingLocation = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (showStepsSheet && activeRoute != null) {
            ModalBottomSheet(
                onDismissRequest = { showStepsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Turn-by-turn Directions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        itemsIndexed(activeRoute.instructions) { index, instruction ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = instruction,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (index < activeRoute.instructions.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createPinDrawable(context: Context, pinColor: Int): Drawable {
    val width = 72
    val height = 100
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val path = Path()
    val radius = width / 2f
    path.addArc(RectF(4f, 4f, width - 4f, width - 4f), 180f, 180f)
    path.lineTo(width / 2f, height - 4f)
    path.lineTo(4f, radius)
    path.close()

    paint.color = pinColor
    paint.style = Paint.Style.FILL
    canvas.drawPath(path, paint)

    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 6f
    paint.strokeJoin = Paint.Join.ROUND
    canvas.drawPath(path, paint)

    paint.style = Paint.Style.FILL
    canvas.drawCircle(width / 2f, width / 2f, radius / 2.5f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createDotDrawable(context: Context, dotColor: Int, size: Int): Drawable {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.FILL
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 2f, paint)

    paint.color = dotColor
    canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createChevronBitmap(): Bitmap {
    val size = 32
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val path = Path().apply {
        moveTo(10f, 6f)
        lineTo(22f, 16f)
        lineTo(10f, 26f)
    }
    canvas.drawPath(path, paint)

    return bitmap
}