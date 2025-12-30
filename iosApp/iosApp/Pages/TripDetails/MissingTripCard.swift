//
//  MissingTripCard.swift
//  iosApp
//
//  Created by esimon on 2025-09-29.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

enum MissingTripCardType {
    case complete
    case notAvailable
}

struct MissingTripCard: View {
    let type: MissingTripCardType
    let routeAccents: TripRouteAccents

    @ScaledMetric var iconSize = 48

    @ViewBuilder
    var headerText: some View {
        switch type {
        case .complete:
            Text("Trip complete")
        case .notAvailable:
            Text("Trip not available")
        }
    }

    @ViewBuilder
    var bodyText: some View {
        switch type {
        case .complete:
            Text("This \(routeAccents.type.typeText(isOnly: true)) has reached its destination.")
        case .notAvailable:
            Text("This \(routeAccents.type.typeText(isOnly: true)) is not assigned to a trip yet.")
        }
    }

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
                headerText
                    .font(Typography.title2Bold)
                    .multilineTextAlignment(.leading)
            }
            routeAccents.color.opacity(0.25).frame(height: 2)
            bodyText
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
        MissingTripCard(type: .complete, routeAccents: .init(route: data.getRoute(id: "Red")))
        MissingTripCard(type: .complete, routeAccents: .init(route: data.getRoute(id: "Green-C")))
        MissingTripCard(type: .complete, routeAccents: .init(route: data.getRoute(id: "742")))
        MissingTripCard(type: .complete, routeAccents: .init(route: data.getRoute(id: "15")))
        MissingTripCard(type: .complete, routeAccents: .init(route: data.getRoute(id: "CR-Lowell")))

        MissingTripCard(type: .notAvailable, routeAccents: .init(route: data.getRoute(id: "Red")))
        MissingTripCard(type: .notAvailable, routeAccents: .init(route: data.getRoute(id: "Green-C")))
        MissingTripCard(type: .notAvailable, routeAccents: .init(route: data.getRoute(id: "742")))
        MissingTripCard(type: .notAvailable, routeAccents: .init(route: data.getRoute(id: "15")))
        MissingTripCard(type: .notAvailable, routeAccents: .init(route: data.getRoute(id: "CR-Lowell")))
    }.padding(16).background(Color.fill2)
}
