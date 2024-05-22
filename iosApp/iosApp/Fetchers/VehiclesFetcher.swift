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
    var channel: PhoenixChannel?
    var onMessageSuccessCallback: (() -> Void)?
    var onErrorCallback: (() -> Void)?

    init(
        socket: PhoenixSocket,
        onMessageSuccessCallback: (() -> Void)? = nil,
        onErrorCallback: (() -> Void)? = nil,
        vehicles: [Vehicle]? = nil
    ) {
        self.socket = socket
        self.onMessageSuccessCallback = onMessageSuccessCallback
        self.onErrorCallback = onErrorCallback
        self.vehicles = vehicles
    }

    func run(routeId: String, directionId: Int) {
        socket.attach()
        let joinPayload = VehiclesOnRouteChannel.shared.joinPayload(routeId: routeId, directionId: Int32(directionId))
        channel = socket.getChannel(topic: VehiclesOnRouteChannel.shared.topic, params: joinPayload)

        channel?.onEvent(event: VehiclesOnRouteChannel.shared.newDataEvent, callback: { message in
            self.handleNewDataMessage(message: message)
        })
        channel?.onFailure { message in
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("A: \(message.body)")
                self.errorText = Text("Failed to load new vehicles, something went wrong")
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
                self.errorText = Text("Failed to load vehicles, could not connect to the server")
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
                Logger().error("No jsonPayload found for message \(message.body)")
                if let callback = onErrorCallback {
                    callback()
                }
            }

        } catch {
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("C: \(message.body)")
                self.errorText = Text("Failed to load new vehicles, something went wrong")
            }
            Logger().error("\(error)")
        }
    }

    func leave() {
        channel?.detach()
        channel = nil
        vehicles = nil
        errorText = nil
        socketError = nil
    }
}
