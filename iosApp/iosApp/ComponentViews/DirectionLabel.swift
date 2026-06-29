//
//  DirectionLabel.swift
//  iosApp
//
//  Created by Simon, Emma on 6/24/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct DirectionLabel: View {
    @ObserveInjection var inject
    let direction: Direction
    let showDestination: Bool
    let pillDecoration: PredictionRowView.PillDecoration
    let isTitle: Bool
    let routeNamePrefix: String?

    init(
        direction: Direction,
        showDestination: Bool = true,
        pillDecoration: PredictionRowView.PillDecoration = .none,
        isTitle: Bool = false,
        routeNamePrefix: String? = nil,
    ) {
        self.direction = direction
        self.showDestination = showDestination
        self.pillDecoration = pillDecoration
        self.isTitle = isTitle
        self.routeNamePrefix = routeNamePrefix
    }

    private static let localizedDirectionNames: [String: String] = [
        "North": NSLocalizedString("Northbound", comment: "A route direction label"),
        "South": NSLocalizedString("Southbound", comment: "A route direction label"),
        "East": NSLocalizedString("Eastbound", comment: "A route direction label"),
        "West": NSLocalizedString("Westbound", comment: "A route direction label"),
        "Inbound": NSLocalizedString("Inbound", comment: "A route direction label"),
        "Outbound": NSLocalizedString("Outbound", comment: "A route direction label"),
    ]

    static func directionNameFormatted(_ direction: Direction) -> String {
        localizedDirectionNames[direction.name ?? ""] ??
            NSLocalizedString("Heading", comment: "A route direction label")
    }

    @ViewBuilder
    func directionTo(_ direction: Direction) -> some View {
        let routeNamePrefix = if let routeNamePrefix { "\(routeNamePrefix) " } else { "" }
        let directionTo = String(format: NSLocalizedString("%@ to", comment: """
        Label the direction a list of arrivals is for.
        Possible values include Northbound, Southbound, Inbound, Outbound, Eastbound, Westbound.
        For example, "[Northbound] to [Alewife]"
        """), Self.directionNameFormatted(direction))
        Text(routeNamePrefix + directionTo)
            .font(Typography.footnote)
            .textCase(.none)
            .multilineTextAlignment(.leading)
    }

    @ViewBuilder
    func destinationLabel(_ destination: String) -> some View {
        HStack(alignment: .center, spacing: 8) {
            if case let .onDirectionDestination(route: route) = pillDecoration {
                RoutePill(route: route, type: .flex)
            }
            Text(destination)
                .font(isTitle ? Typography.title3Bold : Typography.bodySemibold)
                .multilineTextAlignment(.leading)
                .textCase(.none)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: isTitle ? 2 : 6) {
            if !showDestination {
                directionTo(direction)
            } else if let destination = direction.destination {
                directionTo(direction)
                destinationLabel(destination)
            } else {
                destinationLabel(DirectionLabel.directionNameFormatted(direction))
            }
        }.accessibilityElement(children: .combine)
            .enableInjection()
    }
}
