//
//  FormattedAlert.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-16.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared

// swiftlint:disable:next type_body_length
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
        let cause = alert?.cause ?? (alertSummary as? TripSpecificAlertSummary)?.cause
        dueToCause = cause?.causeLowercaseString

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

    static func summaryLocation(effect: Alert.Effect?, location: AlertSummary.Location?) -> String {
        switch onEnum(of: location) {
        case let .directionToStop(location):
            String(format:
                NSLocalizedString(
                    " from **%1$@** stops to **%2$@**",
                    comment: """
                    Alert summary location for branching routes in the format of " from [direction] \
                    stops to [Stop name]" ex. " from [Westbound] stops to [Kenmore]" or " from \
                    [Eastbound] stops to [Government Center]". The leading space should be retained, \
                    because this will be added in the %2 position of the "**%1$@**%2$@%3$@" alert \
                    summary template which may or may not include a location fragment.
                    """
                ),
                DirectionLabel.directionNameFormatted(location.direction),
                location.endStopName)

        case let .singleStop(location):
            String(format:
                NSLocalizedString(
                    " at **%1$@**",
                    comment: """
                    Alert summary location for a single stop in the format of \
                    " at [Stop name]" ex. " at [Haymarket]" or " at [Green St @ Magazine St]". \
                    The leading space should be \
                    retained, because this will be added in the %2 position of the \
                    "**%1$@**%2$@%3$@" alert summary template which may or may not include a \
                    location fragment.
                    """
                ), location.stopName)

        case let .stopToDirection(location):
            String(format:
                NSLocalizedString(
                    " from **%1$@** to **%2$@** stops",
                    comment: """
                    Alert summary location for branching routes in the format of " from [Stop name] \
                    to [direction] stops" ex. " from [Kenmore] to [Westbound] stops" or " from \
                    [JFK/UMass] to [Southbound] stops". The leading space should be retained, \
                    because this will be added in the %2 position of the "**%1$@**%2$@%3$@" alert \
                    summary template which may or may not include a location fragment.
                    """
                ),
                location.startStopName,
                DirectionLabel.directionNameFormatted(location.direction))

        case let .successiveStops(location):
            String(format:
                NSLocalizedString(
                    " from **%1$@** to **%2$@**",
                    comment: """
                    Alert summary location for consecutive stops in the format of " from [Stop name] \
                    to [Other stop name]" ex. " from [Alewife] to [Harvard]" or " from [Lechmere] \
                    to [Park Street]". The leading space should be retained, because this will be \
                    added in the %2 position of the "**%1$@**%2$@%3$@" alert summary template which \
                    may or may not include a location fragment.
                    """
                ),
                location.startStopName,
                location.endStopName)

        case let .wholeRoute(location):
            effect == .shuttle ?
                String(format:
                    NSLocalizedString(
                        " replacing **%1$@**",
                        comment: """
                        Alert summary location for an entire route with shuttle service in the format of \
                        " replacing [RouteLabel]" ex. " replacing [Green Line C]" or " replacing \
                        [Orangle Line]". The leading space should be retained, because this will be added \
                        in the %2 position of the "**%1$@**%2$@%3$@" alert summary template which may or \
                        may not include a location fragment.
                        """
                    ), location.modeLabel) :
                String(format:
                    NSLocalizedString(
                        " on **%1$@**",
                        comment: """
                        Alert summary location for an entire route in the format of " on [RouteLabel]" \
                        ex. " on [Green Line C]" or " on [1 bus]". The leading space should be retained, \
                        because this will be added in the %2 position of the "**%1$@**%2$@%3$@" alert \
                        summary template which may or may not include a location fragment.
                        """
                    ), location.modeLabel)

        case .unknown: ""

        case nil: ""
        }
    }

    static func summaryTimeframe(timeframe: AlertSummary.Timeframe?) -> String {
        switch onEnum(of: timeframe) {
        case .untilFurtherNotice:
            NSLocalizedString(
                " until further notice",
                comment: """
                Alert summary timeframe with no known end. The leading space should be retained.
                """
            )
        case .endOfService:
            NSLocalizedString(
                " through end of service",
                comment: """
                Alert summary timeframe ending at the end of service on the current day. \
                The leading space should be retained, because this will be added in the %3 position \
                of the "**%1$@**%2$@%3$@" alert summary template which may or may not include a \
                timeframe fragment.
                """
            )
        case .tomorrow:
            NSLocalizedString(
                " through tomorrow",
                comment: """
                Alert summary timeframe ending tomorrow. The leading space should be retained, \
                because this will be added in the %3 position of the "**%1$@**%2$@%3$@" alert \
                summary template which may or may not include a timeframe fragment.
                """
            )
        case let .laterDate(timeframe):
            String(format: NSLocalizedString(
                "key/alert_summary_timeframe_later_date",
                comment: """
                Alert summary timeframe ending on a specific date in the future. \
                ex. " through May 11". The date component is localized by the OS. \
                The leading space should be retained, because this will be added in \
                the %3 position of the "**%1$@**%2$@%3$@" alert summary template \
                which may or may not include a timeframe fragment.
                """
            ),
            timeframe.time.coerceInServiceDay(rounding: .backwards)
                .formatted(.init().month(.abbreviated).day()))
        case let .thisWeek(timeframe):
            String(format: NSLocalizedString(
                "key/alert_summary_timeframe_this_week",
                comment: """
                Alert summary timeframe ending on a specific day later this week. \
                ex. " through Thursday". The weekday component is localized by the \
                OS. The leading space should be retained, because this will be added \
                in the %3 position of the "**%1$@**%2$@%3$@" alert summary template \
                which may or may not include a timeframe fragment.
                """
            ), timeframe.time.coerceInServiceDay(rounding: .backwards).formatted(
                .init().weekday(.wide)
            ))
        case let .time(timeframe):
            String(format:
                NSLocalizedString(
                    "key/alert_summary_timeframe_time",
                    comment: """
                    Alert summary timeframe ending on a specific time later today. \
                    ex. " through 10:00 PM". The time component is localized by the OS. The leading \
                    space should be retained, because this will be added in the %3 position of the \
                    "**%1$@**%2$@%3$@" alert summary template which may or may not include a \
                    timeframe fragment.
                    """
                ), timeframe.time.formatted(date: .omitted, time: .shortened))
        case .startingTomorrow:
            NSLocalizedString(
                " starting tomorrow",
                comment: """
                Alert summary timeframe starting tomorrow. The leading space should be retained, \
                because this will be added in the %3 position of the "**%1$@**%2$@%3$@" alert \
                summary template which may or may not include a timeframe fragment.
                """
            )
        case let .startingLaterToday(timeframe):
            String(format:
                NSLocalizedString(
                    " starting **%@** today",
                    comment: """
                    Alert summary timeframe starting on a specific time later today. \
                    ex. " starting 10:00 PM today". The time component is localized by the OS. \
                    The leading space should be retained, because this will be added in the %3 \
                    position of the "**%1$@**%2$@%3$@" alert summary template which may or may not \
                    include a timeframe fragment.
                    """
                ), timeframe.time.formatted(date: .omitted, time: .shortened))
        case let .timeRange(timeframe):
            String(format:
                NSLocalizedString(
                    " from %@ to %@",
                    comment: """
                    Alert summary timeframe with a range today that will recur in the future, \
                    e.g. “from 9:00 PM to end of service”. The leading space should be retained.
                    """
                ), Self.timeRangeBoundary(timeframe.startTime),
                Self.timeRangeBoundary(timeframe.endTime))
        case .unknown: ""
        case nil: ""
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
        case .untilFurtherNotice:
            NSLocalizedString(
                " until further notice",
                comment: """
                Alert summary timeframe with no known end. The leading space should be retained.
                """
            )
        case .tomorrow:
            NSLocalizedString(
                " until tomorrow",
                comment: """
                Alert summary recurrence ending tomorrow. The leading space should be retained.
                """
            )
        case let .laterDate(timeframe):
            String(format: NSLocalizedString(
                "key/alert_summary_recurrence_end_day_later_date",
                comment: """
                Alert summary recurrence ending on a specific date in the future. \
                ex. " until May 11". The date component is localized by the OS. \
                The leading space should be retained.
                """
            ),
            timeframe.time.coerceInServiceDay(rounding: .backwards).formatted(.init().month(.abbreviated).day()))
        case let .thisWeek(timeframe):
            String(format: NSLocalizedString(
                "key/alert_summary_recurrence_end_day_this_week",
                comment: """
                Alert summary recurrence ending on a specific day later this week. \
                ex. " until Thursday". The weekday component is localized by the \
                OS. The leading space should be retained.
                """
            ), timeframe.time.coerceInServiceDay(rounding: .backwards).formatted(
                .init().weekday(.wide)
            ))
        case .unknown: nil
        }
    }

    static func summaryRecurrence(recurrence: AlertSummary.Recurrence?) -> String {
        switch onEnum(of: recurrence) {
        case let .daily(recurrence):
            if let end = Self.summaryRecurrenceEndDay(recurrence.ending) {
                String(format:
                    NSLocalizedString(
                        " daily%@",
                        comment: """
                        Alert summary recurrence every day until the indicated date, e.g. “ daily until Friday”. The \
                        leading space must be retained.
                        """
                    ), end)
            } else { "" }
        case let .someDays(recurrence):
            if let end = Self.summaryRecurrenceEndDay(recurrence.ending) {
                String(format:
                    NSLocalizedString(
                        " some days%@", comment: """
                        Alert summary recurrence on only certain days until the indicated date, e.g. \
                        “some days until Jan 16”. The leading space must be retained.
                        """
                    ), end)
            } else { "" }
        case .unknown, nil: ""
        }
    }

    static func summaryTripIdentity(tripIdentity: TripSpecificAlertSummaryTripIdentity) -> String {
        switch onEnum(of: tripIdentity) {
        case let .tripFrom(tripIdentity): String(
                format: NSLocalizedString(
                    "**%1$@** from **%2$@**",
                    comment: "Trip identity in the form of ”[time] from [stop]”, ex “[12:13 PM] from [Ruggles]”"
                ),
                tripIdentity.tripTime.formatted(date: .omitted, time: .shortened),
                tripIdentity.stopName
            )
        case let .tripTo(tripIdentity): String(
                format: NSLocalizedString(
                    "**%1$@** to **%2$@**",
                    comment: "Trip identity in the form of ”[time] to [headsign]”, ex “[12:13 PM] to [Stoughton]”"
                ),
                tripIdentity.tripTime.formatted(date: .omitted, time: .shortened),
                tripIdentity.headsign
            )
        case .multipleTrips: NSLocalizedString(
                "Multiple trips",
                comment: "Trip identity referring to more than one specific trip"
            )
        }
    }

    static func summaryTripEffect(
        tripIdentity: TripSpecificAlertSummaryTripIdentity,
        effect: Alert.Effect,
        effectStops: [String]?,
        isToday: Bool,
    ) -> String {
        let day = isToday ? NSLocalizedString("today", comment: "") : NSLocalizedString("tomorrow", comment: "")
        let isPlural = tripIdentity is TripSpecificAlertSummary.MultipleTrips
        switch effect {
        case .cancellation where isPlural: return String(format: NSLocalizedString(
                "are cancelled %@",
                comment: "Multiple trip specific alert effect denoting cancellation, will specify “today” or “tomorrow”"
            ), day)
        case .cancellation: return String(format: NSLocalizedString(
                "is cancelled %@",
                comment: "Trip specific alert effect denoting cancellation, will specify “today” or “tomorrow”"
            ), day)
        case .stationClosure: if let effectStops {
                return String(
                    format: NSLocalizedString(
                        "will not stop at %@ %@",
                        comment: "Trip specific alert effect denoting station bypass, ex “will not stop at [Back Bay and Ruggles] [today]”"
                    ),
                    effectStops.map { "**\($0)**" }.reduce(nil) { lhs, rhs in
                        if let lhs { String(
                            format: NSLocalizedString(
                                "%1$@ and %2$@",
                                comment: "Joins two stops into a list, ex “[Back Bay] and [Ruggles]”"
                            ),
                            lhs,
                            rhs
                        ) } else { rhs }
                    } ?? "",
                    day
                )
            }
        case .suspension where isPlural: return String(format: NSLocalizedString(
                "are suspended %@",
                comment: "Multiple trip specific alert effect denoting suspension, will specify “today” or “tomorrow”"
            ), day)
        case .suspension: return String(format: NSLocalizedString(
                "is suspended %@",
                comment: "Trip specific alert effect denoting suspension, will specify “today” or “tomorrow”"
            ), day)
        default:
            break
        }
        return String(
            format: NSLocalizedString(
                "affected by %@ %@",
                comment: "Trip specific alert effect fallback, ex “affected by [snow route] [today]”"
            ),
            effect.effectSentenceCaseString,
            day
        )
    }

    var summaryTripCause: String {
        if let dueToCause {
            String(format: NSLocalizedString(" due to %@", comment: ""), dueToCause)
        } else {
            ""
        }
    }

    static func summaryTripShuttleIdentity(tripIdentity: TripShuttleAlertSummaryTripIdentity) -> String {
        switch onEnum(of: tripIdentity) {
        case let .singleTrip(tripIdentity): String(
                format: NSLocalizedString(
                    "the **%@** %@",
                    comment: "Trip identity in the format of “the [time] [vehicle]”, ex “the [12:13 PM] [train]"
                ),
                tripIdentity.tripTime.formatted(date: .omitted, time: .shortened),
                tripIdentity.routeType.typeText(isOnly: true)
            )
        case .multipleTrips: NSLocalizedString(
                "multiple trips",
                comment: "Trip identity referring to more than one specific trip"
            )
        }
    }

    var summary: AttributedString? {
        summary(alertSummary: alertSummary)
    }

    func summary(alertSummary: AlertSummary?) -> AttributedString? {
        switch onEnum(of: alertSummary) {
        case let .allClear(alertSummary): return AttributedString.tryMarkdown(String(
                format: NSLocalizedString(
                    "**All clear:** Regular service%1$@",
                    comment: """
                    Alert summary in the format of "All clear: Regular service[at location]", \
                    ex "[All clear][Regular service][ from Alewife to Harvard]"
                    """
                ), Self.summaryLocation(effect: nil, location: alertSummary.location)
            ))
        case let .standard(alertSummary):
            let args = [
                sentenceCaseEffect,
                Self.summaryLocation(effect: alertSummary.effect, location: alertSummary.location),
                Self.summaryTimeframe(timeframe: alertSummary.timeframe),
                Self.summaryRecurrence(recurrence: alertSummary.recurrence),
            ]
            if alertSummary.isUpdate {
                return AttributedString.tryMarkdown(String(format:
                    NSLocalizedString(
                        "**Update:** %1$@%2$@%3$@%4$@",
                        comment: """
                        Alert summary in the format of "Update: [Alert effect][at location][through timeframe][until recurrence]", \
                        ex "[Update][Stop closed][ at Haymarket][ through this Friday][]" or \
                        "[Update][Service suspended][ from Alewife to Harvard][ through end of service][ daily until Friday]"
                        """
                    ), args.map { $0 as CVarArg }))
            } else {
                return AttributedString.tryMarkdown(String(format:
                    NSLocalizedString(
                        "**%1$@**%2$@%3$@%4$@",
                        comment: """
                        Alert summary in the format of "[Alert effect][at location][through timeframe][until recurrence]", \
                        ex "[Stop closed][ at Haymarket][ through this Friday][]" or \
                        "[Service suspended][ from Alewife to Harvard][ through end of service][ daily until Friday]"
                        """
                    ), args.map { $0 as CVarArg }))
            }
        case let .tripSpecificAlertSummary(alertSummary): return AttributedString.tryMarkdown(String(
                format: NSLocalizedString(
                    "%1$@ %2$@%3$@%4$@",
                    comment: """
                    Alert summary in the format of “[trip identity] [is affected][due to cause][until recurrence]”, \
                    ex “[12:13 PM from Ruggles] [is cancelled today][ due to a mechanical issue][ \
                    some days until Wednesday]” or “[Multiple trips] [are suspended today][][]”
                    """
                ),
                Self.summaryTripIdentity(tripIdentity: alertSummary.tripIdentity),
                Self.summaryTripEffect(
                    tripIdentity: alertSummary.tripIdentity,
                    effect: alertSummary.effect,
                    effectStops: alertSummary.effectStops,
                    isToday: alertSummary.isToday
                ),
                summaryTripCause,
                Self.summaryRecurrence(recurrence: alertSummary.recurrence)
            ))
        case let .tripShuttleAlertSummary(alertSummary): return AttributedString.tryMarkdown(String(
                format: NSLocalizedString(
                    "Shuttle buses replace %1$@ %2$@ from **%3$@** to **%4$@**%5$@",
                    comment: """
                    Alert summary in the format of “Shuttle buses replace [trip identity] [day] \
                    from [stop] to [stop][until recurrence]”, ex “Shuttle buses replace [the 12:13 PM train] \
                    [today] from [Ruggles] to [Forest Hills][ some days until Friday]”
                    """
                ),
                Self.summaryTripShuttleIdentity(tripIdentity: alertSummary.tripIdentity),
                alertSummary.isToday ? NSLocalizedString("today", comment: "") : NSLocalizedString(
                    "tomorrow",
                    comment: ""
                ),
                alertSummary.currentStopName,
                alertSummary.endStopName,
                Self.summaryRecurrence(recurrence: alertSummary.recurrence)
            ))
        case let .unknown(alertSummary): return summary(alertSummary: alertSummary.fallback)
        case nil: return nil
        }
    }

    var delayHeader: AttributedString {
        if case let .standard(alertSummary) = onEnum(of: alertSummary),
           case .startingLaterToday = onEnum(of: alertSummary.timeframe), let summary {
            return summary
        }
        // Show "Single Tracking" if there is an informational delay alert with that cause
        // (Any other information severity delay alerts are never shown)
        guard let alert, let cause = alert.cause?.causeString,
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

    func alertCardHeader(spec: AlertCardSpec, type: RouteType) -> AttributedString {
        switch spec {
        case .delay: delayHeader
        case .downstream: summary ?? AttributedString.tryMarkdown(downstreamLabel)
        case .elevator: elevatorHeader
        case .secondary: summary ?? AttributedString.tryMarkdown(effect)
        default: switch (type, alert?.effect ?? alertSummary?.effect) {
            case (.bus, .cancellation) where alertSummary is TripSpecificAlertSummary:
                AttributedString(NSLocalizedString("Bus cancelled", comment: ""))
            case (.ferry, .cancellation) where alertSummary is TripSpecificAlertSummary:
                AttributedString(NSLocalizedString("Ferry cancelled", comment: ""))
            case (_, .cancellation) where alertSummary is TripSpecificAlertSummary:
                AttributedString(NSLocalizedString("Train cancelled", comment: ""))
            case (_, .shuttle) where alertSummary is TripShuttleAlertSummary:
                AttributedString(NSLocalizedString("Shuttle bus", comment: ""))
            case (.bus, .suspension) where alertSummary is TripSpecificAlertSummary:
                AttributedString(NSLocalizedString("Bus suspended", comment: ""))
            case (.ferry, .suspension) where alertSummary is TripSpecificAlertSummary:
                AttributedString(NSLocalizedString("Ferry suspended", comment: ""))
            case (_, .suspension) where alertSummary is TripSpecificAlertSummary:
                AttributedString(NSLocalizedString("Train suspended", comment: ""))
            case (_, .stationClosure) where alertSummary is TripSpecificAlertSummary:
                AttributedString(NSLocalizedString("Stop skipped", comment: ""))
            default: AttributedString.tryMarkdown(effect)
            }
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

extension AlertSummary.LocationWholeRoute {
    var modeLabel: String {
        routeType == .bus ? String("\(routeLabel) bus") : routeLabel
    }
}
