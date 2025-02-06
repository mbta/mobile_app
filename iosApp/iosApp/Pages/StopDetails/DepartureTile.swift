//
//  DepartureTile.swift
//  iosApp
//
//  Created by esimon on 11/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct DepartureTile: View {
    var data: TileData
    var onTap: () -> Void
    var pillDecoration: PillDecoration = .none
    var showHeadsign: Bool = true
    var isSelected: Bool = false

    enum PillDecoration {
        case none
        case onPrediction(route: Route)
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 4) {
                if showHeadsign {
                    Text(data.headsign)
                        .font(Typography.footnoteSemibold)
                        .multilineTextAlignment(.leading)
                }
                HStack(spacing: 0) {
                    if case let .onPrediction(route) = pillDecoration {
                        RoutePill(route: route, type: .flex)
                        Spacer(minLength: 8)
                    }
                    TripStatus(predictions: data.formatted)
                }
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 10)
        .frame(minHeight: 56)
        .background(isSelected ? Color.fill3 : Color.deselectedToggle2.opacity(0.6))
        .foregroundStyle(isSelected ? Color.text : Color.deselectedToggleText)
        .clipShape(.rect(cornerRadius: 8))
        .padding(1)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(isSelected ? Color.halo : Color.clear, lineWidth: 2))
        .accessibilityAddTraits(isSelected ? [.isHeader, .isSelected, .updatesFrequently] : [])
        .accessibilityHeading(isSelected ? .h3 : .unspecified)
        .accessibilityHint(isSelected ? "" : NSLocalizedString(
            "displays more information about this trip",
            comment: "Screen reader hint for tapping a departure card in stop details"
        ))
    }
}
