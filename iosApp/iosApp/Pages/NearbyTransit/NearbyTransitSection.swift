//
//  NearbyTransitSection.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 4/16/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct NearbyTransitSection<Content: View>: View {
    let route: Route
    let pinned: Bool
    let onPin: (String) -> Void
    let content: () -> Content

    var body: some View {
        Section(
            content: content,
            header: {
                HStack {
                    RoutePill(route: route)
                        .padding(.leading, -20)
                    Spacer()
                    pinButton
                        .frame(maxHeight: 32)
                        .padding(.trailing, -20)
                }
            }
        )
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
            }
        )
        .accessibilityIdentifier("pinButton")
    }
}
