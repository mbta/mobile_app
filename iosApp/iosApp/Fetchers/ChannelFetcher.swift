//
//  ChannelFetcher.swift
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

class ChannelFetcher<Data: AnyObject & StreamDataResponse>: ObservableObject {
    @Published var data: Data?
    @Published var socketError: Error?
    @Published var errorText: Text?

    let socket: PhoenixSocket
    var spec: ChannelSpec<Data>
    var channel: Channel?
    var onMessageSuccessCallback: (() -> Void)?
    var onErrorCallback: (() -> Void)?

    init(socket: PhoenixSocket, spec: ChannelSpec<Data>,
         onMessageSuccessCallback: (() -> Void)? = nil, onErrorCallback: (() -> Void)? = nil)
    {
        self.socket = socket
        self.spec = spec
        self.onMessageSuccessCallback = onMessageSuccessCallback
        self.onErrorCallback = onErrorCallback
    }

    func run() {
        socket.connect()
        channel?.leave()
        channel = socket.channel(spec.topic, params: spec.joinPayload)

        channel?.on(spec.newDataEvent, callback: { message in
            self.handleNewDataMessage(message: message)
        })
        channel?.onError { message in
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("A: \(message.payload)")
                self.errorText = Text("error in channel \(message.topic), something went wrong")
                self.onErrorCallback?()
            }
        }

        channel?.onClose { message in
            Logger().debug("leaving channel \(message.topic)")
        }
        channel?.join().receive("ok") { message in
            Logger().debug("joined channel \(message.topic)")
            if message.jsonPayload() != "{}" {
                self.handleNewDataMessage(message: message)
            }
        }.receive("error", callback: { message in
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("B: \(message.payload)")
                self.errorText = Text("error in channel \(message.topic), could not connect to the server")
                self.onErrorCallback?()
            }
        })
    }

    private func handleNewDataMessage(message: Message) {
        do {
            let rawPayload: String? = message.jsonPayload()

            if let stringPayload = rawPayload {
                let newData = try spec.parseMessage(payload: stringPayload)
                Logger().debug("Received \(newData.countSummary)")
                DispatchQueue.main.async {
                    self.data = newData
                    self.socketError = nil
                    self.errorText = nil
                    self.onMessageSuccessCallback?()
                }
            } else {
                Logger().error("No jsonPayload found for message \(message.payload)")
                onErrorCallback?()
            }

        } catch {
            DispatchQueue.main.async {
                self.socketError = PhoenixChannelError.channelError("C: \(message.payload)")
                self.errorText = Text("error in channel \(message.topic), something went wrong")
            }
            Logger().error("\(error)")
        }
    }

    func leave() {
        channel?.leave()
        channel = nil
        data = nil
        errorText = nil
        socketError = nil
    }
}
