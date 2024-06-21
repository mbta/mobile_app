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
        ForEach(Array(patternsByStop.patterns.enumerated()),
                id: \.element.id) { index, patterns in
            switch patterns as AnyObject {
            case let patternsByHeadsign as Patterns.ByHeadsign:
                VStack(spacing: 0) {
                    SheetNavigationLink(
                        value: .stopDetails(
                            patternsByStop.stop,
                            .init(
                                routeId: patternsByStop.routeIdentifier,
                                directionId: patternsByHeadsign.directionId()
                            )
                        ),
                        action: pushNavEntry
                    ) {
                        HeadsignRowView(
                            headsign: patternsByHeadsign.headsign,
                            predictions: patternsByHeadsign.format(now: now),
                            routeType: patternsByHeadsign.route.type
                        )
                    }
                    .padding(8)
                    .frame(minHeight: 44)
                    .padding(.leading, 8)

                    if index < patternsByStop.patterns.count - 1 {
                        Divider().background(Color.halo)
                    }
                }
            case let patternsByDirection as Patterns.ByDirection:
                VStack(spacing: 0) {
                    SheetNavigationLink(
                        value: .stopDetails(
                            patternsByStop.stop,
                            .init(
                                routeId: patternsByStop.routeIdentifier,
                                directionId: patternsByDirection.directionId()
                            )
                        ),
                        action: pushNavEntry
                    ) {
                        HeadsignRowView(
                            headsign: patternsByDirection.direction.destination,
                            predictions: patternsByDirection.format(now: now),
                            routeType: patternsByDirection.representativeRoute.type
                        )
                    }
                    .padding(8)
                    .frame(minHeight: 44)
                    .padding(.leading, 8)

                    if index < patternsByStop.patterns.count - 1 {
                        Divider().background(Color.halo)
                    }
                }
            default:
                EmptyView()
            }
        }.accessibilityElement(children: .contain)
            .accessibilityHint(Text("Open for more arrivals"))
    }
}
