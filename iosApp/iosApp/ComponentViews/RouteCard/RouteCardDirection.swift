//
//  RouteCardDirection.swift
//  iosApp
//
//  Created by esimon on 4/16/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteCardDirection: View {
    let direction: Direction
    let formatted: LeafFormat

    var body: some View {
        switch onEnum(of: formatted) {
        case let .branched(branched):
            VStack(alignment: .leading, spacing: 0) {
                DirectionLabel(direction: direction, showDestination: false).foregroundStyle(Color.text)
                ForEach(branched.branches) { branch in
                    let pillDecoration: PredictionRowView.PillDecoration = if let route = branch
                        .route { .onRow(route: route) } else { .none }
                    HeadsignRowView(
                        headsign: branch.headsign,
                        predictions: branch.format,
                        pillDecoration: pillDecoration
                    )
                }
            }

        case let .single(single):
            DirectionRowView(
                direction: .init(
                    name: direction.name,
                    destination: single.headsign == nil || single.headsign?.isEmpty == true
                        ? direction.destination
                        : single.headsign,
                    id: direction.id
                ),
                predictions: single.format
            )
        }
    }
}
