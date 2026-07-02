//
//  LoadingStopCard.swift
//  iosApp
//
//  Created by Melody Horn on 6/25/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct LoadingStopCard: View {
    @ObserveInjection var inject
    var body: some View {
        let placeholderRouteData = LoadingPlaceholders.shared.nearbyRoute()
        let placeholderStopData = StopCardData.companion.fromRouteCardData(
            routeCardData: [placeholderRouteData],
            sortByDistanceFrom: nil
        )
        if let stopData = placeholderStopData.first {
            StopCard(
                cardData: stopData,
                global: nil,
                now: EasternTimeInstant.now(),
                isFavorite: { _ in false },
                pushNavEntry: { _ in }
            )
            .enableInjection()
        }
    }
}
