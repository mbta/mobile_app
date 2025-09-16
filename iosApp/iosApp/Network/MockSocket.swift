//
//  MockSocket.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 3/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftPhoenixClient

open class MockSocket: PhoenixSocket {
    public var channels: [PhoenixChannel] = []
    // Channel.socket is weak, so we need to maintain a reference to the Socket
    private let socket = Socket(endPoint: "/socket", transport: { _ in PhoenixTransportMock() })

    public func attach() {}

    public func detach() {}

    public func getChannel(topic _: String, params _: [String: Any]) -> any PhoenixChannel {
        let channel = MockChannel()
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

class MockPush: PhoenixPush {
    func receive(status _: Shared.PhoenixPushStatus,
                 callback _: @escaping (any PhoenixMessage) -> Void) -> any PhoenixPush {
        MockPush()
    }
}

class MockChannel: PhoenixChannel {
    func attach() -> any PhoenixPush {
        MockPush()
    }

    func detach() -> any PhoenixPush {
        MockPush()
    }

    func onDetach(callback _: @escaping (any PhoenixMessage) -> Void) {
        // no-op
    }

    func onEvent(event _: String, callback _: @escaping (any PhoenixMessage) -> Void) {
        // no-op
    }

    func onFailure(callback _: @escaping (any PhoenixMessage) -> Void) {
        // no-op
    }
}
