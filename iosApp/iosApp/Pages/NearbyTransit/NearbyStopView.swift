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
    let showElevatorAccessibility: Bool
    let now: Instant
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let pinned: Bool

    init(
        patternsAtStop: PatternsByStop,
        condenseHeadsignPredictions: Bool = false,
        showElevatorAccessibility: Bool = false,
        now: Instant,
        pushNavEntry: @escaping (SheetNavigationStackEntry) -> Void,
        pinned: Bool
    ) {
        self.patternsAtStop = patternsAtStop
        self.condenseHeadsignPredictions = condenseHeadsignPredictions
        self.showElevatorAccessibility = showElevatorAccessibility
        self.now = now
        self.pushNavEntry = pushNavEntry
        self.pinned = pinned
    }

    var elevatorAlerts: Int { patternsAtStop.elevatorAlerts.count }
    var isWheelchairAccessible: Bool { patternsAtStop.stop.isWheelchairAccessible }
    var showAccessible: Bool { showElevatorAccessibility && isWheelchairAccessible }
    var showInaccessible: Bool { showElevatorAccessibility && !isWheelchairAccessible }
    var showElevatorAlerts: Bool { showElevatorAccessibility && !patternsAtStop.elevatorAlerts.isEmpty }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                if showElevatorAlerts {
                    Image(.accessibilityIconAlert)
                        .accessibilityHidden(true)
                } else if showAccessible {
                    Image(.accessibilityIconAccessible)
                        .accessibilityHidden(true)
                        .tag("wheelchair_accessible")
                }
                Text(patternsAtStop.stop.name)
                    .font(Typography.callout)
                    .foregroundStyle(Color.text)
            }
            if showInaccessible || showElevatorAlerts {
                Group {
                    if showInaccessible {
                        Text("Not accessible", comment: "Header displayed when station is not wheelchair accessible")
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
                .foregroundColor(Color.text.opacity(0.5))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
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
        showElevatorAccessibility: true,
        now: Date.now.toKotlinInstant(),
        pushNavEntry: { _ in },
        pinned: false
    )
}
