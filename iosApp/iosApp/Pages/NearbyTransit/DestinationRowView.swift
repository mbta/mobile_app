//
//  DestinationRowView.swift
//  iosApp
//
//  Created by Simon, Emma on 6/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct DestinationRowView: View {
    let patterns: RealtimePatterns
    let condenseHeadsignPredictions: Bool
    let now: Instant
    let context: TripInstantDisplay.Context

    init(
        patterns: RealtimePatterns,
        now: Instant,
        context: TripInstantDisplay.Context,
        condenseHeadsignPredictions: Bool = false
    ) {
        self.patterns = patterns
        self.now = now
        self.context = context
        self.condenseHeadsignPredictions = condenseHeadsignPredictions
    }

    var body: some View {
        switch onEnum(of: patterns) {
        case let .byHeadsign(patternsByHeadsign):
            HeadsignRowView(
                headsign: patternsByHeadsign.headsign,
                predictions: patternsByHeadsign.format(
                    now: now,
                    routeType: patternsByHeadsign.route.type,
                    count: condenseHeadsignPredictions ? 1 : 2,
                    context: context
                ),
                pillDecoration: patternsByHeadsign.line != nil ?
                    .onRow(route: patternsByHeadsign.route) : .none
            )
        case let .byDirection(patternsByDirection):
            DirectionRowView(
                direction: patternsByDirection.direction,
                predictions: patternsByDirection.format(
                    now: now,
                    routeType: patternsByDirection.representativeRoute.type,
                    context: context
                ),
                pillDecoration: .onPrediction(routesByTrip: patternsByDirection.routesByTrip)
            )
        }
    }
}
