//
//  TripStops.swift
//  iosApp
//
//  Created by esimon on 12/3/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct TripStops: View {
    let targetId: String
    let stops: TripDetailsStopList
    let stopSequence: Int?
    let now: Date
    let onTapLink: (SheetNavigationStackEntry, TripDetailsStopList.Entry, String?) -> Void
    let routeType: RouteType?

    let splitStops: TripDetailsStopList.TargetSplit?

    private var routeTypeText: String { routeType?.typeText(isOnly: true) ?? "" }
    private var stopsAway: Int? { splitStops?.collapsedStops.count }
    private var target: TripDetailsStopList.Entry? { splitStops?.targetStop }

    init(
        targetId: String,
        stops: TripDetailsStopList,
        stopSequence: Int?,
        now: Date,
        onTapLink: @escaping (SheetNavigationStackEntry, TripDetailsStopList.Entry, String?) -> Void,
        routeType: RouteType?,
        global: GlobalResponse?
    ) {
        self.targetId = targetId
        self.stops = stops
        self.stopSequence = stopSequence
        self.now = now
        self.onTapLink = onTapLink
        self.routeType = routeType

        splitStops = if let stopSequence, let global {
            stops.splitForTarget(
                targetStopId: targetId,
                targetStopSequence: Int32(stopSequence),
                globalData: global
            )
        } else { nil }
    }

    @ViewBuilder
    func stopList(list: [TripDetailsStopList.Entry]) -> some View {
        ForEach(list, id: \.stopSequence) { stop in
            TripDetailsStopView(
                stop: stop,
                now: now.toKotlinInstant(),
                onTapLink: onTapLink,
                routeType: routeType
            )
        }
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .center, spacing: 0) {
                if let splitStops, let target {
                    if !splitStops.collapsedStops.isEmpty, let stopsAway {
                        DisclosureGroup(
                            content: { stopList(list: splitStops.collapsedStops) },
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
                    TripDetailsStopView(
                        stop: target,
                        now: now.toKotlinInstant(),
                        onTapLink: onTapLink,
                        routeType: routeType
                    )
                    .listRowBackground(Color.keyInverse.opacity(0.15))
                    stopList(list: splitStops.followingStops)
                } else {
                    stopList(list: stops.stops)
                }
            }
            .padding(.top, 16)
            .background(Color.fill2)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .padding(1)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 2))
            .padding(.horizontal, 10)
            .padding(.bottom, 48)
        }
    }
}
