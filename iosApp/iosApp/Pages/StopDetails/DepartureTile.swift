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
    @ObserveInjection var inject
    var data: TileData
    var onTap: () -> Void
    var pillDecoration: PillDecoration = .none
    var isSelected: Bool = false

    enum PillDecoration {
        case none
        case onPrediction(route: Route)
    }

    /// Store the size that the tile should be for text to wrap properly.
    /// In order for wrapped text not to get cut off in the horizontal ScrollView, we need to set the fixed width
    /// rather than just using `maxWidth` (see https://stackoverflow.com/a/75331082)
    /// Calculating the size based on an approach taken in
    /// https://nilcoalescing.com/blog/AdaptiveLayoutsWithViewThatFits/#expandable-text-with-line-limit
    /// the ideal width is measured on the background in the initial render, then used on subsequent renders.
    @State var computedMultilineWidth: CGFloat? = nil

    let maxTileWidth: CGFloat = 195.0

    private var hasRoutePill: Bool {
        if case .onPrediction = pillDecoration { true } else { false }
    }

    private var hasPill: Bool { hasRoutePill || data.upcoming.trackChange != nil }

    var body: some View {
        DepartureTileContainer(
            onTap: onTap,
            isSelected: isSelected,
            maxWidth: maxTileWidth,
            accessibilityHint: NSLocalizedString(
                "displays more information about this trip",
                comment: "Screen reader hint for tapping a departure card in stop details"
            )
        ) {
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

                HStack(alignment: .center, spacing: 0) {
                    if let trackChange = data.upcoming.trackChange {
                        let type: TrackChangePill.TrackChangeType = switch onEnum(of: trackChange) {
                        case .alert: .alert
                        case let .scheduled(scheduled):
                            .scheduled(trackNumber: scheduled.platformCode, trackLabel: scheduled.platformName)
                        }
                        TrackChangePill(style: .compact(isDimmed: !isSelected), type: type)
                    }
                    if case let .onPrediction(route) = pillDecoration {
                        RoutePill(route: route, type: .flex)
                    }
                    if hasPill { Spacer(minLength: 8) }
                    TripStatus(predictions: data.formatted)
                }
            }
        }
        .enableInjection()
    }
}

/// Shown after the last ``DepartureTile`` when there are trips scheduled later than the last predicted trip, but we
/// don’t have predictions for them yet.
struct PredictionsNotAvailableYetTile: View {
    @ObserveInjection var inject
    var onTap: () -> Void
    var isSelected: Bool = false

    @ScaledMetric(relativeTo: .footnote) private var iconDiameter: CGFloat = 11
    /// Inter’s cap height is 1490 out of 2048 em (https://rsms.me/inter/#faq-technical-details),
    /// used to vertically center the icon on the text
    @ScaledMetric(relativeTo: .footnote) private var capHeight: CGFloat = 13 * (1490 / 2048)

    private var label: Text {
        Text(
            "Predictions not\navailable yet",
            comment: """
            Label for a departure tile shown after the last predicted trip when later trips are scheduled, or an informational header on an explainer card.
            The line break is intentional, the tile is sized to fit the longest line, so please put the break \
            where it gives two lines of roughly similar length (ideally towards the middle).
            Please keep it to two (or one) lines, since the tile is only a little taller than that.
            """
        )
    }

    var body: some View {
        DepartureTileContainer(
            onTap: onTap,
            isSelected: isSelected,
            maxWidth: nil,
            accessibilityHint: NSLocalizedString(
                "displays more information about later trips",
                comment: "Screen reader hint for tapping the predictions not available yet tile in stop details"
            )
        ) {
            let textColor = isSelected ? Color.text : Color.routeColorContrastText
            ZStack(alignment: .trailingLastTextBaseline) {
                (label + Text(verbatim: "\u{200D}\u{00A0}\u{00A0}\u{00A0}\u{00A0}\u{00A0}\u{200D}"))
                    .font(Typography.footnote)
                    .multilineTextAlignment(.leading)
                    .fixedSize()
                Image(.faCircleInfo)
                    .resizable()
                    .scaledToFit()
                    .frame(width: iconDiameter, height: iconDiameter)
                    .offset(y: (iconDiameter - capHeight) / 2)
                    .foregroundColor(textColor.opacity(0.5))
            }
        }
        .enableInjection()
    }
}

private struct DepartureTileContainer<Content: View>: View {
    var onTap: () -> Void
    var isSelected: Bool
    var maxWidth: CGFloat?
    var accessibilityHint: String
    @ViewBuilder var content: () -> Content

    var body: some View {
        let backgroundColor = isSelected ? Color.fill3 : Color.routeColorContrast

        Button(action: onTap) {
            content()
                .padding(.horizontal, 8)
                .padding(.vertical, 10)
                .frame(maxWidth: maxWidth, minHeight: 56, maxHeight: .infinity)
        }
        .background(backgroundColor)
        // Setting tint helps preserve contrast with accessibility setting "Show Borders" turned on
        .tint(backgroundColor)
        .foregroundStyle(isSelected ? Color.text : Color.routeColorContrastText)
        .clipShape(.rect(cornerRadius: 8))
        .padding(1)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(isSelected ? Color.halo : Color.clear, lineWidth: 2))
        .accessibilityAddTraits(isSelected ? [.isHeader, .isSelected, .updatesFrequently] : [])
        .accessibilityHeading(isSelected ? .h3 : .unspecified)
        .accessibilityHint(isSelected ? "" : accessibilityHint)
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
                                last: false,
                                headline: true,
                            ),
                            lastTrip: false,
                            alert: nil
                        ),
                    ], warningAlert: nil),
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
                        .init(
                            trip: upcomingTrip,
                            routeType: .bus,
                            format: .Minutes(minutes: 9, last: false),
                            lastTrip: false,
                            alert: nil
                        ),
                    ], warningAlert: nil),
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
                                    last: false,
                                    headline: true
                                ),
                                lastTrip: false,
                                alert: nil
                            ),
                        ],
                        warningAlert: nil
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
                        .init(
                            trip: upcomingTrip,
                            routeType: .lightRail,
                            format: .Minutes(minutes: 12, last: false),
                            lastTrip: false,
                            alert: nil
                        ),
                    ], warningAlert: nil),
                    upcoming: upcomingTrip
                ),
                onTap: {},
                pillDecoration: .onPrediction(route: routeB),
                isSelected: false
            )
            PredictionsNotAvailableYetTile(onTap: {})
        }
        .padding(16)
        .background(Color(hex: "00843D"))
        .fixedSize(horizontal: false, vertical: true)
    }
}
