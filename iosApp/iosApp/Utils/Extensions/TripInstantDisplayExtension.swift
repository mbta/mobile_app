//
//  TripInstantDisplayExtension.swift
//  iosApp
//
//  Created by esimon on 6/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

extension TripInstantDisplay {
    func accessibilityLabel(isFirst: Bool, vehicleType: String) -> Text {
        let label = switch onEnum(of: self) {
        case .approaching, .minutes: predictedMinutesLabel(isFirst, vehicleType)
        case .arriving, .now: arrivingLabel(isFirst, vehicleType)
        case .boarding: boardingLabel(isFirst, vehicleType)
        case let .cancelled(trip): cancelledLabel(trip.scheduledTime, isFirst, vehicleType)
        case let .scheduleMinutes(trip): scheduledMinutesLabel(trip.minutes, isFirst, vehicleType)
        case let .scheduleTime(trip): scheduleTimeLabel(trip.scheduledTime, status: nil, isFirst, vehicleType)
        case .time, .timeWithStatus: predictedTimeLabel(isFirst, vehicleType)
        case let .timeWithSchedule(trip): predictedTimeWithScheduleLabel(trip, isFirst, vehicleType)
        case let .scheduleTimeWithStatusColumn(trip): scheduleTimeLabel(
                trip.scheduledTime,
                status: trip.status,
                isFirst,
                vehicleType
            )
        case let .scheduleTimeWithStatusRow(trip): scheduleTimeLabel(
                trip.scheduledTime,
                status: trip.status,
                isFirst,
                vehicleType
            )
        case let .overridden(trip): trip.text
        case .hidden, .skipped: ""
        }

        return Text(verbatim: withLastTripSuffix(label, last: lastTrip()))
    }

    private func arrivingLabel(_ isFirst: Bool, _ vehicleType: String) -> String {
        isFirst
            ? String(format: NSLocalizedString(
                "%@ arriving now",
                comment: """
                Describe that a vehicle is arriving now, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry). For example, 'bus arriving now'
                """
            ), vehicleType)
            : NSLocalizedString(
                "and arriving now",
                comment: """
                The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                 For example, '[bus arriving in 1 minute], and arriving now'
                """
            )
    }

    private func boardingLabel(_ isFirst: Bool, _ vehicleType: String) -> String {
        isFirst
            ? String(format: NSLocalizedString(
                "%@ boarding now",
                comment: """
                Describe that a vehicle is boarding now, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry). For example, 'bus boarding now'
                """
            ), vehicleType)
            : NSLocalizedString(
                "and boarding now",
                comment: """
                The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                 For example, '[bus arriving in 1 minute], and boarding now'
                """
            )
    }

    private func cancelledLabel(_ scheduledTime: EasternTimeInstant, _ isFirst: Bool, _ vehicleType: String) -> String {
        let time = scheduledTime.formatted(date: .omitted, time: .shortened)
        return isFirst
            ? String(format: NSLocalizedString(
                "%@ arriving at %@ cancelled",
                comment: """
                Describe the time at which a cancelled vehicle was scheduled to arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
                For example, 'bus arriving at 10:30AM cancelled'
                """
            ), vehicleType, time)
            : String(format: NSLocalizedString(
                "and at %@ cancelled",
                comment: """
                The second or more cancelled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                For example, '[bus arriving at 10:30AM], and at 10:45 AM cancelled'
                """
            ), time)
    }

    private func delayString(_ scheduledTime: EasternTimeInstant, _ isFirst: Bool, _ vehicleType: String) -> String {
        let time = scheduledTime.formatted(date: .omitted, time: .shortened)
        return isFirst
            ? String(format: NSLocalizedString(
                "%1$@ %2$@ delayed",
                comment: """
                Screen reader text containing an originally scheduled time, which precedes another string containing the delayed predicted time.
                The first interpolated value is the scheduled time, and the second one is the vehicle type ('train', 'bus', or 'ferry').
                ex, '10:00 PM train delayed[, arriving at 10:05 PM]'
                """
            ), time, vehicleType)

            : String(format: NSLocalizedString(
                "and %1$@ %2$@ delayed",
                comment: """
                Screen reader text containing an originally scheduled time, which precedes another string containing
                the delayed predicted time, as the second or later arrival in a list of upcoming arrivals.
                The first interpolated value is the scheduled time, and the second one is the vehicle type ('train', 'bus', or 'ferry').
                ex, 'and 10:00 PM train delayed[, arriving at 10:05 PM]'
                """
            ), time, vehicleType)
    }

    private func predictedMinutesLabel(_ isFirst: Bool, _ vehicleType: String) -> String {
        guard let minutes: Int32 = switch onEnum(of: self) {
        case .approaching: 1
        case let .minutes(trip): trip.minutes
        default: nil
        } else {
            return ""
        }

        let minutesFormat = MinutesFormat.companion.from(minutes: minutes)
        return isFirst
            ? predictedMinutesFirstLabel(minutesFormat, vehicleType)
            : predictedMinutesOtherLabel(minutesFormat)
    }

    private func predictedMinutesFirstLabel(_ minutesFormat: MinutesFormat, _ vehicleType: String) -> String {
        switch onEnum(of: minutesFormat) {
        case let .hour(format):
            String(format: NSLocalizedString(
                "%@ arriving in %d hr",
                comment: """
                Describe the number of hours until a vehicle will arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is the number of hours until it arrives
                For example, '[bus] arriving in [1] hr'
                """
            ), vehicleType, format.hours)
        case let .hourMinute(format):
            String(format: NSLocalizedString(
                "%@ arriving in %d hr %d min",
                comment: """
                Describe the number of hours and minutes until a vehicle will arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is number of hours, and third is minutes until it arrives
                For example, '[bus] arriving in [1] hr [5] min'
                """
            ), vehicleType, format.hours, format.minutes)
        case let .minute(format):
            String(format: NSLocalizedString(
                "%@ arriving in %d min",
                comment: """
                Describe the number of minutes until a vehicle will arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is the number of minutes until it arrives
                For example, '[bus] arriving in [5] min'
                """
            ), vehicleType, format.minutes)
        }
    }

    private func predictedMinutesOtherLabel(_ minutesFormat: MinutesFormat) -> String {
        switch onEnum(of: minutesFormat) {
        case let .hour(format):
            String(format: NSLocalizedString(
                "and in %d hr",
                comment: """
                  The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                  For example, '[bus arriving in 38 min], and in [1] hr'
                """
            ), format.hours)
        case let .hourMinute(format):
            String(format: NSLocalizedString(
                "and in %d hr %d min",
                comment: """
                 The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                 For example, '[bus arriving in 1 hr 5 min], and in [1] hr [45] min'
                """
            ), format.hours, format.minutes)
        case let .minute(format):
            String(format: NSLocalizedString(
                "and in %d min",
                comment: """
                The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                For example, '[bus arriving in 5 minutes], and in [10] min'
                """
            ), format.minutes)
        }
    }

    private func predictedTimeLabel(_ isFirst: Bool, _ vehicleType: String) -> String {
        if case let .timeWithStatus(trip) = onEnum(of: self),
           TripInstantDisplay.companion.delayStatuses.contains(trip.status) {
            return delayString(trip.predictionTime, isFirst, vehicleType)
        }

        guard let predictionInstant: EasternTimeInstant = switch onEnum(of: self) {
        case let .time(trip): trip.predictionTime
        case let .timeWithStatus(trip): trip.predictionTime
        default: nil
        } else { return "" }

        let predictionTime = predictionInstant.formatted(date: .omitted, time: .shortened)
        let timeString = predictedTimeString(predictionTime, isFirst, vehicleType)
        return if case let .timeWithStatus(trip) = onEnum(of: self) {
            "\(timeString), \(trip.status)"
        } else {
            timeString
        }
    }

    private func predictedTimeWithScheduleLabel(
        _ trip: TripInstantDisplay.TimeWithSchedule,
        _ isFirst: Bool,
        _ vehicleType: String
    ) -> String {
        let scheduledTime = trip.scheduledTime.formatted(date: .omitted, time: .shortened)
        let scheduleStatus = if trip.predictionTime.compareTo(other: trip.scheduledTime) >= 0 {
            delayString(trip.scheduledTime, isFirst, vehicleType)
        } else {
            isFirst
                ? String(format: NSLocalizedString(
                    "%1$@ %2$@ early",
                    comment: """
                    Screen reader text containing an originally scheduled time, which precedes another string containing the early predicted time.
                    The first interpolated value is the scheduled time, and the second one is the vehicle type ('train', 'bus', or 'ferry').
                    ex, '10:00 PM train early[, arriving at 9:55 PM]'
                    """
                ), scheduledTime, vehicleType)
                : String(format: NSLocalizedString(
                    "and %1$@ %2$@ early",
                    comment: """
                    Screen reader text containing an originally scheduled time, which precedes another string containing
                    the early predicted time, as the second or later arrival in a list of upcoming arrivals.
                    The first interpolated value is the scheduled time, and the second one is the vehicle type ('train', 'bus', or 'ferry').
                    ex, 'and 10:00 PM train early[, arriving at 9:55 PM]'
                    """
                ), scheduledTime, vehicleType)
        }
        let predictionTime = trip.predictionTime.formatted(date: .omitted, time: .shortened)
        let actualArrival = String(format: NSLocalizedString(
            "arriving at %1$@",
            comment: """
            Screen reader text appended to another string containing an originally scheduled time which is running early or delayed
            compared to the actual predicted time (contained in this string).
            ex, '[10:00 PM train delayed, ]arriving at 10:05 PM'
            """
        ), predictionTime)
        return "\(scheduleStatus), \(actualArrival)"
    }

    private func predictedTimeString(_ predictionTime: String, _ isFirst: Bool, _ vehicleType: String) -> String {
        isFirst
            ? String(format: NSLocalizedString(
                "%1$@ arriving at %2$@",
                comment: """
                Describe the time at which a vehicle will arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
                For example, 'bus arriving at 10:30AM'
                """
            ), vehicleType, predictionTime)
            : String(format: NSLocalizedString(
                "and at %1$@",
                comment: """
                The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                For example, '[bus arriving at 10:30AM], and at 10:45 AM'
                """
            ), predictionTime)
    }

    private func scheduledMinutesLabel(_ minutes: Int32, _ isFirst: Bool, _ vehicleType: String) -> String {
        let minutesFormat = MinutesFormat.companion.from(minutes: minutes)

        return isFirst
            ? scheduledMinutesFirstLabel(minutesFormat, vehicleType)
            : scheduledMinutesOtherLabel(minutesFormat)
    }

    private func scheduledMinutesFirstLabel(_ minutesFormat: MinutesFormat, _ vehicleType: String) -> String {
        switch onEnum(of: minutesFormat) {
        case let .hour(format):
            String(format: NSLocalizedString(
                "%@ arriving in %d hr scheduled",
                comment: """
                Describe the number of hours until a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is the number of hours until it arrives
                For example, 'bus arriving in 1 hr scheduled'
                """
            ), vehicleType, format.hours)
        case let .hourMinute(format):
            String(format: NSLocalizedString(
                "%@ arriving in %d hr %d min scheduled",
                comment: """
                Describe the number of hours and minutes until a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is number of hours, and third is minutes until it arrives
                For example, 'bus arriving in 1 hr 5 min scheduled'
                """
            ), vehicleType, format.hours, format.minutes)
        case let .minute(format):
            String(format: NSLocalizedString(
                "%@ arriving in %d min scheduled",
                comment: """
                Describe the number of minutes until a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is the number of minutes until it arrives
                For example, 'bus arriving in 5 minutes, scheduled scheduled'
                """
            ), vehicleType, format.minutes)
        }
    }

    private func scheduledMinutesOtherLabel(_ minutesFormat: MinutesFormat) -> String {
        switch onEnum(of: minutesFormat) {
        case let .hour(format):
            String(format: NSLocalizedString(
                "and in %d hr scheduled",
                comment: """
                The second or more scheduled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                For example, '[bus arriving in 38 min], and in [1] hr scheduled'
                """
            ), format.hours)
        case let .hourMinute(format):
            String(format: NSLocalizedString(
                "and in %d hr %d min scheduled",
                comment: """
                The second or more scheduled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                For example, '[bus arriving in 1 hr 5 min], and in [1] hr [45] min scheduled'
                """
            ), format.hours, format.minutes)
        case let .minute(format):
            String(format: NSLocalizedString(
                "and in %d min scheduled",
                comment: """
                The second or more scheduled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                For example, '[bus arriving in 5 minutes], and in [10] minutes, scheduled'
                """
            ), format.minutes)
        }
    }

    private func scheduleTimeLabel(
        _ scheduledTime: EasternTimeInstant,
        status: String?,
        _ isFirst: Bool,
        _ vehicleType: String
    ) -> String {
        if let status, TripInstantDisplay.companion.delayStatuses.contains(status) {
            return delayString(scheduledTime, isFirst, vehicleType)
        }

        let scheduledTime = scheduledTime.formatted(date: .omitted, time: .shortened)
        let timeString = scheduleTimeString(scheduledTime, isFirst, vehicleType)
        return if let status {
            "\(timeString), \(status)"
        } else {
            timeString
        }
    }

    private func scheduleTimeString(_ time: String, _ isFirst: Bool, _ vehicleType: String) -> String {
        isFirst
            ? String(format: NSLocalizedString(
                "%@ arriving at %@ scheduled",
                comment: """
                Describe the time at which a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
                For example, 'bus arriving at 10:30AM scheduled'
                """
            ), vehicleType, time)
            : String(format: NSLocalizedString(
                "and at %@ scheduled",
                comment: """
                The second or more arrival in a list of scheduled upcoming arrivals read aloud for VoiceOver users.
                For example, '[bus arriving at 10:30AM scheduled], and at 10:45 AM scheduled'
                """
            ), time)
    }

    private func lastTrip() -> Bool {
        switch onEnum(of: self) {
        case let .approaching(trip): trip.last
        case let .arriving(trip): trip.last
        case let .boarding(trip): trip.last
        case let .minutes(trip): trip.last
        case let .now(trip): trip.last
        case let .overridden(trip): trip.last
        case let .scheduleMinutes(trip): trip.last
        case let .scheduleTime(trip): trip.last
        case let .scheduleTimeWithStatusColumn(trip): trip.last
        case let .time(trip): trip.last
        case let .timeWithSchedule(trip): trip.last
        case let .timeWithStatus(trip): trip.last
        case .cancelled, .hidden, .skipped, .scheduleTimeWithStatusRow: false
        }
    }

    private func withLastTripSuffix(_ label: String, last: Bool) -> String {
        if last {
            let lastTripString = NSLocalizedString(
                "Last trip",
                comment: """
                Screen reader text for indicating the last trip of the day,
                read out in conjunction with a prediction or schedule.
                """
            )
            return "\(label), \(lastTripString)"
        } else {
            return label
        }
    }
}
