//
//  NearbyLineView.swift
//  iosApp
//
//  Created by Simon, Emma on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct NearbyLineView: View {
    let nearbyLine: StopsAssociated.WithLine
    let pinned: Bool
    let onPin: (String) -> Void
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let now: Instant

    var body: some View {
        LineCard(line: nearbyLine.line, routes: nearbyLine.routes, pinned: pinned, onPin: onPin) {
            ForEach(Array(nearbyLine.patternsByStop.enumerated()), id: \.element.stop.id) { index, patternsAtStop in
                VStack(spacing: 0) {
                    NearbyStopView(
                        patternsAtStop: patternsAtStop,
                        condenseHeadsignPredictions: nearbyLine.condensePredictions,
                        now: now,
                        pushNavEntry: pushNavEntry,
                        pinned: pinned
                    )
                    if index < nearbyLine.patternsByStop.count - 1 {
                        Divider().background(Color.halo)
                    }
                }
            }
        }
    }
}
