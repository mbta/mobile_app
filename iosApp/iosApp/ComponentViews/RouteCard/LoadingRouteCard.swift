//
//  LoadingRouteCard.swift
//  iosApp
//
//  Created by Melody Horn on 6/12/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct LoadingRouteCard: View {
    var body: some View {
        RouteCard(
            cardData: LoadingPlaceholders.shared.nearbyRoute(),
            global: nil,
            now: EasternTimeInstant.now(),
            onPin: { _ in },
            pinned: false,
            isFavorite: { _ in false },
            pushNavEntry: { _ in },
            showStopHeader: true
        )
    }
}
