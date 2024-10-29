//
//  OnboardingScreenViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-10-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
@testable import iosApp
import shared
import ViewInspector
import XCTest

final class OnboardingScreenViewTests: XCTestCase {
    @MainActor func testLocationFlow() throws {
        let requestExp = expectation(description: "requests location permission")
        let advanceExp = expectation(description: "calls advance()")
        let locationFetcher = MockLocationFetcher(requestExp: requestExp)
        let sut = OnboardingScreenView(
            screen: .location,
            advance: { advanceExp.fulfill() },
            createLocationFetcher: { locationFetcher }
        )
        let exp = sut.inspection.inspect { view in
            XCTAssertNotNil(try view.find(text: "We’ll use your location to show the lines and bus routes near you."))
            XCTAssertNotNil(try view.find(button: "Not now"))
            try view.find(button: "Share location").tap()
            self.wait(for: [requestExp], timeout: 1)
            locationFetcher.authorizationStatus = .authorizedWhenInUse
            locationFetcher.locationFetcherDelegate?.locationFetcherDidChangeAuthorization(locationFetcher)
            self.wait(for: [advanceExp], timeout: 1)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 5)
    }

    func testHideMapsFlow() throws {
        let saveSettingExp = expectation(description: "saves hide maps setting")
        let settingsRepo = MockSettingsRepository(settings: [.init(key: .hideMaps, isOn: false)], onSaveSettings: {
            XCTAssertEqual($0, [.init(key: .hideMaps, isOn: true)])
            saveSettingExp.fulfill()
        })
        let advanceExp = expectation(description: "calls advance()")
        let sut = OnboardingScreenView(
            screen: .hideMaps,
            advance: { advanceExp.fulfill() },
            settingUseCase: SettingUsecase(repository: settingsRepo)
        )
        XCTAssertNotNil(try sut.inspect()
            .find(text: "For VoiceOver users, we’ll keep maps hidden by default unless you tell us otherwise."))
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
        XCTAssertNotNil(try sut.inspect()
            .find(
                text: "MBTA Go is just getting started! We’re actively making improvements based on feedback from riders like you."
            ))
        try sut.inspect().find(button: "Get started").tap()
        wait(for: [advanceExp], timeout: 1)
    }

    private class MockLocationFetcher: LocationFetcher {
        let requestExp: XCTestExpectation

        init(requestExp: XCTestExpectation) {
            self.requestExp = requestExp
        }

        var locationFetcherDelegate: LocationFetcherDelegate? {
            didSet {
                // the real CLLocationManager will also do this, although maybe not at the same moment
                locationFetcherDelegate?.locationFetcherDidChangeAuthorization(self)
            }
        }

        var authorizationStatus: CLAuthorizationStatus = .notDetermined
        var distanceFilter: CLLocationDistance = 0
        func startUpdatingLocation() {}

        func requestWhenInUseAuthorization() {
            requestExp.fulfill()
        }
    }
}
