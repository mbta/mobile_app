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
    var analytics: StopDetailsAnalytics = AnalyticsProvider.shared
    let globalRepository: IGlobalRepository
    @State var globalResponse: GlobalResponse?
    var stopId: String
    var stopFilter: StopDetailsFilter?
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var departures: StopDetailsDepartures?
    var now = Date.now
    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []
    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    let pinnedRoutes: Set<String>
    @State var predictions: PredictionsStreamDataResponse?

    let togglePinnedRoute: (String) -> Void

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        stopId: String,
        stopFilter: StopDetailsFilter?,
        tripFilter _: TripDetailsFilter?,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        departures: StopDetailsDepartures?,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        now: Date,
        pinnedRoutes: Set<String>,
        togglePinnedRoute: @escaping (String) -> Void
    ) {
        self.globalRepository = globalRepository
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.setStopFilter = setStopFilter
        self.departures = departures
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.now = now
        self.pinnedRoutes = pinnedRoutes
        self.togglePinnedRoute = togglePinnedRoute

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
        globalResponse?.stops[stopId]
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
                            Text(verbatim: "stop id: \(stop?.id ?? "nil stop")")
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
                        global: globalResponse,
                        now: now.toKotlinInstant(),
                        filter: stopFilter,
                        setFilter: setStopFilter,
                        pushNavEntry: nearbyVM.pushNavEntry,
                        pinRoute: togglePinnedRoute,
                        pinnedRoutes: pinnedRoutes
                    ).frame(maxHeight: .infinity)
                } else {
                    loadingBody()
                }
            }
        }
        .task {
            loadGlobal()
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        StopDetailsRoutesView(
            departures: LoadingPlaceholders.shared.stopDetailsDepartures(filter: stopFilter),
            global: globalResponse,
            now: now.toKotlinInstant(),
            filter: stopFilter,
            setStopFilter: { _ in },
            pushNavEntry: { _ in },
            pinRoute: { _ in },
            pinnedRoutes: pinnedRoutes
        )
        .loadingPlaceholder()
    }

    private func loadGlobal() {
        Task {
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "StopDetailsView.loadGlobal",
                getData: globalRepository.getGlobalData,
                onSuccess: { globalResponse = $0 },
                onRefreshAfterError: loadGlobal
            )
        }
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
