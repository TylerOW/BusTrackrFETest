package com.example.bustrackingapp.feature_home.presentation.home

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bustrackingapp.core.presentation.components.CustomLoadingIndicator
import com.example.bustrackingapp.core.presentation.components.RefreshContainer
import com.example.bustrackingapp.feature_bus.domain.models.BusWithRoute
import com.example.bustrackingapp.feature_bus_stop.domain.model.BusStopWithRoutes
import com.example.bustrackingapp.feature_bus_stop.presentation.components.BusStopTile
import com.example.bustrackingapp.feature_home.presentation.components.BusTile
import com.example.bustrackingapp.ui.theme.Blue500
import com.example.bustrackingapp.ui.theme.NavyBlue300
import com.example.bustrackingapp.ui.theme.Red400
import com.example.bustrackingapp.ui.theme.White
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    snackbarState: SnackbarHostState = remember { SnackbarHostState() },
    onNearbyBusClick: (String) -> Unit,
    onNearbyBusStopClick: (String) -> Unit,
    onAllBusStopsClick: () -> Unit
) {
    // show any errors in snackbars
    LaunchedEffect(
        homeViewModel.uiState.errorLocation,
        homeViewModel.uiState.errorNearbyBuses,
        homeViewModel.uiState.errorNearbyStops
    ) {
        when {
            homeViewModel.uiState.errorLocation != null ->
                snackbarState.showSnackbar(homeViewModel.uiState.errorLocation!!)
            homeViewModel.uiState.errorNearbyBuses != null ->
                snackbarState.showSnackbar(homeViewModel.uiState.errorNearbyBuses!!)
            homeViewModel.uiState.errorNearbyStops != null ->
                snackbarState.showSnackbar(homeViewModel.uiState.errorNearbyStops!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bus Tracker", style = MaterialTheme.typography.headlineSmall) }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (
                        homeViewModel.uiState.errorNearbyBuses != null ||
                        homeViewModel.uiState.errorNearbyStops != null
                    ) Red400 else MaterialTheme.colorScheme.surface,
                    contentColor = if (
                        homeViewModel.uiState.errorNearbyBuses != null ||
                        homeViewModel.uiState.errorNearbyStops != null
                    ) White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { paddingValues ->
        Box(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column {
                // ─── Nearby Buses ───
                Text(
                    text = "Nearby Buses",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // ─── Show approaching notification if available ───
                homeViewModel.uiState.notificationMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White
                    )
                }

                // ─── Then the normal list (or loading/empty) ───
                NearbyBusesList(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    buses = { homeViewModel.uiState.nearbyBuses },
                    isLoading = homeViewModel.uiState.isLoadingLocation ||
                            homeViewModel.uiState.isLoadingNearbyBuses,
                    onRefresh = homeViewModel::getNearbyBuses,
                    onBusClick = onNearbyBusClick
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ─── Nearby Bus Stops header ───
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nearby Bus Stops", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "All Bus Stops",
                        style = MaterialTheme.typography.labelMedium,
                        color = Blue500,
                        modifier = Modifier
                            .clickable { onAllBusStopsClick() }
                            .padding(4.dp)
                    )
                }

                // ─── Closest two stops ───
                NearbyBusStopsList(
                    modifier = Modifier.weight(3f),
                    busStops = { homeViewModel.uiState.nearbyBusStops },
                    isLoading = homeViewModel.uiState.isLoadingLocation ||
                            homeViewModel.uiState.isLoadingNearbyStops,
                    isRefreshing = homeViewModel.uiState.isRefreshingNearbyStops,
                    onRefresh = homeViewModel::getNearbyStops,
                    onBusStopClick = onNearbyBusStopClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ─── Map centered on you ───
                val cameraPositionState = rememberCameraPositionState()
                LaunchedEffect(homeViewModel.uiState.location) {
                    homeViewModel.uiState.location?.let { loc ->
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(loc.latitude, loc.longitude),
                                15f
                            )
                        )
                    }
                }
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true)
                ) {
                    Polyline(
                        points = homeViewModel.getRouteLatLng(),
                        color = Color.Blue,
                        width = 6f
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun NearbyBusesList(
    modifier: Modifier = Modifier,
    buses: () -> List<BusWithRoute>,
    isLoading: Boolean,
    onRefresh: (Boolean, Boolean) -> Unit,
    onBusClick: (String) -> Unit
) {
    if (isLoading) {
        CustomLoadingIndicator(modifier = modifier)
        return
    }
    if (buses().isEmpty()) {
        RefreshContainer(
            modifier = Modifier.fillMaxHeight(0.3f),
            message = "No Nearby Buses Found!",
            onRefresh = { onRefresh(false, true) }
        )
        return
    }
    LazyRow(contentPadding = PaddingValues(8.dp)) {
        items(buses()) { bus ->
            BusTile(
                routeNo = bus.route.routeNo,
                routeName = bus.route.name,
                vehNo   = bus.vehNo,
                onClick = { onBusClick(bus.vehNo) }
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
    }
}

@Composable
fun NearbyBusStopsList(
    modifier: Modifier = Modifier,
    busStops: () -> List<BusStopWithRoutes>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: (Boolean, Boolean) -> Unit,
    onBusStopClick: (String) -> Unit
) {
    if (isLoading) {
        CustomLoadingIndicator(modifier = modifier)
        return
    }
    if (busStops().isEmpty()) {
        RefreshContainer(
            modifier = Modifier.fillMaxHeight(0.4f),
            message = "No Nearby Bus Stops Found!",
            onRefresh = { onRefresh(false, true) }
        )
        return
    }
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
        onRefresh = { onRefresh(false, true) }
    ) {
        LazyColumn(contentPadding = PaddingValues(8.dp)) {
            items(busStops()) { stop ->
                BusStopTile(
                    stopNo         = stop.stopNo,
                    stopName       = stop.name,
                    isFavorite     = false,
                    onFavoriteClick = {},
                    onClick        = { onBusStopClick(stop.stopNo) }
                )
                Divider(color = NavyBlue300)
            }
        }
    }
}

