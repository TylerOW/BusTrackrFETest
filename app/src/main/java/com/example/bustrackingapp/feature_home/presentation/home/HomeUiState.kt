package com.example.bustrackingapp.feature_home.presentation.home

import android.location.Location
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.bustrackingapp.feature_bus.domain.models.BusWithRoute
import com.example.bustrackingapp.feature_bus_stop.domain.model.BusStopWithRoutes


data class HomeUiState(
    val nearbyBusStops       : List<BusStopWithRoutes>    = emptyList(),
    val nearbyBuses          : List<BusWithRoute>         = emptyList(),
    val location             : Location?                  = null,
    val isLoadingNearbyStops : Boolean                    = false,
    val isLoadingNearbyBuses : Boolean                    = false,
    val isLoadingLocation    : Boolean                    = false,
    val isRefreshingNearbyStops  : Boolean               = false,
    val isRefreshingNearbyBuses  : Boolean               = false,
    val errorNearbyBuses     : String?                    = null,
    val errorNearbyStops     : String?                    = null,
    val closestStopName      : String?                    = null,
    val errorLocation        : String?                    = null,

    /** NEW: message shown under "Nearby Buses" when a bus is approaching */
    val notificationMessage  : String?                    = null,

    /** Static route points for drawing the map polyline */
    val routePoints : List<Pair<Double, Double>> = listOf(
        2.975298 to 101.729192,  // COE
        2.975777 to 101.728832,  // Murni
        2.977944 to 101.730570,  // Admin
        2.976673 to 101.734034,  // Library
        2.970936 to 101.730657,  // ILMU
        2.968112 to 101.728183,  // DSS
        2.965783 to 101.731220,  // Amanah
        2.962569 to 101.725598   // CIT
    )
)

//@Stable
//interface HomeUiState {
//    val nearbyBusStops : List<BusStopWithRoutes>
//    val nearbyBuses : List<BusWithRoute>
//    val isLoadingNearbyStops : Boolean
//    val isLoadingNearbyBuses : Boolean
//    val isRefreshingNearbyStops : Boolean
//    val isRefreshingNearbyBuses : Boolean
//    val errorNearbyBuses : String?
//    val errorNearbyStops : String?
//}
//
//class MutableHomeUiState: HomeUiState {
//    override var nearbyBusStops : List<BusStopWithRoutes> by mutableStateOf(emptyList())
//    override var nearbyBuses : List<BusWithRoute> by mutableStateOf(emptyList())
//    override var isLoadingNearbyStops : Boolean by mutableStateOf(false)
//    override var isLoadingNearbyBuses : Boolean by mutableStateOf(false)
//    override var isRefreshingNearbyStops : Boolean by mutableStateOf(false)
//    override var isRefreshingNearbyBuses : Boolean by mutableStateOf(false)
//    override var errorNearbyBuses : String? by mutableStateOf(null)
//    override var errorNearbyStops : String? by mutableStateOf(null)
//}

val routePoints : List<Pair<Double,Double>> = listOf(
    2.975298 to 101.729192,  // COE
    2.975777 to 101.728832,  // Murni
    2.977944 to 101.730570,  // Admin
    2.976673 to 101.734034,  // Library
    2.970936 to 101.730657,  // ILMU
    2.968112 to 101.728183,  // DSS
    2.965783 to 101.731220,  // Amanah
    2.962569 to 101.725598   // CIT
)
