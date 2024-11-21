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
    var global: GlobalResponse?
    var pinnedRoutes: Set<String>
    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []
    let togglePinnedRoute: (String) -> Void

    var now = Date.now
    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel

    let tripPredictionsRepository: ITripPredictionsRepository
    let tripRepository: ITripRepository
    let vehicleRepository: IVehicleRepository

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
        global: GlobalResponse?,
        pinnedRoutes: Set<String>,
        togglePinnedRoute: @escaping (String) -> Void,
        now: Date,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,

        tripPredictionsRepository: ITripPredictionsRepository = RepositoryDI().tripPredictions,
        tripRepository: ITripRepository = RepositoryDI().trip,
        vehicleRepository: IVehicleRepository = RepositoryDI().vehicle
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.departures = departures
        self.global = global
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.now = now
        self.pinnedRoutes = pinnedRoutes
        self.togglePinnedRoute = togglePinnedRoute

        self.tripPredictionsRepository = tripPredictionsRepository
        self.tripRepository = tripRepository
        self.vehicleRepository = vehicleRepository

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
        global?.stops[stopId]
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
                        global: global,
                        now: now.toKotlinInstant(),
                        filter: stopFilter,
                        setFilter: setStopFilter,
                        pushNavEntry: { entry in nearbyVM.pushNavEntry(entry) },
                        pinRoute: togglePinnedRoute,
                        pinnedRoutes: pinnedRoutes
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
                        global: global,
                        errorBannerVM: errorBannerVM,
                        nearbyVM: nearbyVM,
                        mapVM: mapVM,
                        tripPredictionsRepository: tripPredictionsRepository,
                        tripRepository: tripRepository,
                        vehicleRepository: vehicleRepository
                    )
                }
            }
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        StopDetailsRoutesView(
            departures: LoadingPlaceholders.shared.stopDetailsDepartures(filter: stopFilter),
            global: global,
            now: now.toKotlinInstant(),
            filter: stopFilter,
            setFilter: { _ in },
            pushNavEntry: { _ in },
            pinRoute: { _ in },
            pinnedRoutes: pinnedRoutes
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
