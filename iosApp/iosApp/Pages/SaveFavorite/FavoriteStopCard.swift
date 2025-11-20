//
//  FavoriteStopCard.swift
//  iosApp
//
//  Created by esimon on 11/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct FavoriteStopCard: View {
    var lineOrRoute: LineOrRoute
    var stop: Stop
    var direction: Direction?
    var toggleDirection: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 12) {
                (stop.locationType == .stop ? Image(.mapStopCloseBUS) : Image(.mbtaLogo))
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .accessibilityHidden(true)
                Text(stop.name).font(Typography.bodySemibold)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.halo)
            HStack(alignment: .center, spacing: 8) {
                RoutePill(lineOrRoute: lineOrRoute, type: .fixed)
                if let direction {
                    DirectionLabel(direction: direction).frame(maxWidth: .infinity, alignment: .leading)
                }
                if let toggleDirection {
                    ActionButton(kind: .exchange, action: toggleDirection)
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 10)
        }
        .background(Color.fill3)
        .withRoundedBorder(width: 2)
    }
}
