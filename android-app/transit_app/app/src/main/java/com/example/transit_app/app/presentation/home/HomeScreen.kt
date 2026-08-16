package com.example.transit_app.app.presentation.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transit_app.app.presentation.home.components.FloatingPlaceCard
import com.example.transit_app.app.presentation.home.components.FloatingSearchBar
import com.example.transit_app.app.presentation.home.components.FullscreenMapView
import com.example.transit_app.app.presentation.home.models.DisplayCardItem
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var triggerUserCentering by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(locationPermissions)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "RouteScape",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )
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
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                FullscreenMapView(
                    selectedLocation = selectedLocation,
                    triggerUserCentering = triggerUserCentering,
                    onCenteringComplete = { triggerUserCentering = false },
                    onRouteError = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.08f)
                        .align(Alignment.CenterStart)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                if (dragAmount > 8) {
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
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.Center),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
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
}