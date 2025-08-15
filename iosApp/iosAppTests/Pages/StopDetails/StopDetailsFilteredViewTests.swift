//
//  StopDetailsFilteredViewTests.swift
//  iosAppTests
//
//  Created by Kayla Brady on 7/2/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp

import Combine
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsFilteredViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testSaveEnhancedFavoriteTriggersSaveFlow() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()
        let directionId: Int32 = 0

        let favoritesRepository = MockFavoritesRepository()
        let pinnedRoutesRepository = MockPinnedRoutesRepository()

        let stopDetailsVM = StopDetailsViewModel(
            favoritesRepository: favoritesRepository,
            pinnedRoutesRepository: pinnedRoutesRepository
        )
        let sut = StopDetailsFilteredView(stopId: stop.id,
                                          stopFilter: .init(routeId: route.id, directionId: directionId),
                                          tripFilter: nil,
                                          setStopFilter: { _ in },
                                          setTripFilter: { _ in },
                                          routeCardData: [],
                                          now: Date.now,
                                          errorBannerVM: .init(),
                                          nearbyVM: .init(),
                                          mapVM: .init(),
                                          stopDetailsVM: stopDetailsVM)

        let tappedPublisher = PassthroughSubject<Void, Never>()

        let tapButtonExp = sut.inspection.inspect(after: 0.5) { view in
            try view.find(StarButton.self).find(ViewType.Button.self)
                .tap()
            tappedPublisher.send()
        }
        let confirmationDialogExp = sut.inspection.inspect(onReceive: tappedPublisher, after: 1) { view in
            XCTAssertTrue(try view.actualView().inSaveFavoritesFlow)
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([
            .devDebugMode: false,
        ]))
        wait(for: [tapButtonExp, confirmationDialogExp], timeout: 2)
    }
}
