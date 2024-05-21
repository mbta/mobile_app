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

enum PhoenixChannelError: Error {
    case channelError(String)
}

class AlertsFetcher: ObservableObject {
    @Published var alerts: AlertsStreamDataResponse?
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

    func run() {
        socket.attach()
        channel?.detach()
        channel = socket.getChannel(topic: AlertsChannel.companion.topic, params: [:])

        channel?.onEvent(event: AlertsChannel.companion.newDataEvent, callback: { message in
            self.handleNewDataMessage(message: message)
        })
        channel?.onFailure { message in
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("A: \(message.subject)")
                self.errorText = Text("Failed to load new alerts, something went wrong")
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
                self.errorText = Text("Failed to load alerts, could not connect to the server")
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
                Logger().error("No jsonPayload found for message \(message.body)")
                if let callback = onErrorCallback {
                    callback()
                }
            }

        } catch {
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("C: \(message.body)")
                self.errorText = Text("Failed to load new alerts, something went wrong")
            }
            Logger().error("\(error)")
        }
    }

    func leave() {
        channel?.detach()
        channel = nil
        alerts = nil
        errorText = nil
        socketError = nil
    }
}
