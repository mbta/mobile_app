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
            Text("\(directionNameFormatted(direction)) to")
                .font(Typography.footnote)
                .textCase(.none)
            Text(direction.destination)
                .font(Typography.bodySemibold)
                .textCase(.none)
        }
    }
}
