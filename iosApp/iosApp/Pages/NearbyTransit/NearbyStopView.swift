//
//  NearbyStopView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct NearbyStopView: View {
    var analytics: Analytics = AnalyticsProvider.shared
    let patternsAtStop: PatternsByStop
    let condenseHeadsignPredictions: Bool
    let showStationAccessibility: Bool
    let now: Instant
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let pinned: Bool

    init(
        patternsAtStop: PatternsByStop,
        condenseHeadsignPredictions: Bool = false,
        showStationAccessibility: Bool = false,
        now: Instant,
        pushNavEntry: @escaping (SheetNavigationStackEntry) -> Void,
        pinned: Bool
    ) {
        self.patternsAtStop = patternsAtStop
        self.condenseHeadsignPredictions = condenseHeadsignPredictions
        self.showStationAccessibility = showStationAccessibility
        self.now = now
        self.pushNavEntry = pushNavEntry
        self.pinned = pinned
    }

    var elevatorAlerts: Int { patternsAtStop.elevatorAlerts.count }
    var isWheelchairAccessible: Bool { patternsAtStop.stop.isWheelchairAccessible }
    var showAccessible: Bool { showStationAccessibility && isWheelchairAccessible }
    var showInaccessible: Bool { showStationAccessibility && !isWheelchairAccessible }
    var showElevatorAlerts: Bool { showStationAccessibility && !patternsAtStop.elevatorAlerts.isEmpty }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(patternsAtStop.stop.name)
                .font(Typography.callout)
                .foregroundStyle(Color.text)

            HStack(spacing: 5) {
                if showElevatorAlerts {
                    Image(.accessibilityIconAlert)
                        .accessibilityHidden(true)
                } else if showInaccessible {
                    Image(.accessibilityIconNotAccessible)
                        .accessibilityHidden(true)
                        .tag("wheelchair_not_accessible")
                }
                if showInaccessible || showElevatorAlerts {
                    Group {
                        if showInaccessible {
                            Text(
                                "Not accessible",
                                comment: "Header displayed when station is not wheelchair accessible"
                            )
                        } else {
                            Text(
                                "\(elevatorAlerts, specifier: "%ld") elevators closed",
                                comment: "Header displayed when elevators are not working at a station"
                            )
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .multilineTextAlignment(.leading)
                    .font(Typography.footnoteSemibold)
                    .foregroundColor(Color.accessibility)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, showInaccessible || showElevatorAlerts ? 8 : 11)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.fill2)
        StopDeparturesSummaryList(
            patternsByStop: patternsAtStop,
            condenseHeadsignPredictions: condenseHeadsignPredictions,
            now: now,
            context: .nearbyTransit,
            pushNavEntry: pushNavEntry,
            analytics: analytics,
            pinned: pinned
        )
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    let route = objects.route { _ in }
    let stop = objects.stop { $0.name = "Long Stop Name like Malcolm X Blvd opp Madison Park HS" }
    let alert = objects.alert { _ in }
    return NearbyStopView(
        patternsAtStop: .init(routes: [route], line: nil, stop: stop, patterns: [], directions: [],
                              elevatorAlerts: [alert]),
        condenseHeadsignPredictions: false,
        showStationAccessibility: true,
        now: Date.now.toKotlinInstant(),
        pushNavEntry: { _ in },
        pinned: false
    )
}
