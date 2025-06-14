package com.example.bustrackingapp.feature_home.presentation.home

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bustrackingapp.MainActivity
import com.example.bustrackingapp.R
import com.example.bustrackingapp.core.domain.repository.LocationRepository
import com.example.bustrackingapp.core.util.LoggerUtil
import com.example.bustrackingapp.core.util.Resource
import com.example.bustrackingapp.feature_bus.domain.use_cases.GetNearbyBusesUseCase
import com.example.bustrackingapp.feature_bus_stop.domain.use_case.BusStopUseCases
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.Socket
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val busStopUseCases: BusStopUseCases,
    private val nearbyBusesUseCase: GetNearbyBusesUseCase,
    private val locationRepository: LocationRepository,
    @ApplicationContext private val appContext: Context,
    private val socket: Socket
) : ViewModel() {

    private val logger = LoggerUtil(c = "HomeViewModel")

    /** Convert UI state's static routePoints to a list of LatLngs */
    fun getRouteLatLng(): List<LatLng> =
        uiState.routePoints.map { (lat, lng) -> LatLng(lat, lng) }

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        getLocationBusesStops()
        setupBusSocketListener()
    }

    /** Obtain current location, then load nearby buses & stops */
    fun getLocationBusesStops() {
        if (uiState.isLoadingLocation) return
        uiState = uiState.copy(isLoadingLocation = true)

        locationRepository.getCurrentLocation(
            callback = {
                uiState = uiState.copy(
                    location = it,
                    errorLocation = null,
                    isLoadingLocation = false
                )
                getNearbyBuses(isLoading = true)
                getNearbyStops(isLoading = true)
            },
            onError = {
                uiState = uiState.copy(
                    errorLocation = it.message,
                    isLoadingLocation = false
                )
            },
            isLive = false
        )
    }

    /** Listen for bus location updates and notify when roughly 5 minutes away */
    private fun setupBusSocketListener() {
        socket.on("busLocationUpdate") { args ->
            val data = (args[0] as? JSONObject) ?: return@on
            val busLat = data.optDouble("lat")
            val busLng = data.optDouble("lng")
            val routeNo = data.optString("routeNo")

            // For each nearby stop, notify when bus is ~5 minutes away
            val busSpeedMps = 8.33f              // ~30 km/h
            val thresholdDistance = busSpeedMps * 300f  // 5 minutes = 300 s

            uiState.nearbyBusStops.forEach { stop ->
                val stopLat = stop.location.lat
                val stopLng = stop.location.lng
                val dist = FloatArray(1)
                Location.distanceBetween(
                    busLat, busLng,
                    stopLat, stopLng,
                    dist
                )
                if (dist[0] <= thresholdDistance) {
                    sendBusArrivalNotification(routeNo, stop.name, dist[0])
                }
            }
        }
        socket.connect()
    }

    /** Build and fire a local notification */
    private fun sendBusArrivalNotification(routeNo: String, stopName: String, distance: Float) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val etaMinutes = (distance / 8.33f / 60f).toInt()
        val content = if (etaMinutes > 0)
            "$etaMinutes min to $stopName"
        else
            "Arriving at $stopName"

        val notif = NotificationCompat.Builder(appContext, "bus_arrival_channel")
            .setSmallIcon(R.drawable.locate_bus)
            .setContentTitle("Bus $routeNo Approaching")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext)
            .notify(routeNo.hashCode() + stopName.hashCode(), notif)
    }

    /** Fires a sample notification for testing */
    fun testNotification() {
        sendBusArrivalNotification(
            routeNo = "UNITEN_A1",
            stopName = "Administration Building",
            distance = 8.33f * 300f  // 5 minutes
        )
    }

    /** Fetch nearby bus stops and sort to the closest two */
    fun getNearbyStops(isLoading: Boolean = false, isRefreshing: Boolean = false) {
        if (uiState.isLoadingLocation ||
            uiState.isLoadingNearbyStops ||
            uiState.isRefreshingNearbyStops
        ) return

        val loc = uiState.location
        if (loc == null) {
            uiState = uiState.copy(errorNearbyStops = "Couldn't fetch current location")
            return
        }

        busStopUseCases.getAllBusStops()
            .onEach { result ->
                uiState = when (result) {
                    is Resource.Success -> {
                        val sorted = (result.data ?: emptyList()).sortedBy { stop ->
                            val d = FloatArray(1)
                            Location.distanceBetween(
                                loc.latitude, loc.longitude,
                                stop.location.lat, stop.location.lng,
                                d
                            )
                            d[0]
                        }.take(2)
                        uiState.copy(
                            nearbyBusStops = sorted,
                            isLoadingNearbyStops = false,
                            isRefreshingNearbyStops = false,
                            errorNearbyStops = null
                        )
                    }
                    is Resource.Error -> uiState.copy(
                        errorNearbyStops = result.message,
                        isLoadingNearbyStops = false,
                        isRefreshingNearbyStops = false
                    )
                    is Resource.Loading -> uiState.copy(
                        errorNearbyStops = null,
                        isLoadingNearbyStops = isLoading,
                        isRefreshingNearbyStops = isRefreshing
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /** Fetch nearby buses */
    fun getNearbyBuses(isLoading: Boolean = false, isRefreshing: Boolean = false) {
        if (uiState.isLoadingLocation ||
            uiState.isLoadingNearbyBuses ||
            uiState.isRefreshingNearbyBuses
        ) return

        val loc = uiState.location
        if (loc == null) {
            uiState = uiState.copy(errorNearbyBuses = "Couldn't fetch current location")
            return
        }

        nearbyBusesUseCase(loc.latitude, loc.longitude)
            .onEach { result ->
                uiState = when (result) {
                    is Resource.Success -> uiState.copy(
                        nearbyBuses = result.data ?: emptyList(),
                        isLoadingNearbyBuses = false,
                        isRefreshingNearbyBuses = false,
                        errorNearbyBuses = null
                    )
                    is Resource.Error -> uiState.copy(
                        errorNearbyBuses = result.message,
                        isLoadingNearbyBuses = false,
                        isRefreshingNearbyBuses = false
                    )
                    is Resource.Loading -> uiState.copy(
                        errorNearbyBuses = null,
                        isLoadingNearbyBuses = isLoading,
                        isRefreshingNearbyBuses = isRefreshing
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
