//
//  MockSocket.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 3/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
@testable import SwiftPhoenixClient

open class MockSocket: PhoenixSocket {
    public var channels: [SwiftPhoenixClient.Channel] = []
    // Channel.socket is weak, so we need to maintain a reference to the Socket
    private let socket = Socket(endPoint: "/socket", transport: { _ in PhoenixTransportMock() })

    public func attach() {}

    public func detach() {}

    public func getChannel(topic: String, params _: [String: Any]) -> any PhoenixChannel {
        let channel = Channel(topic: topic, socket: socket)
        channels.append(channel)
        return channel
    }

    public func onAttach(callback _: @escaping () -> Void) -> String {
        "Opened"
    }

    public func onDetach(callback _: @escaping () -> Void) -> String {
        "Closed"
    }
}
