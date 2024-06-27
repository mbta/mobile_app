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
    let now: Instant
    let pushNavEntry: (SheetNavigationStackEntry) -> Void

    var body: some View {
        ForEach(
            Array(patternsByStop.patterns.enumerated()),
            id: \.element.id
        ) { index, patterns in
            VStack(spacing: 0) {
                SheetNavigationLink(
                    value: .stopDetails(
                        patternsByStop.stop,
                        filterFor(patterns: patterns)
                    ),
                    action: pushNavEntry
                ) {
                    DestinationRowView(
                        patterns: patterns,
                        now: now
                    )
                }
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
        default:
            .init(routeId: "", directionId: 0)
        }
    }
}
