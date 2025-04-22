//
//  LegacyRouteCard.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 4/16/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct LegacyRouteCard<Content: View>: View {
    let route: Route
    let pinned: Bool
    let onPin: (String) -> Void
    let content: () -> Content

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        TransitCard(header: {
            RouteHeader(route: route) {
                PinButton(pinned: pinned, action: { onPin(route.id) })
            }.accessibilityAddTraits(.isButton)
        }, content: content)
    }
}
