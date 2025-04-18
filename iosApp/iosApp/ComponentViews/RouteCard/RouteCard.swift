//
//  RouteCard.swift
//  iosApp
//
//  Created by esimon on 4/11/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteCard: View {
    let cardData: RouteCardData
    let global: GlobalResponse
    let now: Date
    let onPin: (String) -> Void
    let pinned: Bool
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let showStationAccessibility: Bool

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        VStack(spacing: 0) {
            TransitHeader(
                name: cardData.lineOrRoute.name,
                routeType: cardData.lineOrRoute.type,
                backgroundColor: Color(hex: cardData.lineOrRoute.backgroundColor),
                textColor: Color(hex: cardData.lineOrRoute.textColor),
                rightContent: { PinButton(pinned: pinned, action: { onPin(cardData.lineOrRoute.id) }) }
            )
            .accessibilityElement(children: .contain)
            ForEach(Array(cardData.stopData.enumerated()), id: \.element) { index, stopData in
                if cardData.context == .nearbyTransit {
                    RouteCardStopHeader(
                        data: stopData,
                        showStationAccessibility: showStationAccessibility
                    )
                }
                RouteCardDepartures(
                    cardData: cardData,
                    stopData: stopData,
                    global: global,
                    now: now,
                    pinned: pinned,
                    pushNavEntry: pushNavEntry
                )

                if index < cardData.stopData.count - 1 {
                    HaloSeparator()
                }
            }
        }
        .background(Color.fill3)
        .withRoundedBorder()
    }
}
