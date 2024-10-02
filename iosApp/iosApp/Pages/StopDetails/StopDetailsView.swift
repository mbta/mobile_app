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
    let errorBannerRepository: IErrorBannerStateRepository
    let globalRepository: IGlobalRepository
    @State var globalResponse: GlobalResponse?
    var stop: Stop
    var filter: StopDetailsFilter?
    var setFilter: (StopDetailsFilter?) -> Void
    var departures: StopDetailsDepartures?
    var now = Date.now
    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []
    @ObservedObject var nearbyVM: NearbyViewModel
    let pinnedRoutes: Set<String>
    @State var predictions: PredictionsStreamDataResponse?

    let togglePinnedRoute: (String) -> Void

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(
        errorBannerRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        globalRepository: IGlobalRepository = RepositoryDI().global,
        stop: Stop,
        filter: StopDetailsFilter?,
        setFilter: @escaping (StopDetailsFilter?) -> Void,
        departures: StopDetailsDepartures?,
        nearbyVM: NearbyViewModel,
        now: Date,
        pinnedRoutes: Set<String>,
        togglePinnedRoute: @escaping (String) -> Void
    ) {
        self.errorBannerRepository = errorBannerRepository
        self.globalRepository = globalRepository
        self.stop = stop
        self.filter = filter
        self.setFilter = setFilter
        self.departures = departures
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
                    patterns.representativeRoute,
                    globalResponse?.getLine(lineId: patterns.representativeRoute.lineId)
                )
            }
        }
    }

    var body: some View {
        ZStack {
            Color.fill2.ignoresSafeArea(.all)
            VStack(spacing: 0) {
                VStack {
                    SheetHeader(
                        title: stop.name,
                        onBack: nearbyVM.navigationStack.count > 1 ? { nearbyVM.goBack() } : nil,
                        onClose: { nearbyVM.navigationStack.removeAll() }
                    )
                    ErrorBanner()
                    if servedRoutes.count > 1 {
                        StopDetailsFilterPills(
                            servedRoutes: servedRoutes,
                            tapRoutePill: tapRoutePill,
                            filter: filter,
                            setFilter: setFilter
                        )
                        .padding([.bottom], 8)
                    }
                }
                .border(Color.halo.opacity(0.15), width: 2)

                if let departures {
                    StopDetailsRoutesView(
                        departures: departures,
                        global: globalResponse,
                        now: now.toKotlinInstant(),
                        filter: filter,
                        setFilter: setFilter,
                        pushNavEntry: nearbyVM.pushNavEntry,
                        pinRoute: togglePinnedRoute,
                        pinnedRoutes: pinnedRoutes
                    ).frame(maxHeight: .infinity)
                } else {
                    LoadingCard()
                }
            }
        }
        .task {
            loadGlobal()
        }
    }

    private func loadGlobal() {
        Task {
            await fetchApi(
                errorBannerRepository,
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
        case let .route(route, _):
            route.id
        }
        if filter?.routeId == filterId { setFilter(nil); return }
        guard let departures else { return }
        guard let patterns = departures.routes.first(where: { patterns in patterns.routeIdentifier == filterId })
        else { return }
        analytics.tappedRouteFilter(routeId: patterns.routeIdentifier, stopId: stop.id)
        let defaultDirectionId = patterns.patterns.flatMap { headsign in
            headsign.patterns.map { pattern in pattern.directionId }
        }.min() ?? 0
        setFilter(.init(routeId: filterId, directionId: defaultDirectionId))
    }
}
