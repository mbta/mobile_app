//
//  HomeMapViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import ViewInspector
import XCTest

final class HomeMapViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testExample() throws {
        let sut = HomeMapView()

        try print(sut.inspect())
    }
}
