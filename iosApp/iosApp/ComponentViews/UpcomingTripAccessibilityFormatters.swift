//
//  UpcomingTripAccessibilityFormatters.swift
//  iosApp
//
//  Created by esimon on 10/31/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

class UpcomingTripAccessibilityFormatters {
    private let timeFormatter: DateFormatter = makeTimeFormatter()

    public func boardingFirst(vehicleText: String) -> Text {
        Text("\(vehicleText) boarding now",
             comment: """
             Describe that a vehicle is boarding now, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry). For example, 'bus boarding now'
             """)
    }

    public func boardingOther() -> Text {
        Text("and boarding now",
             comment: """
             The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
              For example, '[bus arriving in 1 minute], and boarding now'
             """)
    }

    public func arrivingFirst(vehicleText: String) -> Text {
        Text("\(vehicleText) arriving now",
             comment: """
             Describe that a vehicle is arriving now, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry). For example, 'bus arriving now'
             """)
    }

    public func arrivingOther() -> Text {
        Text("and arriving now",
             comment: """
             The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
              For example, '[bus arriving in 1 minute], and arriving now'
             """)
    }

    public func distantFutureFirst(date: Date, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving at \(timeFormatter.string(from: date))",
             comment: """
             Describe the time at which a vehicle will arrive, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
             For example, 'bus arriving at 10:30AM'
             """)
    }

    public func distantFutureOther(date: Date) -> Text {
        Text("and at \(timeFormatter.string(from: date))",
             comment: """
             The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
             For example, '[bus arriving at 10:30AM], and at 10:45 AM'
             """)
    }

    public func scheduleTimeFirst(date: Date, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving at \(timeFormatter.string(from: date)) scheduled",
             comment: """
             Describe the time at which a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
             For example, 'bus arriving at 10:30AM scheduled'
             """)
    }

    public func scheduleTimeOther(date: Date) -> Text {
        Text("and at \(timeFormatter.string(from: date)) scheduled",
             comment: """
             The second or more arrival in a list of scheduled upcoming arrivals read aloud for VoiceOver users.
             For example, '[bus arriving at 10:30AM scheduled], and at 10:45 AM scheduled'
             """)
    }

    public func scheduleMinutesFirst(minutes: Int32, vehicleText: String) -> Text {
        let countdown = Minutes(minutes)
        let hours = countdown.hours
        let remainingMinutes = countdown.remainingMinutes

        return if hours >= 1 {
            if remainingMinutes == 0 {
                Text("\(vehicleText) arriving in \(hours, specifier: "%d") hr scheduled",
                     comment: """
                     Describe the number of hours until a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                     First value is the type of vehicle (bus, train, ferry), second is the number of hours until it arrives
                     For example, 'bus arriving in 1 hr scheduled'
                     """)
            } else {
                Text(
                    "\(vehicleText) arriving in \(hours, specifier: "%d") hr \(remainingMinutes, specifier: "%d") min scheduled",
                    comment: """
                    Describe the number of hours and minutes until a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                    First value is the type of vehicle (bus, train, ferry), second is number of hours, and third is minutes until it arrives
                    For example, 'bus arriving in 1 hr 5 min scheduled'
                    """
                )
            }
        } else {
            Text("\(vehicleText) arriving in \(minutes, specifier: "%d") min scheduled",
                 comment: """
                 Describe the number of minutes until a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
                 First value is the type of vehicle (bus, train, ferry), second is the number of minutes until it arrives
                 For example, 'bus arriving in 5 minutes, scheduled scheduled'
                 """)
        }
    }

    public func scheduleMinutesOther(minutes: Int32) -> Text {
        let countdown = Minutes(minutes)
        let hours = countdown.hours
        let remainingMinutes = countdown.remainingMinutes

        return if hours >= 1 {
            if remainingMinutes == 0 {
                Text("and in \(hours, specifier: "%d") hr scheduled",
                     comment: """
                     The second or more scheduled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                     For example, '[bus arriving in 38 min], and in [1] hr scheduled'
                     """)
            } else {
                Text("and in \(hours, specifier: "%d") hr \(remainingMinutes, specifier: "%d") min scheduled",
                     comment: """
                     The second or more scheduled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                     For example, '[bus arriving in 1 hr 5 min], and in [1] hr [45] min scheduled'
                     """)
            }
        } else {
            Text("and in \(minutes, specifier: "%d") min scheduled",
                 comment: """
                 The second or more scheduled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                 For example, '[bus arriving in 5 minutes], and in [10] minutes, scheduled'
                 """)
        }
    }

    public func predictionMinutesFirst(minutes: Int32, vehicleText: String) -> Text {
        let countdown = Minutes(minutes)
        let hours = countdown.hours
        let remainingMinutes = countdown.remainingMinutes

        return if hours >= 1 {
            if remainingMinutes == 0 {
                Text("\(vehicleText) arriving in \(hours, specifier: "%d") hr",
                     comment: """
                     Describe the number of hours until a vehicle will arrive, as read aloud for VoiceOver users.
                     First value is the type of vehicle (bus, train, ferry), second is the number of hours until it arrives
                     For example, '[bus] arriving in [1] hr'
                     """)
            } else {
                Text("\(vehicleText) arriving in \(hours, specifier: "%d") hr \(remainingMinutes, specifier: "%d") min",
                     comment: """
                     Describe the number of hours and minutes until a vehicle will arrive, as read aloud for VoiceOver users.
                     First value is the type of vehicle (bus, train, ferry), second is number of hours, and third is minutes until it arrives
                     For example, '[bus] arriving in [1] hr [5] min'
                     """)
            }
        } else {
            Text("\(vehicleText) arriving in \(minutes, specifier: "%d") min",
                 comment: """
                 Describe the number of minutes until a vehicle will arrive, as read aloud for VoiceOver users.
                 First value is the type of vehicle (bus, train, ferry), second is the number of minutes until it arrives
                 For example, '[bus] arriving in [5] min'
                 """)
        }
    }

    public func predictionMinutesOther(minutes: Int32) -> Text {
        let countdown = Minutes(minutes)
        let hours = countdown.hours
        let remainingMinutes = countdown.remainingMinutes

        return if hours >= 1 {
            if remainingMinutes == 0 {
                Text("and in \(hours, specifier: "%d") hr",
                     comment: """
                       The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                       For example, '[bus arriving in 38 min], and in [1] hr'
                     """)
            } else {
                Text("and in \(hours, specifier: "%d") hr \(remainingMinutes, specifier: "%d") min",
                     comment: """
                      The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                      For example, '[bus arriving in 1 hr 5 min], and in [1] hr [45] min'
                     """)
            }
        } else {
            Text("and in \(minutes, specifier: "%d") min",
                 comment: """
                 The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
                 For example, '[bus arriving in 5 minutes], and in [10] min'
                 """)
        }
    }

    public func predictionTimeFirst(date: Date, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving at \(timeFormatter.string(from: date))",
             comment: """
             Describe the time at which a vehicle will arrive, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
             For example, 'bus arriving at 10:30AM'
             """)
    }

    public func predictionTimeOther(date: Date) -> Text {
        Text("and at \(timeFormatter.string(from: date))",
             comment: """
             The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
             For example, '[bus arriving at 10:30AM], and at 10:45 AM'
             """)
    }

    public func cancelledFirst(date: Date, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving at \(timeFormatter.string(from: date)) cancelled",
             comment: """
             Describe the time at which a cancelled vehicle was scheduled to arrive, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
             For example, 'bus arriving at 10:30AM cancelled'
             """)
    }

    public func cancelledOther(date: Date) -> Text {
        Text("and at \(timeFormatter.string(from: date)) cancelled",
             comment: """
             The second or more cancelled arrival in a list of upcoming arrivals read aloud for VoiceOver users.
             For example, '[bus arriving at 10:30AM], and at 10:45 AM cancelled'
             """)
    }
}
