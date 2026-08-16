package com.example.transit_app.app.presentation.home

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

data class DisplayCardItem(val title: String, val subtitle: String, val icon: ImageVector, val geoPoint: GeoPoint)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var triggerUserCentering by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(locationPermissions)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("RouteScape", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Login") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null) },
                    selected = false,
                    onClick = { }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = false,
                    onClick = { }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            FullscreenMapView(
                selectedLocation = selectedLocation,
                triggerUserCentering = triggerUserCentering,
                onCenteringComplete = { triggerUserCentering = false }
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.1f)
                    .align(Alignment.CenterStart)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount > 5) {
                                change.consume()
                                scope.launch { drawerState.open() }
                            }
                        }
                    }
            )

            FloatingSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onMenuClick = { scope.launch { drawerState.open() } },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            )

            FloatingActionButton(
                onClick = { triggerUserCentering = true },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 140.dp, end = 16.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = MaterialTheme.colorScheme.primary)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is HomeUiState.Error -> {
                        Card(
                            modifier = Modifier.padding(16.dp).align(Alignment.Center),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = "Error: ${state.message}",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    is HomeUiState.Success -> {
                        val itemsList = remember(state.data) {
                            val list = mutableListOf<DisplayCardItem>()
                            state.data.transit_options.forEach { option ->
                                list.add(
                                    DisplayCardItem(
                                        title = option.name,
                                        subtitle = "${option.distance_meters}m • ${option.status}",
                                        icon = if (option.type == "metro") Icons.Default.Train else Icons.Default.ElectricRickshaw,
                                        geoPoint = GeoPoint(option.coordinates.lat, option.coordinates.lng)
                                    )
                                )
                            }
                            state.data.heritage_sites.forEach { site ->
                                list.add(
                                    DisplayCardItem(
                                        title = site.name,
                                        subtitle = "${site.distance_meters}m • ${site.category}",
                                        icon = Icons.Default.AccountBalance,
                                        geoPoint = GeoPoint(site.coordinates.lat, site.coordinates.lng)
                                    )
                                )
                            }
                            list
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = itemsList,
                                key = { it.title }
                            ) { item ->
                                FloatingPlaceCard(
                                    item = item,
                                    onClick = { selectedLocation = item.geoPoint }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenMapView(
    selectedLocation: GeoPoint?,
    triggerUserCentering: Boolean,
    onCenteringComplete: () -> Unit
) {
    val context = LocalContext.current

    val sharedPreferences = context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
    Configuration.getInstance().load(context, sharedPreferences)
    Configuration.getInstance().userAgentValue = "RouteScapeMVP/1.0"

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                val cartoDbSource = org.osmdroid.tileprovider.tilesource.XYTileSource(
                    "CartoDB",
                    1, 20, 256, ".png",
                    arrayOf("https://a.basemaps.cartocdn.com/light_all/")
                )
                setTileSource(cartoDbSource)
                setMultiTouchControls(true)
                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS)

                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                locationOverlay.enableMyLocation()
                this.overlays.add(locationOverlay)

                val nsutLocation = GeoPoint(28.60882, 77.03588)
                controller.setZoom(15.0)
                controller.setCenter(nsutLocation)
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

            mapView.overlays.removeAll { it is org.osmdroid.views.overlay.Polyline }

            selectedLocation?.let { targetPoint ->
                val routeLine = org.osmdroid.views.overlay.Polyline(mapView)
                routeLine.outlinePaint.color = android.graphics.Color.parseColor("#1976D2")
                routeLine.outlinePaint.strokeWidth = 10f

                val anchorLocation = GeoPoint(28.60882, 77.03588)
                routeLine.setPoints(listOf(anchorLocation, targetPoint))

                mapView.overlays.add(routeLine)
                mapView.controller.animateTo(targetPoint)
            }

            mapView.invalidate()
        }
    )
}

@Composable
fun FloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search Location") },
        leadingIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        trailingIcon = {
            Icon(Icons.Default.Mic, contentDescription = "Voice Search")
        },
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp)),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
fun FloatingPlaceCard(item: DisplayCardItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .width(260.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = item.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 1)
            }
        }
    }
}