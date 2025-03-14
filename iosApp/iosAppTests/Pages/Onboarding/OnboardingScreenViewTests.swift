//
//  OnboardingScreenViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-10-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
@testable import iosApp
import Shared
import ViewInspector
import XCTest

final class OnboardingScreenViewTests: XCTestCase {
    @MainActor func testLocationFlow() throws {
        let requestExp = expectation(description: "requests location permission")
        let advanceExp = expectation(description: "calls advance()")
        let locationFetcher = MockOnboardingLocationFetcher(requestExp: requestExp)
        let sut = OnboardingScreenView(
            screen: .location,
            advance: { advanceExp.fulfill() },
            createLocationFetcher: { locationFetcher }
        )
        let exp = sut.inspection.inspect { view in
            XCTAssertNotNil(try view.find(where: { view in
                try view.text().string().contains("We use your location to show you nearby transit options.")
            }))
            try view.find(button: "Continue").tap()
            await self.fulfillment(of: [requestExp], timeout: 1)
            locationFetcher.authorizationStatus = .authorizedWhenInUse
            locationFetcher.locationFetcherDelegate?.locationFetcherDidChangeAuthorization(locationFetcher)
            await self.fulfillment(of: [advanceExp], timeout: 1)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 5)
    }

    @MainActor func testStationAccessibilityFlow() throws {
        let saveSettingsExp = expectation(description: "saves station accessibility setting")
        let settingsRepo = MockSettingsRepository(settings: [.elevatorAccessibility: false], onSaveSettings: {
            XCTAssertEqual($0, [.elevatorAccessibility: true])
            saveSettingsExp.fulfill()
        })
        let advanceExp = expectation(description: "calls advance()")
        let sut = OnboardingScreenView(
            screen: .stationAccessibility,
            advance: { advanceExp.fulfill() },
            settingsRepository: settingsRepo
        )
        XCTAssertNotNil(try sut.inspect().find(text: "We can tell you when elevators are closed at a station."))
        XCTAssertNotNil(try sut.inspect().find(button: "Skip"))
        try sut.inspect().find(button: "Show elevator closures").tap()
        wait(for: [saveSettingsExp, advanceExp], timeout: 1)
    }

    func testHideMapsFlow() throws {
        let saveSettingExp = expectation(description: "saves hide maps setting")
        let settingsRepo = MockSettingsRepository(settings: [.hideMaps: false], onSaveSettings: {
            XCTAssertEqual($0, [.hideMaps: true])
            saveSettingExp.fulfill()
        })
        let advanceExp = expectation(description: "calls advance()")
        let sut = OnboardingScreenView(
            screen: .hideMaps,
            advance: { advanceExp.fulfill() },
            settingsRepository: settingsRepo
        )
        XCTAssertNotNil(try sut.inspect().find(
            text: "When using VoiceOver, we can skip reading out maps to keep you focused on transit information."
        ))
        XCTAssertNotNil(try sut.inspect().find(button: "Show maps"))
        try sut.inspect().find(button: "Hide maps").tap()
        wait(for: [saveSettingExp, advanceExp], timeout: 1)
    }

    func testFeedbackFlow() throws {
        let advanceExp = expectation(description: "calls advance()")
        let sut = OnboardingScreenView(
            screen: .feedback,
            advance: { advanceExp.fulfill() }
        )
        XCTAssertNotNil(try sut.inspect().find(
            text: "MBTA Go is in the early stages! We want your feedback" +
                " as we continue making improvements and adding new features."
        ))
        try sut.inspect().find(button: "Get started").tap()
        wait(for: [advanceExp], timeout: 1)
    }
}
