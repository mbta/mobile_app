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
    var predictionString: AttributedString {
        var prediction = AttributedString(localized: "\(minutes, specifier: "%ld") min")
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
            .accessibilityLabel("in \(minutes) min")
    }
}
