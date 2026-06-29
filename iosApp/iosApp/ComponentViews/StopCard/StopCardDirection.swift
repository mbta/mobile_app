//
//  StopCardDirection.swift
//  iosApp
//
//  Created by Melody Horn on 6/25/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopCardDirection: View {
    @ObserveInjection var inject

    let direction: Direction
    let formatted: LeafFormat
    let lineOrRoute: LineOrRoute

    var body: some View {
        switch onEnum(of: formatted) {
        case let .branched(branched):
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .center) {
                    if let warningAlert = branched.warningAlert {
                        Image(warningAlert.iconName)
                            .accessibilityLabel("Alert")
                            .frame(width: 18, height: 18)
                    }
                    let noRoutePills = branched.branchRows.allSatisfy { $0.route == nil }
                    DirectionLabel(
                        direction: direction,
                        showDestination: false,
                        routeNamePrefix: noRoutePills ? nil : lineOrRoute.name
                    ).foregroundStyle(Color.text)
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
            }
            .accessibilityElement(children: .combine)
            .accessibilityInputLabels(Set([
                DirectionLabel.directionNameFormatted(direction),
                direction.destination,
            ] +
                Set(branched.branchRows.map(\.headsign)))
                .compactMap { text in if let text { Text(text) } else { nil }})
            .enableInjection()

        case let .single(single):
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
                pillDecoration: .onRow(route: single.route ?? lineOrRoute.sortRoute)
            )
            .accessibilityElement(children: .combine)
            .accessibilityInputLabels(Set([DirectionLabel.directionNameFormatted(direction), destination])
                .compactMap { text in if let text { Text(text) } else { nil }})
            .enableInjection()
        }
    }
}
