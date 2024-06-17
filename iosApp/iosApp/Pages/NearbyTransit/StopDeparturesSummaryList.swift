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
        ForEach(Array(patternsByStop.patternsByHeadsign.enumerated()),
                id: \.element.headsign) { index, patternsByHeadsign in

            VStack(spacing: 0) {
                SheetNavigationLink(value: .stopDetails(patternsByStop.stop,
                                                        .init(routeId: patternsByStop.route.id,
                                                              directionId: patternsByHeadsign.directionId())),
                                    action: pushNavEntry) {
                    HeadsignRowView(
                        headsign: patternsByHeadsign.headsign,
                        predictions: patternsByHeadsign.format(now: now),
                        routeType: patternsByHeadsign.route.type
                    )
                }
                .padding(8)
                .padding(.leading, 8)

                if index < patternsByStop.patternsByHeadsign.count - 1 {
                    Divider().background(Color.halo)
                }
            }
        }.accessibilityElement(children: .contain)
            .accessibilityHint(Text("Open for more arrivals"))
    }
}
