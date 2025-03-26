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

    var departures: StopDetailsDepartures?
    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    var analytics: Analytics = AnalyticsProvider.shared
    let inspection = Inspection<Self>()

    init(
        stopId: String,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        departures: StopDetailsDepartures?,
        now: Date,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        stopDetailsVM: StopDetailsViewModel
    ) {
        self.stopId = stopId
        self.setStopFilter = setStopFilter
        self.departures = departures
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
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

    var hasAccessibilityWarning: Bool {
        departures?.elevatorAlerts.isEmpty == false || stop?.isWheelchairAccessible == false
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
                            filter: nil,
                            setFilter: setStopFilter
                        )
                    }
                }
                .padding(.bottom, 16)
                .border(Color.halo.opacity(0.15), width: 2)
                ZStack {
                    Color.fill1.ignoresSafeArea(.all)
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            if let departures {
                                if stopDetailsVM.showElevatorAccessibility, hasAccessibilityWarning {
                                    if !departures.elevatorAlerts.isEmpty {
                                        ForEach(departures.elevatorAlerts, id: \.id) { alert in
                                            AlertCard(
                                                alert: alert,
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
                                    }
                                } else {
                                    NotAccessibleCard()
                                        .padding(.horizontal, 16)
                                        .padding(.bottom, 16)
                                }

                                ForEach(departures.routes, id: \.routeIdentifier) { patternsByStop in
                                    StopDetailsRouteView(
                                        patternsByStop: patternsByStop,
                                        now: now.toKotlinInstant(),
                                        pushNavEntry: { entry in nearbyVM.pushNavEntry(entry) },
                                        pinned: stopDetailsVM.pinnedRoutes.contains(patternsByStop.routeIdentifier),
                                        onPin: { routeId in Task { await stopDetailsVM.togglePinnedRoute(routeId) } }
                                    )
                                }
                            } else {
                                loadingBody()
                            }
                        }
                    }.padding(.top, 16)
                }
            }
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        let placeholderDepartures = LoadingPlaceholders.shared.stopDetailsDepartures(filter: nil)
        VStack(spacing: 0) {
            ForEach(placeholderDepartures.routes, id: \.routeIdentifier) { patternsByStop in
                StopDetailsRouteView(
                    patternsByStop: patternsByStop,
                    now: now.toKotlinInstant(),
                    pushNavEntry: { _ in },
                    pinned: false,
                    onPin: { _ in }
                )
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
