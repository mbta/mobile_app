//
//  PredictionsFetcher.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-02-05.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class PredictionsFetcher: ObservableObject {
    @Published var predictions: [Prediction]?
    let backend: any BackendProtocol
    var channel: PredictionsStopsChannel?

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    @MainActor func run(stopIds: [String]) async throws {
        let _ = try await channel?.leave()
        let channel = try await backend.predictionsStopsChannel(stopIds: stopIds)
        let _ = try await channel.join()
        self.channel = channel
        for await predictions in channel.predictions {
            self.predictions = predictions
        }
    }

    func leave() async throws {
        let _ = try await channel?.leave()
        channel = nil
        predictions = nil
    }
}
