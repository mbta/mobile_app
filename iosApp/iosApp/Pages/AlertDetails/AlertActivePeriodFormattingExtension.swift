//
//  AlertActivePeriodFormattingExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 8/8/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

// This is for performing iOS specific date string formatting and localization for AlertDetails
extension shared.Alert.ActivePeriod {
    private func format(instant: Instant, start: Bool) -> AttributedString {
        let date = instant.toNSDate()
        let dateFormat = Date.FormatStyle().weekday(.wide).month().day()
        var formattedDate = date.formatted(dateFormat)
        var formattedTime = date.formatted(date: .omitted, time: .shortened)

        let comp: DateComponents? = if let eastern = TimeZone(identifier: "America/New_York") {
            Calendar.current.dateComponents(in: eastern, from: date)
        } else { nil }
        let hour = comp?.hour
        let minute = comp?.minute

        if start, hour == 3, minute == 0 {
            formattedTime = NSLocalizedString(
                "start of service",
                comment: "Used when an alert begins at the start of a service day"
            )
        } else if !start, hour == 2, minute == 59 {
            formattedTime = NSLocalizedString(
                "end of service",
                comment: "Used when an alert ends at the end of a service day"
            )
            let previousDate = date - (3600.0 * 24)
            formattedDate = previousDate.formatted(dateFormat)
        } else if !start, durationCertainty == .estimated {
            formattedTime = NSLocalizedString(
                "later today",
                comment: "Used when an alert ends at an indeterminite time later the same day"
            )
        }

        do {
            return try AttributedString(markdown: "**\(formattedDate)**, \(formattedTime)")
        } catch {
            return AttributedString("\(formattedDate), \(formattedTime)")
        }
    }

    func formatStart() -> AttributedString {
        format(instant: start, start: true)
    }

    func formatEnd() -> AttributedString {
        guard let end else {
            var furtherNotice = AttributedString(NSLocalizedString(
                "Until further notice",
                comment: "Used when the end of an alert is indefinite"
            ))
            furtherNotice.font = .body.bold()
            return furtherNotice
        }
        return format(instant: end, start: false)
    }
}
