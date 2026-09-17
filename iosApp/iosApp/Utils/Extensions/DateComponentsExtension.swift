//
//  DateComponentsExtension.swift
//  iosApp
//
//  Created by Kayla Brady on 9/16/26.
//  Copyright © 2026 MBTA. All rights reserved.
//
import Foundation
import Shared

public extension DateComponents {
    var nextDate: Date {
        get {
            let calendar = Calendar(identifier: .iso8601)
            let beforeDayStart = calendar.startOfDay(for: .now).addingTimeInterval(-0.01)
            return calendar.nextDate(after: beforeDayStart, matching: self, matchingPolicy: .strict)!
        }
        set {
            // in this file, we only use hour/minute/second
            let components: Set<Calendar.Component> = [.hour, .minute, .second]
            let calendar = Calendar(identifier: .iso8601)
            self = calendar.dateComponents(components, from: newValue)
        }
    }

    static func fromLocalTime(_ localTime: Kotlinx_datetimeLocalTime) -> Self {
        .init(
            hour: Int(localTime.hour),
            minute: Int(localTime.minute),
            second: Int(localTime.second)
        )
    }

    func toLocalTime() -> Kotlinx_datetimeLocalTime {
        .init(
            hour: Int32(hour ?? 0),
            minute: Int32(minute ?? 0),
            second: Int32(second ?? 0),
            nanosecond: Int32(nanosecond ?? 0)
        )
    }

    func formatHour() -> String? {
        guard let date = Calendar.current.date(from: self) else { return nil }

        return date.formatted(.dateTime.hour(.defaultDigits(amPM: .abbreviated)))
    }
}
