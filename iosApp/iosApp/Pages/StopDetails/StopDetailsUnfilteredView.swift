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

    var routeData: StopDetailsViewModel.RouteData?
    var favorites: Favorites
    var global: GlobalResponse?
    var now: EasternTimeInstant

    var setStopFilter: (StopDetailsFilter?) -> Void

    let navCallbacks: NavigationCallbacks
    var errorBannerVM: IErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel

    @EnvironmentObject var settingsCache: SettingsCache

    var debugMode: Bool { settingsCache.get(.devDebugMode) }
    var stationAccessibility: Bool { settingsCache.get(.stationAccessibility) }

    var analytics: Analytics = AnalyticsProvider.shared
    let inspection = Inspection<Self>()

    init(
        stopId: String,
        routeData: StopDetailsViewModel.RouteData?,
        favorites: Favorites,
        global: GlobalResponse?,
        now: EasternTimeInstant,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        navCallbacks: NavigationCallbacks,
        errorBannerVM: IErrorBannerViewModel,
        nearbyVM: NearbyViewModel
    ) {
        self.stopId = stopId
        self.routeData = routeData
        self.favorites = favorites
        self.global = global
        self.now = now
        self.setStopFilter = setStopFilter
        self.navCallbacks = navCallbacks
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
    }

    var stop: Stop? {
        global?.getStop(stopId: stopId)
    }

    var routeCardData: [RouteCardData]? {
        if case let .unfiltered(data) = onEnum(of: routeData),
           data.filteredWith.stopId == stopId {
            data.routeCards
        } else { nil }
    }

    var servedRoutes: [StopDetailsFilterPills.FilterBy] { routeCardData?.servedRouteFilters ?? [] }

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

    var hasPillFilters: Bool { servedRoutes.count > 1 }

    var body: some View {
        ZStack {
            Color.fill2.ignoresSafeArea(.all)
            VStack(spacing: 0) {
                VStack(spacing: 0) {
                    SheetHeader(
                        title: stop?.name ?? "Invalid Stop",
                        buttonColor: .text.opacity(0.6),
                        buttonTextColor: .fill2,
                        navCallbacks: navCallbacks,
                    ).padding(.bottom, hasPillFilters ? 4 : 16)
                    if debugMode {
                        DebugView {
                            Text(verbatim: "stop id: \(stopId)")
                        }.padding([.horizontal, .bottom], 16)
                    }
                    ErrorBanner(errorBannerVM, padding: [
                        .init([.horizontal], 16),
                        .init([.top], hasPillFilters ? 8 : 0),
                        .init([.bottom], hasPillFilters ? 8 : 16),
                    ])
                    if hasPillFilters {
                        StopDetailsFilterPills(
                            servedRoutes: servedRoutes,
                            tapRoutePill: tapRoutePill,
                            filter: nil,
                            setFilter: setStopFilter
                        ).padding(.bottom, 10)
                    }
                }
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
                                                    routeId: nil,
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

                            if let routeCardData, let global {
                                ForEach(routeCardData, id: \.lineOrRoute.id) { routeCardData in
                                    RouteCard(
                                        cardData: routeCardData,
                                        global: global,
                                        now: now,
                                        isFavorite: { favorites.isFavorite($0) },
                                        pushNavEntry: { entry in nearbyVM.pushNavEntry(entry) },
                                        showStopHeader: false
                                    )
                                    .padding(.top, 2)
                                    .padding(.horizontal, 16)
                                    .padding(.bottom, 14)
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
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @ViewBuilder private func loadingBody() -> some View {
        let placeholderCards = LoadingPlaceholders.shared.stopDetailsRouteCards()
        VStack(spacing: 0) {
            ForEach(placeholderCards, id: \.id) { card in
                RouteCard(
                    cardData: card,
                    global: global,
                    now: now,
                    isFavorite: { _ in false },
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
