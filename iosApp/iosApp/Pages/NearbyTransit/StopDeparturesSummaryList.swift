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
    let pushNavEntry: (SheetNavigationStackEntry, Bool) -> Void

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
                SheetNavigationLink(
                    value: .legacyStopDetails(
                        patternsByStop.stop,
                        filterFor(patterns: patterns)
                    ),
                    action: { entry in pushNavEntry(entry, (patterns.alertsHere?.count ?? 0) > 0) }
                ) {
                    DestinationRowView(
                        patterns: patterns,
                        now: now, context: context,
                        condenseHeadsignPredictions: condenseHeadsignPredictions
                    )
                }
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

    func filterFor(patterns: RealtimePatterns) -> StopDetailsFilter {
        switch onEnum(of: patterns) {
        case let .byHeadsign(patternsByHeadsign):
            .init(
                routeId: patternsByStop.routeIdentifier,
                directionId: patternsByHeadsign.directionId()
            )
        case let .byDirection(patternsByDirection):
            .init(
                routeId: patternsByStop.routeIdentifier,
                directionId: patternsByDirection.directionId()
            )
        }
    }
}
