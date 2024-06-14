//
//  DirectionPicker.swift
//  iosApp
//
//  Created by Simon, Emma on 4/24/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct DirectionPicker: View {
    @Binding var filter: StopDetailsFilter?

    let availableDirections: [Int32]
    let directions: [Direction]
    let route: Route

    init(patternsByStop: PatternsByStop, filter: Binding<StopDetailsFilter?>) {
        availableDirections = Set(patternsByStop.patternsByHeadsign.map { pattern in
            pattern.directionId()
        }).sorted()
        directions = patternsByStop.directions
        route = patternsByStop.route

        _filter = filter
    }

    var body: some View {
        if availableDirections.count > 1 {
            let deselectedBackroundColor = deselectedBackgroundColor(route)
            HStack(alignment: .center) {
                ForEach(availableDirections, id: \.hashValue) { direction in
                    let isSelected = filter?.directionId == direction
                    let action = { $filter.wrappedValue = .init(routeId: route.id, directionId: direction) }

                    Button(action: action) {
                        VStack(alignment: .leading) {
                            Text("\(directionNameFormatted(directions[Int(direction)])) to")
                                .font(.footnote)
                                .textCase(.none)
                            Text(directions[Int(direction)].destination)
                                .font(.body)
                                .fontWeight(.bold)
                                .textCase(.none)
                        }
                        .padding(8)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    }
                    .background(isSelected ? Color(hex: route.color) : deselectedBackroundColor)
                    .foregroundStyle(isSelected ? Color(hex: route.textColor) : .deselectedToggleText)
                    .clipShape(.rect(cornerRadius: 6))
                }
            }
            .padding(2)
            .background(deselectedBackroundColor)
            .clipShape(.rect(cornerRadius: 6))
        }
    }

    private func directionNameFormatted(_ direction: Direction) -> String {
        if direction.name == "North" ||
            direction.name == "South" ||
            direction.name == "East" ||
            direction.name == "West" {
            return "\(direction.name)bound"
        }
        return direction.name
    }

    private func deselectedBackgroundColor(_ route: Route) -> Color {
        // Exceptions for contrast
        if route.type == RouteType.commuterRail || route.id == "Blue" {
            return Color.deselectedToggle2
        }
        return Color.deselectedToggle1
    }
}
