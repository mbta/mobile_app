package com.mbta.tid.mbta_app.android.util

import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.ModalRoutes.*
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.TrackChange
import com.mbta.tid.mbta_app.model.TripDetailsStopList

fun TripDetailsStopList.Entry.trackChangeTapCallback(
    route: Route,
    openModal: (ModalRoutes) -> Unit,
): (() -> Unit)? =
    when (val trackNumber = this.trackNumber) {
        is TripDetailsStopList.Track.TrackChange -> {
            when (val trackChange = trackNumber.trackChange) {
                is TrackChange.Alert -> {
                    {
                        openModal(
                            AlertDetails(
                                alertId = trackChange.alert.id,
                                lineId = null,
                                routeIds = listOf(route.id),
                                stopId = null,
                            )
                        )
                    }
                }
                is TrackChange.Scheduled -> {
                    {
                        openModal(
                            Explainer(
                                type =
                                    com.mbta.tid.mbta_app.model.stopDetailsPage.ExplainerType
                                        .ScheduledTrackChange,
                                routeAccents = TripRouteAccents(route),
                            )
                        )
                    }
                }
            }
        }
        else -> null
    }
