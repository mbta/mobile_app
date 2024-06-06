//
//  NearbyStopView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct NearbyStopView: View {
    let patternsAtStop: PatternsByStop
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let now: Instant

    @ScaledMetric private var chevronHeight: CGFloat = 14
    @ScaledMetric private var chevronWidth: CGFloat = 8

    var body: some View {
        Text(patternsAtStop.stop.name)
            .font(.callout)
            .foregroundStyle(Color.text)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.fill2)

        ForEach(Array(patternsAtStop.patternsByHeadsign.enumerated()), id: \.offset) { index, patternsByHeadsign in
            Button(action: { pushNavEntry(.stopDetails(patternsAtStop.stop,
                                                       .init(routeId: patternsAtStop.route.id,
                                                             directionId: patternsByHeadsign.directionId()))) }) {
                VStack(spacing: 0) {
                    HStack(spacing: 0) {
                        HeadsignRowView(
                            headsign: patternsByHeadsign.headsign,
                            predictions: patternsByHeadsign.format(now: now),
                            routeType: patternsByHeadsign.route.type
                        )
                        Image(.faChevronRight)
                            .resizable()
                            .scaledToFit()
                            .frame(width: chevronWidth, height: chevronHeight)
                            .padding(5)
                            .foregroundStyle(Color.deemphasized)
                    }
                    .padding(8)
                    .padding(.leading, 8)
                    if index < patternsAtStop.patternsByHeadsign.count - 1 {
                        Divider().background(Color.halo)
                    }
                }.accessibilityElement(children: .contain)
            }.accessibilityHint(Text("Open for more arrivals"))
        }
    }
}
