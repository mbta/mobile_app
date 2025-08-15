//
//  RoutePickerRow.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/21/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RoutePickerRow: View {
    let route: RouteCardData.LineOrRoute
    let onTap: () -> Void

    var body: some View {
        Button(
            action: onTap,
            label: {
                HStack(spacing: 0) {
                    HStack(spacing: 8) {
                        RoutePill(route: route.sortRoute, type: .fixed)
                            // Route pill VoiceOver text is redundant except for bus
                            .accessibilityHidden(route.type != .bus)
                        Text(route.sortRoute.longName)
                            .multilineTextAlignment(.leading)
                            .foregroundColor(Color.text)
                    }
                    .padding(.leading, 4)
                    Spacer()
                    Image(.faChevronRight)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 8)
                        .padding(.horizontal, 8)
                        .foregroundColor(Color.translucentContrast)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 12)
            }
        ).preventScrollTaps()
    }
}
