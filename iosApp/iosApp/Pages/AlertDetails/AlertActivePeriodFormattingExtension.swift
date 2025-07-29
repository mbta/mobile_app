//
//  AlertActivePeriodFormattingExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 8/8/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared

/**
 This is for performing iOS specific date string formatting and localization for AlertDetails.
 This includes special handling to check if the alert starts at the beginning of the service day,
 ends at the end of the service day, or should be shown as ending "later today".

 - Parameters:
     - instant: The instant of the active period to format.
     - isStart: True if the provided instant is the start of the period, false if it's the end.
 - Returns: A localized and formatted string describing the active period.
  */
extension Shared.Alert.ActivePeriod {
    private func format(instant: EasternTimeInstant, isStart: Bool) -> AttributedString {
        let dateFormat = EasternTimeInstant.FormatStyle().weekday(.wide).month().day()
        var formattedDate = instant.formatted(dateFormat)
        var formattedTime = instant.formatted(date: .omitted, time: .shortened)

        if isStart, fromStartOfService {
            formattedTime = NSLocalizedString(
                "start of service",
                comment: "Used when an alert begins at the start of a service day"
            )
        } else if !isStart, toEndOfService {
            formattedTime = NSLocalizedString(
                "end of service",
                comment: "Used when an alert ends at the end of a service day"
            )
            let previousDate = instant.minus(hours: 24)
            formattedDate = previousDate.formatted(dateFormat)
        } else if !isStart, endingLaterToday {
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
