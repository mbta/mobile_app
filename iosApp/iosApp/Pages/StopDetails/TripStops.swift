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
    let routeAccents: TripRouteAccents

    let splitStops: TripDetailsStopList.TargetSplit?

    @State private var stopsExpanded = false

    private var routeTypeText: String { routeAccents.type.typeText(isOnly: true) }
    private var stopsAway: Int? { splitStops?.collapsedStops.count }
    private var target: TripDetailsStopList.Entry? { splitStops?.targetStop }

    init(
        targetId: String,
        stops: TripDetailsStopList,
        stopSequence: Int?,
        now: Date,
        onTapLink: @escaping (SheetNavigationStackEntry, TripDetailsStopList.Entry, String?) -> Void,
        routeAccents: TripRouteAccents,
        global: GlobalResponse?
    ) {
        self.targetId = targetId
        self.stops = stops
        self.stopSequence = stopSequence
        self.now = now
        self.onTapLink = onTapLink
        self.routeAccents = routeAccents

        splitStops = if let stopSequence, let global {
            stops.splitForTarget(
                targetStopId: targetId,
                targetStopSequence: Int32(stopSequence),
                globalData: global,
                combinedStopDetails: true
            )
        } else { nil }
    }

    @ViewBuilder
    func stopList(list: [TripDetailsStopList.Entry]) -> some View {
        ForEach(list, id: \.stopSequence) { stop in
            TripStopRow(
                stop: stop,
                now: now.toKotlinInstant(),
                onTapLink: onTapLink,
                routeAccents: routeAccents,
                lastStop: stop.stopSequence == stops.stops.last?.stopSequence
            )
        }
    }

    @ViewBuilder
    var routeLineTwist: some View {
        VStack(spacing: 0) {
            ColoredRouteLine(routeAccents.color)
            ZStack {
                Image(.stopTripLineTwist).foregroundStyle(routeAccents.color)
                Image(.stopTripLineTwistShadow)
            }
            ColoredRouteLine(routeAccents.color)
        }
    }

    var body: some View {
        VStack(alignment: .center, spacing: 0) {
            if let splitStops, let target {
                if !splitStops.collapsedStops.isEmpty, let stopsAway {
                    DisclosureGroup(
                        isExpanded: $stopsExpanded,
                        content: {
                            VStack(spacing: 0) {
                                HaloSeparator().overlay(alignment: .leading) {
                                    // Lil 1x4 pt route color bar to maintain an
                                    // unbroken route color line over the separator
                                    ColoredRouteLine(routeAccents.color).padding(.leading, 42)
                                }
                                stopList(list: splitStops.collapsedStops)
                            }
                        },
                        label: {
                            HStack(spacing: 0) {
                                VStack(spacing: 0) {
                                    if stopsExpanded {
                                        ColoredRouteLine(routeAccents.color)
                                    } else {
                                        routeLineTwist
                                    }
                                }
                                .transition(.opacity.animation(.easeInOut(duration: 0.2)))
                                .frame(minWidth: 24)

                                Text(
                                    "\(stopsAway, specifier: "%ld") stops away",
                                    comment: "How many stops away the vehicle is from the target stop"
                                )
                                .foregroundStyle(Color.text)
                                .padding(.leading, 16)
                                Spacer()
                            }.frame(maxWidth: .infinity, minHeight: 56)
                        }
                    )
                    .disclosureGroupStyle(.tripDetails)
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
                if target != stops.startTerminalEntry, target.vehicle != nil {
                    // If the target is the first stop and there's no vehicle,
                    // it's already displayed in the trip header
                    TripStopRow(
                        stop: target,
                        now: now.toKotlinInstant(),
                        onTapLink: onTapLink,
                        routeAccents: routeAccents,
                        targeted: true
                    )
                    .background(Color.fill3)
                }
                stopList(list: splitStops.followingStops)
            } else {
                stopList(list: stops.stops)
            }
        }
        .padding(.top, 56)
        .overlay(alignment: .topLeading) {
            ColoredRouteLine(routeAccents.color).frame(maxHeight: 56).padding(.leading, 42)
        }
        .background(Color.fill2)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .padding(1)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 2))
        .padding(.horizontal, 10)
        .padding(.bottom, 48)
    }
}
