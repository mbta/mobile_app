//
//  StopDetailsFilteredView.swift
//  iosApp
//
//  Created by esimon on 11/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import shared
import SwiftUI

struct StopDetailsFilteredView: View {
    var stopId: String
    var stopFilter: StopDetailsFilter
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var departures: StopDetailsDepartures?
    var now: Date

    var alerts: [shared.Alert]
    var patternsByStop: PatternsByStop?
    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    var analytics: StopDetailsAnalytics = AnalyticsProvider.shared

    var tiles: [TileData] = []
    var statuses: [TileData] = []

    var stop: Stop? { stopDetailsVM.global?.stops[stopId] }
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
                alerts = patternsByStop.alertsHereFor(directionId: stopFilter.directionId, global: global)
            } else {
                alerts = []
            }

            tiles = departures.stopDetailsFormattedTrips(
                routeId: patternsByStop.routeIdentifier,
                directionId: stopFilter.directionId,
                filterAtTime: nowInstant
            ).compactMap { tripAndFormat in
                let upcoming = tripAndFormat.upcoming
                guard let route = (patternsByStop.routes.first { $0.id == upcoming.trip.routeId }) else {
                    Logger().error("""
                    Failed to find route ID \(upcoming.trip.routeId) from upcoming \
                    trip in patternsByStop.routes (\(patternsByStop.routes.map(\.id)))
                    """)
                    return nil
                }
                return TileData(
                    upcoming: upcoming,
                    route: route,
                    now: nowInstant
                )
            }
            let realtimePatterns = patternsByStop.patterns.filter { $0.directionId() == stopFilter.directionId }
            statuses = Self.getStatusDepartures(realtimePatterns: realtimePatterns, now: nowInstant)

        } else {
            alerts = []
            statuses = []
            tiles = []
        }
    }

    var pinned: Bool {
        stopDetailsVM.pinnedRoutes.contains(stopFilter.routeId)
    }

    static func getStatusDepartures(realtimePatterns: [RealtimePatterns], now: Instant) -> [TileData] {
        StopDetailsDepartures.companion.getStatusDepartures(realtimePatterns: realtimePatterns, now: now)
            .map {
                TileData(
                    route: $0.route,
                    headsign: $0.headsign,
                    formatted: $0.formatted
                )
            }
    }

    func toggledPinnedRoute() {
        Task {
            if let routeId = patternsByStop?.routeIdentifier {
                do {
                    let pinned = try await stopDetailsVM.togglePinnedUsecase.execute(route: routeId).boolValue
                    analytics.toggledPinnedRouteAtStop(pinned: pinned, routeId: routeId)
                    stopDetailsVM.loadPinnedRoutes()
                } catch is CancellationError {
                    // do nothing on cancellation
                } catch {
                    // execute shouldn't actually fail
                    debugPrint(error)
                }
            }
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Color.fill2
                header
            }
            .fixedSize(horizontal: false, vertical: true)
            .ignoresSafeArea(.all)

            if let patternsByStop {
                StopDetailsFilteredDepartureDetails(
                    stopId: stopId,
                    stopFilter: stopFilter,
                    tripFilter: tripFilter,
                    setStopFilter: setStopFilter,
                    setTripFilter: setTripFilter,
                    tiles: tiles,
                    statuses: statuses,
                    alerts: alerts,
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
            stopDetailsVM.global?.routes[routeId]
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
        let tiles = (0 ..< 4).map { index in TileData(
            route: loadingPatterns.representativeRoute,
            headsign: "placeholder",
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "\(index)", routeType: .lightRail, format: .Boarding())],
                secondaryAlert: nil
            )
        ) }
        StopDetailsFilteredDepartureDetails(
            stopId: stopId,
            stopFilter: stopFilter,
            tripFilter: tripFilter,
            setStopFilter: setStopFilter,
            setTripFilter: setTripFilter,
            tiles: tiles,
            statuses: statuses,
            alerts: alerts,
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
