//
//  TripStopRow.swift
//  iosApp
//
//  Created by esimon on 12/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct TripStopRow: View {
    var stop: TripDetailsStopList.Entry
    var now: Instant
    var onTapLink: (SheetNavigationStackEntry, TripDetailsStopList.Entry, String?) -> Void
    var routeAccents: TripRouteAccents
    var targeted: Bool = false
    var firstStop: Bool = false
    var lastStop: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            stopRow.overlay {
                if targeted {
                    Rectangle().inset(by: -1).stroke(Color.halo, lineWidth: 2)
                }
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    @ViewBuilder
    var stopRow: some View {
        ZStack(alignment: .bottom) {
            if !lastStop, !targeted {
                HaloSeparator()
            }
            HStack(alignment: .center, spacing: 0) {
                routeLine
                VStack(alignment: .leading, spacing: 8) {
                    Button(
                        action: {
                            onTapLink(.legacyStopDetails(stop.stop, nil), stop, nil)
                        },
                        label: {
                            HStack {
                                Text(stop.stop.name)
                                    .font(Typography.body)
                                    .foregroundStyle(Color.text)
                                    .multilineTextAlignment(.leading)
                                    .accessibilityLabel(stopAccessibilityLabel)
                                Spacer()
                                UpcomingTripView(
                                    prediction: upcomingTripViewState,
                                    routeType: routeAccents.type,
                                    hideRealtimeIndicators: true
                                ).foregroundStyle(Color.text).opacity(0.6)
                            }
                        }
                    )
                    .accessibilityElement(children: .combine)
                    .accessibilityInputLabels([stop.stop.name])
                    .accessibilityAddTraits(targeted ? [.isHeader] : [])
                    .accessibilityHeading(targeted ? .h4 : .unspecified)

                    if !stop.routes.isEmpty {
                        scrollRoutes
                            .accessibilityElement()
                            .accessibilityLabel(scrollRoutesAccessibilityLabel)
                    }
                }
                .padding(.leading, 8)
                .padding(.vertical, 12)
                .padding(.trailing, 8)
                .padding(.bottom, lastStop ? 0 : 1)
                .frame(minHeight: 56)
            }
        }
    }

    @ViewBuilder
    var routeLine: some View {
        ZStack(alignment: .center) {
            VStack(spacing: 0) {
                if firstStop {
                    // Use a clear rectangle as a spacer, the Spacer() view doesn't
                    // take up enough space, this is always exactly half
                    ColoredRouteLine(Color.clear)
                }
                ColoredRouteLine(routeAccents.color)
                if lastStop {
                    // Use a clear rectangle as a spacer, the Spacer() view doesn't
                    // take up enough space, this is always exactly half
                    ColoredRouteLine(Color.clear)
                }
            }
            StopDot(routeAccents: routeAccents, targeted: targeted)
        }
        .padding(.leading, 37)
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
        }.onTapGesture {
            onTapLink(.legacyStopDetails(stop.stop, nil), stop, nil)
        }
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

    var upcomingTripViewState: UpcomingTripView.State {
        if let alert = stop.alert {
            .disruption(alert.effect)
        } else {
            .some(stop.format(now: now, routeType: routeAccents.type))
        }
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    return TripStopRow(
        stop: .init(
            stop: objects.stop { $0.name = "ABC" },
            stopSequence: 10,
            alert: nil,
            schedule: nil,
            prediction: nil,
            vehicle: nil,
            routes: [
                objects.route {
                    $0.longName = "Red Line"
                    $0.color = "#DA291C"
                    $0.textColor = "#ffffff"
                },
                objects.route {
                    $0.longName = "Green Line"
                    $0.color = "#00843D"
                    $0.textColor = "#ffffff"
                },
            ]
        ),
        now: Date.now.toKotlinInstant(),
        onTapLink: { _, _, _ in },
        routeAccents: TripRouteAccents(type: .lightRail)
    ).font(Typography.body)
}
