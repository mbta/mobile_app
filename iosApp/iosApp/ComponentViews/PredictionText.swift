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
    var predictionKey: String.LocalizationValue {
        let hours = Int32(
            (Float(minutes) / 60).rounded(FloatingPointRoundingRule.down)
        )
        let remainingMinutes = minutes - (hours * 60)
        let prediction: String.LocalizationValue = if hours >= 1 {
            "in \(hours, specifier: "%ld") hr \(remainingMinutes, specifier: "%ld") min"
        } else {
            "in \(minutes, specifier: "%ld") min"
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

    var body: some View {
        Text(predictionString)
            .accessibilityLabel(String(localized: predictionKey))
    }
}
