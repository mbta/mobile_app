//
//  AlertActivePeriodFormattingExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 8/8/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

/**
 This is for performing iOS specific date string formatting and localization for AlertDetails.
 This includes special handling to check if the alert starts at the beginning of the service day,
 ends at the end of the service day, or should be shown as ending "later today".

 - Parameters:
     - instant: The instant of the active period to format.
     - isStart: True if the provided instant is the start of the period, false if it's the end.
 - Returns: A localized and formatted string describing the active period.
  */
extension shared.Alert.ActivePeriod {
    private func format(instant: Instant, isStart: Bool) -> AttributedString {
        let date = instant.toNSDate()
        let dateFormat = Date.FormatStyle().weekday(.wide).month().day()
        var formattedDate = date.formatted(dateFormat)
        var formattedTime = date.formatted(date: .omitted, time: .shortened)

        let comp: DateComponents? = if let eastern = TimeZone(identifier: "America/New_York") {
            Calendar.current.dateComponents(in: eastern, from: date)
        } else { nil }
        let hour = comp?.hour
        let minute = comp?.minute

        if isStart, hour == 3, minute == 0 {
            formattedTime = NSLocalizedString(
                "start of service",
                comment: "Used when an alert begins at the start of a service day"
            )
        } else if !isStart, hour == 2, minute == 59 {
            formattedTime = NSLocalizedString(
                "end of service",
                comment: "Used when an alert ends at the end of a service day"
            )
            let previousDate = date - (60 * 60 * 24)
            formattedDate = previousDate.formatted(dateFormat)
        } else if !isStart, durationCertainty == .estimated {
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
        format(instant: start, isStart: true)
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
        return format(instant: end, isStart: false)
    }
}
