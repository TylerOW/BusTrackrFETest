package com.example.bustrackingapp.feature_bus_stop.presentation.stop_details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bustrackingapp.core.data.local.staticBusStops
import com.example.bustrackingapp.core.presentation.components.CustomLoadingIndicator
import com.example.bustrackingapp.core.presentation.components.FieldValue
import com.example.bustrackingapp.feature_bus_routes.presentation.components.BusRouteTile
import com.example.bustrackingapp.feature_bus_stop.domain.model.BusStopWithRoutes
import com.example.bustrackingapp.ui.theme.NavyBlue300
import com.example.bustrackingapp.ui.theme.Red400
import com.example.bustrackingapp.ui.theme.White
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailsScreen(
    stopNo: String,
    stopDetailsViewModel: StopDetailsViewModel = hiltViewModel(),
    snackbarState: SnackbarHostState = remember { SnackbarHostState() },
    onBusRouteClick: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        stopDetailsViewModel.getBusStopDetails(stopNo, isLoading = true)
    }

    LaunchedEffect(stopDetailsViewModel.uiState.error) {
        stopDetailsViewModel.uiState.error?.let {
            snackbarState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            val isFav = stopDetailsViewModel.uiState.isFavorite
            TopAppBar(
                title = { Text("Stop Details", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton { stopDetailsViewModel.toggleFavorite() } {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFav) "Unfavorite" else "Favorite",
                            tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(it) { data ->
                if (stopDetailsViewModel.uiState.error != null) {
                    Snackbar(snackbarData = data, containerColor = Red400, contentColor = White)
                } else {
                    Snackbar(snackbarData = data)
                }
            }
        }
    ) { paddingValues ->
        Box(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            BusStopDetailsContainer(
                isLoading = stopDetailsViewModel.uiState.isLoading,
                isRefreshing = { stopDetailsViewModel.uiState.isRefreshing },
                onRefresh = stopDetailsViewModel::getBusStopDetails,
                busStop = stopDetailsViewModel.uiState.stop,
                onBusRouteClick = onBusRouteClick
            )
        }
    }
}

@Composable
fun BusStopDetailsContainer(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    isRefreshing: () -> Boolean,
    onRefresh: (routeNo: String, isLoading: Boolean, isRefreshing: Boolean) -> Unit,
    busStop: BusStopWithRoutes?,
    onBusRouteClick: (String) -> Unit
) {
    if (isLoading) {
        CustomLoadingIndicator()
        return
    }
    if (busStop == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Something Went Wrong!")
        }
        return
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing()),
        onRefresh = { onRefresh(busStop.stopNo, false, true) }
    ) {
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            FieldValue(field = "Stop Name", value = busStop.name)
            Spacer(Modifier.height(6.dp))
            FieldValue(field = "Stop No", value = busStop.stopNo)
            Spacer(Modifier.height(24.dp))

            // Location section
            Text("Location:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            StopLocationMap(busStop)
            Spacer(Modifier.height(24.dp))

            // Routes section
            Text("Routes:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Column {
                busStop.routes.forEach { route ->
                    BusRouteTile(
                        routeNo = route.routeNo,
                        routeName = route.name,
                        onClick = { onBusRouteClick(route.routeNo) }
                    )
                    Divider(color = NavyBlue300)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StopLocationMap(busStop: BusStopWithRoutes) {
    val latLng = busStop.correctedLatLng()
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(latLng) {
        cameraPositionState.move(
            CameraUpdateFactory.newLatLngZoom(latLng, 16f)
        )
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(),
        uiSettings = MapUiSettings(zoomControlsEnabled = true)
    ) {
        Marker(
            state = MarkerState(position = latLng),
            title = busStop.name
        )
    }
}

/** 
 * First tries to look up a hardcoded lat/lng from our static list, 
 * otherwise swaps coordinates if they look inverted. 
 */
private fun BusStopWithRoutes.correctedLatLng(): LatLng {
    staticBusStops.find { it.stopNo == stopNo }?.let {
        return LatLng(it.lat, it.lng)
    }
    val lat = location.lat
    val lng = location.lng
    return if (lat !in -90.0..90.0) LatLng(lng, lat) else LatLng(lat, lng)
}
