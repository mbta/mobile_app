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
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .center) {
                    if let secondaryAlert = branched.secondaryAlert {
                        Image(secondaryAlert.iconName)
                            .accessibilityLabel("Alert")
                            .frame(width: 18, height: 18)
                    }
                    DirectionLabel(direction: direction, showDestination: false).foregroundStyle(Color.text)
                }
                ForEach(branched.branchRows) { branchRow in
                    let pillDecoration: PredictionRowView.PillDecoration = if let route = branchRow
                        .route { .onRow(route: route) } else { .none }
                    HeadsignRowView(
                        headsign: branchRow.headsign,
                        predictions: branchRow.format,
                        pillDecoration: pillDecoration
                    )
                }
            }.accessibilityElement(children: .combine)
                .accessibilityInputLabels(Set([
                    DirectionLabel.directionNameFormatted(direction),
                    direction.destination,
                ] +
                    Set(branched.branchRows.map(\.headsign)))
                    .compactMap { text in if let text { Text(text) } else { nil }})

        case let .single(single):
            let pillDecoration: PredictionRowView.PillDecoration =
                if let route = single.route { .onDirectionDestination(route: route) } else { .none }
            let destination = single.headsign == nil || single.headsign?.isEmpty == true
                ? direction.destination
                : single.headsign
            DirectionRowView(
                direction: .init(
                    name: direction.name,
                    destination: destination,
                    id: direction.id
                ),
                predictions: single.format,
                pillDecoration: pillDecoration
            ).accessibilityElement(children: .combine)
                .accessibilityInputLabels(Set([DirectionLabel.directionNameFormatted(direction), destination])
                    .compactMap { text in if let text { Text(text) } else { nil }})
        }
    }
}
