package com.example.bustrackingapp.feature_bus_stop.domain.use_case

data class BusStopUseCases(
    val getAllBusStops: GetAllBusStopsUseCase,
    val getNearbyBusStops: GetNearbyBusStopsUseCase,
    val getBusStop: GetBusStopUseCase,
    val toggleFavorite: ToggleFavoriteUseCase
)
