//
//  StopDeparturesSummaryList.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct StopDeparturesSummaryList: View {
    let patternsByStop: PatternsByStop
    let condenseHeadsignPredictions: Bool
    let now: Instant
    let context: TripInstantDisplay.Context
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let analytics: Analytics
    let pinned: Bool

    var body: some View {
        ForEach(
            Array(patternsByStop.patterns.enumerated()),
            id: \.element.id
        ) { index, patterns in

            let inputLabel = switch onEnum(of: patterns) {
            case let .byHeadsign(byHeadsign): byHeadsign.headsign
            case let .byDirection(byDirection): byDirection.direction.destination ?? byDirection.direction.name
            }

            VStack(spacing: 0) {
                DestinationRowView(
                    patterns: patterns,
                    stop: patternsByStop.stop, routeId: patternsByStop.routeIdentifier,
                    now: now, context: context,
                    condenseHeadsignPredictions: condenseHeadsignPredictions,
                    pushNavEntry: pushNavEntry,
                    analytics: analytics, pinned: pinned, routeType: patternsByStop.representativeRoute.type
                )
                .accessibilityInputLabels([inputLabel])
                .padding(8)
                .frame(minHeight: 44)
                .padding(.leading, 8)

                if index < patternsByStop.patterns.count - 1 {
                    Divider().background(Color.halo)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityHint(Text("Open for more arrivals"))
    }
}
