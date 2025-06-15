package com.example.bustrackingapp.feature_bus_routes.presentation.bus_routes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bustrackingapp.R
import com.example.bustrackingapp.core.presentation.components.CustomLoadingIndicator
import com.example.bustrackingapp.core.util.LoggerUtil
import com.example.bustrackingapp.feature_bus_routes.domain.models.BusRouteWithStops
import com.example.bustrackingapp.feature_bus_routes.presentation.components.BusRouteTile
import com.example.bustrackingapp.feature_bus_routes.presentation.components.ShuttleRouteMap
import com.example.bustrackingapp.ui.theme.Blue500
import com.example.bustrackingapp.ui.theme.NavyBlue300
import com.example.bustrackingapp.ui.theme.Red400
import com.example.bustrackingapp.ui.theme.White
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRoutesScreen(
    busRoutesViewModel : BusRoutesViewModel = hiltViewModel(),
    snackbarState : SnackbarHostState = remember {
        SnackbarHostState()
    },
    onRouteItemClick : (String)->Unit
){
    val focusManager = LocalFocusManager.current
    val logger = LoggerUtil(c = "BusRoutesScreen")
    LaunchedEffect(key1 = busRoutesViewModel.uiState.error){
        logger.info("Show Snackbar")

        if(busRoutesViewModel.uiState.error!=null){
            snackbarState.showSnackbar(busRoutesViewModel.uiState.error!!)
        }
    }

    Scaffold(
        modifier = Modifier
            .clickable(
                // Remove Ripple Effect
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bus Routes",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarState,
            ){
                if(busRoutesViewModel.uiState.error!=null){
                    Snackbar(
                        snackbarData = it,
                        containerColor = Red400,
                        contentColor = White,

                        )
                }else{
                    Snackbar(snackbarData = it)
                }
            }
        },

        ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            BusRouteList(
                busRoutes = { busRoutesViewModel.uiState.busRoutes },
                isLoading = { busRoutesViewModel.uiState.isLoading },
                isRefreshing = { busRoutesViewModel.uiState.isRefreshing },
                onRefresh = busRoutesViewModel::getAllBusRoutes,
                onRouteItemClick = onRouteItemClick
            )
        }
    } 
}

@Composable
private fun ShuttleRulesInfo(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = "UNITEN INTERNAL SHUTTLE SERVICE (Bus Routes ONLY)",
            style = MaterialTheme.typography.titleMedium,
            color = Blue500
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Operating Hours:",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "07:30 – 22:30 (Monday - Friday)",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Full Route:",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "ATM/LIBRARY → ADMIN → MURNI → BW → AMANAH → DSS → ILMU → COE → ATM/LIBRARY",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Complete Departure Times:",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "07:30, 08:00, 08:30, 09:00, 09:30, 10:00, 10:30, 11:00, 11:30, 12:00,\n" +
                    "12:30, 13:00, 13:30, 14:00, 14:30, 15:00, 15:30, 16:00, 16:30, 17:00,\n" +
                    "17:30, 18:00, 18:30, 19:00, 20:00, 20:30, 21:00, 21:30, 22:00, 22:30\n" +
                    "(Departure at 19:30 intentionally excluded)",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Shuttle departs every 30 minutes, excluding:",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "• 19:30 – 20:00 (Maghrib Prayer)",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "• 12:30 – 14:30 (Friday Prayer)",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "• The shuttle service is free of charge.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "• No shuttle service during semester breaks.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun BusRouteList(
    busRoutes: () -> List<BusRouteWithStops>,
    isLoading: () -> Boolean,
    isRefreshing: () -> Boolean,
    onRefresh: (isLoading: Boolean, isRefreshing: Boolean) -> Unit,
    onRouteItemClick: (String) -> Unit
){
    if(isLoading()){
        return CustomLoadingIndicator()
    }

    Column {
        ShuttleRulesInfo(modifier = Modifier.padding(horizontal = 8.dp))
        ShuttleRouteMap(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.CenterHorizontally)
        )

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = isRefreshing()),
            onRefresh = { onRefresh(false, true) },
        ) {
            LazyColumn(
                content = {
                    itemsIndexed(busRoutes()) { index, item ->
                        if (index == 0) {
                            Divider(color = NavyBlue300)
                        }
                        BusRouteTile(
                            routeNo = item.routeNo,
                            routeName = item.name,
                            totalStops = item.stops.size,
                            onClick = { onRouteItemClick(item.routeNo) }
                        )
                        Divider(color = NavyBlue300)
                    }
                },
                contentPadding = PaddingValues(8.dp)
            )
        }
    }

}