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

    var tiles: [TileData]
    var noPredictionsStatus: UpcomingFormat.NoTripsFormat?
    var alerts: [Shared.Alert]
    var downstreamAlerts: [Shared.Alert]
    var patternsByStop: PatternsByStop
    var pinned: Bool

    var now: Date

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    @EnvironmentObject var viewportProvider: ViewportProvider

    var analytics: Analytics = AnalyticsProvider.shared

    var showTileHeadsigns: Bool {
        patternsByStop.line != nil || !tiles.allSatisfy { tile in
            tile.headsign == tiles.first?.headsign
        }
    }

    var stop: Stop? { stopDetailsVM.global?.stops[stopId] }

    var routeColor: Color { Color(hex: patternsByStop.representativeRoute.color) }
    var routeTextColor: Color { Color(hex: patternsByStop.representativeRoute.textColor) }
    var routeType: RouteType { patternsByStop.representativeRoute.type }

    var selectedTripIsCancelled: Bool {
        if let tripFilter {
            patternsByStop.tripIsCancelled(tripId: tripFilter.tripId)

        } else {
            false
        }
    }

    var hasMajorAlert: Bool {
        alerts.contains(where: { $0.significance == .major })
    }

    var hasAccessibilityWarning: Bool {
        !patternsByStop.elevatorAlerts.isEmpty || !patternsByStop.stop.isWheelchairAccessible
    }

    @AccessibilityFocusState private var selectedDepartureFocus: String?
    private let cardFocusId = "_card"

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
                            patternsByStop: patternsByStop,
                            filter: stopFilter,
                            setFilter: { setStopFilter($0) }
                        )
                        .onChange(of: tripFilter) { filter in if let filter { view.scrollTo(filter.tripId) } }
                        .fixedSize(horizontal: false, vertical: true)
                        .padding([.horizontal, .top], 16)
                        .padding(.bottom, 6)
                        .dynamicTypeSize(...DynamicTypeSize.accessibility1)

                        if !hasMajorAlert, !tiles.isEmpty {
                            departureTiles(view)
                                .dynamicTypeSize(...DynamicTypeSize.accessibility3)
                                .onAppear { if let id = tripFilter?.tripId { view.scrollTo(id) } }
                        }
                    }
                    alertCards

                    if hasMajorAlert {
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
                            stopDetailsVM: stopDetailsVM
                        )
                    }
                }
            }
        }
        .onAppear { handleViewportForStatus(noPredictionsStatus) }
        .onChange(of: noPredictionsStatus) { status in handleViewportForStatus(status) }
        .onChange(of: selectedTripIsCancelled) { if $0 { setViewportToStop() } }
        .onChange(of: tripFilter) { tripFilter in
            selectedDepartureFocus = tiles.first { $0.upcoming?.trip.id == tripFilter?.tripId }?.id ?? cardFocusId
        }
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
                ForEach(tiles) { tileData in
                    DepartureTile(
                        data: tileData,
                        onTap: {
                            if let upcoming = tileData.upcoming {
                                nearbyVM.navigationStack.lastTripDetailsFilter = .init(
                                    tripId: upcoming.trip.id,
                                    vehicleId: upcoming.prediction?.vehicleId,
                                    stopSequence: upcoming.stopSequence,
                                    selectionLock: false
                                )
                                analytics.tappedDeparture(
                                    routeId: patternsByStop.routeIdentifier,
                                    stopId: patternsByStop.stop.id,
                                    pinned: pinned,
                                    alert: alerts.count > 0,
                                    routeType: patternsByStop.representativeRoute.type,
                                    noTrips: nil
                                )
                                view.scrollTo(tileData.id)
                            }
                        },
                        pillDecoration: patternsByStop
                            .line != nil ? .onPrediction(route: tileData.route) : .none,
                        showHeadsign: showTileHeadsigns,
                        isSelected: tileData.upcoming?.trip.id == tripFilter?.tripId
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

    func getAlertDetailsHandler(_ alertId: String, spec: AlertCardSpec) -> () -> Void {
        {
            nearbyVM.pushNavEntry(.alertDetails(
                alertId: alertId,
                line: spec == .elevator ? nil : patternsByStop.line,
                routes: spec == .elevator ? nil : patternsByStop.routes,
                stop: patternsByStop.stop
            ))
            analytics.tappedAlertDetails(
                routeId: patternsByStop.routeIdentifier,
                stopId: patternsByStop.stop.id,
                alertId: alertId,
                elevator: spec == .elevator
            )
        }
    }

    @ViewBuilder
    func alertCard(_ alert: Shared.Alert, _ spec: AlertCardSpec? = nil) -> some View {
        let spec: AlertCardSpec = if let spec {
            spec
        } else if alert.significance == .major {
            .major
        } else if alert.significance == .minor, alert.effect == .delay {
            .delay
        } else {
            .secondary
        }

        AlertCard(
            alert: alert,
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
            (stopDetailsVM.showElevatorAccessibility && hasAccessibilityWarning) {
            VStack(spacing: 16) {
                ForEach(alerts, id: \.id) { alert in
                    alertCard(alert)
                }
                ForEach(downstreamAlerts, id: \.id) { alert in
                    alertCard(alert, .downstream)
                }
                if stopDetailsVM.showElevatorAccessibility, hasAccessibilityWarning {
                    if !patternsByStop.elevatorAlerts.isEmpty {
                        ForEach(patternsByStop.elevatorAlerts, id: \.id) { alert in
                            alertCard(alert, .elevator)
                        }
                    } else {
                        NotAccessibleCard()
                    }
                }
            }.padding(.horizontal, 16)
        }
    }
}
