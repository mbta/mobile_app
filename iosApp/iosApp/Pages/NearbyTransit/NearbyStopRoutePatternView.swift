//
//  NearbyStopRoutePatternView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct NearbyStopRoutePatternView: View {
    let headsign: String
    let predictions: PredictionState

    struct PredictionWithFormat: Identifiable {
        let prediction: Prediction
        let format: Prediction.Format

        var id: String { prediction.id }

        init(_ prediction: PredictionWithVehicle, now: Instant) {
            self.prediction = prediction.prediction
            format = prediction.format(now: now)
        }

        func isHidden() -> Bool {
            format is Prediction.FormatHidden
        }
    }

    enum PredictionState {
        case loading
        case none
        case some([PredictionWithFormat])

        static func from(predictions: [PredictionWithVehicle]?, now: Instant) -> Self {
            guard let predictions else { return .loading }
            let predictionsToShow = predictions
                .map { PredictionWithFormat($0, now: now) }
                .filter { !$0.isHidden() }
                .prefix(2)
            if predictionsToShow.isEmpty {
                return .none
            }
            return .some(Array(predictionsToShow))
        }
    }

    var body: some View {
        HStack {
            Text(headsign)
            Spacer()
            switch predictions {
            case let .some(predictions):
                ForEach(predictions) { prediction in
                    PredictionView(prediction: .some(prediction.format))
                }
            case .none:
                PredictionView(prediction: .none)
            case .loading:
                PredictionView(prediction: .loading)
            }
        }
    }
}
