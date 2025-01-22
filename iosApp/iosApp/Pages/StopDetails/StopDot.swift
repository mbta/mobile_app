//
//  StopDot.swift
//  iosApp
//
//  Created by esimon on 12/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct StopDot: View {
    let routeAccents: TripRouteAccents
    let targeted: Bool

    var body: some View {
        Circle()
            .strokeBorder(Color.stopDotHalo, lineWidth: 1)
            .background(Circle().fill(routeAccents.color))
            .frame(width: 14, height: 14)
            .overlay {
                if targeted {
                    Image(.stopPinIndicator).padding(.bottom, 32).accessibilityHidden(true)
                }
            }
    }
}
