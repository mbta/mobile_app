//
//  Socket.swift
//  iosApp
//
//  Created by Brady, Kayla on 3/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import os
import Shared
import SwiftPhoenixClient

extension Socket: PhoenixSocket {
    public func onAttach(callback: @escaping () -> Void) -> String {
        onOpen(callback: callback)
    }

    public func onDetach(callback: @escaping () -> Void) -> String {
        onClose(callback: callback)
    }

    public func attach() {
        connect()
    }

    public func getChannel(topic: String, params: [String: Any]) -> any PhoenixChannel {
        channel(topic, params: params)
    }

    public func detach() {
        disconnect(code: .normal, reason: "backgrounded", callback: nil)
    }

    func withRawMessages() {
        decode = decodeWithRawMessage
    }
}

class PhoenixTransportMock: PhoenixTransport {
    var readyState: PhoenixTransportReadyState {
        get { underlyingReadyState }
        set(value) { underlyingReadyState = value }
    }

    var underlyingReadyState: PhoenixTransportReadyState!
    var delegate: PhoenixTransportDelegate?

    var connectClosure: (() -> Void)?

    func connect() {
        connectClosure?()
    }

    func connect(with _: [String: Any]) {
        connectClosure?()
    }

    func disconnect(code _: Int, reason _: String?) {}

    func send(data _: Data) {}
}

/*
 Return the decoded message in the expected [Any?] format:
 [joinReference, messageReference, topic, eventName, eventPayload]

 where eventPayload has type
 ["jsonPayload": String, "payload": Any?] or
 ["response": ["jsonPayload": String, "payload": Any?], "status": String]
 "jsonPayload" is the raw payload string, and "payload" contains the decoded json payload object.
 */
func decodeWithRawMessage(data: Data) -> Any? {
    let messageParts = decodeMessageParts(data: data)

    guard let data = messageParts.rawEventString?.data(using: String.Encoding.utf8),
          let eventPayload = Defaults.decode(data)
    else {
        Logger().warning("unable to parse raw payload \(messageParts.rawEventString ?? "")")
        return nil
    }

    var event: [String: Any?] = [:]

    if let eventPayloadMap = eventPayload as? [String: Any?], isResponseEvent(payload: eventPayloadMap) {
        let existingResponse = eventPayloadMap["response"]!
        event = eventPayloadMap
        // Include the jsonPayload on the response
        event["response"] = ["jsonPayload": String(data: Defaults.encode(existingResponse!),
                                                   encoding: String.Encoding.utf8), "payload": existingResponse]
    } else {
        event = ["jsonPayload": messageParts.rawEventString, "payload": eventPayload]
    }

    return [messageParts.joinRef as Any, messageParts.messageRef as Any,
            messageParts.topic, messageParts.eventName, event]
}

struct DecodedMessageParts {
    var joinRef: String?
    var messageRef: String?
    var topic: String
    var eventName: String
    var rawEventString: String?
}

private func decodeMessageParts(data: Data) -> DecodedMessageParts {
    // Parsing based on
    // https://github.com/dsrees/JavaPhoenixClient/blob/master/src/main/kotlin/org/phoenixframework/Defaults.kt#L68
    var rawStringMessage = String(data: data, encoding: .utf8)!

    rawStringMessage.removeFirst(1) // remove '['
    let joinRef = String(rawStringMessage.prefix(while: { $0 != "," })) // take join ref, 'null' or '5'

    rawStringMessage.removeFirst(joinRef.count) // remove join ref
    rawStringMessage.removeFirst(1) // remove ','

    let messageRef = String(rawStringMessage.prefix(while: { $0 != "," })) // take ref, 'null' or '5'
    rawStringMessage.removeFirst(messageRef.count) // remove ref
    rawStringMessage.removeFirst(2) // remove ',"'

    let topic = String(rawStringMessage.prefix(while: { $0 != "\"" }))
    rawStringMessage.removeFirst(topic.count)
    rawStringMessage.removeFirst(3) // remove '","'

    let eventName = String(rawStringMessage.prefix(while: { $0 != "\"" }))
    rawStringMessage.removeFirst(eventName.count)
    rawStringMessage.removeFirst(2) // remove '",'

    let rawEventString = String(rawStringMessage.dropLast()) // remove ]

    return .init(joinRef: parseValue(str: joinRef), messageRef: parseValue(str: messageRef),
                 topic: topic, eventName: eventName, rawEventString: rawEventString)
}

private func parseValue(str: String) -> String? {
    str == "null" ? nil : str.replacingOccurrences(of: "\"", with: "")
}

private func isResponseEvent(payload: [String: Any?]) -> Bool {
    payload["response"] != nil
}
