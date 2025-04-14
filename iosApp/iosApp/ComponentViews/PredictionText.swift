//
//  PredictionText.swift
//  iosApp
//
//  Created by Simon, Emma on 5/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct PredictionText: View {
    var minutes: Int32

    var countdown: Minutes {
        Minutes(minutes)
    }

    var predictionKey: String {
        if countdown.hours >= 1 {
            if countdown.remainingMinutes == 0 {
                String(format: NSLocalizedString(
                    "**%ld** hr",
                    comment: "Shorthand displayed number of hours and minutes until arrival, ex \"1 hr\""
                ), countdown.hours)
            } else {
                String(format: NSLocalizedString(
                    "**%ld** hr **%ld** min",
                    comment: "Shorthand displayed number of hours and minutes until arrival, ex \"1 hr 32 min\""
                ), countdown.hours, countdown.remainingMinutes)
            }
        } else {
            String(format: NSLocalizedString(
                "**%ld** min",
                comment: "Shorthand displayed number of minutes until arrival, ex \"12 min\""
            ), countdown.minutes)
        }
    }

    var predictionString: AttributedString {
        return AttributedString.boldOrDefault(predictionKey)
    }

    var accessibilityString: String {
        if countdown.hours >= 1 {
            if countdown.remainingMinutes == 0 {
                String(format: NSLocalizedString(
                    "in %ld hr",
                    comment: """
                    Shorthand displayed number of hours  until arrival for VoiceOver, ex 'in 1 hr'
                    """
                ), countdown.hours)
            } else {
                String(format: NSLocalizedString(
                    "in %ld hr %ld min",
                    comment: """
                    Shorthand displayed number of hours and minutes until arrival for VoiceOver,
                    ex 'in 1 hr 32 min'
                    """
                ),
                countdown.hours,
                countdown.remainingMinutes)
            }
        } else {
            String(format: NSLocalizedString(
                "in %ld min",
                comment: "Shorthand displayed number of minutes until arrival for VoiceOver, ex 'in 7 min'"
            ), countdown.minutes)
        }
    }

    var body: some View {
        Text(predictionString)
            .accessibilityLabel(accessibilityString)
    }
}
