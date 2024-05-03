//
//  SettingsPageTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-05-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import shared
import ViewInspector
import XCTest

final class SettingsPageTests: XCTestCase {
    class FakeSettingsRepository: ISettingsRepository {
        var mapDebug: Bool
        let onGet: (() -> Void)?
        let onSet: ((Bool) -> Void)?

        init(mapDebug: Bool, onGet: (() -> Void)? = nil, onSet: ((Bool) -> Void)? = nil) {
            self.mapDebug = mapDebug
            self.onGet = onGet
            self.onSet = onSet
        }

        func __getMapDebug() async throws -> KotlinBoolean {
            onGet?()
            return KotlinBoolean(bool: mapDebug)
        }

        func __setMapDebug(mapDebug: Bool) async throws {
            onSet?(mapDebug)
            self.mapDebug = mapDebug
        }
    }

    func testLoadsState() throws {
        let loadedPublisher = PassthroughSubject<Void, Never>()

        let settingsRepository = FakeSettingsRepository(mapDebug: true, onGet: {
            loadedPublisher.send(())
        })
        let sut = SettingsPage(settingsRepository: settingsRepository)

        let exp = sut.inspection.inspect(onReceive: loadedPublisher, after: 0.01) { view in
            XCTAssertTrue(try view.find(ViewType.Toggle.self).isOn())
        }

        ViewHosting.host(view: sut)

        wait(for: [exp], timeout: 1)
    }

    func testSavesState() throws {
        let loadedPublisher = PassthroughSubject<Void, Never>()
        let savedExp = expectation(description: "saved state")

        let settingsRepository = FakeSettingsRepository(mapDebug: false, onGet: {
            loadedPublisher.send(())
        }, onSet: {
            XCTAssertTrue($0)
            savedExp.fulfill()
        })
        let sut = SettingsPage(settingsRepository: settingsRepository)

        ViewHosting.host(view: sut)

        let tapExp = sut.inspection.inspect(onReceive: loadedPublisher, after: 0.01) { view in
            try view.find(ViewType.Toggle.self).tap()
        }

        wait(for: [tapExp, savedExp], timeout: 1)
    }
}
