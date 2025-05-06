//
//  StopDetailsFilteredDepartureDetails.swift
//  iosApp
//
//  Created by esimon on 12/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopDetailsFilteredDepartureDetails: View {
    var stopId: String
    var stopFilter: StopDetailsFilter
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var data: DepartureDataBundle

    var pinned: Bool

    var now: Date

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    @EnvironmentObject var viewportProvider: ViewportProvider

    var testTiles: [TileData]? = nil

    let inspection = Inspection<Self>()

    var analytics: Analytics = AnalyticsProvider.shared

    @State var leafFormat: LeafFormat

    var tiles: [TileData] { testTiles ?? leafFormat.tileData() }
    var noPredictionsStatus: UpcomingFormat.NoTripsFormat? { leafFormat.noPredictionsStatus() }
    var isAllServiceDisrupted: Bool { leafFormat.isAllServiceDisrupted }

    var patternsHere: [RoutePattern] { data.leaf.routePatterns }
    var alerts: [Shared.Alert] { data.leaf.alertsHere }
    var downstreamAlerts: [Shared.Alert] { data.leaf.alertsDownstream }

    var stop: Stop? { stopDetailsVM.global?.getStop(stopId: stopId) }

    var routeColor: Color { Color(hex: data.routeData.lineOrRoute.backgroundColor) }
    var routeTextColor: Color { Color(hex: data.routeData.lineOrRoute.textColor) }
    var routeType: RouteType { data.routeData.lineOrRoute.type }

    var selectedTripIsCancelled: Bool {
        if let tripFilter {
            data.leaf.upcomingTrips.contains { upcoming in
                upcoming.trip.id == tripFilter.tripId && upcoming.isCancelled
            }
        } else {
            false
        }
    }

    var hasAccessibilityWarning: Bool {
        data.stopData.hasElevatorAlerts || !data.stopData.stop.isWheelchairAccessible
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
        data: DepartureDataBundle, pinned: Bool, now: Date,
        errorBannerVM: ErrorBannerViewModel, nearbyVM: NearbyViewModel, mapVM: MapViewModel,
        stopDetailsVM: StopDetailsViewModel, viewportProvider _: ViewportProvider,
        testTiles: [TileData]? = nil
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.data = data
        self.pinned = pinned
        self.now = now
        self.testTiles = testTiles
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM

        leafFormat =
            data.leaf.format(
                now: now.toKotlinInstant(),
                representativeRoute: data.routeData.lineOrRoute.sortRoute,
                globalData: stopDetailsVM.global,
                context: .stopDetailsFiltered
            )
        setAlertSummaries(AlertSummaryParams(
            global: stopDetailsVM.global,
            alerts: alerts,
            downstreamAlerts: downstreamAlerts,
            stopId: stopId,
            directionId: stopFilter.directionId,
            patternsHere: patternsHere,
            now: now
        ))
    }

    var body: some View {
        ZStack(alignment: .top) {
            routeColor.ignoresSafeArea(.all)
            Rectangle()
                .fill(Color.halo)
                .frame(height: 2)
                .frame(maxWidth: .infinity)
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 16) {
                    ScrollViewReader { view in
                        DirectionPicker(
                            data: data,
                            filter: stopFilter,
                            setFilter: { setStopFilter($0) }
                        )
                        .onChange(of: tripFilter) { filter in if let filter { view.scrollTo(filter.tripId) } }
                        .fixedSize(horizontal: false, vertical: true)
                        .padding([.horizontal, .top], 16)
                        .padding(.bottom, 6)
                        .dynamicTypeSize(...DynamicTypeSize.accessibility1)

                        if !isAllServiceDisrupted, !tiles.isEmpty {
                            departureTiles(view)
                                .dynamicTypeSize(...DynamicTypeSize.accessibility3)
                                .onAppear { if let id = tripFilter?.tripId { view.scrollTo(id) } }
                        }
                    }
                    alertCards

                    if isAllServiceDisrupted {
                        EmptyView()
                    } else if let noPredictionsStatus {
                        StopDetailsNoTripCard(
                            status: noPredictionsStatus,
                            accentColor: routeColor,
                            routeType: routeType,
                            hideMaps: stopDetailsVM.hideMaps
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
            }
        }
        .onAppear { handleViewportForStatus(noPredictionsStatus) }
        .onChange(of: noPredictionsStatus) { status in handleViewportForStatus(status) }
        .onChange(of: selectedTripIsCancelled) { if $0 { setViewportToStop() } }
        .onChange(of: tripFilter) { tripFilter in
            selectedDepartureFocus = tiles.first { $0.id == tripFilter?.tripId }?.id ?? cardFocusId
        }
        .onChange(of: data.leaf) { leaf in
            leafFormat = leaf.format(
                now: now.toKotlinInstant(),
                representativeRoute: data.routeData.lineOrRoute.sortRoute,
                globalData: stopDetailsVM.global,
                context: .stopDetailsFiltered
            )
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
            leafFormat = data.leaf.format(
                now: now.toKotlinInstant(),
                representativeRoute: data.routeData.lineOrRoute.sortRoute,
                globalData: global,
                context: .stopDetailsFiltered
            )
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
                                routeId: data.routeData.lineOrRoute.id,
                                stopId: data.stopData.stop.id,
                                pinned: pinned,
                                alert: alerts.count > 0,
                                routeType: data.routeData.lineOrRoute.type,
                                noTrips: nil
                            )
                            view.scrollTo(tileData.id)
                        },
                        pillDecoration: pillDecoration(tileData: tileData),
                        isSelected: tileData.upcoming.trip.id == tripFilter?.tripId
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
        if case .line = onEnum(of: data.routeData.lineOrRoute), let route = tileData.route {
            .onPrediction(route: route)
        } else {
            .none
        }
    }

    func getAlertDetailsHandler(_ alertId: String, spec: AlertCardSpec) -> () -> Void {
        {
            let line: Line? = switch onEnum(of: data.routeData.lineOrRoute) {
            case let .line(line): line.line
            default: nil
            }
            let routes = switch onEnum(of: data.routeData.lineOrRoute) {
            case let .line(line): Array(line.routes)
            case let .route(route): [route.route]
            }
            nearbyVM.pushNavEntry(.alertDetails(
                alertId: alertId,
                line: spec == .elevator ? nil : line,
                routes: spec == .elevator ? nil : routes,
                stop: data.stopData.stop
            ))
            analytics.tappedAlertDetails(
                routeId: data.routeData.lineOrRoute.id,
                stopId: data.stopData.stop.id,
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
            (stopDetailsVM.showStationAccessibility && hasAccessibilityWarning) {
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
                if stopDetailsVM.showStationAccessibility, hasAccessibilityWarning {
                    if data.stopData.hasElevatorAlerts {
                        ForEach(data.stopData.elevatorAlerts, id: \.id) { alert in
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
