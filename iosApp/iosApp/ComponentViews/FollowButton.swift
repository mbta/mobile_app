//
//  FollowButton.swift
//  iosApp
//
//  Created by esimon on 9/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

struct FollowButton: View {
    var action: () -> Void
    var routeAccents: TripRouteAccents

    var body: some View {
        Button(action: action, label: {
            HStack(alignment: .center, spacing: 8) {
                Image(.liveData)
                    .resizable()
                    .frame(width: 12, height: 12)
                    .accessibilityHidden(true)

                Text("Follow").font(Typography.callout)
            }
            .padding(.vertical, 7)
            .padding(.leading, 12)
            .padding(.trailing, 16)
            .foregroundStyle(routeAccents.textColor)
        })
        .frame(minWidth: 97, minHeight: 36)
        .background(routeAccents.color)
        .withRoundedBorder(radius: 88, width: 2)
    }
}
