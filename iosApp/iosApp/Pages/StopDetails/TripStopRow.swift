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
    var now: KotlinInstant
    var onTapLink: (TripDetailsStopList.Entry) -> Void
    var onOpenAlertDetails: (Shared.Alert) -> Void
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
        guard let formatted = stop.format(trip: trip, now: now, routeType: routeAccents.type) else { return nil }
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
    let now = Date.now
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
                    objects.route { route in
                        route.longName = "Red Line"
                        route.color = "DA291C"
                        route.textColor = "FFFFFF"
                    },
                    objects.route { route in
                        route.longName = "Green Line"
                        route.color = "00843D"
                        route.textColor = "FFFFFF"
                    },
                ]
            ),
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(
                color: Color(hex: "DA291C"),
                type: .heavyRail
            ),
            alertSummaries: [:]
        )
        TripStopRow(
            stop: .init(
                stop: objects.stop { $0.name = "Park Street" },
                stopSequence: 10,
                disruption: nil,
                schedule: nil,
                prediction: objects.prediction { $0.departureTime = (now + 5 * 60).toKotlinInstant() },
                vehicle: nil,
                routes: [
                    objects.route { route in
                        route.longName = "Red Line"
                        route.color = "DA291C"
                        route.textColor = "FFFFFF"
                    },
                    objects.route { route in
                        route.longName = "Green Line"
                        route.color = "00843D"
                        route.textColor = "FFFFFF"
                    },
                ]
            ),
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(
                color: Color(hex: "DA291C"),
                type: .heavyRail
            ),
            alertSummaries: [:]
        )
        TripStopRow(
            stop: .init(
                stop: objects.stop { $0.name = "South Station" },
                stopSequence: 10,
                disruption: nil,
                schedule: nil,
                prediction: objects.prediction { $0.departureTime = (now + 5 * 60).toKotlinInstant() },
                predictionStop: objects.stop { $0.platformCode = "1" },
                vehicle: nil,
                routes: [],
                elevatorAlerts: [
                    objects.alert {
                        $0.activePeriod(
                            start: (now - 20 * 60).toKotlinInstant(),
                            end: (now + 20 * 60).toKotlinInstant()
                        )
                    },
                ]
            ),
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(
                color: Color(hex: "DA291C"),
                type: .heavyRail
            ),
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
    let now = Date.now
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
                        objects.route { route in
                            route.longName = "Red Line"
                            route.color = "DA291C"
                            route.textColor = "FFFFFF"
                        },
                        objects.route { route in
                            route.longName = "Green Line"
                            route.color = "00843D"
                            route.textColor = "FFFFFF"
                        },
                    ]
                ),
                trip: trip,
                now: now.toKotlinInstant(),
                onTapLink: { _ in },
                onOpenAlertDetails: { _ in },
                routeAccents: TripRouteAccents(
                    color: Color(hex: "DA291C"),
                    type: .heavyRail
                ),
                alertSummaries: [:]
            )
            TripStopRow(
                stop:
                .init(
                    stop: objects.stop { $0.name = "Park Street" },
                    stopSequence: 10,
                    disruption: nil,
                    schedule: nil,
                    prediction: objects.prediction { $0.departureTime = (now + 5 * 60).toKotlinInstant() },
                    vehicle: nil,
                    routes: [
                        objects.route { route in
                            route.longName = "Red Line"
                            route.color = "DA291C"
                            route.textColor = "FFFFFF"
                        },
                        objects.route { route in
                            route.longName = "Green Line"
                            route.color = "00843D"
                            route.textColor = "FFFFFF"
                        },
                    ]
                ),
                trip: trip,
                now: now.toKotlinInstant(),
                onTapLink: { _ in },
                onOpenAlertDetails: { _ in },
                routeAccents: TripRouteAccents(
                    color: Color(hex: "DA291C"),
                    type: .heavyRail
                ),
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
                    prediction: objects.prediction { $0.departureTime = (now + 5 * 60).toKotlinInstant() },
                    predictionStop: objects.stop { $0.platformCode = "1" },
                    vehicle: nil,
                    routes: [],
                    elevatorAlerts: []
                ),
                trip: trip,
                now: now.toKotlinInstant(),
                onTapLink: { _ in },
                onOpenAlertDetails: { _ in },
                routeAccents: TripRouteAccents(
                    color: Color(hex: "DA291C"),
                    type: .heavyRail
                ),
                alertSummaries: [:],
                showDownstreamAlert: true
            )
        }
    }
    .padding(6)
    .withFixedSettings([.stationAccessibility: true])
}
