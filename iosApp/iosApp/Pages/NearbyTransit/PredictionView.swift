//
//  PredictionView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

extension Prediction.FormatOverridden {
    func textWithLocale() -> AttributedString {
        var result = AttributedString(text)
        result.languageIdentifier = "en-US"
        return result
    }
}

struct PredictionView: View {
    let prediction: State

    enum State: Equatable {
        case loading
        case none
        case some(Prediction.Format)
    }

    var body: some View {
        let predictionView: any View = switch prediction {
        case let .some(prediction):
            switch onEnum(of: prediction) {
            case let .overridden(overridden):
                Text(overridden.textWithLocale())
            case .hidden:
                // should have been filtered out already
                Text(verbatim: "")
            case .boarding:
                Text("BRD")
            case .arriving:
                Text("ARR")
            case .approaching:
                Text("1 min")
            case let .distantFuture(format):
                Text(Date(instant: format.predictionTime), style: .time)
            case let .minutes(format):
                Text("\(format.minutes, specifier: "%ld") min")
            }
        case .none:
            Text("No Predictions")
        case .loading:
            ProgressView()
        }
        AnyView(predictionView)
            .frame(minWidth: 48, alignment: .trailing)
    }
}
