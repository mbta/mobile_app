//
//  AlertDetails.swift
//  iosApp
//
//  Created by Simon, Emma on 8/7/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct AlertDetails: View {
    var analytics: AlertDetailsAnalytics = AnalyticsProvider.shared
    var alert: shared.Alert
    var line: Line?
    var routes: [Route]?
    var affectedStops: [Stop]
    var stopId: String?
    var now: Date

    @ScaledMetric()
    private var iconSize = 16

    @State var areStopsExpanded = false

    private var routeLabel: String {
        line?.longName ?? routes?.first?.label ?? ""
    }

    private var effectLabel: String? {
        // TODO: Add all possible alert effects if/when we start displaying non-disruption alerts here
        switch alert.effect {
        case .detour: NSLocalizedString("Detour", comment: "Possible alert effect")
        case .dockClosure: NSLocalizedString("Dock Closure", comment: "Possible alert effect")
        case .shuttle: NSLocalizedString("Shuttle", comment: "Possible alert effect")
        case .stationClosure: NSLocalizedString("Station Closure", comment: "Possible alert effect")
        case .stopClosure: NSLocalizedString("Stop Closure", comment: "Possible alert effect")
        case .stopMove, .stopMoved: NSLocalizedString("Station Moved", comment: "Possible alert effect")
        case .suspension: NSLocalizedString("Suspension", comment: "Possible alert effect")
        default: nil
        }
    }

    private var causeLabel: String? {
        switch alert.cause {
        case .accident: NSLocalizedString("Accident", comment: "Possible alert cause")
        case .amtrak: NSLocalizedString("Amtrak", comment: "Possible alert cause")
        case .anEarlierMechanicalProblem: NSLocalizedString(
                "An Earlier Mechanical Problem",
                comment: "Possible alert cause"
            )
        case .anEarlierSignalProblem: NSLocalizedString("An Earlier Signal Problem", comment: "Possible alert cause")
        case .autosImpedingService: NSLocalizedString("Autos Impeding Service", comment: "Possible alert cause")
        case .coastGuardRestriction: NSLocalizedString("Coast Guard Restriction", comment: "Possible alert cause")
        case .congestion: NSLocalizedString("Congestion", comment: "Possible alert cause")
        case .construction: NSLocalizedString("Construction", comment: "Possible alert cause")
        case .crossingMalfunction: NSLocalizedString("Crossing Malfunction", comment: "Possible alert cause")
        case .demonstration: NSLocalizedString("Demonstration", comment: "Possible alert cause")
        case .disabledBus: NSLocalizedString("Disabled Bus", comment: "Possible alert cause")
        case .disabledTrain: NSLocalizedString("Disabled Train", comment: "Possible alert cause")
        case .drawbridgeBeingRaised: NSLocalizedString("Drawbridge Being Raised", comment: "Possible alert cause")
        case .electricalWork: NSLocalizedString("Electrical Work", comment: "Possible alert cause")
        case .fire: NSLocalizedString("Fire", comment: "Possible alert cause")
        case .fog: NSLocalizedString("Fog", comment: "Possible alert cause")
        case .freightTrainInterference: NSLocalizedString("Freight Train Interference", comment: "Possible alert cause")
        case .hazmatCondition: NSLocalizedString("Hazmat Condition", comment: "Possible alert cause")
        case .heavyRidership: NSLocalizedString("Heavy Ridership", comment: "Possible alert cause")
        case .highWinds: NSLocalizedString("High Winds", comment: "Possible alert cause")
        case .holiday: NSLocalizedString("Holiday", comment: "Possible alert cause")
        case .hurricane: NSLocalizedString("Hurricane", comment: "Possible alert cause")
        case .iceInHarbor: NSLocalizedString("Ice In Harbor", comment: "Possible alert cause")
        case .maintenance: NSLocalizedString("Maintenance", comment: "Possible alert cause")
        case .mechanicalProblem: NSLocalizedString("Mechanical Problem", comment: "Possible alert cause")
        case .medicalEmergency: NSLocalizedString("Medical Emergency", comment: "Possible alert cause")
        case .parade: NSLocalizedString("Parade", comment: "Possible alert cause")
        case .policeAction: NSLocalizedString("Police Action", comment: "Possible alert cause")
        case .policeActivity: NSLocalizedString("Police Activity", comment: "Possible alert cause")
        case .powerProblem: NSLocalizedString("Power Problem", comment: "Possible alert cause")
        case .severeWeather: NSLocalizedString("Severe Weather", comment: "Possible alert cause")
        case .signalProblem: NSLocalizedString("Signal Problem", comment: "Possible alert cause")
        case .slipperyRail: NSLocalizedString("Slippery Rail", comment: "Possible alert cause")
        case .snow: NSLocalizedString("Snow", comment: "Possible alert cause")
        case .specialEvent: NSLocalizedString("Special Event", comment: "Possible alert cause")
        case .speedRestriction: NSLocalizedString("Speed Restriction", comment: "Possible alert cause")
        case .strike: NSLocalizedString("Strike", comment: "Possible alert cause")
        case .switchProblem: NSLocalizedString("Switch Problem", comment: "Possible alert cause")
        case .technicalProblem: NSLocalizedString("Technical Problem", comment: "Possible alert cause")
        case .tieReplacement: NSLocalizedString("Tie Replacement", comment: "Possible alert cause")
        case .trackProblem: NSLocalizedString("Track Problem", comment: "Possible alert cause")
        case .trackWork: NSLocalizedString("Track Work", comment: "Possible alert cause")
        case .traffic: NSLocalizedString("Traffic", comment: "Possible alert cause")
        case .unrulyPassenger: NSLocalizedString("Unruly Passenger", comment: "Possible alert cause")
        case .weather: NSLocalizedString("Weather", comment: "Possible alert cause")
        default: nil
        }
    }

    @ViewBuilder
    private var alertTitle: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let effectLabel {
                Text("\(routeLabel) \(effectLabel)",
                     comment: """
                     First value is the route label, second value is an alert effect, \
                     resulting in something like 'Red Line Suspension' or 'Green Line Shuttle'
                     """).font(.title2).bold()
            }
            if let causeLabel { Text(causeLabel).font(.body).bold() }
        }
    }

    private var currentPeriod: shared.Alert.ActivePeriod? {
        let nowInstant = now.toKotlinInstant()
        return alert.activePeriod.first { period in period.activeAt(instant: nowInstant) }
    }

    @ViewBuilder
    private var alertPeriod: some View {
        if let currentPeriod {
            HStack(alignment: .center, spacing: 8) {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Start", comment: "Label for the start date of a disruption")
                    Text("End", comment: "Label for the end date of a disruption")
                }.frame(minWidth: 48)
                VStack(alignment: .leading, spacing: 14) {
                    Text(currentPeriod.formatStart())
                    Text(currentPeriod.formatEnd())
                }.layoutPriority(1)
            }
        } else {
            Text("Alert is no longer in effect")
        }
    }

    @ViewBuilder
    private var affectedStopCollapsible: some View {
        if !affectedStops.isEmpty {
            asTile(DisclosureGroup(
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
                        Text("**\(affectedStops.count)** affected stops").multilineTextAlignment(.leading)
                        Spacer()
                        Image(.faChevronRight)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(height: iconSize)
                            .rotationEffect(.degrees(areStopsExpanded ? 90 : 0))
                    }.padding(.leading, 16).padding(.trailing, -2).padding(.vertical, 12)
                }
            ).foregroundStyle(Color.text, .clear).onChange(of: areStopsExpanded) { expanded in
                if expanded {
                    analytics.tappedAffectedStops(
                        routeId: line?.id ?? routes?.first?.id ?? "",
                        stopId: stopId ?? "",
                        alertId: alert.id
                    )
                }
            })
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
                routeId: line?.id ?? routes?.first?.id ?? "",
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
        if let header = alert.header {
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
                Text("Full Description", comment: "Header for the details of a disruption").bold()
                ForEach(alertDescriptionParagraphs, id: \.hashValue) { section in
                    Text(section).fixedSize(horizontal: false, vertical: true)
                }
            }
        }
    }

    @ViewBuilder
    private var alertFooter: some View {
        let updatedDate = alert.updatedAt.toNSDate()
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
                alertPeriod
                VStack(alignment: .leading, spacing: 16) {
                    affectedStopCollapsible
                    tripPlannerLink
                }
                alertDescription
                alertFooter
            }.padding(.horizontal, 16).padding(.top, 24)
        }
        if #available(iOS 16.4, *) {
            scrollContent.scrollBounceBehavior(.basedOnSize, axes: [.vertical])
        } else {
            scrollContent
        }
    }

    var body: some View {
        scrollContent
    }
}