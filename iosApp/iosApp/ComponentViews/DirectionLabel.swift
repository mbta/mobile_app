//
//  DirectionLabel.swift
//  iosApp
//
//  Created by Simon, Emma on 6/24/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct DirectionLabel: View {
    let direction: Direction

    private let localizedDirectionNames: [String: String] = [
        "North": NSLocalizedString("Northbound", comment: "A route direction label"),
        "South": NSLocalizedString("Southbound", comment: "A route direction label"),
        "East": NSLocalizedString("Eastbound", comment: "A route direction label"),
        "West": NSLocalizedString("Westbound", comment: "A route direction label"),
        "Inbound": NSLocalizedString("Inbound", comment: "A route direction label"),
        "Outbound": NSLocalizedString("Outbound", comment: "A route direction label"),
    ]

    private func directionNameFormatted(_ direction: Direction) -> String {
        localizedDirectionNames[direction.name] ?? NSLocalizedString("Heading", comment: "A route direction label")
    }

    var body: some View {
        VStack(alignment: .leading) {
            if let destination = direction.destination {
                Text("\(directionNameFormatted(direction)) to",
                     comment: """
                     Label the direction a list of arrivals is for.
                     Possible values include Northbound, Southbound, Inbound, Outbound, Eastbound, Westbound.
                     For example, "[Northbound] to [Alewife]
                     """)
                     .font(Typography.footnote)
                     .textCase(.none)
                Text(destination)
                    .font(Typography.bodySemibold)
                    .multilineTextAlignment(.leading)
                    .textCase(.none)
            } else {
                Text(directionNameFormatted(direction))
                    .font(Typography.bodySemibold)
                    .textCase(.none)
                    .frame(maxHeight: .infinity, alignment: .leading)
            }
        }
    }
}
