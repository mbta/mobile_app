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
    let direction: Direction
    let showDestination: Bool
    let pillDecoration: PredictionRowView.PillDecoration

    init(direction: Direction, showDestination: Bool = true, pillDecoration: PredictionRowView.PillDecoration = .none) {
        self.direction = direction
        self.showDestination = showDestination
        self.pillDecoration = pillDecoration
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
        Text("\(DirectionLabel.directionNameFormatted(direction)) to",
             comment: """
             Label the direction a list of arrivals is for.
             Possible values include Northbound, Southbound, Inbound, Outbound, Eastbound, Westbound.
             For example, "[Northbound] to [Alewife]"
             """)
             .font(Typography.footnote)
             .textCase(.none)
    }

    @ViewBuilder
    func destinationLabel(_ destination: String) -> some View {
        HStack(alignment: .center, spacing: 8) {
            if case let .onDirectionDestination(route: route) = pillDecoration {
                RoutePill(route: route, type: .flex)
            }
            Text(destination)
                .font(Typography.bodySemibold)
                .multilineTextAlignment(.leading)
                .textCase(.none)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if !showDestination {
                directionTo(direction)
            } else if let destination = direction.destination {
                directionTo(direction)
                destinationLabel(destination)
            } else {
                destinationLabel(DirectionLabel.directionNameFormatted(direction))
            }
        }.accessibilityElement(children: .combine)
    }
}
