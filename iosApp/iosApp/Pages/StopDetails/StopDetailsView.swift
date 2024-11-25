//
//  StopDetailsView.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation

import OrderedCollections
import shared
import SwiftPhoenixClient
import SwiftUI

struct StopDetailsView: View {
    var stopId: String
    var stopFilter: StopDetailsFilter?
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var departures: StopDetailsDepartures?
    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []

    var now = Date.now
    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    var analytics: StopDetailsAnalytics = AnalyticsProvider.shared
    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(
        stopId: String,
        stopFilter: StopDetailsFilter?,
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
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM
        self.now = now

        if let departures {
            servedRoutes = departures.routes.map { patterns in
                if let line = patterns.line {
                    return .line(line)
                }
                return .route(
                    patterns.representativeRoute
                )
            }
        }
    }

    var stop: Stop? {
        stopDetailsVM.global?.stops[stopId]
    }

    var body: some View {
        ZStack {
            Color.fill2.ignoresSafeArea(.all)
            VStack(spacing: 0) {
                VStack(spacing: 16) {
                    SheetHeader(
                        title: stop?.name ?? "Invalid Stop",
                        onBack: nearbyVM.navigationStack.count > 1 ? { nearbyVM.goBack() } : nil,
                        onClose: { nearbyVM.navigationStack.removeAll() }
                    )
                    if nearbyVM.showDebugMessages {
                        DebugView {
                            Text(verbatim: "stop id: \(stopId)")
                        }
                    }
                    ErrorBanner(errorBannerVM).padding(.horizontal, 16)
                    if servedRoutes.count > 1 {
                        StopDetailsFilterPills(
                            servedRoutes: servedRoutes,
                            tapRoutePill: tapRoutePill,
                            filter: stopFilter,
                            setFilter: setStopFilter
                        )
                    }
                }
                .padding(.bottom, 16)
                .border(Color.halo.opacity(0.15), width: 2)

                if let departures {
                    StopDetailsRoutesView(
                        departures: departures,
                        global: stopDetailsVM.global,
                        now: now.toKotlinInstant(),
                        filter: stopFilter,
                        setFilter: setStopFilter,
                        pushNavEntry: { entry in nearbyVM.pushNavEntry(entry) },
                        pinRoute: stopDetailsVM.togglePinnedRoute,
                        pinnedRoutes: stopDetailsVM.pinnedRoutes
                    ).frame(maxHeight: .infinity)
                } else {
                    loadingBody()
                }

                if let stopFilter, let tripFilter {
                    TripDetailsView(
                        tripId: tripFilter.tripId,
                        vehicleId: tripFilter.vehicleId,
                        routeId: stopFilter.routeId,
                        stopId: stopId,
                        stopSequence: tripFilter.stopSequence?.intValue,
                        global: stopDetailsVM.global,
                        errorBannerVM: errorBannerVM,
                        nearbyVM: nearbyVM,
                        mapVM: mapVM
                    )
                }
            }
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        StopDetailsRoutesView(
            departures: LoadingPlaceholders.shared.stopDetailsDepartures(filter: stopFilter),
            global: stopDetailsVM.global,
            now: now.toKotlinInstant(),
            filter: stopFilter,
            setFilter: { _ in },
            pushNavEntry: { _ in },
            pinRoute: { _ in },
            pinnedRoutes: stopDetailsVM.pinnedRoutes
        )
        .loadingPlaceholder()
    }

    func tapRoutePill(_ filterBy: StopDetailsFilterPills.FilterBy) {
        let filterId = switch filterBy {
        case let .line(line):
            line.id
        case let .route(route):
            route.id
        }
        if stopFilter?.routeId == filterId { setStopFilter(nil); return }
        guard let departures else { return }
        guard let patterns = departures.routes.first(where: { patterns in patterns.routeIdentifier == filterId })
        else { return }
        analytics.tappedRouteFilter(routeId: patterns.routeIdentifier, stopId: stopId)
        let defaultDirectionId = patterns.patterns.flatMap { headsign in
            // RealtimePatterns.patterns is a List<RoutePattern?> but that gets bridged as [Any] for some reason
            headsign.patterns.compactMap { pattern in (pattern as? RoutePattern)?.directionId }
        }.min() ?? 0
        setStopFilter(.init(routeId: filterId, directionId: defaultDirectionId))
    }
}
