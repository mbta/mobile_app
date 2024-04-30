//
//  VehiclesFetcher.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import os
import shared
import SwiftPhoenixClient
import SwiftUI

class VehiclesFetcher: ObservableObject {
    @Published var response: VehiclesStreamDataResponse?
    @Published var vehicles: [Vehicle]?
    @Published var socketError: Error?
    @Published var errorText: Text?

    let socket: PhoenixSocket
    var channel: Channel?
    var onMessageSuccessCallback: (() -> Void)?
    var onErrorCallback: (() -> Void)?

    init(socket: PhoenixSocket, onMessageSuccessCallback: (() -> Void)? = nil, onErrorCallback: (() -> Void)? = nil) {
        self.socket = socket
        self.onMessageSuccessCallback = onMessageSuccessCallback
        self.onErrorCallback = onErrorCallback
    }

    func run(routeId: String, directionId: Int) {
        socket.connect()
        let joinPayload = VehiclesOnRouteChannel.shared.joinPayload(routeId: routeId, directionId: Int32(directionId))
        channel = socket.channel(VehiclesOnRouteChannel.shared.topic, params: joinPayload)

        channel?.on(VehiclesOnRouteChannel.shared.newDataEvent, callback: { message in
            self.handleNewDataMessage(message: message)
        })
        channel?.onError { message in
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("A: \(message.payload)")
                self.errorText = Text("Failed to load new vehicles, something went wrong")
                if let callback = self.onErrorCallback {
                    callback()
                }
            }
        }

        channel?.onClose { message in
            Logger().debug("leaving channel \(message.topic)")
        }
        channel?.join().receive("ok") { message in
            Logger().debug("joined channel \(message.topic)")
            self.handleNewDataMessage(message: message)
        }.receive("error", callback: { message in
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("B: \(message.payload)")
                self.errorText = Text("Failed to load vehicles, could not connect to the server")
                if let callback = self.onErrorCallback {
                    callback()
                }
            }
        })
    }

    private func handleNewDataMessage(message: SwiftPhoenixClient.Message) {
        do {
            let rawPayload: String? = message.jsonPayload()

            if let stringPayload = rawPayload {
                let newVehicles = try VehiclesOnRouteChannel.shared
                    .parseMessage(payload: stringPayload)
                Logger().debug("Received \(newVehicles.vehicles.count) vehicles")
                DispatchQueue.main.async {
                    self.response = newVehicles
                    self.vehicles = Array(newVehicles.vehicles.values)
                    self.socketError = nil
                    self.errorText = nil
                    if let callback = self.onMessageSuccessCallback {
                        callback()
                    }
                }
            } else {
                Logger().error("No jsonPayload found for message \(message.payload)")
                if let callback = onErrorCallback {
                    callback()
                }
            }

        } catch {
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("C: \(message.payload)")
                self.errorText = Text("Failed to load new vehicles, something went wrong")
            }
            Logger().error("\(error)")
        }
    }

    func leave() {
        channel?.leave()
        channel = nil
        vehicles = nil
        errorText = nil
        socketError = nil
    }
}
