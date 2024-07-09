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
    var analytics: NearbyTransitAnalytics = AnalyticsProvider()
    let patternsAtStop: PatternsByStop
    let condenseHeadsignPredictions: Bool
    let now: Instant
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let pinned: Bool

    init(
        patternsAtStop: PatternsByStop,
        condenseHeadsignPredictions: Bool = false,
        now: Instant,
        pushNavEntry: @escaping (SheetNavigationStackEntry) -> Void,
        pinned: Bool
    ) {
        self.patternsAtStop = patternsAtStop
        self.condenseHeadsignPredictions = condenseHeadsignPredictions
        self.now = now
        self.pushNavEntry = pushNavEntry
        self.pinned = pinned
    }

    var body: some View {
        Text(patternsAtStop.stop.name)
            .font(Typography.callout)
            .foregroundStyle(Color.text)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.fill2)
        StopDeparturesSummaryList(
            patternsByStop: patternsAtStop,
            condenseHeadsignPredictions: condenseHeadsignPredictions,
            now: now,
            pushNavEntry: { entry in
                pushNavEntry(entry)
                analytics.tappedDeparture(
                    routeId: patternsAtStop.routeIdentifier,
                    stopId: patternsAtStop.stop.id,
                    pinned: pinned
                )
            }
        )
    }
}
