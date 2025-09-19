//
//  TripDetailsHeader.swift
//  iosApp
//
//  Created by esimon on 9/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

struct TripDetailsHeader: View {
    let route: Route?
    let direction: Direction?
    let onClose: () -> Void

    var pillDescription: String { if let route { routeModeLabel(route: route) } else { "" } }
    var textColor: Color { if let route { Color(hex: route.textColor) } else { Color.text } }

    var body: some View {
        HStack(alignment: .center, spacing: 16) {
            if let route {
                RoutePill(
                    route: route,
                    line: nil,
                    type: .flexCompact,
                    height: .large,
                    borderWidth: 2,
                    borderColor: .routeColorContrast
                )
                .accessibilityLabel(Text(pillDescription))
            }
            if let direction {
                DirectionLabel(direction: direction, isTitle: true)
                    .foregroundStyle(textColor)
            }
            Spacer()
            ActionButton(
                kind: .close,
                circleColor: .routeColorContrast,
                iconColor: .routeColorContrastText,
                action: onClose
            ).preventScrollTaps()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.all, 16)
        .padding(.top, 3)
    }
}

struct TripDetailsHeader_Previews: PreviewProvider {
    static var previews: some View {
        let routeGreen = TestData.getRoute(id: "Green-C")
        let routeOrange = TestData.getRoute(id: "Orange")
        let route87 = TestData.getRoute(id: "87")

        List {
            ZStack {
                Color(hex: routeGreen.color)
                TripDetailsHeader(
                    route: routeGreen,
                    direction: Direction(directionId: 0, route: routeGreen),
                    onClose: {},
                ).padding(.bottom, 16)
            }
            ZStack {
                Color(hex: routeOrange.color)
                TripDetailsHeader(
                    route: routeOrange,
                    direction: Direction(directionId: 0, route: routeOrange),
                    onClose: {},
                ).padding(.bottom, 16)
            }
            ZStack {
                Color(hex: route87.color)
                TripDetailsHeader(
                    route: route87,
                    direction: Direction(directionId: 0, route: route87),
                    onClose: {},
                ).padding(.bottom, 16)
            }
        }
    }
}
