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

    init(patterns: RealtimePatterns, now: Instant, singleHeadsignPredictions: Bool = false) {
        self.patterns = patterns
        self.now = now
        condenseHeadsignPredictions = singleHeadsignPredictions
    }

    var body: some View {
        switch onEnum(of: patterns) {
        case let .byHeadsign(patternsByHeadsign):
            HeadsignRowView(
                headsign: patternsByHeadsign.headsign,
                predictions: patternsByHeadsign.format(now: now, count: 2),
                routeType: patternsByHeadsign.route.type
            )
        default:
            EmptyView()
        }
    }
}
