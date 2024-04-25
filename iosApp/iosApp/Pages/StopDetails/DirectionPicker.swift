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
            HStack(alignment: .center) {
                ForEach(availableDirections, id: \.hashValue) { direction in
                    let action = { $filter.wrappedValue = .init(routeId: route.id, directionId: direction) }

                    Button(action: action) {
                        VStack(alignment: .leading) {
                            Text("\(directions[Int(direction)].name.uppercased()) to")
                                .font(.footnote)
                                .textCase(.none)
                            Text(directions[Int(direction)].destination)
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .textCase(.none)
                        }
                        .padding(8)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    }
                    .background(filter?.directionId == direction ? Color(hex: route.color) : .clear)
                    .foregroundStyle(filter?.directionId == direction ? Color(hex: route.textColor) : .black)
                    .clipShape(.rect(cornerRadius: 10))
                }
            }
            .padding(3)
            .background(.white)
            .clipShape(.rect(cornerRadius: 10))
            .padding(.horizontal, -20)
        }
    }
}
