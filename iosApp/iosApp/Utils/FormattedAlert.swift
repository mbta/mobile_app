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
    let alert: Alert?
    let alertSummary: AlertSummary?
    let effect: String
    let sentenceCaseEffect: String
    let dueToCause: String?
    /// Represents the text and possible accessibility label that would be used if replacing predictions. Does not
    /// guarantee that the alert should replace predictions.
    let predictionReplacement: PredictionReplacement

    // swiftlint:disable:next cyclomatic_complexity
    init(alert: Alert?, alertSummary: AlertSummary? = nil) {
        self.alert = alert
        let effect = alert?.effect ?? alertSummary?.effect ?? .unknownEffect
        self.effect = "**\(effect.effectString)**"
        sentenceCaseEffect = effect.effectSentenceCaseString
        dueToCause = alert?.cause.causeLowercaseString

        // a handful of cases have different text when replacing predictions than in a details title
        predictionReplacement = switch effect {
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
        default: .init(text: effect.effectString, accessibilityLabel: nil)
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
                                      Alert summary location for branching routes in the format of " from [direction] \
                                      stops to [Stop name]" ex. " from [Westbound] stops to [Kenmore]" or " from \
                                      [Eastbound] stops to [Government Center]". The leading space should be retained, \
                                      because this will be added in the %2 position of the "**%1$@**%2$@%3$@" alert \
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
                                      The leading space should be \
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
                                      [JFK/UMass] to [Southbound] stops". The leading space should be retained, \
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

            case .unknown: ""
            }
        } else {
            ""
        }
    }

    var summaryTimeframe: String {
        if let alertSummary, let timeframe = alertSummary.timeframe {
            switch onEnum(of: timeframe) {
            case .endOfService:
                NSLocalizedString(" through end of service",
                                  comment: """
                                  Alert summary timeframe ending at the end of service on the current day. \
                                  The leading space should be retained, because this will be added in the %3 position \
                                  of the "**%1$@**%2$@%3$@" alert summary template which may or may not include a \
                                  timeframe fragment.
                                  """)
            case .tomorrow:
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
                                                 which may or may not include a timeframe fragment.
                                                 """),
                       timeframe.time.coerceInServiceDay(rounding: .backwards)
                           .formatted(.init().month(.abbreviated).day()))
            case let .thisWeek(timeframe):
                String(format: NSLocalizedString("key/alert_summary_timeframe_this_week",
                                                 comment: """
                                                 Alert summary timeframe ending on a specific day later this week. \
                                                 ex. " through Thursday". The weekday component is localized by the \
                                                 OS. The leading space should be retained, because this will be added \
                                                 in the %3 position of the "**%1$@**%2$@%3$@" alert summary template \
                                                 which may or may not include a timeframe fragment.
                                                 """), timeframe.time.coerceInServiceDay(rounding: .backwards).formatted(
                        .init().weekday(.wide)
                    ))
            case let .time(timeframe):
                String(format:
                    NSLocalizedString("key/alert_summary_timeframe_time",
                                      comment: """
                                      Alert summary timeframe ending on a specific time later today. \
                                      ex. " through 10:00 PM". The time component is localized by the OS. The leading \
                                      space should be retained, because this will be added in the %3 position of the \
                                      "**%1$@**%2$@%3$@" alert summary template which may or may not include a \
                                      timeframe fragment.
                                      """), timeframe.time.formatted(date: .omitted, time: .shortened))
            case .startingTomorrow:
                NSLocalizedString(" starting tomorrow",
                                  comment: """
                                  Alert summary timeframe starting tomorrow. The leading space should be retained, \
                                  because this will be added in the %3 position of the "**%1$@**%2$@%3$@" alert \
                                  summary template which may or may not include a timeframe fragment.
                                  """)
            case let .startingLaterToday(timeframe):
                String(format:
                    NSLocalizedString(" starting **%@** today",
                                      comment: """
                                      Alert summary timeframe starting on a specific time later today. \
                                      ex. " starting 10:00 PM today". The time component is localized by the OS. \
                                      The leading space should be retained, because this will be added in the %3 \
                                      position of the "**%1$@**%2$@%3$@" alert summary template which may or may not \
                                      include a timeframe fragment.
                                      """), timeframe.time.formatted(date: .omitted, time: .shortened))
            case let .timeRange(timeframe):
                String(format:
                    NSLocalizedString(" from %@ to %@",
                                      comment: """
                                      Alert summary timeframe with a range today that will recur in the future, \
                                      e.g. “from 9:00 PM to end of service”. The leading space should be retained.
                                      """), Self.timeRangeBoundary(timeframe.startTime),
                    Self.timeRangeBoundary(timeframe.endTime))
            case .unknown: ""
            }
        } else {
            ""
        }
    }

    private static func timeRangeBoundary(_ boundary: AlertSummaryTimeframeTimeRangeStartTime) -> String {
        switch onEnum(of: boundary) {
        case .startOfService: NSLocalizedString("start of service", comment: "")
        case let .time(boundary): boundary.time.formatted(date: .omitted, time: .shortened)
        case .unknown: ""
        }
    }

    private static func timeRangeBoundary(_ boundary: AlertSummaryTimeframeTimeRangeEndTime) -> String {
        switch onEnum(of: boundary) {
        case .endOfService: NSLocalizedString("end of service", comment: "")
        case let .time(boundary): boundary.time.formatted(date: .omitted, time: .shortened)
        case .unknown: ""
        }
    }

    private static func summaryRecurrenceEndDay(_ endDay: AlertSummaryRecurrenceEndDay) -> String? {
        switch onEnum(of: endDay) {
        case .tomorrow:
            NSLocalizedString(" until tomorrow",
                              comment: """
                              Alert summary recurrence ending tomorrow. The leading space should be retained.
                              """)
        case let .laterDate(timeframe):
            String(format: NSLocalizedString("key/alert_summary_recurrence_end_day_later_date",
                                             comment: """
                                             Alert summary recurrence ending on a specific date in the future. \
                                             ex. " until May 11". The date component is localized by the OS. \
                                             The leading space should be retained.
                                             """),
                   timeframe.time.coerceInServiceDay(rounding: .backwards).formatted(.init().month(.abbreviated).day()))
        case let .thisWeek(timeframe):
            String(format: NSLocalizedString("key/alert_summary_recurrence_end_day_this_week",
                                             comment: """
                                             Alert summary recurrence ending on a specific day later this week. \
                                             ex. " until Thursday". The weekday component is localized by the \
                                             OS. The leading space should be retained.
                                             """), timeframe.time.coerceInServiceDay(rounding: .backwards).formatted(
                    .init().weekday(.wide)
                ))
        case .unknown: nil
        }
    }

    var summaryRecurrence: String {
        switch onEnum(of: alertSummary?.recurrence) {
        case let .daily(recurrence):
            if let end = Self.summaryRecurrenceEndDay(recurrence.ending) {
                String(format:
                    NSLocalizedString(" daily%@",
                                      comment: """
                                      Alert summary recurrence every day until the indicated date, e.g. “ daily until Friday”. The \
                                      leading space must be retained.
                                      """), end)
            } else { "" }
        case let .someDays(recurrence):
            if let end = Self.summaryRecurrenceEndDay(recurrence.ending) {
                String(format:
                    NSLocalizedString(" some days%@", comment: """
                    Alert summary recurrence on only certain days until the indicated date, e.g. \
                    “some days until Jan 16”. The leading space must be retained.
                    """), end)
            } else { "" }
        case .unknown, nil: ""
        }
    }

    var summary: AttributedString? {
        if alertSummary != nil {
            AttributedString.tryMarkdown(String(format:
                NSLocalizedString("**%1$@**%2$@%3$@%4$@",
                                  comment: """
                                  Alert summary in the format of "[Alert effect][at location][through timeframe][until recurrence]", \
                                  ex "[Stop closed][ at Haymarket][ through this Friday][]" or \
                                  "[Service suspended][ from Alewife to Harvard][ through end of service][ daily until Friday]"
                                  """), sentenceCaseEffect, summaryLocation, summaryTimeframe, summaryRecurrence))
        } else { nil }
    }

    var delayHeader: AttributedString {
        // Show "Single Tracking" if there is an informational delay alert with that cause
        // (Any other information severity delay alerts are never shown)
        guard let alert, let cause = alert.cause.causeString,
              alert.cause == .singleTracking,
              alert.severity < 3
        else {
            return AttributedString.tryMarkdown(delaysDueToCause)
        }
        return AttributedString.tryMarkdown("**\(cause)**")
    }

    var elevatorHeader: AttributedString {
        let facilities = alert?.informedEntity.compactMap { entity in
            if let facilityId = entity.facility { alert?.facilities?[facilityId] } else { nil }
        }.filter { $0.type == .elevator }
        let headerString =
            if let facilities, let facility = Set(facilities).count == 1 ? facilities.first : nil,
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
            } else if let header = alert?.header {
                header
            } else {
                effect
            }
        return AttributedString.tryMarkdown(headerString)
    }

    func alertCardHeader(spec: AlertCardSpec) -> AttributedString {
        switch spec {
        case .delay: delayHeader
        case .downstream: summary ?? AttributedString.tryMarkdown(downstreamLabel)
        case .elevator: elevatorHeader
        case .secondary: summary ?? AttributedString.tryMarkdown(effect)
        default: AttributedString.tryMarkdown(effect)
        }
    }

    var alertCardMajorBody: AttributedString {
        summary ?? AttributedString(alert?.header ?? "")
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
