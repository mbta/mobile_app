//
//  AlertActivePeriodExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 8/8/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

extension shared.Alert.ActivePeriod {
    private func format(instant: Instant) -> AttributedString {
        let date = instant.toNSDate()
        let formattedDate = date.formatted(Date.FormatStyle().weekday(.wide).month().day())
        let formattedTime = date.formatted(date: .omitted, time: .shortened)
        do {
            return try AttributedString(markdown: "**\(formattedDate)**  \(formattedTime)")
        } catch {
            return AttributedString("\(formattedDate)  \(formattedTime)")
        }
    }

    func formatStart() -> AttributedString {
        format(instant: start)
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
        return format(instant: end)
    }
}
