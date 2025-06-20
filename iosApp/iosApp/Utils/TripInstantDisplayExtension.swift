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
        case .time, .timeWithSchedule, .timeWithStatus: predictedTimeLabel(isFirst, vehicleType)
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
        guard let predictionInstant: Instant = switch onEnum(of: self) {
        case let .time(trip): trip.predictionTime
        case let .timeWithSchedule(trip): trip.predictionTime
        case let .timeWithStatus(trip): trip.predictionTime
        default: nil
        } else { return Text(verbatim: "") }

        let predictionTime = timeFormatter.string(from: Date(instant: predictionInstant))
        return isFirst
            ? Text("\(vehicleType) arriving at \(predictionTime)",
                   comment: """
                   Describe the time at which a vehicle will arrive, as read aloud for VoiceOver users.
                   First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
                   For example, 'bus arriving at 10:30AM'
                   """)
            : Text("and at \(predictionTime)",
                   comment: """
                   The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                   For example, '[bus arriving at 10:30AM], and at 10:45 AM'
                   """)
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
