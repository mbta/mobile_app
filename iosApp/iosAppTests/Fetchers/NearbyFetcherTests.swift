//
//  NearbyFetcherTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import SwiftUI
import XCTest

final class NearbyFetcherTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testErrorText() {
        let fetcher = NearbyFetcher(backend: IdleBackend())

        let timeoutError = NSError(
            domain: "KotlinException",
            code: 1,
            userInfo: ["KotlinException": Ktor_client_coreHttpRequestTimeoutException(url: "a", timeoutMillis: 1)]
        )
        XCTAssertEqual(
            fetcher.getErrorText(error: timeoutError),
            Text("Couldn't load nearby transit, no response from the server")
        )

        let jsonError = NSError(
            domain: "KotlinException",
            code: 1,
            userInfo: ["KotlinException": Ktor_serializationJsonConvertException(message: "parse failure", cause: nil)]
        )
        XCTAssertEqual(
            fetcher.getErrorText(error: jsonError),
            Text("Couldn't load nearby transit, unable to parse response")
        )
    }
}
