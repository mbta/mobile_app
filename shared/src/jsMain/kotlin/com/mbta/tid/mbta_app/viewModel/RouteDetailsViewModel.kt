package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getRouteStops
import com.mbta.tid.mbta_app.wrapper.RouteDetails
import com.mbta.tid.mbta_app.wrapper.wrapped
import kotlinx.coroutines.flow.StateFlow

internal interface IRouteDetailsViewModel {
    val models: StateFlow<RouteDetails.State?>

    fun setSelection(routeId: String, direction: Int)
}

internal class RouteDetailsViewModel() :
    MoleculeViewModel<RouteDetailsViewModel.Event, RouteDetails.State?>(), IRouteDetailsViewModel {
    enum class Event

    private var selectedRouteId by mutableStateOf<String?>(null)
    private var selectedDirection by mutableStateOf<Int?>(null)

    @Composable
    override fun runLogic(): RouteDetails.State? {
        var stopList: RouteDetails.State? by remember { mutableStateOf(null) }

        val routeStops =
            getRouteStops(selectedRouteId, selectedDirection, "RouteDetailsViewModel.routeStopIds")
        val globalData = getGlobalData("RouteDetailsViewModel")

        LaunchedEffect(selectedRouteId, selectedDirection, routeStops, globalData) {
            val selectedRouteId = selectedRouteId
            val selectedDirection = selectedDirection
            stopList =
                if (selectedRouteId != null && selectedDirection != null && globalData != null)
                    RouteDetailsStopList.fromPieces(
                            selectedRouteId,
                            selectedDirection,
                            routeStops,
                            globalData,
                        )
                        ?.wrapped(globalData.getRoute(selectedRouteId))
                else null
        }

        return stopList
    }

    override val models: StateFlow<RouteDetails.State?>
        get() = internalModels

    override fun setSelection(routeId: String, direction: Int) {
        selectedRouteId = routeId
        selectedDirection = direction
    }
}
