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

    var predictionKey: String.LocalizationValue {
        let prediction: String.LocalizationValue = if hours >= 1 {
            "\(hours, specifier: "%ld") hr \(remainingMinutes, specifier: "%ld") min"
        } else {
            "\(minutes, specifier: "%ld") min"
        }
        return prediction
    }

    var predictionString: AttributedString {
        var prediction = AttributedString(localized: predictionKey)
        for run in prediction.runs {
            if run.localizedNumericArgument != nil {
                prediction[run.range].font = Typography.headlineBold
            } else {
                prediction[run.range].font = Typography.body
            }
        }
        return prediction
    }

    var accessibilityKey: String.LocalizationValue {
        let prediction: String.LocalizationValue = if hours >= 1 {
            "in \(hours, specifier: "%ld") hr \(remainingMinutes, specifier: "%ld") min"
        } else {
            "in \(minutes, specifier: "%ld") min"
        }
        return prediction
    }

    var accessibilityString: String {
        String(localized: accessibilityKey)
    }

    var body: some View {
        Text(predictionString)
            .accessibilityLabel(accessibilityString)
    }
}
