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
    var showStationAccessibility: Bool = false
    var showDownstreamAlert: Bool = false
    var targeted: Bool = false
    var firstStop: Bool = false
    var lastStop: Bool = false
    var background: Color? = nil

    var disruption: UpcomingFormat.Disruption? {
        if let disruption = stop.disruption, disruption.alert.hasNoThroughService, showDownstreamAlert {
            disruption
        } else {
            nil
        }
    }

    var stateBefore: ColoredRouteLine.State {
        if firstStop {
            .empty
        } else {
            .regular
        }
    }

    var stateAfter: ColoredRouteLine.State {
        if lastStop {
            .empty
        } else if disruption?.alert.effect == .shuttle {
            .shuttle
        } else {
            .regular
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            stopRow
                .background(background?.padding(.horizontal, 2))
                .overlay {
                    if targeted {
                        VStack {
                            HaloSeparator(height: 2)
                            Spacer()
                            HaloSeparator(height: 2)
                        }
                    }
                }
            if let disruption {
                ZStack(alignment: .leading) {
                    VStack(spacing: 0) {
                        ColoredRouteLine(routeAccents.color, state: stateAfter)
                        if stop.isTruncating {
                            ColoredRouteLine(routeAccents.color, state: .empty)
                        }
                    }
                    .padding(.leading, 42)
                    AlertCard(
                        alert: disruption.alert,
                        alertSummary: alertSummaries[disruption.alert.id] ?? nil,
                        spec: .downstream,
                        color: routeAccents.color,
                        textColor: routeAccents.textColor,
                        onViewDetails: { onOpenAlertDetails(disruption.alert) },
                        internalPadding: .init(top: 0, leading: 21, bottom: 0, trailing: 0)
                    )
                    .padding(.horizontal, -4)
                }
            }
        }
        .fixedSize(horizontal: false, vertical: true).padding(.horizontal, 6)
    }

    @ViewBuilder
    var stopRow: some View {
        let activeElevatorAlerts = stop.activeElevatorAlerts(now: now)

        ZStack(alignment: .bottom) {
            if !lastStop, !targeted, disruption == nil {
                HaloSeparator()
            }
            HStack(alignment: .center, spacing: 0) {
                HStack(alignment: .center) {
                    if showStationAccessibility, !activeElevatorAlerts.isEmpty {
                        Image(.accessibilityIconAlert)
                            .accessibilityHidden(true)
                            .tag("elevator_alert")
                    } else if showStationAccessibility, !stop.stop.isWheelchairAccessible {
                        Image(.accessibilityIconNotAccessible)
                            .accessibilityHidden(true)
                            .tag("wheelchair_not_accessible")
                    } else {
                        EmptyView().accessibilityHidden(true)
                    }
                }
                .frame(minWidth: 28, maxWidth: 28)
                .padding(.leading, 6)
                routeLine
                VStack(alignment: .leading, spacing: 8) {
                    Button(
                        action: { onTapLink(stop) },
                        label: {
                            HStack(alignment: .center, spacing: 0) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(stop.stop.name)
                                        .font(targeted ? Typography.headlineBold : Typography.body)
                                        .foregroundStyle(Color.text)
                                        .multilineTextAlignment(.leading)
                                        .accessibilityLabel(stopAccessibilityLabel)
                                    if let trackNumber = stop.trackNumber {
                                        Text("Track \(trackNumber)")
                                            .font(Typography.footnote)
                                            .foregroundStyle(Color.text)
                                            .multilineTextAlignment(.leading)
                                            .accessibilityLabel(Text("Boarding on track \(trackNumber)"))
                                    }
                                }
                                Spacer()
                                if let upcomingTripViewState {
                                    UpcomingTripView(
                                        prediction: upcomingTripViewState,
                                        routeType: routeAccents.type,
                                        hideRealtimeIndicators: true,
                                        maxTextAlpha: 0.6
                                    ).foregroundStyle(Color.text)
                                }

                                // Adding the accessibility description into the stop label rather than on the
                                // accessibility icon so that it is clear which stop it is associated with
                                if showStationAccessibility, !activeElevatorAlerts.isEmpty {
                                    HStack {}
                                        .accessibilityLabel(Text(
                                            "This stop has \(activeElevatorAlerts.count, specifier: "%ld") elevators closed",
                                            comment: "Describe an elevator outage at the stop in the list of all stops on the trip"
                                        ))
                                } else if showStationAccessibility, !stop.stop.isWheelchairAccessible {
                                    HStack {}
                                        .accessibilityLabel(Text("This stop is not accessible"))
                                }
                            }
                        }
                    )
                    .accessibilityElement(children: .combine)
                    .accessibilityInputLabels([stop.stop.name])
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityHeading(.h4)

                    if !stop.routes.isEmpty {
                        scrollRoutes
                            .accessibilityElement()
                            .accessibilityLabel(scrollRoutesAccessibilityLabel)
                    }
                }
                .accessibilitySortPriority(1)
                .padding(.leading, 8)
                .padding(.vertical, 12)
                .padding(.trailing, 8)
                .padding(.bottom, lastStop ? 0 : 1)
                .frame(minHeight: 56)
            }.accessibilityElement(children: .contain)
        }
    }

    @ViewBuilder
    var routeLine: some View {
        ZStack(alignment: .center) {
            VStack(spacing: 0) {
                ColoredRouteLine(routeAccents.color, state: stateBefore)
                ColoredRouteLine(routeAccents.color, state: stateAfter)
            }
            StopDot(routeAccents: routeAccents, targeted: targeted)
        }
        .padding(.leading, 3)
        .padding(.trailing, 8)
    }

    func connectionLabel(route: Route) -> String {
        String(format: NSLocalizedString(
            "%@ %@",
            comment: """
            A route label and route type pair,
            ex 'Red Line train' or '73 bus', used in connecting stop labels
            """
        ), route.label, route.type.typeText(isOnly: true))
    }

    var scrollRoutes: some View {
        let routeView = ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                ForEach(stop.routes, id: \.id) { route in
                    RoutePill(route: route, line: nil, type: .flex)
                }
            }.padding(.trailing, 20)
        }.onTapGesture { onTapLink(stop) }
        return routeView.scrollBounceBehavior(.basedOnSize, axes: [.horizontal])
    }

    var scrollRoutesAccessibilityLabel: String {
        if stop.routes.isEmpty {
            return ""
        } else if stop.routes.count == 1 {
            return String(format: NSLocalizedString(
                "Connection to %@",
                comment: "VoiceOver label for a single connecting route at a stop, ex 'Connection to 1 bus'"
            ), connectionLabel(route: stop.routes.first!))
        } else {
            let firstConnections = stop.routes.prefix(stop.routes.count - 1)
            let lastConnection = stop.routes.last!
            return String(
                format: NSLocalizedString(
                    "Connections to %@ and %@",
                    comment: """
                    VoiceOver label for multiple connecting routes at a stop,
                    ex 'Connections to Red Line train, 71 bus, and 73 bus',
                    the first replaced value can be any number of comma separated route labels
                    """
                ),
                firstConnections.map(connectionLabel).joined(separator: ", "),
                connectionLabel(route: lastConnection)
            )
        }
    }

    var stopAccessibilityLabel: String {
        let name = stop.stop.name
        return if targeted, firstStop {
            String(format: NSLocalizedString(
                "%@, selected stop, first stop",
                comment: "Screen reader text for a stop name on the stop details page when that stop is both selected and first"
            ), name)
        } else if targeted {
            String(format: NSLocalizedString(
                "%@, selected stop",
                comment: "Screen reader text for a stop name on the stop details page when that stop is the selected one"
            ), name)
        } else if firstStop {
            String(format: NSLocalizedString(
                "%@, first stop",
                comment: "Screen reader text for a stop name on the stop details page when that stop is the first stop on the line"
            ), name)
        } else {
            name
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
    VStack {
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
            alertSummaries: [:],
            showStationAccessibility: true
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
            alertSummaries: [:],
            showStationAccessibility: true
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
            alertSummaries: [:],
            showStationAccessibility: true
        )
    }
    .font(Typography.body)
    .background(Color.fill3)
}

#Preview("Disruptions") {
    let objects = ObjectCollectionBuilder()
    let trip = objects.trip { _ in }
    let now = Date.now
    ZStack {
        Color.fill3.padding(6)
        VStack {
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
                alertSummaries: [:],
                showDownstreamAlert: true
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
                alertSummaries: [:],
                showDownstreamAlert: true
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
    }.padding(6)
}
