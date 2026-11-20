//
//  StopOverviewView.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import OrderedCollections
import Shared
import SwiftPhoenixClient
import SwiftUI

struct StopOverviewView: View {
    @ObserveInjection var inject

    var filters: StopOverviewPageFilters

    var routeData: StopCentricViewModel.RouteData?
    var alerts: AlertsStreamDataResponse?
    var favorites: Favorites?
    var global: GlobalResponse?
    var now: EasternTimeInstant

    var setRouteStopFilter: (RouteStopFilter?) -> Void
    var navCallbacks: NavigationCallbacks

    var errorBannerVM: IErrorBannerViewModel

    @ObservedObject var navManager: NavigationManager

    @EnvironmentObject var settingsCache: SettingsCache

    var analytics: Analytics = AnalyticsProvider.shared
    let inspection = Inspection<Self>()

    init(
        filters: StopOverviewPageFilters,
        routeData: StopCentricViewModel.RouteData?,
        favorites: Favorites?,
        global: GlobalResponse?,
        now: EasternTimeInstant,
        setRouteStopFilter: @escaping (RouteStopFilter?) -> Void,
        navCallbacks: NavigationCallbacks,
        errorBannerVM: IErrorBannerViewModel,
        navManager: NavigationManager,
    ) {
        self.filters = filters
        self.routeData = routeData
        self.favorites = favorites
        self.global = global
        self.now = now
        self.setRouteStopFilter = setRouteStopFilter
        self.navCallbacks = navCallbacks
        self.errorBannerVM = errorBannerVM
        self.navManager = navManager
    }

    var stopId: String { filters.stopId }

    var stop: Stop? {
        global?.getStop(stopId: stopId)
    }

    var stationAccessibility: Bool {
        settingsCache.get(.stationAccessibility) && !global.isStopBlocklisted(stop?.parentStationId ?? stopId)
    }

    var routeCardData: [RouteCardData]? {
        if case let .stopOverview(data) = onEnum(of: routeData),
           data.filteredWith.stopId == stopId {
            data.routeCards
        } else { nil }
    }

    var servedRoutes: [StopOverviewFilterPills.FilterBy] { routeCardData?.servedRouteFilters ?? [] }

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
                        buttonColor: .deemphasized,
                        buttonTextColor: .fill2,
                        navCallbacks: navCallbacks,
                    ).padding(.bottom, hasPillFilters ? 4 : 16)
                    ChannelDebugView {
                        Text(verbatim: "stop id: \(stopId)")
                    }
                    ErrorBanner(errorBannerVM, padding: [
                        .init([.horizontal], 16),
                        .init([.top], hasPillFilters ? 8 : 0),
                        .init([.bottom], hasPillFilters ? 8 : 16),
                    ])
                    if hasPillFilters {
                        StopOverviewFilterPills(
                            servedRoutes: servedRoutes,
                            tapRoutePill: tapRoutePill,
                            filter: nil,
                            setFilter: setRouteStopFilter
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
                                    let displayAlerts: DisplayAlerts = .init(highPriority: elevatorAlerts.map { alert in
                                        DisplayAlert(alert: alert, isDownstream: false)
                                    },
                                    lowPriority: [])
                                    AlertListContainer(displayAlerts: displayAlerts,
                                                       showNotAccessibleCard: false,
                                                       now: now,
                                                       isAllServiceDisrupted: false,
                                                       routeIdMatcher: MatcherWildcard(),
                                                       stopIdMatcher: MatcherData(value: stopId as NSString),
                                                       directionIdMatcher: MatcherWildcard(),
                                                       tripId: nil,
                                                       routeAccents: .init(),
                                                       onRowTap: { id, _ in
                                                           navManager.pushNavEntry(.alertDetails(
                                                               alertId: id,
                                                               line: nil,
                                                               routes: nil,
                                                               stop: stop
                                                           ))
                                                           analytics.tappedAlertDetails(
                                                               routeId: nil,
                                                               stopId: stopId,
                                                               alertId: id,
                                                               elevator: true
                                                           )
                                                       })
                                                       .padding(.horizontal, 16)
                                                       .padding(.bottom, 16)

                                } else {
                                    NotAccessibleCard()
                                        .standaloneStyling()
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
                                        isFavorite: { favorites?.isFavorite($0) ?? false },
                                        pushNavEntry: { entry in navManager.pushNavEntry(entry) },
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
        .enableInjection()
    }

    @ViewBuilder private func loadingBody() -> some View {
        let placeholderCards = LoadingPlaceholders.shared.stopOverviewRouteCards()
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

    func tapRoutePill(_ filterBy: StopOverviewFilterPills.FilterBy) {
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
            stopData.data.map(\.direction.id)
        }.min() ?? 0
        setRouteStopFilter(.init(routeId: filterId, directionId: defaultDirectionId))
    }
}
