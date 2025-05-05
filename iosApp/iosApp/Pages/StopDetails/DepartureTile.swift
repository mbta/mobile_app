//
//  DepartureTile.swift
//  iosApp
//
//  Created by esimon on 11/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct DepartureTile: View {
    var data: TileData
    var onTap: () -> Void
    var pillDecoration: PillDecoration = .none
    var isSelected: Bool = false

    enum PillDecoration {
        case none
        case onPrediction(route: Route)
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 4) {
                if let headsign = data.headsign {
                    Text(headsign)
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
        .simultaneousGesture(TapGesture())
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

#Preview {
    let objects = ObjectCollectionBuilder()
    let route1 = objects.route { _ in }
    let routeB = objects.route { route in
        route.color = "00843D"
        route.longName = "Green Line B"
        route.shortName = "B"
        route.textColor = "FFFFFF"
        route.type = .lightRail
    }
    let upcomingTrip = objects.upcomingTrip()

    HStack {
        DepartureTile(
            data: .init(
                route: route1,
                headsign: "Framingham",
                formatted: UpcomingFormat.Some(trips: [
                    .init(
                        trip: upcomingTrip,
                        routeType: .commuterRail,
                        format: .TimeWithStatus(
                            predictionTime: Date.now.toKotlinInstant(),
                            status: "Delay",
                            headline: true
                        )
                    ),
                ], secondaryAlert: nil),
                upcoming: upcomingTrip
            ),
            onTap: {},
            isSelected: true
        )
        DepartureTile(
            data: .init(
                route: route1,
                headsign: "Harvard",
                formatted: UpcomingFormat.Some(trips: [
                    .init(trip: upcomingTrip, routeType: .bus, format: .Minutes(minutes: 9)),
                ], secondaryAlert: nil),
                upcoming: upcomingTrip
            ),
            onTap: {},
            isSelected: false
        )
        DepartureTile(
            data: .init(
                route: routeB,
                headsign: "Government Center",
                formatted: UpcomingFormat.Some(trips: [
                    .init(trip: upcomingTrip, routeType: .lightRail, format: .Minutes(minutes: 12)),
                ], secondaryAlert: nil),
                upcoming: upcomingTrip
            ),
            onTap: {},
            pillDecoration: .onPrediction(route: routeB),
            isSelected: false
        )
    }
    .padding(16)
    .background(Color(hex: "00843D"))
    .fixedSize(horizontal: true, vertical: true)
}
