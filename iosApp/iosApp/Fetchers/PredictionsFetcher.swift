//
//  PredictionsFetcher.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-02-05.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

class PredictionsFetcher: ObservableObject {
    @Published var connecting: Bool = false
    @Published var predictions: PredictionsStreamDataResponse?
    @Published var error: NSError?
    @Published var errorText: Text?

    let backend: any BackendProtocol
    var channel: PredictionsStopsChannel?

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    @MainActor func run(stopIds: [String]) async {
        connecting = true
        do {
            _ = try await channel?.leave()
            let channel = try await backend.predictionsStopsChannel(stopIds: stopIds)
            _ = try await channel.join()
            self.channel = channel
            error = nil
            errorText = nil
            for await predictions in channel.predictions {
                self.predictions = predictions
            }
        } catch let error as NSError {
            self.error = error
            errorText = getErrorText(error: error)
        }
        connecting = false
    }

    @MainActor func leave() async {
        do {
            _ = try await channel?.leave()
            channel = nil
            predictions = nil
            error = nil
            errorText = nil
        } catch let error as NSError {
            self.error = error
            self.errorText = getErrorText(error: error)
        }
    }

    func getErrorText(error: NSError) -> Text {
        switch error.kotlinException {
        case is Ktor_client_coreHttpRequestTimeoutException:
            Text("Couldn't load real time predictions, no response from the server")
        case is Ktor_ioIOException:
            Text("Couldn't load real time predictions, connection was interrupted")
        case is Ktor_serializationJsonConvertException:
            Text("Couldn't load real time predictions, unable to parse response")
        case is Ktor_client_coreResponseException:
            Text("Couldn't load real time predictions, invalid response")
        default:
            Text("Couldn't load real time predictions, something went wrong")
        }
    }
}
