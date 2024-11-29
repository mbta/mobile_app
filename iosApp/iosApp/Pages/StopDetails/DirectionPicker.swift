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
    var filter: StopDetailsFilter?
    var setFilter: (StopDetailsFilter?) -> Void
    let availableDirections: [Int32]
    let directions: [Direction]
    let route: Route
    let line: Line?

    init(patternsByStop: PatternsByStop, filter: StopDetailsFilter?,
         setFilter: @escaping (StopDetailsFilter?) -> Void) {
        self.filter = filter
        self.setFilter = setFilter
        availableDirections = Set(patternsByStop.patterns.map { pattern in
            pattern.directionId()
        }).sorted()
        directions = patternsByStop.directions
        route = patternsByStop.representativeRoute
        line = patternsByStop.line
    }

    var body: some View {
        if availableDirections.count > 1 {
            let deselectedBackroundColor = deselectedBackgroundColor(route)
            HStack(alignment: .center) {
                ForEach(availableDirections, id: \.hashValue) { direction in
                    let isSelected = filter?.directionId == direction
                    let action = { setFilter(.init(routeId: line?.id ?? route.id, directionId: direction)) }

                    Button(action: action) {
                        DirectionLabel(direction: directions[Int(direction)])
                            .padding(8)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    }
                    .accessibilityAddTraits(isSelected ? [.isSelected] : [])
                    .background(isSelected ? Color(hex: route.color) : deselectedBackroundColor)
                    .foregroundStyle(isSelected ? Color(hex: route.textColor) : .deselectedToggleText)
                    .clipShape(.rect(cornerRadius: 6))
                }
            }
            .padding(2)
            .background(deselectedBackroundColor)
            .clipShape(.rect(cornerRadius: 8))
        } else if availableDirections.count == 1, let direction = availableDirections.first {
            DirectionLabel(direction: directions[Int(direction)])
                .padding(8)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
    }

    private func deselectedBackgroundColor(_ route: Route) -> Color {
        // Exceptions for contrast
        if route.type == RouteType.commuterRail || route.id == "Blue" {
            return Color.deselectedToggle2
        }
        return Color.deselectedToggle1
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    let stop = objects.stop { _ in }
    let route = objects.route { route in
        route.color = "FFC72C"
        route.textColor = "000000"
    }
    let patternOutbound = objects.routePattern(route: route) { pattern in
        pattern.directionId = 0
    }
    let patternInbound = objects.routePattern(route: route) { pattern in
        pattern.directionId = 1
    }
    return DirectionPicker(
        patternsByStop: .init(
            routes: [route],
            line: nil,
            stop: stop,
            patterns: [
                .ByHeadsign(
                    route: route,
                    headsign: "Out",
                    line: nil,
                    patterns: [patternOutbound],
                    upcomingTrips: []
                ),
                .ByHeadsign(
                    route: route,
                    headsign: "In",
                    line: nil,
                    patterns: [patternInbound],
                    upcomingTrips: []
                ),
            ],
            directions: [
                .init(name: "Outbound", destination: "Out", id: 0),
                .init(name: "Inbound", destination: "In", id: 1),
            ]
        ),
        filter: .init(routeId: route.id, directionId: 0),
        setFilter: { _ in }
    )
    .fixedSize(horizontal: false, vertical: true)
    .padding(16)
}
