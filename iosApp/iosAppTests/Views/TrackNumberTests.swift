//
//  TrackNumberTests.swift
//  iosApp
//
//  Created by esimon on 10/8/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class TrackNumberTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testNumber() throws {
        let sut = TrackNumber(track: TripDetailsStopList.TrackNumber(number: "5"))
        XCTAssertNotNil(try sut.inspect().find(text: "Track 5"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Boarding on track 5"))
    }

    func testTBD() throws {
        let sut = TrackNumber(track: TripDetailsStopList.TrackTBD())
        XCTAssertNotNil(try sut.inspect().find(text: "Track TBD"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Track to be determined"))
    }
}
