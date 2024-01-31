//
//  ContentViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 12/28/23.
//  Copyright © 2023 orgName. All rights reserved.
//

@testable import iosApp
import ViewInspector
import XCTest

final class ContentViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testExample() throws {
        let sut = ContentView()
            .environmentObject(LocationDataManager())
            .environmentObject(BackendDispatcher(backend: IdleBackend()))

        let greeting = try sut.inspect().navigationView().vStack()[1].text().string()
        XCTAssertTrue(greeting.hasPrefix("Hello"), "displays greeting")
    }
}
