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
    @ObserveInjection var inject
    var lineOrRoute: LineOrRoute
    var stop: Stop
    var direction: Direction?
    var toggleDirection: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            StopCardHeader(stop: stop, subheader: {})
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
        .enableInjection()
    }
}
