//
//  TripDetailsStopView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-17.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct TripDetailsStopView: View {
    let stop: TripDetailsStopList.Entry
    let now: Instant
    let onTapLink: (SheetNavigationStackEntry, TripDetailsStopList.Entry, String?) -> Void
    let routeType: RouteType?

    var body: some View {
        VStack(alignment: .leading) {
            SheetNavigationLink(
                value: .legacyStopDetails(stop.stop, nil),
                action: { entry in onTapLink(entry, stop, nil) },
                label: {
                    HStack {
                        Text(stop.stop.name).foregroundStyle(Color.text)
                        Spacer()
                        UpcomingTripView(prediction: upcomingTripViewState, routeType: routeType)
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityHeading(.h2)
                }
            ).accessibilityInputLabels([stop.stop.name])
            if !stop.routes.isEmpty {
                scrollRoutes
                    .accessibilityElement()
                    .accessibilityLabel(scrollRoutesAccessibilityLabel)
            }
        }
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

    var upcomingTripViewState: UpcomingTripView.State {
        if let alert = stop.alert {
            .disruption(alert.effect)
        } else {
            .some(stop.format(now: now, routeType: routeType))
        }
    }

    var scrollRoutes: some View {
        let routeView = ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                ForEach(stop.routes, id: \.id) { route in
                    RoutePill(route: route, line: nil, type: .flex)
                }
            }.padding(.horizontal, 20)
        }.padding(.horizontal, -20).onTapGesture {
            onTapLink(.legacyStopDetails(stop.stop, nil), stop, nil)
        }
        return routeView.scrollBounceBehavior(.basedOnSize, axes: [.horizontal])
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    return TripDetailsStopView(
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
        routeType: .lightRail
    ).font(Typography.body)
}
