//
//  FormattedAlert.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-16.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared

struct FormattedAlert: Equatable {
    let alert: Alert
    let alertSummary: AlertSummary?
    let effect: String
    let sentenceCaseEffect: String
    let dueToCause: String?
    /// Represents the text and possible accessibility label that would be used if replacing predictions. Does not
    /// guarantee that the alert should replace predictions.
    let predictionReplacement: PredictionReplacement

    // swiftlint:disable:next cyclomatic_complexity
    init(alert: Alert, alertSummary: AlertSummary? = nil) {
        self.alert = alert
        let effect = switch alert.effect {
        case .accessIssue: NSLocalizedString("Access Issue", comment: "Possible alert effect")
        case .additionalService: NSLocalizedString("Additional Service", comment: "Possible alert effect")
        case .amberAlert: NSLocalizedString("Amber Alert", comment: "Possible alert effect")
        case .bikeIssue: NSLocalizedString("Bike Issue", comment: "Possible alert effect")
        case .cancellation: NSLocalizedString("Cancellation", comment: "Possible alert effect")
        case .delay: NSLocalizedString("Delay", comment: "Possible alert effect")
        case .detour: NSLocalizedString("Detour", comment: "Possible alert effect")
        case .dockClosure: NSLocalizedString("Dock Closure", comment: "Possible alert effect")
        case .dockIssue: NSLocalizedString("Dock Issue", comment: "Possible alert effect")
        case .elevatorClosure: NSLocalizedString("Elevator Closure", comment: "Possible alert effect")
        case .escalatorClosure: NSLocalizedString("Escalator Closure", comment: "Possible alert effect")
        case .extraService: NSLocalizedString("Extra Service", comment: "Possible alert effect")
        case .facilityIssue: NSLocalizedString("Facility Issue", comment: "Possible alert effect")
        case .modifiedService: NSLocalizedString("Modified Service", comment: "Possible alert effect")
        case .noService: NSLocalizedString("No Service", comment: "Possible alert effect")
        case .parkingClosure: NSLocalizedString("Parking Closure", comment: "Possible alert effect")
        case .parkingIssue: NSLocalizedString("Parking Issue", comment: "Possible alert effect")
        case .policyChange: NSLocalizedString("Policy Change", comment: "Possible alert effect")
        case .scheduleChange: NSLocalizedString("Schedule Change", comment: "Possible alert effect")
        case .serviceChange: NSLocalizedString("Service Change", comment: "Possible alert effect")
        case .shuttle: NSLocalizedString("Shuttle", comment: "Possible alert effect")
        case .snowRoute: NSLocalizedString("Snow Route", comment: "Possible alert effect")
        case .stationClosure: NSLocalizedString("Station Closure", comment: "Possible alert effect")
        case .stationIssue: NSLocalizedString("Station Issue", comment: "Possible alert effect")
        case .stopClosure: NSLocalizedString("Stop Closure", comment: "Possible alert effect")
        case .stopMove, .stopMoved: NSLocalizedString("Stop Moved", comment: "Possible alert effect")
        case .stopShoveling: NSLocalizedString("Stop Shoveling", comment: "Possible alert effect")
        case .summary: NSLocalizedString("Summary", comment: "Possible alert effect")
        case .suspension: NSLocalizedString("Suspension", comment: "Possible alert effect")
        case .trackChange: NSLocalizedString("Track Change", comment: "Possible alert effect")
        case .otherEffect, .unknownEffect: NSLocalizedString("Alert", comment: "Possible alert")
        }
        self.effect = "**\(effect)**"

        sentenceCaseEffect = switch alert.effect {
        case .accessIssue: NSLocalizedString("Access issue", comment: "Possible alert effect")
        case .additionalService: NSLocalizedString("Additional service", comment: "Possible alert effect")
        case .amberAlert: NSLocalizedString("Amber alert", comment: "Possible alert effect")
        case .bikeIssue: NSLocalizedString("Bike issue", comment: "Possible alert effect")
        case .cancellation: NSLocalizedString("Trip cancelled", comment: "Possible alert effect")
        case .delay: NSLocalizedString("Delay", comment: "Possible alert effect")
        case .detour: NSLocalizedString("Detour", comment: "Possible alert effect")
        case .dockClosure: NSLocalizedString("Dock closed", comment: "Possible alert effect")
        case .dockIssue: NSLocalizedString("Dock issue", comment: "Possible alert effect")
        case .elevatorClosure: NSLocalizedString("Elevator closed", comment: "Possible alert effect")
        case .escalatorClosure: NSLocalizedString("Escalator closed", comment: "Possible alert effect")
        case .extraService: NSLocalizedString("Extra service", comment: "Possible alert effect")
        case .facilityIssue: NSLocalizedString("Facility issue", comment: "Possible alert effect")
        case .modifiedService: NSLocalizedString("Modified service", comment: "Possible alert effect")
        case .noService: NSLocalizedString("No service", comment: "Possible alert effect")
        case .parkingClosure: NSLocalizedString("Parking closed", comment: "Possible alert effect")
        case .parkingIssue: NSLocalizedString("Parking issue", comment: "Possible alert effect")
        case .policyChange: NSLocalizedString("Policy change", comment: "Possible alert effect")
        case .scheduleChange: NSLocalizedString("Schedule change", comment: "Possible alert effect")
        case .serviceChange: NSLocalizedString("Service change", comment: "Possible alert effect")
        case .shuttle: NSLocalizedString("Shuttle buses", comment: "Possible alert effect")
        case .snowRoute: NSLocalizedString("Snow route", comment: "Possible alert effect")
        case .stationClosure: NSLocalizedString("Station closed", comment: "Possible alert effect")
        case .stationIssue: NSLocalizedString("Station issue", comment: "Possible alert effect")
        case .stopClosure: NSLocalizedString("Stop closed", comment: "Possible alert effect")
        case .stopMove, .stopMoved: NSLocalizedString("Stop moved", comment: "Possible alert effect")
        case .stopShoveling: NSLocalizedString("Stop shoveling", comment: "Possible alert effect")
        case .summary: NSLocalizedString("Summary", comment: "Possible alert effect")
        case .suspension: NSLocalizedString("Service suspended", comment: "Possible alert effect")
        case .trackChange: NSLocalizedString("Track change", comment: "Possible alert effect")
        case .otherEffect, .unknownEffect: NSLocalizedString("Alert", comment: "Possible alert effect")
        }

        dueToCause = dueToCauseLabel(alert.cause)

        // a handful of cases have different text when replacing predictions than in a details title
        predictionReplacement = switch alert.effect {
        case .dockClosure: .init(text: NSLocalizedString("Dock Closed", comment: "Possible alert effect"))
        case .shuttle: .init(
                text: NSLocalizedString("Shuttle Bus", comment: "Possible alert effect"),
                accessibilityLabel: NSLocalizedString("Shuttle buses replace service",
                                                      comment: "Shuttle alert VoiceOver text")
            )
        case .stationClosure: .init(text: NSLocalizedString("Station Closed", comment: "Possible alert effect"))
        case .stopClosure: .init(text: NSLocalizedString("Stop Closed", comment: "Possible alert effect"))
        case .suspension: .init(
                text: NSLocalizedString("Suspension", comment: "Possible alert effect"),
                accessibilityLabel: NSLocalizedString("Service suspended", comment: "Suspension alert VoiceOver text")
            )
        default: .init(text: effect, accessibilityLabel: nil)
        }
        self.alertSummary = alertSummary
    }

    var downstreamLabel: String {
        String(format: NSLocalizedString("**%@** ahead", comment: """
        Label for an alert that exists on a future stop along the selected route,
        the interpolated value can be any alert effect,
        ex. "[Detour] ahead", "[Shuttle buses] ahead"
        """), sentenceCaseEffect)
    }

    var delaysDueToCause: String {
        if let cause = dueToCause {
            String(format: NSLocalizedString(
                "**Delays** due to %@",
                comment: "Describes the cause of a delay. Ex: 'Delays due to [traffic]'"
            ), cause)
        } else {
            NSLocalizedString("**Delays**", comment: "Generic delay alert label when cause is unknown")
        }
    }

    var summaryLocation: String {
        if let alertSummary, let location = alertSummary.location {
            switch onEnum(of: location) {
            case let .directionToStop(location):
                String(format:
                    NSLocalizedString(" from **%1$@** stops to **%2$@**",
                                      comment: """
                                      Alert summary location for branching routes in the format of " from [direction]
                                      stops to [Stop name]" ex. " from [Westbound] stops to [Kenmore]" or " from
                                      [Eastbound] stops to [Government Center]". The leading space should be retained,
                                      because this will be added in the %2 position of the "**%1$@**%2$@%3$@" alert
                                      summary template which may or may not include a location fragment.
                                      """),
                    DirectionLabel.directionNameFormatted(location.direction),
                    location.endStopName)

            case let .singleStop(location):
                String(format:
                    NSLocalizedString(" at **%1$@**",
                                      comment: """
                                      Alert summary location for a single stop in the format of \
                                      " at [Stop name]" ex. " at [Haymarket]" or " at [Green St @ Magazine St]". \
                                      The leading space should be
                                      retained, because this will be added in the %2 position of the \
                                      "**%1$@**%2$@%3$@" alert summary template which may or may not include a \
                                      location fragment.
                                      """), location.stopName)

            case let .stopToDirection(location):
                String(format:
                    NSLocalizedString(" from **%1$@** to **%2$@** stops",
                                      comment: """
                                      Alert summary location for branching routes in the format of " from [Stop name] \
                                      to [direction] stops" ex. " from [Kenmore] to [Westbound] stops" or " from \
                                      [JFK/UMass] to [Southbound] stops". The leading space should be retained,
                                      because this will be added in the %2 position of the "**%1$@**%2$@%3$@" alert \
                                      summary template which may or may not include a location fragment.
                                      """),
                    location.startStopName,
                    DirectionLabel.directionNameFormatted(location.direction))

            case let .successiveStops(location):
                String(format:
                    NSLocalizedString(" from **%1$@** to **%2$@**",
                                      comment: """
                                      Alert summary location for consecutive stops in the format of " from [Stop name] \
                                      to [Other stop name]" ex. " from [Alewife] to [Harvard]" or " from [Lechmere] \
                                      to [Park Street]". The leading space should be retained, because this will be \
                                      added in the %2 position of the "**%1$@**%2$@%3$@" alert summary template which \
                                      may or may not include a location fragment.
                                      """),
                    location.startStopName,
                    location.endStopName)
            }
        } else {
            ""
        }
    }

    var summaryTimeframe: String {
        if let alertSummary, let timeframe = alertSummary.timeframe {
            switch onEnum(of: timeframe) {
            case let .endOfService(timeframe):
                NSLocalizedString(" through end of service",
                                  comment: """
                                  Alert summary timeframe ending at the end of service on the current day. \
                                  The leading space should be retained, because this will be added in the %3 position \
                                  of the "**%1$@**%2$@%3$@" alert summary template which may or may not include a \
                                  timeframe fragment.
                                  """)
            case let .tomorrow(timeframe):
                NSLocalizedString(" through tomorrow",
                                  comment: """
                                  Alert summary timeframe ending tomorrow. The leading space should be retained, \
                                  because this will be added in the %3 position of the "**%1$@**%2$@%3$@" alert \
                                  summary template which may or may not include a timeframe fragment.
                                  """)
            case let .laterDate(timeframe):
                String(format: NSLocalizedString("key/alert_summary_timeframe_later_date",
                                                 comment: """
                                                 Alert summary timeframe ending on a specific date in the future. \
                                                 ex. " through May 11". The date component is localized by the OS. \
                                                 The leading space should be retained, because this will be added in \
                                                 the %3 position of the "**%1$@**%2$@%3$@" alert summary template \
                                                 which may or may not include a timeframe fragment. fragment.
                                                 """),
                       Date(instant: timeframe.time.coerceInServiceDay())
                           .formatted(Date.FormatStyle().month(.abbreviated).day()))
            case let .thisWeek(timeframe):
                String(format: NSLocalizedString("key/alert_summary_timeframe_this_week",
                                                 comment: """
                                                 Alert summary timeframe ending on a specific day later this week. \
                                                 ex. " through Thursday". The weekday component is localized by the \
                                                 OS. The leading space should be retained, because this will be added \
                                                 in the %3 position of the "**%1$@**%2$@%3$@" alert summary template \
                                                 which may or may not include a timeframe fragment.
                                                 """), Date(instant: timeframe.time.coerceInServiceDay())
                        .formatted(Date.FormatStyle().weekday(.wide)))
            case let .time(timeframe):
                String(format:
                    NSLocalizedString("key/alert_summary_timeframe_time",
                                      comment: """
                                      Alert summary timeframe ending on a specific time later today. \
                                      ex. " through 10:00 PM". The time component is localized by the OS. The leading \
                                      space should be retained, because this will be added in the %3 position of the \
                                      "**%1$@**%2$@%3$@" alert summary template which may or may not include a \
                                      timeframe fragment.
                                      """), Date(instant: timeframe.time).formatted(date: .omitted, time: .shortened))
            }
        } else {
            ""
        }
    }

    var summary: AttributedString? {
        if alertSummary != nil {
            AttributedString.tryMarkdown(String(format:
                NSLocalizedString("**%1$@**%2$@%3$@",
                                  comment: """
                                  Alert summary in the format of "[Alert effect][at location][through timeframe]", \
                                  ex "[Stop closed][ at Haymarket][ through this Friday]" or \
                                  "[Service suspended][ from Alewife to Harvard][ through end of service]"
                                  """), sentenceCaseEffect, summaryLocation, summaryTimeframe))
        } else { nil }
    }

    var elevatorHeader: AttributedString {
        let facilities = alert.informedEntity.compactMap { entity in
            if let facilityId = entity.facility { alert.facilities?[facilityId] } else { nil }
        }.filter { $0.type == .elevator }
        let headerString =
            if let facility = Set(facilities).count == 1 ? facilities.first : nil,
            let facilityName = facility.shortName {
                String(format:
                    NSLocalizedString(
                        "Elevator closure (%1$@)",
                        comment: """
                        Alert header for elevator closure alerts, \
                        the interpolated value is the short name field of the elevator facility, \
                        ex "Elevator closure (Red Line platforms to lobby)"
                        """
                    ), facilityName)
            } else if let header = alert.header {
                header
            } else {
                effect
            }
        return AttributedString.tryMarkdown(headerString)
    }

    func alertCardHeader(spec: AlertCardSpec) -> AttributedString {
        switch spec {
        case .downstream: summary ?? AttributedString.tryMarkdown(downstreamLabel)
        case .elevator: elevatorHeader
        case .delay: AttributedString.tryMarkdown(delaysDueToCause)
        // TODO: confirm this is desired
        case .secondary: summary ?? AttributedString.tryMarkdown(effect)
        default: AttributedString.tryMarkdown(effect)
        }
    }

    var alertCardMajorBody: AttributedString {
        summary ?? AttributedString(alert.header ?? "")
    }

    struct PredictionReplacement: Equatable {
        let text: String
        let accessibilityLabel: String?

        init(text: String, accessibilityLabel: String? = nil) {
            self.text = text
            self.accessibilityLabel = accessibilityLabel
        }
    }
}

// swiftlint:disable:next cyclomatic_complexity function_body_length
private func dueToCauseLabel(_ cause: Alert.Cause) -> String? {
    switch cause {
    case .accident: NSLocalizedString("accident", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .amtrak: NSLocalizedString("Amtrak", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .amtrakTrainTraffic: NSLocalizedString(
            "Amtrak train traffic",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .anEarlierMechanicalProblem: NSLocalizedString(
            "an earlier mechanical problem",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .anEarlierSignalProblem:
        NSLocalizedString("an earlier signal problem", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .autosImpedingService: NSLocalizedString(
            "autos impeding service",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .coastGuardRestriction: NSLocalizedString(
            "Coast Guard restriction",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .congestion: NSLocalizedString("congestion", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .construction: NSLocalizedString("construction", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .crossingIssue: NSLocalizedString("crossing issue", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .crossingMalfunction: NSLocalizedString(
            "crossing malfunction",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .demonstration: NSLocalizedString("demonstration", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .disabledBus: NSLocalizedString("disabled bus", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .disabledTrain: NSLocalizedString("disabled train", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .drawbridgeBeingRaised: NSLocalizedString(
            "drawbridge being raised",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .electricalWork: NSLocalizedString("electrical work", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .fire: NSLocalizedString("fire", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .fireDepartmentActivity: NSLocalizedString(
            "fire department activity",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .flooding: NSLocalizedString("flooding", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .fog: NSLocalizedString("fog", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .freightTrainInterference: NSLocalizedString(
            "freight train interference",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .hazmatCondition: NSLocalizedString(
            "hazmat condition",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .heavyRidership: NSLocalizedString("heavy ridership", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .highWinds: NSLocalizedString("high winds", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .holiday: NSLocalizedString("holiday", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .hurricane: NSLocalizedString("hurricane", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .iceInHarbor: NSLocalizedString("ice in harbor", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .maintenance: NSLocalizedString("maintenance", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .mechanicalIssue: NSLocalizedString(
            "mechanical issue",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .mechanicalProblem: NSLocalizedString(
            "mechanical problem",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .medicalEmergency: NSLocalizedString(
            "medical emergency",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .parade: NSLocalizedString("parade", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .policeAction: NSLocalizedString("police action", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .policeActivity: NSLocalizedString("police activity", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .powerProblem: NSLocalizedString("power problem", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .railDefect: NSLocalizedString("rail defect", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .severeWeather: NSLocalizedString("severe weather", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .signalIssue: NSLocalizedString("signal issue", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .signalProblem: NSLocalizedString("signal problem", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .singleTracking: NSLocalizedString("single tracking", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .slipperyRail: NSLocalizedString("slippery rail", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .snow: NSLocalizedString("snow", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .specialEvent: NSLocalizedString("special event", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .speedRestriction: NSLocalizedString(
            "speed restriction",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .strike: NSLocalizedString("strike", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .switchIssue: NSLocalizedString("switch issue", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .switchProblem: NSLocalizedString("switch problem", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .technicalProblem: NSLocalizedString(
            "technical problem",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .tieReplacement: NSLocalizedString("tie replacement", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .trackProblem: NSLocalizedString("track problem", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .trackWork: NSLocalizedString("track work", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .traffic: NSLocalizedString("traffic", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .trainTraffic: NSLocalizedString("train traffic", comment: "Alert cause, used in 'Delays due to [cause]'")
    case .unrulyPassenger: NSLocalizedString(
            "unruly passenger",
            comment: "Alert cause, used in 'Delays due to [cause]'"
        )
    case .weather: NSLocalizedString("weather", comment: "Alert cause, used in 'Delays due to [cause]'")
    default: nil
    }
}
