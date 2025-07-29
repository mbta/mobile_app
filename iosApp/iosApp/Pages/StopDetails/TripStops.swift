//
//  TripStops.swift
//  iosApp
//
//  Created by esimon on 12/3/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct TripStops: View {
    let targetId: String
    let stops: TripDetailsStopList
    let stopSequence: Int?
    let headerSpec: TripHeaderSpec?
    let now: EasternTimeInstant
    let alertSummaries: [String: AlertSummary?]
    let onTapLink: (TripDetailsStopList.Entry) -> Void
    let onOpenAlertDetails: (Shared.Alert) -> Void
    let routeAccents: TripRouteAccents
    let splitStops: TripDetailsStopList.TargetSplit

    @State private var stopsExpanded = false

    private var routeTypeText: String { routeAccents.type.typeText(isOnly: true) }
    private var collapsedStops: [TripDetailsStopList.Entry]? { splitStops.collapsedStops }
    private var stopsAway: Int? { collapsedStops?.count }
    private var target: TripDetailsStopList.Entry? { splitStops.targetStop }
    private var hideTarget: Bool {
        if case .scheduled = headerSpec, target != nil, target == stops.startTerminalEntry { true } else { false }
    }

    init(
        targetId: String,
        stops: TripDetailsStopList,
        stopSequence: Int?,
        headerSpec: TripHeaderSpec?,
        now: EasternTimeInstant,
        alertSummaries: [String: AlertSummary?],
        onTapLink: @escaping (TripDetailsStopList.Entry) -> Void,
        onOpenAlertDetails: @escaping (Shared.Alert) -> Void,
        routeAccents: TripRouteAccents,
        global: GlobalResponse?
    ) {
        self.targetId = targetId
        self.stops = stops
        self.stopSequence = stopSequence
        self.headerSpec = headerSpec
        self.now = now
        self.alertSummaries = alertSummaries
        self.onTapLink = onTapLink
        self.onOpenAlertDetails = onOpenAlertDetails
        self.routeAccents = routeAccents

        splitStops = stops.splitForTarget(
            targetStopId: targetId,
            targetStopSequence: KotlinInt(value: stopSequence),
            globalData: global
        )
    }

    var showFirstStopSeparately: Bool {
        switch headerSpec {
        case .finishingAnotherTrip, .noVehicle: true
        default: false
        }
    }

    @ViewBuilder
    func stopList(list: [TripDetailsStopList.Entry], showDownstreamAlerts: Bool = false) -> some View {
        ForEach(list, id: \.stopSequence) { stop in
            TripStopRow(
                stop: stop,
                trip: stops.trip,
                now: now,
                onTapLink: onTapLink,
                onOpenAlertDetails: onOpenAlertDetails,
                routeAccents: routeAccents,
                alertSummaries: alertSummaries,
                showDownstreamAlert: showDownstreamAlerts,
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
        ZStack {
            Color.fill2
                .padding(1)
                .withRoundedBorder(color: Color.halo, width: 2)
                .padding(.horizontal, 6)
                .padding(.bottom, splitStops.followingStops.last?.disruption?.alert.significance == .major ? 6 : 0)
            VStack(alignment: .center, spacing: 0) {
                if showFirstStopSeparately, let firstStop = splitStops.firstStop {
                    TripStopRow(
                        stop: firstStop,
                        trip: stops.trip,
                        now: now,
                        onTapLink: onTapLink,
                        onOpenAlertDetails: onOpenAlertDetails,
                        routeAccents: routeAccents,
                        alertSummaries: alertSummaries,
                        firstStop: true
                    )
                }
                if let collapsedStops, !collapsedStops.isEmpty, let stopsAway, let target {
                    DisclosureGroup(
                        isExpanded: $stopsExpanded,
                        content: {
                            VStack(spacing: 0) {
                                HaloSeparator().overlay(alignment: .leading) {
                                    if !showFirstStopSeparately {
                                        // Lil 1x4 pt route color bar to maintain an
                                        // unbroken route color line over the separator
                                        ColoredRouteLine(routeAccents.color).padding(.leading, 42)
                                    }
                                }
                                .padding(.horizontal, 6)
                                stopList(list: collapsedStops)
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
                                .accessibilityLabel(Text(
                                    "\(routeTypeText) is \(stopsAway, specifier: "%ld") stops away from \(target.stop.name)",
                                    comment: """
                                    VoiceOver label for how many stops away a vehicle is from a stop,
                                    ex 'bus is 4 stops away from Harvard'
                                    """
                                ))
                                .accessibilityHint(stopsExpanded ? Text(
                                    "Hides remaining stops",
                                    comment: """
                                    Screen reader hint explaining what happens when 'x stops away'
                                    is selected when it's already open (closes the accordion listing those stops)
                                    """
                                ) : Text(
                                    "Lists remaining stops",
                                    comment: """
                                    Screen reader hint explaining what happens when 'x stops away'
                                    is selected (open an accordion listing those stops)
                                    """
                                ))
                                .accessibilityAddTraits(.updatesFrequently)
                                Spacer()
                            }
                            .frame(maxWidth: .infinity, minHeight: 56)
                        }
                    )
                    .disclosureGroupStyle(.tripDetails)
                }
                if let target, !hideTarget {
                    // If the target is the first stop and there's no vehicle,
                    // it's already displayed in the trip header
                    TripStopRow(
                        stop: target,
                        trip: stops.trip,
                        now: now,
                        onTapLink: onTapLink,
                        onOpenAlertDetails: onOpenAlertDetails,
                        routeAccents: routeAccents,
                        alertSummaries: alertSummaries,
                        targeted: true,
                        firstStop: showFirstStopSeparately && target == stops.startTerminalEntry,
                        background: Color.fill3
                    )
                }
                stopList(list: splitStops.followingStops, showDownstreamAlerts: true)
            }
            .padding(.top, 56)
            .overlay(alignment: .topLeading) {
                if !showFirstStopSeparately {
                    ColoredRouteLine(routeAccents.color).frame(maxHeight: 56).padding(.leading, 42)
                }
            }
        }
        .padding(.horizontal, 10)
        .padding(.bottom, 48)
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    let route = objects.route { route in
        route.color = "FFC72C"
        route.shortName = "109"
        route.textColor = "000000"
        route.type = .bus
    }
    let stops = (1 ... 10).map { index in
        objects.stop { stop in
            stop.name = "Stop \(index)"
            stop.wheelchairBoarding = .accessible
        }
    }
    let trip = objects.trip { _ in }
    let now = EasternTimeInstant.now()
    let alertStartIndex = 7
    let alert = objects.alert { $0.effect = .shuttle }
    let stopList = TripDetailsStopList(
        trip: trip,
        stops: stops.enumerated().map { index, stop in
            .init(
                stop: stop,
                stopSequence: Int32(index),
                disruption: index >= alertStartIndex ? .init(alert: alert, iconName: "alert-borderless-suspension") :
                    nil,
                schedule: nil,
                prediction: objects
                    .prediction { $0.departureTime = now.plus(minutes: Int32(2 * index)) },
                vehicle: nil,
                routes: []
            )
        }
    )

    VStack {
        TripStops(
            targetId: stops[4].id,
            stops: stopList,
            stopSequence: 4,
            headerSpec: .noVehicle,
            now: now,
            alertSummaries: [:],
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: .init(route: route),
            global: .init(objects: objects)
        )
    }
    .padding(12)
    .font(Typography.body)
    .withFixedSettings([:])
}
