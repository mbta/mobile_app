//
//  PredictionsFetcher.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-02-05.
//  Copyright © 2024 MBTA. All rights reserved.
//

enum PhoenixChannelError: Error {
    case channelError(String)
}

import Foundation
import os
import shared
import SwiftPhoenixClient

class PredictionsFetcher: ObservableObject {
    @Published var predictions: [Prediction]?
    @Published var socketError: Error?
    let socket: Socket
    var channel: Channel?

    init(socket: Socket) {
        self.socket = socket
    }

    public func subscribeToPredictions(stopIds _: [String]) {
        print("Starting subscription")
        let joinPayload = ["stop_ids": [2433,
                                        2226,
                                        1402]]
        print("JOIN PAYLOAD \(joinPayload)")
        // TODO: - static funcs
        channel = socket.channel("predictions:stops", params: joinPayload)
        channel?.onMessage(callback: { message in
            do {
                print("NEW MESSAGE RECEIVED \(message.event) \(message.payload)")
                let newPredictions = try PredictionsForStops().parseMessage(event: message.event, payload: message.payload)
                DispatchQueue.main.async {
                    self.predictions = newPredictions
                }
            } catch {
                // TODO: Sentry?
                Logger().error("\(error)")
            }
            return message

        })

        channel?.onError { message in
            self.socketError = PhoenixChannelError.channelError(message.payload.debugDescription)
        }
        print("JOINING NOW")
        channel?.join().receive("ok", callback: { message in
            print("JOINED OK \(message)")
        }).receive("error", callback: { message in
            print("ERROR JOINING \(message.payload)")
        })
    }

    func leave() {
        channel?.leave()
        channel = nil
        predictions = nil
    }
}
