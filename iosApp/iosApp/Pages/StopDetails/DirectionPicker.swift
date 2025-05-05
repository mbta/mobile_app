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

    init(data: DepartureDataBundle, filter: StopDetailsFilter?,
         setFilter: @escaping (StopDetailsFilter?) -> Void) {
        self.filter = filter
        self.setFilter = setFilter
        availableDirections = Set(data.stopData.data.map(\.directionId)).sorted()
        directions = data.stopData.directions
        route = data.routeData.lineOrRoute.sortRoute
        line = switch onEnum(of: data.routeData.lineOrRoute) {
        case let .line(line): line.line
        default: nil
        }
    }

    var body: some View {
        if availableDirections.count > 1 {
            let deselectedBackroundColor = Color.deselectedToggle2.opacity(0.6)
            HStack(alignment: .center, spacing: 2) {
                ForEach(availableDirections, id: \.hashValue) { direction in
                    let isSelected = filter?.directionId == direction
                    let action = { setFilter(.init(routeId: line?.id ?? route.id, directionId: direction)) }

                    Button(action: action) {
                        DirectionLabel(direction: directions[Int(direction)])
                            .padding(8)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    }
                    .simultaneousGesture(TapGesture())
                    .accessibilityAddTraits(isSelected ? [.isSelected, .isHeader] : [])
                    .accessibilityHeading(isSelected ? .h2 : .unspecified)
                    .accessibilitySortPriority(isSelected ? 1 : 0)
                    .accessibilityHint(isSelected ? "" : NSLocalizedString(
                        "switches direction",
                        comment: "Screen reader hint for the direction toggle action"
                    ))
                    .background(isSelected ? route.uiColor : Color.clear)
                    .foregroundStyle(isSelected ? route.uiTextColor : .deselectedToggleText)
                    .clipShape(.rect(cornerRadius: 6))
                }
            }
            .padding(2)
            .background(deselectedBackroundColor)
            .clipShape(.rect(cornerRadius: 8))
        } else if availableDirections.count == 1, let direction = availableDirections.first {
            DirectionLabel(direction: directions[Int(direction)])
                .foregroundStyle(route.uiTextColor)
                .padding(8)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .accessibilityElement(children: .combine)
                .accessibilityAddTraits(.isHeader)
                .accessibilityHeading(.h2)
        }
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
            ],
            elevatorAlerts: []
        ),
        filter: .init(routeId: route.id, directionId: 0),
        setFilter: { _ in }
    )
    .fixedSize(horizontal: false, vertical: true)
    .padding(16)
}
