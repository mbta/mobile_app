//
//  TripStopRow.swift
//  iosApp
//
//  Created by esimon on 12/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct TripStopRow: View {
    var stop: TripDetailsStopList.Entry
    var trip: Trip
    var now: EasternTimeInstant
    var onTapLink: (TripDetailsStopList.Entry) -> Void
    var onOpenAlertDetails: (Shared.Alert) -> Void
    var route: Route
    var routeAccents: TripRouteAccents
    var alertSummaries: [String: AlertSummary?]
    var showDownstreamAlert: Bool = false
    var targeted: Bool = false
    var firstStop: Bool = false
    var lastStop: Bool = false
    var background: Color? = nil

    var activeElevatorAlerts: [Shared.Alert] {
        stop.activeElevatorAlerts(now: now)
    }

    var disruption: UpcomingFormat.Disruption? {
        if let disruption = stop.disruption, disruption.alert.hasNoThroughService, showDownstreamAlert {
            disruption
        } else {
            nil
        }
    }

    var stickConnections: [RouteBranchSegment.StickConnection] {
        RouteBranchSegment.StickConnection.companion.forward(
            stopBefore: firstStop ? nil : "",
            stop: stop.stop.id,
            stopAfter: lastStop ? nil : "",
            lane: .center
        )
        .compactMap { $0 }
    }

    var body: some View {
        StopListRow(
            stop: stop.stop,
            stopLane: .center,
            stickConnections: stickConnections,
            onClick: { onTapLink(stop) },
            routeAccents: routeAccents,
            stopListContext: .trip,
            activeElevatorAlerts: activeElevatorAlerts.count,
            alertSummaries: alertSummaries,
            background: background,
            connectingRoutes: stop.routes,
            disruption: disruption,
            getAlertState: { fromStop, _ in
                if fromStop == stop.stop.id, showDownstreamAlert, disruption?.alert.effect == .shuttle {
                    .shuttle
                } else {
                    .normal
                }
            },
            stopPlacement: .init(isFirst: firstStop, isLast: lastStop),
            onOpenAlertDetails: onOpenAlertDetails,
            targeted: targeted,
            trackNumber: stop.trackNumber,
            descriptor: { EmptyView() },
            rightSideContent: rightSideContent
        )
    }

    @ViewBuilder func rightSideContent() -> some View {
        if let upcomingTripViewState {
            UpcomingTripView(
                prediction: upcomingTripViewState,
                routeType: routeAccents.type,
                hideRealtimeIndicators: true,
                maxTextAlpha: 0.6
            ).foregroundStyle(Color.text)
        }
    }

    var upcomingTripViewState: UpcomingTripView.State? {
        guard let formatted = stop.format(trip: trip, now: now, route: route) else { return nil }
        switch onEnum(of: formatted) {
        case let .disruption(disruption): return .disruption(
                .init(alert: disruption.alert),
                iconName: disruption.iconName
            )
        case let .some(some): return .some(some.trips[0].format)
        default: return nil
        }
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    let trip = objects.trip { _ in }
    let now = EasternTimeInstant.now()
    let red = objects.route { route in
        route.color = "DA291C"
        route.longName = "Red Line"
        route.textColor = "FFFFFF"
        route.type = .heavyRail
    }
    VStack(spacing: 0) {
        TripStopRow(
            stop: .init(
                stop: objects.stop { stop in
                    stop.name = "Charles/MGH"
                    stop.wheelchairBoarding = .accessible
                },
                stopSequence: 10,
                disruption: nil,
                schedule: nil,
                prediction: objects.prediction { $0.status = "Stopped 5 stops away" },
                vehicle: nil,
                routes: [
                    red,
                    objects.route { route in
                        route.longName = "Green Line"
                        route.color = "00843D"
                        route.textColor = "FFFFFF"
                    },
                ]
            ),
            trip: trip,
            now: now,
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            route: red,
            routeAccents: .init(route: red),
            alertSummaries: [:]
        )
        TripStopRow(
            stop: .init(
                stop: objects.stop { $0.name = "Park Street" },
                stopSequence: 10,
                disruption: nil,
                schedule: nil,
                prediction: objects.prediction { $0.departureTime = now.plus(minutes: 5) },
                vehicle: nil,
                routes: [
                    red,
                    objects.route { route in
                        route.longName = "Green Line"
                        route.color = "00843D"
                        route.textColor = "FFFFFF"
                    },
                ]
            ),
            trip: trip,
            now: now,
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            route: red,
            routeAccents: .init(route: red),
            alertSummaries: [:]
        )
        TripStopRow(
            stop: .init(
                stop: objects.stop { $0.name = "South Station" },
                stopSequence: 10,
                disruption: nil,
                schedule: nil,
                prediction: objects.prediction { $0.departureTime = now.plus(minutes: 5) },
                predictionStop: objects.stop { $0.platformCode = "1" },
                vehicle: nil,
                routes: [],
                elevatorAlerts: [
                    objects.alert {
                        $0.activePeriod(
                            start: now.minus(minutes: 20),
                            end: now.plus(minutes: 20)
                        )
                    },
                ]
            ),
            trip: trip,
            now: now,
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            route: red,
            routeAccents: .init(route: red),
            alertSummaries: [:]
        )
    }
    .withFixedSettings([.stationAccessibility: true])
    .font(Typography.body)
    .background(Color.fill3)
}

#Preview("Disruptions") {
    let objects = ObjectCollectionBuilder()
    let trip = objects.trip { _ in }
    let now = EasternTimeInstant.now()
    let red = objects.route { route in
        route.color = "DA291C"
        route.longName = "Red Line"
        route.textColor = "FFFFFF"
        route.type = .heavyRail
    }
    ZStack {
        Color.fill3.padding(6)
        VStack(spacing: 0) {
            TripStopRow(
                stop:
                .init(
                    stop: objects.stop { $0.name = "Charles/MGH" },
                    stopSequence: 10,
                    disruption: .init(
                        alert: objects.alert { $0.effect = .stopClosure },
                        mapStopRoute: .red
                    ),
                    schedule: nil,
                    prediction: objects.prediction { $0.status = "Stopped 5 stops away" },
                    vehicle: nil,
                    routes: [
                        red,
                        objects.route { route in
                            route.longName = "Green Line"
                            route.color = "00843D"
                            route.textColor = "FFFFFF"
                        },
                    ]
                ),
                trip: trip,
                now: now,
                onTapLink: { _ in },
                onOpenAlertDetails: { _ in },
                route: red,
                routeAccents: .init(route: red),
                alertSummaries: [:]
            )
            TripStopRow(
                stop:
                .init(
                    stop: objects.stop { $0.name = "Park Street" },
                    stopSequence: 10,
                    disruption: nil,
                    schedule: nil,
                    prediction: objects.prediction { $0.departureTime = now.plus(minutes: 5) },
                    vehicle: nil,
                    routes: [
                        red,
                        objects.route { route in
                            route.longName = "Green Line"
                            route.color = "00843D"
                            route.textColor = "FFFFFF"
                        },
                    ]
                ),
                trip: trip,
                now: now,
                onTapLink: { _ in },
                onOpenAlertDetails: { _ in },
                route: red,
                routeAccents: .init(route: red),
                alertSummaries: [:]
            )
            TripStopRow(
                stop:
                .init(
                    stop: objects.stop { $0.name = "South Station" },
                    stopSequence: 10,
                    disruption: .init(alert: objects.alert { $0.effect = .shuttle },
                                      mapStopRoute: .red),
                    schedule: nil,
                    prediction: objects.prediction { $0.departureTime = now.plus(minutes: 5) },
                    predictionStop: objects.stop { $0.platformCode = "1" },
                    vehicle: nil,
                    routes: [],
                    elevatorAlerts: []
                ),
                trip: trip,
                now: now,
                onTapLink: { _ in },
                onOpenAlertDetails: { _ in },
                route: red,
                routeAccents: .init(route: red),
                alertSummaries: [:],
                showDownstreamAlert: true
            )
        }
    }
    .padding(6)
    .withFixedSettings([.stationAccessibility: true])
}
