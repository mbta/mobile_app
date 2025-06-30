//
//  PinButton.swift
//  iosApp
//
//  Created by Simon, Emma on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct PinButton: View {
    let pinned: Bool
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(
            action: action,
            label: { StarIcon(pinned: pinned, color: color) }
        )
        .accessibilityIdentifier("pinButton")
        .accessibilityAddTraits(pinned ? [.isSelected] : [])
        .simultaneousGesture(TapGesture())
    }
}

struct StarIcon: View {
    let pinned: Bool
    let color: Color

    @ScaledMetric private var pinIconHeight: CGFloat = 20

    var body: some View {
        Image(pinned ? .pinnedRouteActive : .pinnedRouteInactive)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .foregroundStyle(color)
            .frame(maxHeight: pinIconHeight)
            .accessibilityLabel(Text(
                "Star route",
                comment: "VoiceOver label for the button to favorite a route"
            ))
            .accessibilityHint(pinned ? NSLocalizedString(
                "Unpins route from the top of the list",
                comment: "VoiceOver hint for favorite button when a route is already favorited"
            ) : NSLocalizedString(
                "Pins route to the top of the list",
                comment: "VoiceOver hint for favorite button when a route is not favorited"
            ))
    }
}
