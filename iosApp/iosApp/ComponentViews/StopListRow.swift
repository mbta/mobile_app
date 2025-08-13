//
//  StopListRow.swift
//  iosApp
//
//  Created by Melody Horn on 7/8/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopPlacement {
    let isFirst: Bool
    let isLast: Bool

    init(isFirst: Bool = false, isLast: Bool = false) {
        self.isFirst = isFirst
        self.isLast = isLast
    }
}

enum StopListContext {
    case trip
    case routeDetails
}

struct StopListRow<Descriptor: View, RightSideContent: View>: View {
    var stop: Stop
    var stopLane: RouteBranchSegment.Lane
    var stickConnections: [RouteBranchSegment.StickConnection]
    var onClick: () -> Void
    var routeAccents: TripRouteAccents
    var stopListContext: StopListContext
    var activeElevatorAlerts: Int
    var alertSummaries: [String: AlertSummary?]
    var background: Color?
    var connectingRoutes: [Route]?
    var disruption: UpcomingFormat.Disruption?
    var getAlertState: (_ fromStop: String, _ toStop: String) -> SegmentAlertState
    var stopPlacement: StopPlacement
    var onOpenAlertDetails: (Shared.Alert) -> Void
    var targeted: Bool
    var trackNumber: String?
    var descriptor: () -> Descriptor
    var rightSideContent: () -> RightSideContent

    @EnvironmentObject var settingsCache: SettingsCache
    var showStationAccessibility: Bool { settingsCache.get(.stationAccessibility) }

    init(
        stop: Stop,
        stopLane: RouteBranchSegment.Lane,
        stickConnections: [RouteBranchSegment.StickConnection],
        onClick: @escaping () -> Void,
        routeAccents: TripRouteAccents,
        stopListContext: StopListContext,
        activeElevatorAlerts: Int = 0,
        alertSummaries: [String: AlertSummary?] = [:],
        background: Color? = nil,
        connectingRoutes: [Route]? = nil,
        disruption: UpcomingFormat.Disruption? = nil,
        getAlertState: @escaping (_ fromStop: String, _ toStop: String) -> SegmentAlertState = { _, _ in .normal },
        stopPlacement: StopPlacement = .init(),
        onOpenAlertDetails: @escaping (Shared.Alert) -> Void = { _ in },
        targeted: Bool = false,
        trackNumber: String? = nil,
        descriptor: @escaping () -> Descriptor,
        rightSideContent: @escaping () -> RightSideContent
    ) {
        self.stop = stop
        self.stopLane = stopLane
        self.stickConnections = stickConnections
        self.onClick = onClick
        self.routeAccents = routeAccents
        self.stopListContext = stopListContext
        self.activeElevatorAlerts = activeElevatorAlerts
        self.alertSummaries = alertSummaries
        self.background = background
        self.connectingRoutes = connectingRoutes
        self.disruption = disruption
        self.getAlertState = getAlertState
        self.stopPlacement = stopPlacement
        self.onOpenAlertDetails = onOpenAlertDetails
        self.targeted = targeted
        self.trackNumber = trackNumber
        self.descriptor = descriptor
        self.rightSideContent = rightSideContent
    }

    var body: some View {
        VStack(spacing: 0) {
            stopRow
                .background(background)
                .overlay {
                    if targeted {
                        VStack {
                            HaloSeparator(height: 1)
                            Spacer()
                            HaloSeparator(height: 2)
                        }
                    }
                }
            if let disruption {
                ZStack(alignment: .leading) {
                    AlertCard(
                        alert: disruption.alert,
                        alertSummary: alertSummaries[disruption.alert.id] ?? nil,
                        spec: .downstream,
                        color: routeAccents.color,
                        textColor: routeAccents.textColor,
                        onViewDetails: { onOpenAlertDetails(disruption.alert) },
                        internalPadding: .init(top: 0, leading: 5, bottom: 0, trailing: 0)
                    )
                    .padding(.horizontal, -4)
                }
            }
        }
        .fixedSize(horizontal: false, vertical: true).padding(.horizontal, stopListContext == .trip ? 7 : 0)
    }

    @ViewBuilder
    var stopRow: some View {
        ZStack(alignment: .bottom) {
            if !stopPlacement.isLast, !targeted, disruption == nil {
                HaloSeparator()
            }
            HStack(alignment: .center, spacing: 0) {
                routeLine
                VStack(alignment: .leading, spacing: 8) {
                    Button(
                        action: { onClick() },
                        label: {
                            HStack(alignment: .center, spacing: 0) {
                                if showStationAccessibility, activeElevatorAlerts > 0 || !stop.isWheelchairAccessible {
                                    HStack(alignment: .center) {
                                        if showStationAccessibility, activeElevatorAlerts > 0 {
                                            Image(.accessibilityIconAlert).tag("elevator_alert")
                                        } else {
                                            Image(.accessibilityIconNotAccessible).tag("wheelchair_not_accessible")
                                        }
                                    }
                                    .accessibilityHidden(true)
                                    .frame(minWidth: 28, maxWidth: 28)
                                    .padding(.horizontal, 6)
                                }
                                VStack(alignment: .leading, spacing: 4) {
                                    descriptor()
                                    Text(stop.name)
                                        .font(targeted ? Typography.headlineBold : Typography.body)
                                        .foregroundStyle(Color.text)
                                        .multilineTextAlignment(.leading)
                                        .accessibilityLabel(stopAccessibilityLabel)
                                    if let trackNumber {
                                        Text("Track \(trackNumber)")
                                            .font(Typography.footnote)
                                            .foregroundStyle(Color.text)
                                            .multilineTextAlignment(.leading)
                                            .accessibilityLabel(Text("Boarding on track \(trackNumber)"))
                                    }
                                }
                                Spacer()
                                rightSideContent()

                                // Adding the accessibility description into the stop label rather than on the
                                // accessibility icon so that it is clear which stop it is associated with
                                if showStationAccessibility, activeElevatorAlerts > 0 {
                                    HStack {}
                                        .accessibilityLabel(Text(
                                            "This stop has \(activeElevatorAlerts, specifier: "%ld") elevators closed",
                                            comment: "Describe an elevator outage at the stop in the list of all stops on the trip"
                                        ))
                                } else if showStationAccessibility, !stop.isWheelchairAccessible {
                                    HStack {}
                                        .accessibilityLabel(Text("This stop is not accessible"))
                                }
                            }
                        }
                    )
                    .preventScrollTaps()
                    .accessibilityElement(children: .combine)
                    .accessibilityInputLabels([stop.name])
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityHeading(.h4)

                    if let connectingRoutes, !connectingRoutes.isEmpty {
                        scrollRoutes
                            .accessibilityElement()
                            .accessibilityLabel(scrollRoutesAccessibilityLabel)
                    }
                }
                .accessibilitySortPriority(1)
                .padding(.leading, 8)
                .padding(.vertical, 12)
                .padding(.trailing, 8)
                .padding(.bottom, stopPlacement.isLast ? 0 : 1)
                .frame(minHeight: 56)
            }.accessibilityElement(children: .contain)
        }
    }

    @ViewBuilder
    var routeLine: some View {
        ZStack(alignment: .center) {
            VStack(spacing: 0) {
                StickDiagram(routeAccents.color, stickConnections, getAlertState: getAlertState)
            }
            let dot = StopDot(routeAccents: routeAccents, targeted: targeted)
            switch stopLane {
            case .left: dot.padding(.trailing, 20)
            case .center: dot.padding(.horizontal, 10)
            case .right: dot.padding(.leading, 20)
            }
        }
        .padding(.horizontal, 7)
    }

    func connectionLabel(route: Route) -> String { routeModeLabel(route: route) }

    var scrollRoutes: some View {
        let routeView = ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                if let connectingRoutes {
                    ForEach(connectingRoutes, id: \.id) { route in
                        RoutePill(route: route, line: nil, type: .flex)
                    }
                }
            }.padding(.trailing, 20)
        }.onTapGesture { onClick() }
        return routeView.scrollBounceBehavior(.basedOnSize, axes: [.horizontal])
    }

    var scrollRoutesAccessibilityLabel: String {
        guard let connectingRoutes, !connectingRoutes.isEmpty else { return "" }
        if connectingRoutes.count == 1 {
            return String(format: NSLocalizedString(
                "Connection to %@",
                comment: "VoiceOver label for a single connecting route at a stop, ex 'Connection to 1 bus'"
            ), connectionLabel(route: connectingRoutes.first!))
        } else {
            let firstConnections = connectingRoutes.prefix(connectingRoutes.count - 1)
            let lastConnection = connectingRoutes.last!
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
        let name = stop.name
        return if targeted, stopPlacement.isFirst {
            String(format: NSLocalizedString(
                "%@, selected stop, first stop",
                comment: "Screen reader text for a stop name on the stop details page when that stop is both selected and first"
            ), name)
        } else if targeted {
            String(format: NSLocalizedString(
                "%@, selected stop",
                comment: "Screen reader text for a stop name on the stop details page when that stop is the selected one"
            ), name)
        } else if stopPlacement.isFirst {
            String(format: NSLocalizedString(
                "%@, first stop",
                comment: "Screen reader text for a stop name on the stop details page when that stop is the first stop on the line"
            ), name)
        } else {
            name
        }
    }
}
