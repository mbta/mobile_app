//
//  AlertsFetcher.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-15.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import os
import shared
import SwiftPhoenixClient
import SwiftUI

class AlertsFetcher: ObservableObject {
    @Published var alerts: AlertsStreamDataResponse?
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

    func run() {
        socket.connect()
        channel = socket.channel(AlertsChannel.companion.topic, params: [:])

        channel?.on(AlertsChannel.companion.newDataEvent, callback: { message in
            self.handleNewDataMessage(message: message)
        })
        channel?.onError { message in
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("A: \(message.payload)")
                self.errorText = Text("Failed to load new alerts, something went wrong")
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
                self.errorText = Text("Failed to load alerts, could not connect to the server")
                if let callback = self.onErrorCallback {
                    callback()
                }
            }
        })
    }

    private func handleNewDataMessage(message: Message) {
        do {
            let rawPayload: String? = message.jsonPayload()

            if let stringPayload = rawPayload {
                let newAlerts = try AlertsChannel.companion
                    .parseMessage(payload: stringPayload)
                Logger().debug("Received \(newAlerts.alerts.count) alerts")
                DispatchQueue.main.async {
                    self.alerts = newAlerts
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
                self.errorText = Text("Failed to load new alerts, something went wrong")
            }
            Logger().error("\(error)")
        }
    }

    func leave() {
        channel?.leave()
        channel = nil
        alerts = nil
        errorText = nil
        socketError = nil
    }
}
