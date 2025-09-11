@file:JsExport
@file:OptIn(ExperimentalJsExport::class)

package com.mbta.tid.mbta_app.wrapper

import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.dependencyInjection.appModule
import com.mbta.tid.mbta_app.dependencyInjection.makeNativeModule
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.phoenix.JsPhoenixSocket
import com.mbta.tid.mbta_app.platformModule
import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.MockCurrentAppVersionRepository
import com.mbta.tid.mbta_app.viewModel.IFilteredStopDetailsViewModel
import com.mbta.tid.mbta_app.viewModel.viewModelModule
import kotlin.js.Promise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.promise
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

/**
 * The main entry point for the JS wrapper. Actually all global state for dependency injection
 * reasons, so don’t try to use more than one.
 */
public class FilteredStopDetails(backendRoot: String) : KoinComponent {
    private val socket = JsPhoenixSocket(backendRoot.replace(Regex("^http"), "ws") + "/socket")

    init {
        socket.attach()
        val modules =
            listOf(
                appModule(backendRoot),
                viewModelModule(),
                platformModule(),
                makeNativeModule(
                    object : IAccessibilityStatusRepository {
                        override fun isScreenReaderEnabled() = false
                    },
                    MockAnalytics(),
                    MockCurrentAppVersionRepository(null),
                    socket,
                ),
            )
        if (KoinPlatform.getKoinOrNull() == null) startKoin { modules(modules) }
        else loadKoinModules(modules)
    }

    private val vm: IFilteredStopDetailsViewModel by inject()

    /** Can be used to reduce WebSocket activity while in the background. */
    public fun setActive(active: Boolean) {
        vm.setActive(active)
    }

    /**
     * Sets the selected stop, route, direction, and possibly trip, vehicle, and stop sequence.
     *
     * If no trip is selected manually, one will be selected automatically, so check [State.tripId].
     */
    public fun setFilters(
        stopId: String,
        routeId: String,
        directionId: Int,
        tripId: String?,
        vehicleId: String?,
        stopSequence: Int?,
    ) {
        val tripFilter =
            if (tripId != null) TripDetailsFilter(tripId, vehicleId, stopSequence) else null
        vm.setFilters(
            StopDetailsPageFilters(stopId, StopDetailsFilter(routeId, directionId), tripFilter)
        )
    }

    /** Runs the logic and calls the callback whenever a new state is emitted. */
    public fun onNewState(callback: (State?) -> Unit): Promise<Unit> =
        CoroutineScope(Dispatchers.Default).promise { vm.models.collect { callback(it) } }
}
