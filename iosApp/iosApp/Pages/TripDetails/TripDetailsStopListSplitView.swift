//
//  TripDetailsStopListSplitView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-17.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct TripDetailsStopListSplitView: View {
    let splitStops: TripDetailsStopList.TargetSplit
    let now: Instant
    let onTapLink: (SheetNavigationStackEntry, TripDetailsStopList.Entry, String?) -> Void
    let routeType: RouteType?

    private var routeTypeText: String { routeType?.typeText(isOnly: true) ?? "" }
    private var stopsAway: Int { splitStops.collapsedStops.count }
    private var target: TripDetailsStopList.Entry { splitStops.targetStop }

    var body: some View {
        List {
            if !splitStops.collapsedStops.isEmpty {
                DisclosureGroup(
                    content: {
                        ForEach(splitStops.collapsedStops, id: \.stopSequence) { stop in
                            TripDetailsStopView(
                                stop: stop,
                                now: now,
                                onTapLink: onTapLink,
                                routeType: routeType
                            )
                        }
                    },
                    label: { Text(
                        "\(stopsAway, specifier: "%ld") stops away",
                        comment: "How many stops away the vehicle is from the target stop"
                    ) }
                )
                .padding(.bottom, 16)
                .accessibilityElement()
                .accessibilityAddTraits(.isHeader)
                .accessibilityHeading(.h2)
                .accessibilityLabel(Text(
                    "\(routeTypeText) is \(stopsAway, specifier: "%ld") stops away from \(target.stop.name)",
                    comment: """
                    VoiceOver label for how many stops away a vehicle is from a stop,
                    ex 'bus is 4 stops away from Harvard'
                    """
                ))
                .accessibilityHint(Text(
                    "List remaining stops",
                    comment: """
                    VoiceOver hint explaining what happens when 'x stops away'
                    is selected (open an accordion listing those stops)
                    """
                ))
            }
            TripDetailsStopView(stop: target, now: now, onTapLink: onTapLink, routeType: routeType)
                .listRowBackground(Color.keyInverse.opacity(0.15))
            ForEach(splitStops.followingStops, id: \.stopSequence) { stop in
                TripDetailsStopView(stop: stop, now: now, onTapLink: onTapLink, routeType: routeType)
            }
        }.listStyle(.plain)
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    let stop1 = objects.stop { $0.name = "A" }
    let stop2 = objects.stop { $0.name = "B" }
    let stop3 = objects.stop { $0.name = "C" }

    let pred1 = objects.prediction { $0.departureTime = Date.now.toKotlinInstant() }
    let pred2 = objects.prediction { $0.departureTime = Date.now.addingTimeInterval(60).toKotlinInstant() }
    let pred3 = objects.prediction { $0.departureTime = Date.now.addingTimeInterval(2 * 60).toKotlinInstant() }

    func entry(_ stop: Stop, _ stopSequence: Int, _ prediction: Prediction) -> TripDetailsStopList.Entry {
        TripDetailsStopList.Entry(
            stop: stop,
            stopSequence: Int32(stopSequence),
            alert: nil,
            schedule: nil,
            prediction: prediction,
            predictionStop: nil,
            vehicle: nil,
            routes: []
        )
    }

    return TripDetailsStopListSplitView(
        splitStops: .init(
            firstStop: nil,
            collapsedStops: [entry(stop1, 10, pred1)],
            targetStop: entry(stop2, 20, pred2),
            followingStops: [entry(stop3, 30, pred3)]
        ),
        now: Date.now.toKotlinInstant(),
        onTapLink: { _, _, _ in },
        routeType: nil
    ).font(Typography.body)
}
