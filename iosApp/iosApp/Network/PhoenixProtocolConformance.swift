//
//  PhoenixProtocolConformance.swift
//  iosApp
//
//  Created by Brady, Kayla on 3/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftPhoenixClient

extension SwiftPhoenixClient.Message: PhoenixMessage {
    public var subject: String {
        topic
    }

    public var body: [String: Any] {
        payload
    }

    public var jsonBody: String? {
        payload["jsonPayload"] as? String
    }
}

extension SwiftPhoenixClient.Push: PhoenixPush {
    public func receive(
        status: PhoenixPushStatus,
        callback: @escaping (any PhoenixMessage) -> Void
    ) -> any PhoenixPush {
        receive(status.value, callback: callback)
    }
}

extension SwiftPhoenixClient.Channel: PhoenixChannel {
    public func onEvent(event: String, callback: @escaping ((any PhoenixMessage) -> Void)) {
        on(event, callback: callback)
    }

    public func onFailure(callback: @escaping ((_ message: any PhoenixMessage) -> Void)) {
        onError(callback)
    }

    public func onDetach(callback: @escaping ((any PhoenixMessage) -> Void)) {
        onClose(callback)
    }

    public func attach() -> PhoenixPush {
        join()
    }

    public func detach() -> PhoenixPush {
        leave()
    }
}
