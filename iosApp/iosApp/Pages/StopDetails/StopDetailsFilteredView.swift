//
//  StopDetailsFilteredView.swift
//  iosApp
//
//  Created by esimon on 11/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import Shared
import SwiftUI

struct StopDetailsFilteredView: View {
    var stopId: String
    var stopFilter: StopDetailsFilter
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var departures: StopDetailsDepartures?
    var now: Date

    var alerts: [Shared.Alert]
    var downstreamAlerts: [Shared.Alert]
    var patternsByStop: PatternsByStop?
    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    var analytics: Analytics = AnalyticsProvider.shared

    var tiles: [TileData] = []
    var noPredictionsStatus: UpcomingFormat.NoTripsFormat?

    var stop: Stop? { stopDetailsVM.global?.getStop(stopId: stopId) }
    var nowInstant: Instant { now.toKotlinInstant() }

    init(
        stopId: String,
        stopFilter: StopDetailsFilter,
        tripFilter: TripDetailsFilter?,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        departures: StopDetailsDepartures?,
        now: Date,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
        stopDetailsVM: StopDetailsViewModel
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.departures = departures
        self.now = now
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM

        let patternsByStop = departures?.routes.first(where: { $0.routeIdentifier == stopFilter.routeId })
        self.patternsByStop = patternsByStop

        if let departures, let patternsByStop {
            if let global = stopDetailsVM.global {
                alerts = patternsByStop.alertsHereFor(
                    directionId: stopFilter.directionId,
                    tripId: tripFilter?.tripId,
                    global: global
                )
                downstreamAlerts = patternsByStop.alertsDownstream(directionId: stopFilter.directionId)
            } else {
                alerts = []
                downstreamAlerts = []
            }

            tiles = departures.tileData(
                routeId: patternsByStop.routeIdentifier,
                directionId: stopFilter.directionId,
                filterAtTime: nowInstant,
                globalData: stopDetailsVM.global
            )
            let realtimePatterns = patternsByStop.patterns.filter { $0.directionId() == stopFilter.directionId }
            noPredictionsStatus = tiles.isEmpty ? StopDetailsDepartures.companion.getNoPredictionsStatus(
                realtimePatterns: realtimePatterns,
                now: nowInstant
            ) : nil

        } else {
            alerts = []
            downstreamAlerts = []
            noPredictionsStatus = nil
            tiles = []
        }
    }

    var pinned: Bool {
        stopDetailsVM.pinnedRoutes.contains(stopFilter.routeId)
    }

    func toggledPinnedRoute() {
        Task {
            if let routeId = patternsByStop?.routeIdentifier {
                let pinned = await stopDetailsVM.togglePinnedRoute(routeId)
                analytics.toggledPinnedRoute(pinned: pinned, routeId: routeId)
                stopDetailsVM.loadPinnedRoutes()
            }
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Color.fill2.ignoresSafeArea(.all)
                header
            }
            .fixedSize(horizontal: false, vertical: true)

            if let patternsByStop {
                StopDetailsFilteredDepartureDetails(
                    stopId: stopId,
                    stopFilter: stopFilter,
                    tripFilter: tripFilter,
                    setStopFilter: setStopFilter,
                    setTripFilter: setTripFilter,
                    tiles: tiles,
                    noPredictionsStatus: noPredictionsStatus,
                    alerts: alerts,
                    downstreamAlerts: downstreamAlerts,
                    patternsByStop: patternsByStop,
                    pinned: pinned,
                    now: now,
                    errorBannerVM: errorBannerVM,
                    nearbyVM: nearbyVM,
                    mapVM: mapVM,
                    stopDetailsVM: stopDetailsVM
                )
            } else {
                loadingBody()
            }
        }
    }

    @ViewBuilder
    var header: some View {
        let route: Route? = if let routeId = patternsByStop?.representativeRoute.id {
            stopDetailsVM.global?.getRoute(routeId: routeId)
        } else {
            nil
        }
        VStack(spacing: 8) {
            StopDetailsFilteredHeader(
                route: route,
                line: patternsByStop?.line,
                stop: stop,
                pinned: pinned,
                onPin: toggledPinnedRoute,
                onClose: { nearbyVM.goBack() }
            )
            if nearbyVM.showDebugMessages {
                DebugView {
                    Text(verbatim: "stop id: \(stopId)")
                }.padding(.horizontal, 16)
            }
            ErrorBanner(errorBannerVM).padding(.horizontal, 16)
        }
        .padding(.bottom, 11)
        .fixedSize(horizontal: false, vertical: true)
        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
    }

    @ViewBuilder private func loadingBody() -> some View {
        let loadingPatterns = LoadingPlaceholders.shared.patternsByStop(routeId: stopFilter.routeId, trips: 10)
        let upcomingTrip = loadingPatterns.patterns.first?.upcomingTrips.first

        let tiles = (0 ..< 4).map { _ in TileData(
            route: loadingPatterns.representativeRoute,
            headsign: "placeholder",
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: upcomingTrip!, routeType: .lightRail, format: .Boarding())],
                secondaryAlert: nil,
            ),
            upcoming: upcomingTrip!
        ) }
        StopDetailsFilteredDepartureDetails(
            stopId: stopId,
            stopFilter: stopFilter,
            tripFilter: tripFilter,
            setStopFilter: setStopFilter,
            setTripFilter: setTripFilter,
            tiles: tiles,
            noPredictionsStatus: nil,
            alerts: alerts,
            downstreamAlerts: downstreamAlerts,
            patternsByStop: loadingPatterns,
            pinned: pinned,
            now: now,
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            mapVM: mapVM,
            stopDetailsVM: stopDetailsVM
        ).loadingPlaceholder()
    }
}
