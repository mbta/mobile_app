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
    let action: () -> Void

    @ScaledMetric private var pinIconHeight: CGFloat = 20

    var body: some View {
        Button(
            action: action,
            label: {
                Image(pinned ? .pinnedRouteActive : .pinnedRouteInactive)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .accessibilityLabel(Text(
                        "Star route",
                        comment: "VoiceOver label for the button to favorite a route"
                    ))
                    .accessibilityHint(pinned ? NSLocalizedString(
                        "Removes route from the top of the list",
                        comment: "VoiceOver hint for favorite button when a route is already favorited"
                    ) : NSLocalizedString(
                        "Pins route to the top of the list",
                        comment: "VoiceOver hint for favorite button when a route is not favorited"
                    ))
            }
        )
        .accessibilityIdentifier("pinButton")
        .accessibilityAddTraits(pinned ? [.isSelected] : [])
        .frame(maxHeight: pinIconHeight)
    }
}
