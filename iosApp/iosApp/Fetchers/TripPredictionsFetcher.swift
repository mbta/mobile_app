//
//  TripPredictionsFetcher.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-08.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import os
import shared
import SwiftPhoenixClient
import SwiftUI

class TripPredictionsFetcher: ObservableObject {
    @Published var predictions: PredictionsStreamDataResponse?
    @Published var socketError: Error?
    @Published var errorText: Text?

    let socket: PhoenixSocket
    var channel: PhoenixChannel?
    var onMessageSuccessCallback: (() -> Void)?
    var onErrorCallback: (() -> Void)?

    init(socket: PhoenixSocket, onMessageSuccessCallback: (() -> Void)? = nil, onErrorCallback: (() -> Void)? = nil) {
        self.socket = socket
        self.onMessageSuccessCallback = onMessageSuccessCallback
        self.onErrorCallback = onErrorCallback
    }

    func run(tripId: String) {
        leave()
        socket.attach()
        channel = socket.getChannel(topic: "predictions:trip:\(tripId)", params: [:])

        channel?.onEvent(event: PredictionsForStopsChannel.companion.newDataEvent, callback: { message in
            self.handleNewDataMessage(message: message)
        })
        channel?.onFailure { message in
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("A: \(message.body)")
                self.errorText = Text("Failed to load new predictions, something went wrong")
                if let callback = self.onErrorCallback {
                    callback()
                }
            }
        }

        channel?.onDetach { message in
            Logger().debug("leaving channel \(message.subject)")
        }
        channel?.attach().receive(status: .ok) { message in
            Logger().debug("joined channel \(message.subject)")
            self.handleNewDataMessage(message: message)
        }.receive(status: .error, callback: { message in
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("B: \(message.body)")
                self.errorText = Text("Failed to load predictions, could not connect to the server")
                if let callback = self.onErrorCallback {
                    callback()
                }
            }
        })
    }

    private func handleNewDataMessage(message: PhoenixMessage) {
        do {
            let rawPayload: String? = message.jsonBody

            if let stringPayload = rawPayload {
                let newPredictions = try PredictionsForStopsChannel.companion
                    .parseMessage(payload: stringPayload)

                Logger().debug("Received \(newPredictions.predictions.count) predictions")
                DispatchQueue.main.async {
                    self.predictions = newPredictions
                    self.socketError = nil
                    self.errorText = nil
                    if let callback = self.onMessageSuccessCallback {
                        callback()
                    }
                }
            } else {
                Logger().error("No jsonPayload found for message \(message.body)")
                if let callback = onErrorCallback {
                    callback()
                }
            }

        } catch {
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("C: \(message.body)")
                self.errorText = Text("Failed to load new predictions, something went wrong")
            }
            Logger().error("\(error)")
        }
    }

    func leave() {
        channel?.detach()
        channel = nil
        predictions = nil
        errorText = nil
        socketError = nil
    }
}
