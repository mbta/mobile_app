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

    var hours: Int32 {
        Int32(
            (Float(minutes) / 60).rounded(FloatingPointRoundingRule.down)
        )
    }

    var remainingMinutes: Int32 {
        minutes - (hours * 60)
    }

    var predictionKey: String {
        if hours >= 1 {
            if remainingMinutes == 0 {
                String(format: NSLocalizedString(
                    "**%ld** hr",
                    comment: "Shorthand displayed number of hours and minutes until arrival, ex \"1 hr 32 min\""
                ), hours)
            } else {
                String(format: NSLocalizedString(
                    "**%ld** hr **%ld** min",
                    comment: "Shorthand displayed number of hours and minutes until arrival, ex \"1 hr 32 min\""
                ), hours, remainingMinutes)
            }
        } else {
            String(format: NSLocalizedString(
                "**%ld** min",
                comment: "Shorthand displayed number of minutes until arrival, ex \"12 min\""
            ), minutes)
        }
    }

    var predictionString: AttributedString {
        do {
            return try AttributedString(markdown: predictionKey)
        } catch {
            return AttributedString(predictionKey.filter { $0 != "*" })
        }
    }

    var accessibilityString: String {
        if hours >= 1 {
            String(format: NSLocalizedString(
                "in %ld hr %ld min",
                comment: """
                Shorthand displayed number of hours and minutes until arrival for VoiceOver,
                ex 'in 1 hr 32 min'
                """
            ),
            hours,
            remainingMinutes)
        } else {
            String(format: NSLocalizedString(
                "in %ld min",
                comment: "Shorthand displayed number of minutes until arrival for VoiceOver, ex 'in 7 min'"
            ), minutes)
        }
    }

    var body: some View {
        Text(predictionString)
            .accessibilityLabel(accessibilityString)
    }
}
