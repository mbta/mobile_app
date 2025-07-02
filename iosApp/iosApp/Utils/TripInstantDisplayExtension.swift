//
//  TripInstantDisplayExtension.swift
//  iosApp
//
//  Created by esimon on 6/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

private let timeFormatter: DateFormatter = makeTimeFormatter()

extension TripInstantDisplay {
    func accessibilityLabel(isFirst: Bool, vehicleType: String) -> Text {
        switch onEnum(of: self) {
        case .approaching, .minutes: predictedMinutesLabel(isFirst, vehicleType)
        case .arriving, .now: arrivingLabel(isFirst, vehicleType)
        case .boarding: boardingLabel(isFirst, vehicleType)
        case let .cancelled(trip): cancelledLabel(trip.scheduledTime, isFirst, vehicleType)
        case let .scheduleMinutes(trip): scheduledMinutesLabel(trip.minutes, isFirst, vehicleType)
        case let .scheduleTime(trip): scheduleTimeLabel(trip.scheduledTime, isFirst, vehicleType)
        case .time, .timeWithStatus: predictedTimeLabel(isFirst, vehicleType)
        case let .timeWithSchedule(trip): predictedTimeWithScheduleLabel(trip, isFirst, vehicleType)
        default: Text(verbatim: "")
        }
    }

    private func arrivingLabel(_ isFirst: Bool, _ vehicleType: String) -> Text {
        isFirst
            ? Text("\(vehicleType) arriving now",
                   comment: """
                   Describe that a vehicle is arriving now, as read aloud for VoiceOver users.
                   First value is the type of vehicle (bus, train, ferry). For example, 'bus arriving now'
                   """)
            : Text("and arriving now",
                   comment: """
                   The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                    For example, '[bus arriving in 1 minute], and arriving now'
                   """)
    }

    private func boardingLabel(_ isFirst: Bool, _ vehicleType: String) -> Text {
        isFirst
            ? Text("\(vehicleType) boarding now",
                   comment: """
                   Describe that a vehicle is boarding now, as read aloud for VoiceOver users.
                   First value is the type of vehicle (bus, train, ferry). For example, 'bus boarding now'
                   """)
            : Text("and boarding now",
                   comment: """
                   The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                    For example, '[bus arriving in 1 minute], and boarding now'
                   """)
    }

    private func cancelledLabel(_ scheduledTime: Instant, _ isFirst: Bool, _ vehicleType: String) -> Text {
        let time = timeFormatter.string(from: Date(instant: scheduledTime))
        return isFirst
            ? Text(
                "\(vehicleType) arriving at \(time) cancelled",
                comment: """
                Describe the time at which a cancelled vehicle was scheduled to arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
                For example, 'bus arriving at 10:30AM cancelled'
                """
            )
            : Text(
                "and at \(time) cancelled",
                comment: """
                The second or more cancelled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                For example, '[bus arriving at 10:30AM], and at 10:45 AM cancelled'
                """
            )
    }

    private func delayString(_ scheduledTime: Instant, _ isFirst: Bool, _ vehicleType: String) -> String {
        let time = timeFormatter.string(from: Date(instant: scheduledTime))
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

    private func predictedMinutesLabel(_ isFirst: Bool, _ vehicleType: String) -> Text {
        guard let minutes: Int32 = switch onEnum(of: self) {
        case .approaching: 1
        case let .minutes(trip): trip.minutes
        default: nil
        } else {
            return Text(verbatim: "")
        }

        let minutesFormat = MinutesFormat.companion.from(minutes: minutes)
        return isFirst
            ? predictedMinutesFirstLabel(minutesFormat, vehicleType)
            : predictedMinutesOtherLabel(minutesFormat)
    }

    private func predictedMinutesFirstLabel(_ minutesFormat: MinutesFormat, _ vehicleType: String) -> Text {
        switch onEnum(of: minutesFormat) {
        case let .hour(format):
            Text("\(vehicleType) arriving in \(format.hours, specifier: "%d") hr",
                 comment: """
                 Describe the number of hours until a vehicle will arrive, as read aloud for VoiceOver users.
                 First value is the type of vehicle (bus, train, ferry), second is the number of hours until it arrives
                 For example, '[bus] arriving in [1] hr'
                 """)
        case let .hourMinute(format):
            Text(
                "\(vehicleType) arriving in \(format.hours, specifier: "%d") hr \(format.minutes, specifier: "%d") min",
                comment: """
                Describe the number of hours and minutes until a vehicle will arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is number of hours, and third is minutes until it arrives
                For example, '[bus] arriving in [1] hr [5] min'
                """
            )
        case let .minute(format):
            Text("\(vehicleType) arriving in \(format.minutes, specifier: "%d") min",
                 comment: """
                 Describe the number of minutes until a vehicle will arrive, as read aloud for VoiceOver users.
                 First value is the type of vehicle (bus, train, ferry), second is the number of minutes until it arrives
                 For example, '[bus] arriving in [5] min'
                 """)
        }
    }

    private func predictedMinutesOtherLabel(_ minutesFormat: MinutesFormat) -> Text {
        switch onEnum(of: minutesFormat) {
        case let .hour(format):
            Text("and in \(format.hours, specifier: "%d") hr",
                 comment: """
                   The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                   For example, '[bus arriving in 38 min], and in [1] hr'
                 """)
        case let .hourMinute(format):
            Text("and in \(format.hours, specifier: "%d") hr \(format.minutes, specifier: "%d") min",
                 comment: """
                  The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                  For example, '[bus arriving in 1 hr 5 min], and in [1] hr [45] min'
                 """)
        case let .minute(format):
            Text("and in \(format.minutes, specifier: "%d") min",
                 comment: """
                 The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                 For example, '[bus arriving in 5 minutes], and in [10] min'
                 """)
        }
    }

    private func predictedTimeLabel(_ isFirst: Bool, _ vehicleType: String) -> Text {
        if case let .timeWithStatus(trip) = onEnum(of: self),
           TripInstantDisplay.companion.delayStatuses.contains(trip.status) {
            return Text(delayString(trip.predictionTime, isFirst, vehicleType))
        }

        guard let predictionInstant: Instant = switch onEnum(of: self) {
        case let .time(trip): trip.predictionTime
        case let .timeWithStatus(trip): trip.predictionTime
        default: nil
        } else { return Text(verbatim: "") }

        let predictionTime = timeFormatter.string(from: Date(instant: predictionInstant))
        let timeString = predictedTimeString(predictionTime, isFirst, vehicleType)
        let finalLabel = if case let .timeWithStatus(trip) = onEnum(of: self) {
            "\(timeString), \(trip.status)"
        } else {
            timeString
        }
        return Text(verbatim: finalLabel)
    }

    private func predictedTimeWithScheduleLabel(
        _ trip: TripInstantDisplay.TimeWithSchedule,
        _ isFirst: Bool,
        _ vehicleType: String
    ) -> Text {
        let scheduledTime = timeFormatter.string(from: Date(instant: trip.scheduledTime))
        let scheduleStatus = if trip.predictionTime.epochSeconds >= trip.scheduledTime.epochSeconds {
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
        let predictionTime = timeFormatter.string(from: Date(instant: trip.predictionTime))
        let actualArrival = String(format: NSLocalizedString(
            "arriving at %1$@",
            comment: """
            Screen reader text appended to another string containing an originally scheduled time which is running early or delayed
            compared to the actual predicted time (contained in this string).
            ex, '[10:00 PM train delayed, ]arriving at 10:05 PM'
            """
        ), predictionTime)
        return Text(verbatim: "\(scheduleStatus), \(actualArrival)")
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

    private func scheduledMinutesLabel(_ minutes: Int32, _ isFirst: Bool, _ vehicleType: String) -> Text {
        let minutesFormat = MinutesFormat.companion.from(minutes: minutes)

        return isFirst
            ? scheduledMinutesFirstLabel(minutesFormat, vehicleType)
            : scheduledMinutesOtherLabel(minutesFormat)
    }

    private func scheduledMinutesFirstLabel(_ minutesFormat: MinutesFormat, _ vehicleType: String) -> Text {
        switch onEnum(of: minutesFormat) {
        case let .hour(format):
            Text("\(vehicleType) arriving in \(format.hours, specifier: "%d") hr scheduled",
                 comment: """
                 Describe the number of hours until a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                 First value is the type of vehicle (bus, train, ferry), second is the number of hours until it arrives
                 For example, 'bus arriving in 1 hr scheduled'
                 """)
        case let .hourMinute(format):
            Text(
                "\(vehicleType) arriving in \(format.hours, specifier: "%d") hr \(format.minutes, specifier: "%d") min scheduled",
                comment: """
                Describe the number of hours and minutes until a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                First value is the type of vehicle (bus, train, ferry), second is number of hours, and third is minutes until it arrives
                For example, 'bus arriving in 1 hr 5 min scheduled'
                """
            )
        case let .minute(format):
            Text("\(vehicleType) arriving in \(format.minutes, specifier: "%d") min scheduled",
                 comment: """
                 Describe the number of minutes until a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                 First value is the type of vehicle (bus, train, ferry), second is the number of minutes until it arrives
                 For example, 'bus arriving in 5 minutes, scheduled scheduled'
                 """)
        }
    }

    private func scheduledMinutesOtherLabel(_ minutesFormat: MinutesFormat) -> Text {
        switch onEnum(of: minutesFormat) {
        case let .hour(format):
            Text("and in \(format.hours, specifier: "%d") hr scheduled",
                 comment: """
                 The second or more scheduled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                 For example, '[bus arriving in 38 min], and in [1] hr scheduled'
                 """)
        case let .hourMinute(format):
            Text("and in \(format.hours, specifier: "%d") hr \(format.minutes, specifier: "%d") min scheduled",
                 comment: """
                 The second or more scheduled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                 For example, '[bus arriving in 1 hr 5 min], and in [1] hr [45] min scheduled'
                 """)
        case let .minute(format):
            Text("and in \(format.minutes, specifier: "%d") min scheduled",
                 comment: """
                 The second or more scheduled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                 For example, '[bus arriving in 5 minutes], and in [10] minutes, scheduled'
                 """)
        }
    }

    private func scheduleTimeLabel(_ scheduledTime: Instant, _ isFirst: Bool, _ vehicleType: String) -> Text {
        let time = timeFormatter.string(from: Date(instant: scheduledTime))
        return isFirst
            ? Text("\(vehicleType) arriving at \(time) scheduled",
                   comment: """
                   Describe the time at which a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                   First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
                   For example, 'bus arriving at 10:30AM scheduled'
                   """)
            : Text("and at \(time) scheduled",
                   comment: """
                   The second or more arrival in a list of scheduled upcoming arrivals read aloud for VoiceOver users.
                   For example, '[bus arriving at 10:30AM scheduled], and at 10:45 AM scheduled'
                   """)
    }
}
