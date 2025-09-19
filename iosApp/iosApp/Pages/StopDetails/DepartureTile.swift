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

    // Store the size that the tile should be for text to wrap properly.
    // In order for wrapped text not to get cut off in the horizontal ScrollView, we need to set the fixed width
    // rather than just using `maxWidth` (see https://stackoverflow.com/a/75331082)
    // Calculating the size based on an approach taken in
    // https://nilcoalescing.com/blog/AdaptiveLayoutsWithViewThatFits/#expandable-text-with-line-limit
    // the ideal width is measured on the background in the initial render, then used on subsequent renders.
    @State var computedMultilineWidth: CGFloat? = nil

    let maxTileWidth: CGFloat = 195.0

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 4) {
                if let headsign = data.headsign {
                    if let computedMultilineWidth {
                        Text(headsign)
                            .fixedSize(horizontal: false, vertical: true)
                            .frame(width: computedMultilineWidth)
                            .font(Typography.footnoteSemibold)
                            .multilineTextAlignment(.leading)
                    } else {
                        Text(headsign)
                            .lineLimit(1)
                            .frame(maxWidth: maxTileWidth)
                            .font(Typography.footnoteSemibold)
                            .multilineTextAlignment(.leading)
                            .background {
                                Text(headsign).fixedSize(horizontal: false, vertical: true)
                                    .font(Typography.footnoteSemibold)
                                    .background(
                                        GeometryReader { geo in
                                            Color.clear.onAppear {
                                                let width = geo.size.width
                                                /*
                                                 Sometimes at large text sizes, when navigating from unfiltered stop
                                                 details, the first TYPICAL_LEAF_ROWS or BRANCHING_LEAF_ROWS end up
                                                 with a geo.size.width of 0.0. This results in the tile stretching
                                                 very tall with no visible headsign. If for some reason the width is
                                                 0.0, set it to the max width of 195 so that it at least renders.
                                                 */
                                                if width > 0.0, width < maxTileWidth {
                                                    computedMultilineWidth = width
                                                } else {
                                                    computedMultilineWidth = maxTileWidth
                                                }
                                            }
                                        }
                                    ).hidden()
                            }
                    }
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
        .frame(maxWidth: maxTileWidth, minHeight: 56, maxHeight: .infinity)
        .background(isSelected ? Color.fill3 : Color.routeColorContrast)
        .foregroundStyle(isSelected ? Color.text : Color.routeColorContrastText)
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
    let trip = objects.trip { _ in }
    let upcomingTrip = UpcomingTrip(trip: trip)

    ScrollView(.horizontal) {
        HStack {
            DepartureTile(
                data: .init(
                    route: route1,
                    headsign: nil,
                    formatted: UpcomingFormat.Some(trips: [
                        .init(
                            trip: upcomingTrip,
                            routeType: .commuterRail,
                            format: .TimeWithStatus(
                                predictionTime: EasternTimeInstant.now(),
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
                    route: route1,
                    headsign: nil,
                    formatted: UpcomingFormat.Some(
                        trips: [
                            .init(
                                trip: upcomingTrip,
                                routeType: .commuterRail,
                                format: .ScheduleTimeWithStatusColumn(
                                    scheduledTime: .now().minus(minutes: 15),
                                    status: "Delay",
                                    headline: true
                                )
                            ),
                        ],
                        secondaryAlert: nil
                    ),
                    upcoming: upcomingTrip
                ),
                onTap: {},
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
        .fixedSize(horizontal: false, vertical: true)
    }
}
