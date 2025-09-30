//
//  TripCompleteCard.swift
//  iosApp
//
//  Created by esimon on 2025-09-29.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct TripCompleteCard: View {
    let routeAccents: TripRouteAccents

    @ScaledMetric var iconSize = 48

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center, spacing: 16) {
                ZStack(alignment: .center) {
                    Circle()
                        .fill(routeAccents.color)
                        .frame(width: iconSize, height: iconSize, alignment: .center)
                    routeIcon(routeAccents.type)
                        .resizable()
                        .scaledToFit()
                        .foregroundStyle(routeAccents.textColor)
                        .frame(width: iconSize, height: iconSize, alignment: .center)
                }
                Text("Trip complete")
                    .font(Typography.title2Bold)
                    .multilineTextAlignment(.leading)
            }
            routeAccents.color.opacity(0.25).frame(height: 2)
            Text("This \(routeAccents.type.typeText(isOnly: true)) has reached its destination.")
                .font(Typography.callout)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .background(Color.fill3)
        .withRoundedBorder(width: 2)
    }
}

#Preview {
    let data = TestData.clone()
    VStack(spacing: 16) {
        TripCompleteCard(routeAccents: .init(route: data.getRoute(id: "Red")))
        TripCompleteCard(routeAccents: .init(route: data.getRoute(id: "Green-C")))
        TripCompleteCard(routeAccents: .init(route: data.getRoute(id: "742")))
        TripCompleteCard(routeAccents: .init(route: data.getRoute(id: "15")))
        TripCompleteCard(routeAccents: .init(route: data.getRoute(id: "CR-Lowell")))
    }.padding(16).background(Color.fill2)
}
