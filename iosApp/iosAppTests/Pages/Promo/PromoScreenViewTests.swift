//
//  PromoScreenViewTests.swift
//  iosAppTests
//
//  Created by esimon on 2/5/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreLocation
@testable import iosApp
import Shared
import ViewInspector
import XCTest

final class PromoScreenViewTests: XCTestCase {
    @MainActor func testCombinedStopPromoFlow() throws {
        let advanceExp = expectation(description: "calls advance()")
        let sut = PromoScreenView(
            screen: .combinedStopAndTrip,
            advance: { advanceExp.fulfill() }
        )
        let exp = sut.inspection.inspect { view in
            XCTAssertNotNil(try view.find(text: "Check out the new stop view"))
            XCTAssertNotNil(try view.find(
                text: "We now show arrivals and detailed vehicle locations all at once. "
                    + "Let us know what you think!"
            ))
            try view.find(button: "Got it").tap()
            await self.fulfillment(of: [advanceExp], timeout: 1)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 5)
    }

    @MainActor func testEnhancedFavorites() throws {
        let advanceExp = expectation(description: "calls advance()")
        let sut = PromoScreenView(
            screen: .enhancedFavorites,
            advance: { advanceExp.fulfill() }
        )
        let exp = sut.inspection.inspect { view in
            XCTAssertNotNil(try view.find(text: "Add your favorites"))
            XCTAssertNotNil(try view.find(
                text: "Now save your frequently used stops to one easy place."
            ))
            try view.find(button: "Got it").tap()
            await self.fulfillment(of: [advanceExp], timeout: 1)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 5)
    }
}
