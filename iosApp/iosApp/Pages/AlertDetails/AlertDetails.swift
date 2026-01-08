//
//  AlertDetails.swift
//  iosApp
//
//  Created by Simon, Emma on 8/7/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct AlertDetails: View {
    var analytics: Analytics = AnalyticsProvider.shared
    var alert: Shared.Alert
    var line: Line?
    var routes: [Route]?
    var stop: Stop?
    var affectedStops: [Stop]
    var stopId: String?
    var now: EasternTimeInstant

    @ScaledMetric()
    private var iconSize = 16

    @State var areStopsExpanded = false

    private var routeLabel: String? {
        line?.longName ?? routes?.first?.label
    }

    private var stopLabel: String? {
        stop?.name
    }

    private var isElevatorClosure: Bool {
        alert.effect == .elevatorClosure
    }

    private var effectLabel: String { alert.effect.effectString }

    private var causeLabel: String? { alert.cause.causeString }

    private var affectedStopsLabel: AttributedString {
        AttributedString.tryMarkdown(String(format: NSLocalizedString(
            "**%ld** affected stops",
            comment: "The number of stops affected by an alert"
        ), affectedStops.count))
    }

    @ViewBuilder var effectTitle: some View {
        if let routeLabel, !isElevatorClosure {
            Text("\(routeLabel) \(effectLabel)",
                 comment: """
                 First value is the route label, second value is an alert effect, \
                 resulting in something like 'Red Line Suspension' or 'Green Line Shuttle'
                 """)
        } else if let stopLabel {
            Text("\(stopLabel) \(effectLabel)")
        } else {
            Text(effectLabel)
        }
    }

    @ViewBuilder
    private var alertTitle: some View {
        VStack(alignment: .leading, spacing: 10) {
            effectTitle.font(Typography.title2Bold)
            if isElevatorClosure, let header = alert.header {
                Text(header).font(Typography.bodySemibold)
            } else if let causeLabel { Text(causeLabel).font(Typography.bodySemibold) }
        }
    }

    private var currentPeriod: Shared.Alert.ActivePeriod? {
        alert.currentPeriod(time: now)
    }

    @ViewBuilder
    private var alertPeriod: some View {
        if let currentPeriod {
            Grid(alignment: .leading, horizontalSpacing: 8, verticalSpacing: 14) {
                GridRow {
                    Text("Start", comment: "Label for the start date of a disruption")
                        .frame(minWidth: 48, alignment: .leading)
                    Text(currentPeriod.formatStart())
                }
                GridRow {
                    Text("End", comment: "Label for the end date of a disruption")
                    Text(currentPeriod.formatEnd())
                }
            }
        } else {
            Text("Alert is no longer in effect")
        }
    }

    @ViewBuilder
    private var affectedStopCollapsible: some View {
        if !affectedStops.isEmpty {
            asTile(
                DisclosureGroup(
                    isExpanded: $areStopsExpanded,
                    content: {
                        VStack(alignment: .leading, spacing: 0) {
                            ForEach(affectedStops, id: \.id) { stop in
                                Divider().background(Color.halo)
                                Text(stop.name).bold().padding(16)
                            }
                        }
                    },
                    label: {
                        HStack(alignment: .center, spacing: 16) {
                            Text(affectedStopsLabel).multilineTextAlignment(.leading)
                            Spacer()
                            Image(.faChevronRight)
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(height: iconSize)
                                .rotationEffect(.degrees(areStopsExpanded ? 90 : 0))
                        }.padding(.leading, 16).padding(.trailing, -2).padding(.vertical, 12)
                    }
                )
                .tint(.clear) // Hide default chevron
                .foregroundStyle(Color.text, .clear)
                .onChange(of: areStopsExpanded) { expanded in
                    if expanded {
                        analytics.tappedAffectedStops(
                            routeId: line?.id ?? routes?.first?.id,
                            stopId: stopId ?? "",
                            alertId: alert.id
                        )
                    }
                }
            )
        }
    }

    @ViewBuilder
    private var tripPlannerLink: some View {
        asTile(Link(
            destination: URL(string: "https://www.mbta.com/trip-planner")!
        ) {
            Text("Plan a route with Trip Planner").multilineTextAlignment(.leading).padding(16)
            Spacer()
            Image(.faRoute).resizable().frame(width: iconSize, height: iconSize).padding(16)
        }.environment(\.openURL, OpenURLAction { _ in
            analytics.tappedTripPlanner(
                routeId: line?.id ?? routes?.first?.id,
                stopId: stopId ?? "",
                alertId: alert.id
            )
            return .systemAction
        }))
    }

    private func asTile(_ view: some View) -> some View {
        view
            .background(Color.fill3)
            .foregroundStyle(Color.text)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 1))
    }

    private func splitDetails(_ details: String, separator: String? = nil) -> [String] {
        (separator != nil
            ? details.components(separatedBy: separator!)
            : details.components(separatedBy: .newlines))
            .filter { !$0.isEmpty }
            .map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }
    }

    private var alertDescriptionParagraphs: [String] {
        var paragraphs: [String] = []
        if let header = alert.header, !isElevatorClosure {
            paragraphs += splitDetails(header)
        }
        if let description = alert.description_ {
            paragraphs += splitDetails(description, separator: "\n\n")
                .filter { section in affectedStops.isEmpty || !section.hasPrefix("Affected stops:") }
        }
        return paragraphs
    }

    @ViewBuilder
    private var alertDescription: some View {
        if !alertDescriptionParagraphs.isEmpty {
            VStack(alignment: .leading, spacing: 16) {
                if isElevatorClosure {
                    Text("Alternative path", comment: "Header for the details of an elevator closure")
                        .font(Typography.bodySemibold)
                } else {
                    Text("Full Description", comment: "Header for the details of a disruption")
                        .font(Typography.bodySemibold)
                }
                ForEach(alertDescriptionParagraphs, id: \.hashValue) { section in
                    Text(section).fixedSize(horizontal: false, vertical: true)
                }
            }
        }
    }

    @ViewBuilder
    private var alertFooter: some View {
        let updatedDate = alert.updatedAt
        let formattedTimestamp = updatedDate.formatted(date: .numeric, time: .shortened)
        VStack(alignment: .leading, spacing: 16) {
            Divider().background(Color.halo)
            Text(
                "Updated: \(formattedTimestamp)",
                comment: "Interpolated value is a timestamp for when displayed alert details were last changed"
            ).foregroundStyle(Color.deemphasized)
        }
    }

    @ViewBuilder
    private var scrollContent: some View {
        let scrollContent = ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                alertTitle
                if !isElevatorClosure {
                    alertPeriod
                    VStack(alignment: .leading, spacing: 16) {
                        affectedStopCollapsible
                        tripPlannerLink
                    }
                }
                alertDescription
                if isElevatorClosure {
                    alertPeriod
                }
                alertFooter
            }.padding(.horizontal, 16).padding(.top, 24)
        }
        scrollContent.scrollBounceBehavior(.basedOnSize, axes: [.vertical])
    }

    var body: some View {
        scrollContent
            .font(Typography.body)
    }
}

#Preview("Suspension") {
    let now = EasternTimeInstant.now()
    let objects = ObjectCollectionBuilder()
    let route = objects.route { route in
        route.color = "ED8B00"
        route.textColor = "FFFFFF"
        route.longName = "Orange Line"
    }
    let stop = objects.stop { _ in }
    let alert = objects.alert { alert in
        alert.effect = .suspension
        alert.cause = .maintenance
        alert.activePeriod(
            start: now.minus(hours: 3 * 24),
            end: now.plus(hours: 3 * 24)
        )
        alert.description_ =
            """
            Orange Line service between Ruggles and Jackson Square will be suspended from Thursday, May 23 through \
            Friday, May 31.

            An accessible van will be available for riders. Please see Station Personnel or Transit Ambassadors for \
            assistance.

            The Haverhill Commuter Rail Line will be Fare Free between Oak Grove, Malden Center, and North Station \
            during this work.
            """
        alert.updatedAt = now.minus(minutes: 10)
    }
    return AlertDetails(
        alert: alert,
        routes: [route],
        affectedStops: [stop],
        stopId: stop.id,
        now: now
    )
}

#Preview("Elevator Closure") {
    let now = EasternTimeInstant.now()
    let objects = ObjectCollectionBuilder()
    let route = objects.route { route in
        route.color = "DA291C"
        route.textColor = "FFFFFF"
        route.longName = "Red Line"
    }
    let stop = objects.stop { $0.name = "Park Street" }
    let alert = objects.alert { alert in
        alert.effect = .elevatorClosure
        alert.header =
            "Elevator 804 (Government Center & North lobby to Tremont Street, Winter Street)"
                + "unavailable on Thu Feb 6 due to maintenance"
        alert.cause = .maintenance
        alert.activePeriod(
            start: now.minus(hours: 3 * 24),
            end: nil
        )
        alert.description_ =
            """
            To exit, travel down the Winter St concourse towards Downtown Crossing. At the end of the concourse, exit \
            through the faregate and use Downtown Crossing Elevator 892 to access the corner of Washington St and \
            Winter St. To board, travel down Winter St towards Downtown Crossing to the corner of Washington St and \
            Winter St, and use Downtown Crossing 892 to access the Downtown Crossing lobby. At the lobby, exit \
            through the faregate, turn left and proceed up the Winter St concourse to Park Street. Alternatively, \
            cross Tremont St to the Boston Common and use Park Street Elevator 978 to access the Green Line Copley \
            and westbound platform.
            """
        alert.updatedAt = now.minus(minutes: 10)
    }
    return AlertDetails(
        alert: alert,
        routes: [route],
        stop: stop,
        affectedStops: [stop],
        stopId: stop.id,
        now: now
    )
}
