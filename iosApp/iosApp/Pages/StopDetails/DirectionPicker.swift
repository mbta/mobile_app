//
//  DirectionPicker.swift
//  iosApp
//
//  Created by Simon, Emma on 4/24/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct DirectionPicker: View {
    @ObserveInjection var inject
    var availableDirections: [Int32]
    var directions: [Direction]
    let route: Route
    let selectedDirectionId: Int32?
    let updateDirectionId: (Int32) -> Void

    var body: some View {
        if availableDirections.count > 1 {
            HStack(alignment: .center, spacing: 2) {
                ForEach(availableDirections, id: \.hashValue) { direction in
                    let isSelected = selectedDirectionId == direction
                    let action = { updateDirectionId(direction) }
                    let selectedDirection = directions[Int(direction)]
                    let backgroundColor = isSelected ? route.uiColor : Color.clear

                    Button(action: action) {
                        DirectionLabel(direction: selectedDirection)
                            .padding(8)
                            .frame(
                                maxWidth: .infinity,
                                maxHeight: .infinity,
                                alignment: selectedDirection.destination == nil ? .center : .leading
                            )
                    }
                    .preventScrollTaps()
                    .accessibilityAddTraits(isSelected ? [.isSelected, .isHeader] : [])
                    .accessibilityHeading(isSelected ? .h2 : .unspecified)
                    .accessibilitySortPriority(isSelected ? 1 : 0)
                    .accessibilityHint(isSelected ? "" : NSLocalizedString(
                        "switches direction",
                        comment: "Screen reader hint for the direction toggle action"
                    ))
                    // Setting tint helps preserve contrast with accessibility setting "Show Borders" turned on

                    .background(backgroundColor)
                    .tint(backgroundColor)
                    .foregroundStyle(isSelected ? route.uiTextColor : Color.routeColorContrastText)
                    .clipShape(.rect(cornerRadius: 6))
                }
            }
            .accessibilityElement(children: .contain)
            .frame(minHeight: 44)
            .padding(2)
            .background(Color.routeColorContrast)
            .clipShape(.rect(cornerRadius: 8))
            .enableInjection()
        } else if availableDirections.count == 1, let direction = availableDirections.first {
            DirectionLabel(direction: directions[Int(direction)])
                .foregroundStyle(route.uiTextColor)
                .padding(8)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .accessibilityElement(children: .combine)
                .accessibilityAddTraits(.isHeader)
                .accessibilityHeading(.h2)
                .enableInjection()
        }
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    let route = objects.route { route in
        route.color = "FFC72C"
        route.textColor = "000000"
    }

    DirectionPicker(
        availableDirections: [0, 1],
        directions: [
            .init(name: "Outbound", destination: "Out", id: 0),
            .init(name: "Inbound", destination: "In", id: 1),
        ],
        route: route,
        selectedDirectionId: 0,
        updateDirectionId: { _ in }
    )
    .fixedSize(horizontal: false, vertical: true)
    .padding(16)
}
