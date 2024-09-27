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
    var stop: Stop
    @Binding var filter: StopDetailsFilter?
    @State var now = Date.now
    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []
    @ObservedObject var nearbyVM: NearbyViewModel
    let pinnedRoutes: Set<String>
    @State var predictions: PredictionsStreamDataResponse?

    let togglePinnedRoute: (String) -> Void

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        stop: Stop,
        filter: Binding<StopDetailsFilter?>,
        nearbyVM: NearbyViewModel,
        pinnedRoutes: Set<String>,
        togglePinnedRoute: @escaping (String) -> Void
    ) {
        self.globalRepository = globalRepository
        self.stop = stop
        _filter = filter
        self.nearbyVM = nearbyVM
        self.pinnedRoutes = pinnedRoutes
        self.togglePinnedRoute = togglePinnedRoute

        if let departures = nearbyVM.departures {
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
                    SheetHeader(onClose: { nearbyVM.goBack() }, title: stop.name)
                    ErrorBanner()
                    if servedRoutes.count > 1 {
                        StopDetailsFilterPills(
                            servedRoutes: servedRoutes,
                            tapRoutePill: tapRoutePill,
                            filter: $filter
                        )
                        .padding([.bottom], 8)
                    }
                }
                .border(Color.halo.opacity(0.15), width: 2)

                if let departures = nearbyVM.departures {
                    StopDetailsRoutesView(
                        departures: departures,
                        global: globalResponse,
                        now: now.toKotlinInstant(),
                        filter: $filter,
                        pushNavEntry: nearbyVM.pushNavEntry,
                        pinRoute: togglePinnedRoute,
                        pinnedRoutes: pinnedRoutes
                    ).frame(maxHeight: .infinity)
                } else {
                    ProgressView()
                }
            }
        }
        .onReceive(timer) { input in
            now = input
        }
        .task {
            do {
                globalResponse = try await globalRepository.getGlobalData()
            } catch {
                debugPrint(error)
            }
        }
    }

    func tapRoutePill(_ filterBy: StopDetailsFilterPills.FilterBy) {
        let filterId = switch filterBy {
        case let .line(line):
            line.id
        case let .route(route, _):
            route.id
        }
        if filter?.routeId == filterId { filter = nil; return }
        guard let departures = nearbyVM.departures else { return }
        guard let patterns = departures.routes.first(where: { patterns in patterns.routeIdentifier == filterId })
        else { return }
        analytics.tappedRouteFilter(routeId: patterns.routeIdentifier, stopId: stop.id)
        let defaultDirectionId = patterns.patterns.flatMap { headsign in
            headsign.patterns.map { pattern in pattern.directionId }
        }.min() ?? 0
        filter = .init(routeId: filterId, directionId: defaultDirectionId)
    }
}
