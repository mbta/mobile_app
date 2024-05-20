//
//  VehicleFetcher.swift
//  iosApp
//
//  Created by Brady, Kayla on 5/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import os
import shared
import SwiftPhoenixClient
import SwiftUI

class VehicleFetcher: ObservableObject {
    @Published var response: VehicleStreamDataResponse?
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

    func run(vehicleId: String) {
        socket.connect()
        leave()
        channel = socket.channel(VehicleChannel.companion.topic(vehicleId: vehicleId), params: [:])

        channel?.on(VehicleChannel.companion.newDataEvent, callback: { message in
            self.handleNewDataMessage(message: message)
        })
        channel?.onError { message in
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("\(message.payload)")
                self.errorText = Text("Failed to load data for vehicle \(vehicleId), something went wrong")
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
                self.socketError = PhoenixChannelError.channelError("\(message.payload)")
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
                let newVehicleData = try VehicleChannel.companion
                    .parseMessage(payload: stringPayload)
                Logger().debug("Received vehicle update")
                DispatchQueue.main.async {
                    self.response = newVehicleData
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
                self.socketError = PhoenixChannelError.channelError("\(message.payload)")
                self.errorText = Text("Failed to load new vehicles, something went wrong")
            }
            Logger().error("\(error)")
        }
    }

    func leave() {
        channel?.leave()
        channel = nil
        response = nil
        errorText = nil
        socketError = nil
    }
}
