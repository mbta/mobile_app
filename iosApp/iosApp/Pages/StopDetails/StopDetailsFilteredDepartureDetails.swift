//
//  StopDetailsFilteredDepartureDetails.swift
//  iosApp
//
//  Created by esimon on 12/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

// swiftlint:disable:next type_body_length
struct StopDetailsFilteredDepartureDetails: View {
    var stopId: String
    var stopFilter: StopDetailsFilter
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var leaf: RouteCardData.Leaf
    var selectedDirection: Direction

    var favorite: Bool

    var now: Date

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: iosApp.MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    @EnvironmentObject var viewportProvider: ViewportProvider

    @EnvironmentObject var settingsCache: SettingsCache

    let inspection = Inspection<Self>()

    var analytics: Analytics = AnalyticsProvider.shared

    @State var leafFormat: LeafFormat

    var tiles: [TileData] { leafFormat.tileData(directionDestination: selectedDirection.destination) }
    var noPredictionsStatus: UpcomingFormat.NoTripsFormat? { leafFormat.noPredictionsStatus() }
    var isAllServiceDisrupted: Bool { leafFormat.isAllServiceDisrupted }

    var patternsHere: [RoutePattern] { leaf.routePatterns }
    var alerts: [Shared.Alert] { leaf.alertsHere(tripId: tripFilter?.tripId).filter { $0.effect != .elevatorClosure } }
    var elevatorAlerts: [Shared.Alert] {
        leaf.alertsHere(tripId: tripFilter?.tripId).filter { $0.effect == .elevatorClosure }
    }

    var downstreamAlerts: [Shared.Alert] { leaf.alertsDownstream(tripId: tripFilter?.tripId) }

    var stop: Stop? { stopDetailsVM.global?.getStop(stopId: stopId) }

    var routeColor: Color { Color(hex: leaf.lineOrRoute.backgroundColor) }
    var routeTextColor: Color { Color(hex: leaf.lineOrRoute.textColor) }
    var routeType: RouteType { leaf.lineOrRoute.type }

    var selectedTripIsCancelled: Bool {
        if let tripFilter {
            leaf.upcomingTrips.contains { upcoming in
                upcoming.trip.id == tripFilter.tripId && upcoming.isCancelled
            }
        } else {
            false
        }
    }

    var hasAccessibilityWarning: Bool {
        !elevatorAlerts.isEmpty || !leaf.stop.isWheelchairAccessible
    }

    @AccessibilityFocusState private var selectedDepartureFocus: String?
    private let cardFocusId = "_card"

    struct AlertSummaryParams: Equatable {
        let global: GlobalResponse?
        let alerts: [Shared.Alert]
        let downstreamAlerts: [Shared.Alert]
        let stopId: String
        let directionId: Int32
        let patternsHere: [RoutePattern]?
        let now: Date
    }

    init(
        stopId: String,
        stopFilter: StopDetailsFilter,
        tripFilter: TripDetailsFilter? = nil,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        leaf: RouteCardData.Leaf, selectedDirection: Direction, favorite: Bool, now: Date,
        errorBannerVM: ErrorBannerViewModel, nearbyVM: NearbyViewModel, mapVM: iosApp.MapViewModel,
        stopDetailsVM: StopDetailsViewModel, viewportProvider _: ViewportProvider
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.leaf = leaf
        self.selectedDirection = selectedDirection
        self.favorite = favorite
        self.now = now
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM

        leafFormat = leaf.format(now: now.toKotlinInstant(), globalData: stopDetailsVM.global)
    }

    var body: some View {
        VStack(spacing: 16) {
            ScrollViewReader { view in
                if !isAllServiceDisrupted, !tiles.isEmpty {
                    departureTiles(view)
                        .dynamicTypeSize(...DynamicTypeSize.accessibility3)
                        .onAppear { if let id = tiles.first(where: { $0.isSelected(tripFilter: tripFilter) })?.id {
                            view.scrollTo(id)
                        }
                        }
                        .onChange(of: tripFilter) { filter in
                            if let id = tiles.first(where: { $0.isSelected(tripFilter: filter) })?.id {
                                view.scrollTo(id)
                            }
                        }
                }
            }
            alertCards

            if isAllServiceDisrupted {
                EmptyView()
            } else if let noPredictionsStatus {
                StopDetailsNoTripCard(
                    status: noPredictionsStatus,
                    accentColor: routeColor,
                    routeType: routeType
                )
                .accessibilityHeading(.h3)
                .accessibilityFocused($selectedDepartureFocus, equals: cardFocusId)
            } else if selectedTripIsCancelled {
                StopDetailsIconCard(
                    accentColor: routeColor,
                    details: Text(
                        "This trip has been cancelled. We’re sorry for the inconvenience.",
                        comment: "Explanation for a cancelled trip on stop details"
                    ),
                    header: Text(
                        "Trip cancelled",
                        comment: "Header for a cancelled trip card on stop details"
                    ),
                    icon: routeSlashIcon(routeType)
                )
                .accessibilityHeading(.h4)
            } else {
                TripDetailsView(
                    tripFilter: tripFilter,
                    stopId: stopId,
                    now: now,
                    errorBannerVM: errorBannerVM,
                    nearbyVM: nearbyVM,
                    mapVM: mapVM,
                    stopDetailsVM: stopDetailsVM,
                    onOpenAlertDetails: { alert in getAlertDetailsHandler(alert.id, spec: .downstream)() }
                )
            }
        }
        .onAppear {
            handleViewportForStatus(noPredictionsStatus)
            setAlertSummaries(
                AlertSummaryParams(
                    global: stopDetailsVM.global,
                    alerts: alerts,
                    downstreamAlerts: downstreamAlerts,
                    stopId: stopId,
                    directionId: stopFilter.directionId,
                    patternsHere: patternsHere,
                    now: now
                )
            )
        }
        .onChange(of: noPredictionsStatus) { status in handleViewportForStatus(status) }
        .onChange(of: selectedTripIsCancelled) { if $0 { setViewportToStop() } }
        .onChange(of: tripFilter) { tripFilter in
            selectedDepartureFocus = tiles.first { $0.isSelected(tripFilter: tripFilter) }?.id ?? cardFocusId
        }
        .onChange(of: leaf) { leaf in
            leafFormat = leaf.format(now: now.toKotlinInstant(), globalData: stopDetailsVM.global)
        }
        .onChange(of: AlertSummaryParams(
            global: stopDetailsVM.global,
            alerts: alerts,
            downstreamAlerts: downstreamAlerts,
            stopId: stopId,
            directionId: stopFilter.directionId,
            patternsHere: patternsHere,
            now: now
        )) { newParams in
            setAlertSummaries(newParams)
        }
        .onChange(of: stopDetailsVM.global) { global in
            leafFormat = leaf.format(now: now.toKotlinInstant(), globalData: global)
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .ignoresSafeArea(.all)
    }

    func handleViewportForStatus(_ status: UpcomingFormat.NoTripsFormat?) {
        if let status {
            switch onEnum(of: status) {
            case .predictionsUnavailable: setViewportToStop(midZoom: true)
            case .noSchedulesToday, .serviceEndedToday: setViewportToStop()
            }
        }
    }

    func setViewportToStop(midZoom: Bool = false) {
        if let stop {
            viewportProvider.animateTo(
                coordinates: stop.coordinate,
                zoom: midZoom ? MapDefaults.shared.midZoomThreshold : nil
            )
        }
    }

    @ViewBuilder
    func departureTiles(_ view: ScrollViewProxy) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(alignment: .top, spacing: 0) {
                ForEach(tiles, id: \.id) { tileData in
                    DepartureTile(
                        data: tileData,
                        onTap: {
                            nearbyVM.navigationStack.lastTripDetailsFilter = .init(
                                tripId: tileData.upcoming.trip.id,
                                vehicleId: tileData.upcoming.prediction?.vehicleId,
                                stopSequence: tileData.upcoming.stopSequence,
                                selectionLock: false
                            )
                            analytics.tappedDeparture(
                                routeId: leaf.lineOrRoute.id,
                                stopId: leaf.stop.id,
                                pinned: favorite,
                                alert: alerts.count > 0,
                                routeType: leaf.lineOrRoute.type,
                                noTrips: nil
                            )
                            view.scrollTo(tileData.id)
                        },
                        pillDecoration: pillDecoration(tileData: tileData),
                        isSelected: tileData.isSelected(tripFilter: tripFilter)
                    )
                    .accessibilityFocused($selectedDepartureFocus, equals: tileData.id)
                    .padding(.horizontal, 4)
                }
            }

            .padding(.horizontal, 12)
            .padding(.vertical, 1)
            .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func setAlertSummaries(_ alertSummaryParams: AlertSummaryParams) {
        Task {
            let summaries = await alertSummaries(alertSummaryParams)
            stopDetailsVM.setAlertSummaries(summaries)
        }
    }

    private func alertSummaries(_ alertSummaryParams: AlertSummaryParams) async -> [String: AlertSummary?] {
        let allAlerts = alertSummaryParams.alerts + alertSummaryParams.downstreamAlerts
        var alertMap: [String: AlertSummary?] = [:]

        if let global = alertSummaryParams.global, let patternsHere = alertSummaryParams.patternsHere {
            for alert in allAlerts {
                let summary = try? await alert.summary(
                    stopId: alertSummaryParams.stopId,
                    directionId: alertSummaryParams.directionId,
                    patterns: patternsHere,
                    atTime: alertSummaryParams.now.toKotlinInstant(),
                    global: global
                )
                alertMap[alert.id] = summary
            }
        }
        return alertMap
    }

    private func pillDecoration(tileData: TileData) -> DepartureTile.PillDecoration {
        if case .line = onEnum(of: leaf.lineOrRoute), let route = tileData.route {
            .onPrediction(route: route)
        } else {
            .none
        }
    }

    func getAlertDetailsHandler(_ alertId: String, spec: AlertCardSpec) -> () -> Void {
        {
            let line: Line? = switch onEnum(of: leaf.lineOrRoute) {
            case let .line(line): line.line
            default: nil
            }
            let routes = switch onEnum(of: leaf.lineOrRoute) {
            case let .line(line): Array(line.routes)
            case let .route(route): [route.route]
            }
            nearbyVM.pushNavEntry(.alertDetails(
                alertId: alertId,
                line: spec == .elevator ? nil : line,
                routes: spec == .elevator ? nil : routes,
                stop: leaf.stop
            ))
            analytics.tappedAlertDetails(
                routeId: leaf.lineOrRoute.id,
                stopId: leaf.stop.id,
                alertId: alertId,
                elevator: spec == .elevator
            )
        }
    }

    @ViewBuilder
    func alertCard(_ alert: Shared.Alert, _ summary: AlertSummary?, _ spec: AlertCardSpec? = nil) -> some View {
        let spec: AlertCardSpec = if let spec {
            spec
        } else if alert.significance == .major, isAllServiceDisrupted {
            .major
        } else if alert.significance == .minor, alert.effect == .delay {
            .delay
        } else {
            .secondary
        }

        AlertCard(
            alert: alert,
            alertSummary: summary,
            spec: spec,
            color: routeColor,
            textColor: routeTextColor,
            onViewDetails: getAlertDetailsHandler(alert.id, spec: spec)
        )
    }

    @ViewBuilder
    var alertCards: some View {
        if !alerts.isEmpty ||
            !downstreamAlerts.isEmpty ||
            (settingsCache.get(.stationAccessibility) && hasAccessibilityWarning) {
            VStack(spacing: 16) {
                ForEach(alerts, id: \.id) { alert in
                    if stopDetailsVM.alertSummaries.keys.contains(alert.id) {
                        alertCard(alert, stopDetailsVM.alertSummaries[alert.id] ?? nil)
                    }
                }
                ForEach(downstreamAlerts, id: \.id) { alert in
                    if stopDetailsVM.alertSummaries.keys.contains(alert.id) {
                        alertCard(alert, stopDetailsVM.alertSummaries[alert.id] ?? nil, .downstream)
                    }
                }
                if settingsCache.get(.stationAccessibility), hasAccessibilityWarning {
                    if !elevatorAlerts.isEmpty {
                        ForEach(elevatorAlerts, id: \.id) { alert in
                            alertCard(alert, nil, .elevator)
                        }
                    } else {
                        NotAccessibleCard()
                    }
                }
            }.padding(.horizontal, 16)
        }
    }
}
