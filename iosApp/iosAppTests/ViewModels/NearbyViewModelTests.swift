//
//  NearbyViewModelTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 5/8/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import XCTest

final class NearbyViewModelTests: XCTestCase {
    func isNearbyVisibleWhenNoStack() {
        XCTAssert(NearbyViewModel(navigationStack: []).isNearbyVisible())
    }

    func isNearbyVisibleFalseWhenStack() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        XCTAssert(NearbyViewModel(navigationStack: [.stopDetails(stop, nil)]).isNearbyVisible())
    }
}
