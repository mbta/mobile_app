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
    var availableDirections: [Int32]
    var directions: [Direction]
    let route: Route
    let selectedDirectionId: Int32?
    let updateDirectionId: (Int32) -> Void

    init(
        stopData: RouteCardData.RouteStopData,
        filter: StopDetailsFilter?,
        setFilter: @escaping (StopDetailsFilter?) -> Void
    ) {
        availableDirections = Set(stopData.data.map(\.directionId)).sorted()
        directions = stopData.directions
        let route = stopData.lineOrRoute.sortRoute
        self.route = route
        let line: Line? = switch onEnum(of: stopData.lineOrRoute) {
        case let .line(line): line.line
        default: nil
        }
        selectedDirectionId = filter?.directionId
        updateDirectionId = { setFilter(.init(routeId: line?.id ?? route.id, directionId: $0)) }
    }

    init(
        availableDirections: [Int32],
        directions: [Direction],
        route: Route,
        selectedDirectionId: Int32?,
        updateDirectionId: @escaping (Int32) -> Void
    ) {
        self.availableDirections = availableDirections
        self.directions = directions
        self.route = route
        self.selectedDirectionId = selectedDirectionId
        self.updateDirectionId = updateDirectionId
    }

    var body: some View {
        if availableDirections.count > 1 {
            HStack(alignment: .center, spacing: 2) {
                ForEach(availableDirections, id: \.hashValue) { direction in
                    let isSelected = selectedDirectionId == direction
                    let action = { updateDirectionId(direction) }
                    let selectedDirection = directions[Int(direction)]

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
                    .background(isSelected ? route.uiColor : Color.clear)
                    .foregroundStyle(isSelected ? route.uiTextColor : Color.routeColorContrastText)
                    .clipShape(.rect(cornerRadius: 6))
                }
            }
            .accessibilityElement(children: .contain)
            .frame(minHeight: 44)
            .padding(2)
            .background(Color.routeColorContrast)
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

    let lineOrRoute = LineOrRoute.route(route)
    let context = RouteCardData.Context.stopDetailsFiltered
    let leaf0 = RouteCardData.Leaf(
        lineOrRoute: lineOrRoute,
        stop: stop,
        directionId: 0,
        routePatterns: [patternOutbound],
        stopIds: [stop.id],
        upcomingTrips: [],
        alertsHere: [],
        allDataLoaded: true,
        hasSchedulesToday: true,
        alertsDownstream: [],
        context: context
    )
    let leaf1 = RouteCardData.Leaf(
        lineOrRoute: lineOrRoute,
        stop: stop,
        directionId: 1,
        routePatterns: [patternInbound],
        stopIds: [stop.id],
        upcomingTrips: [],
        alertsHere: [],
        allDataLoaded: true,
        hasSchedulesToday: true,
        alertsDownstream: [],
        context: context
    )
    let stopCard = RouteCardData.RouteStopData(lineOrRoute: lineOrRoute, stop: stop, directions: [
        .init(name: "Outbound", destination: "Out", id: 0),
        .init(name: "Inbound", destination: "In", id: 1),
    ], data: [leaf0, leaf1])

    DirectionPicker(
        stopData: stopCard,
        filter: .init(routeId: route.id, directionId: 0),
        setFilter: { _ in }
    )
    .fixedSize(horizontal: false, vertical: true)
    .padding(16)
}
