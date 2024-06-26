//
//  RouteSection.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 4/16/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct RouteSection<Content: View>: View {
    let route: Route
    let pinned: Bool
    let onPin: (String) -> Void
    let content: () -> Content

    @ScaledMetric private var modeIconHeight: CGFloat = 24
    @ScaledMetric private var pinIconHeight: CGFloat = 20

    var body: some View {
        VStack(spacing: 0) {
            RouteHeader(route: route) {
                pinButton
            }
            content()
        }
        .background(Color.fill3)
        .withRoundedBorder()
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }

    private var pinButton: some View {
        Button(
            action: {
                onPin(route.id)
            },
            label: {
                Image(pinned ? .pinnedRouteActive : .pinnedRouteInactive)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .shadow(color: Color.fill1, radius: pinned ? 1 : 0)
                    .accessibilityLabel("pin route")
            }
        )
        .accessibilityIdentifier("pinButton")
        .accessibilityAddTraits(pinned ? [.isSelected] : [])
        .frame(maxHeight: pinIconHeight)
    }
}
