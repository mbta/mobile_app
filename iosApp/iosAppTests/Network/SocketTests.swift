//
//  SocketTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 3/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
@testable import SwiftPhoenixClient
import XCTest

final class SocketTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDecodeMessageWithObjectEvent() {
        let body: [Any] = ["join_ref", "ref", "topic", "event", ["user_id": "abc123"]]
        let data = Defaults.encode(body)
        let jsonParts = decodeWithRawMessage(data: data) as? [Any]

        let message = Message(json: jsonParts!)
        XCTAssertEqual(message?.joinRef, "join_ref")
        XCTAssertEqual(message?.ref, "ref")
        XCTAssertEqual(message?.topic, "topic")
        XCTAssertEqual(message?.event, "event")

        let payload = message?.payload as? [String: Any]
        XCTAssertEqual(payload?["payload"] as? [String: String], ["user_id": "abc123"])
        XCTAssertEqual(payload?["jsonPayload"] as? String, "{\"user_id\":\"abc123\"}")
    }

    func testDecodeMessageWithResponseBody() {
        let body: [Any] = ["join_ref", "ref", "topic", "event", ["status": "ok", "response": ["user_id": "abc123"]]]
        let data = Defaults.encode(body)
        let jsonParts = decodeWithRawMessage(data: data) as? [Any]

        let message = Message(json: jsonParts!)
        XCTAssertEqual(message?.joinRef, "join_ref")
        XCTAssertEqual(message?.ref, "ref")
        XCTAssertEqual(message?.topic, "topic")
        XCTAssertEqual(message?.event, "event")

        let payload = message?.payload as? [String: Any]
        XCTAssertEqual(payload?["payload"] as? [String: String], ["user_id": "abc123"])
        XCTAssertEqual(payload?["jsonPayload"] as? String, "{\"user_id\":\"abc123\"}")
    }
}
