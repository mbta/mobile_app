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

    private let reformatDirectionNames: Set<String> = ["North", "South", "East", "West"]

    private func directionNameFormatted(_ direction: Direction) -> String {
        if reformatDirectionNames.contains(direction.name) {
            return "\(direction.name)bound"
        }
        return direction.name
    }

    var body: some View {
        VStack(alignment: .leading) {
            if let destination = direction.destination {
                Text("\(directionNameFormatted(direction)) to",
                     comment: """
                     Label the direction a list of arrivals is for. Possible values include Northbound, Southbound, Inbound, Outbound, Eastbound, Westbound. For example, "[Northbound] to [Alewife]
                     """)
                     .font(Typography.footnote)
                     .textCase(.none)
                Text(destination)
                    .font(Typography.bodySemibold)
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
