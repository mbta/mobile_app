//
//  StopDetailsUnfilteredView.swift
//  iosApp
//
//  Created by esimon on 11/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import OrderedCollections
import Shared
import SwiftPhoenixClient
import SwiftUI

struct StopDetailsUnfilteredView: View {
    var stopId: String
    var now: Date
    var setStopFilter: (StopDetailsFilter?) -> Void

    var routeCardData: [RouteCardData]?
    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    @EnvironmentObject var settingsCache: SettingsCache

    var debugMode: Bool { settingsCache.get(.devDebugMode) }
    var stationAccessibility: Bool { settingsCache.get(.stationAccessibility) }

    var analytics: Analytics = AnalyticsProvider.shared
    let inspection = Inspection<Self>()

    init(
        stopId: String,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        routeCardData: [RouteCardData]?,
        now: Date,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        stopDetailsVM: StopDetailsViewModel
    ) {
        self.stopId = stopId
        self.setStopFilter = setStopFilter
        self.routeCardData = routeCardData
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.stopDetailsVM = stopDetailsVM
        self.now = now

        if let routeCardData {
            servedRoutes = routeCardData.map { routeCardData in
                switch onEnum(of: routeCardData.lineOrRoute) {
                case let .line(line): .line(line.line)
                case let .route(route): .route(route.route)
                }
            }
        }
    }

    var stop: Stop? {
        stopDetailsVM.global?.getStop(stopId: stopId)
    }

    var elevatorAlerts: [Shared.Alert]? {
        if let routeCardData {
            routeCardData.flatMap { $0.stopData.flatMap(\.elevatorAlerts) }.removingDuplicates()
        } else {
            nil
        }
    }

    var hasAccessibilityWarning: Bool {
        elevatorAlerts?.isEmpty == false || stop?.isWheelchairAccessible == false
    }

    var body: some View {
        ZStack {
            Color.fill2.ignoresSafeArea(.all)
            VStack(spacing: 0) {
                VStack(spacing: 16) {
                    SheetHeader(
                        title: stop?.name ?? "Invalid Stop",
                        onClose: { nearbyVM.goBack() }
                    )
                    if debugMode {
                        DebugView {
                            Text(verbatim: "stop id: \(stopId)")
                        }
                    }
                    ErrorBanner(errorBannerVM).padding(.horizontal, 16)
                    if servedRoutes.count > 1 {
                        StopDetailsFilterPills(
                            servedRoutes: servedRoutes,
                            tapRoutePill: tapRoutePill,
                            filter: nil,
                            setFilter: setStopFilter
                        )
                    }
                }
                .padding(.bottom, 6)
                .border(Color.halo.opacity(0.15), width: 2)
                ZStack {
                    Color.fill1.ignoresSafeArea(.all)
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            if stationAccessibility, hasAccessibilityWarning, let elevatorAlerts {
                                if !elevatorAlerts.isEmpty {
                                    ForEach(elevatorAlerts, id: \.id) { alert in
                                        AlertCard(
                                            alert: alert,
                                            alertSummary: nil,
                                            spec: .elevator,
                                            color: Color.clear,
                                            textColor: Color.text,
                                            onViewDetails: {
                                                nearbyVM.pushNavEntry(.alertDetails(
                                                    alertId: alert.id,
                                                    line: nil,
                                                    routes: nil,
                                                    stop: stop
                                                ))
                                                analytics.tappedAlertDetails(
                                                    routeId: "",
                                                    stopId: stopId,
                                                    alertId: alert.id,
                                                    elevator: true
                                                )
                                            }
                                        )
                                        .padding(.horizontal, 16)
                                        .padding(.bottom, 16)
                                    }
                                } else {
                                    NotAccessibleCard()
                                        .padding(.horizontal, 16)
                                        .padding(.bottom, 16)
                                }
                            }

                            if let routeCardData, let global = stopDetailsVM.global {
                                ForEach(routeCardData, id: \.lineOrRoute.id) { routeCardData in
                                    RouteCard(
                                        cardData: routeCardData,
                                        global: global,
                                        now: now,
                                        onPin: { routeId in Task {
                                            await stopDetailsVM.updateFavorites(
                                                .Pinned(routeId: routeId),
                                                enhancedFavorites: false
                                            )
                                        } },
                                        pinned: stopDetailsVM.isFavorite(
                                            .Pinned(routeId: routeCardData.lineOrRoute.id),
                                            enhancedFavorites: false
                                        ),
                                        pushNavEntry: { entry in nearbyVM.pushNavEntry(entry) },
                                        showStopHeader: false
                                    )
                                    .padding(.horizontal, 16)
                                    .padding(.bottom, 16)
                                }
                            } else {
                                loadingBody()
                            }
                        }
                    }
                    .padding(.top, 16)
                }
            }
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        let placeholderCards = LoadingPlaceholders.shared.stopDetailsRouteCards()
        VStack(spacing: 0) {
            ForEach(placeholderCards, id: \.id) { card in
                RouteCard(
                    cardData: card,
                    global: stopDetailsVM.global,
                    now: now,
                    onPin: { _ in },
                    pinned: false,
                    pushNavEntry: { _ in },
                    showStopHeader: false
                )
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
        }.loadingPlaceholder()
    }

    func tapRoutePill(_ filterBy: StopDetailsFilterPills.FilterBy) {
        let filterId = switch filterBy {
        case let .line(line):
            line.id
        case let .route(route):
            route.id
        }

        guard let routeCardData,
              let route = routeCardData.first(where: { $0.lineOrRoute.id == filterId }) else { return }
        analytics.tappedRouteFilter(routeId: route.lineOrRoute.id, stopId: stopId)
        let defaultDirectionId = route.stopData.flatMap { stopData in
            stopData.data.map(\.directionId)
        }.min() ?? 0
        setStopFilter(.init(routeId: filterId, directionId: defaultDirectionId))
    }
}
