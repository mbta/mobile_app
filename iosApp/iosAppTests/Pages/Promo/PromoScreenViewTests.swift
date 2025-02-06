//
//  PromoScreenViewTests.swift
//  iosAppTests
//
//  Created by esimon on 2/5/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreLocation
@testable import iosApp
import shared
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
            XCTAssertNotNil(try view.find(text: "See arrivals and track vehicles in one place"))
            XCTAssertNotNil(try view.find(
                text: "We created a new view that allows you to see arrivals at your stop "
                    + "and track vehicle locations all at once. Send us feedback to let us know what you think!"
            ))
            try view.find(button: "Got it").tap()
            await self.fulfillment(of: [advanceExp], timeout: 1)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 5)
    }
}
