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
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 5)
    }

    @MainActor func testStationAccessibilityFlow() throws {
        let saveSettingsExp = expectation(description: "saves station accessibility setting")
        let settingsRepo = MockSettingsRepository(settings: [.stationAccessibility: false], onSaveSettings: {
            XCTAssertEqual($0, [.stationAccessibility: true])
            saveSettingsExp.fulfill()
        })
        let advanceExp = expectation(description: "calls advance()")
        let sut = OnboardingScreenView(
            screen: .stationAccessibility,
            advance: { advanceExp.fulfill() }
        )

        ViewHosting.host(view: sut.environmentObject(SettingsCache(settingsRepo: settingsRepo)))

        XCTAssertNotNil(try sut.inspect().find(where: { view in
            try view.text().string()
                .contains("we can show you which stations are inaccessible or have elevator closures.")
        }))
        XCTAssertNotNil(try sut.inspect().find(button: "Continue"))
        try sut.inspect().find(ViewType.Toggle.self).tap()
        wait(for: [saveSettingsExp], timeout: 1)
        try sut.inspect().find(button: "Continue").tap()
        wait(for: [advanceExp], timeout: 1)
    }

    @MainActor
    func testMapDisplayFlow() throws {
        let saveSettingExp = expectation(description: "saves hide maps setting")
        let settingsRepo = MockSettingsRepository(settings: [.hideMaps: false], onSaveSettings: {
            XCTAssertEqual($0, [.hideMaps: false])
            saveSettingExp.fulfill()
        })
        let advanceExp = expectation(description: "calls advance()")

        let sut = OnboardingScreenView(
            screen: .hideMaps,
            advance: { advanceExp.fulfill() }
        )

        ViewHosting.host(view: sut.environmentObject(SettingsCache(settingsRepo: settingsRepo)))

        XCTAssertNotNil(try sut.inspect().find(
            text: "When using VoiceOver, we can hide maps to make the app easier to navigate."
        ))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Toggle.self))

        let exp = sut.inspection.inspect { view in
            try view.find(ViewType.Toggle.self).tap()
            try view.find(button: "Continue").tap()
        }

        wait(for: [exp, saveSettingExp, advanceExp], timeout: 2)
    }

    func testFeedbackFlow() throws {
        let advanceExp = expectation(description: "calls advance()")
        let sut = OnboardingScreenView(
            screen: .feedback,
            advance: { advanceExp.fulfill() }
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(
            text: "MBTA Go is in the early stages! We want your feedback" +
                " as we continue making improvements and adding new features."
        ))
        try sut.inspect().find(button: "Get started").tap()
        wait(for: [advanceExp], timeout: 1)
    }
}
