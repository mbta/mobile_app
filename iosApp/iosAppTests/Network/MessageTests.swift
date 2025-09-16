//
//  MessageTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 3/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
@testable import SwiftPhoenixClient
import XCTest

final class MessageTest: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDecodesJsonPayload() {
        let message = Message(
            ref: "ref",
            topic: "topic",
            event: "event",
            payload: ["jsonPayload": "{\"user_id\":\"abc123\"}", "payload": ["user_id": "abc123"]],
            joinRef: "join_ref"
        )

        XCTAssertEqual(message.jsonBody, "{\"user_id\":\"abc123\"}")
    }

    func testDecodesJsonPayloadNestedInResponse() {
        let message = Message(
            ref: "ref",
            topic: "topic",
            event: "event",
            payload: [
                "response": ["jsonPayload": "{\"user_id\":\"abc123\"}", "payload": ["user_id": "abc123"]],
                "status": "ok",
            ],
            joinRef: "join_ref"
        )

        XCTAssertEqual(message.jsonBody, "{\"user_id\":\"abc123\"}")
    }
}
