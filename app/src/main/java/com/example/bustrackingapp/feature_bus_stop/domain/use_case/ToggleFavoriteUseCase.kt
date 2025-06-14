package com.example.bustrackingapp.feature_bus_stop.domain.use_case

import com.example.bustrackingapp.core.domain.repository.UserPrefsRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val prefs: UserPrefsRepository
) {
    suspend operator fun invoke(stopNo: String) = prefs.toggleFavoriteStop(stopNo)

    fun favorites() = prefs.getFavoriteStops()
}
