//
//  TripDetailsStopViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-07-11.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import ViewInspector
import XCTest

final class TripDetailsStopViewTests: XCTestCase {
    func testShowsAlertOverPrediction() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let alert = objects.alert { alert in
            alert.effect = .stopClosure
            alert.activePeriod(start: .companion.DISTANT_PAST, end: nil)
            alert.informedEntity(
                activities: [.board],
                directionId: nil,
                facility: nil,
                route: nil,
                routeType: nil,
                stop: stop.id,
                trip: nil
            )
        }
        let prediction = objects.prediction { prediction in
            prediction.departureTime = Date.now.addingTimeInterval(60).toKotlinInstant()
        }
        let sut = TripDetailsStopView(
            stop: .init(stop: stop, stopSequence: 1, alert: alert, schedule: nil, prediction: prediction, vehicle: nil,
                        routes: []),
            now: Date.now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeType: nil
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Stop Closed"))
    }

    func testHandlesTapOnBody() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.name = "Boylston"
        }
        let now = Date.now
        let prediction = objects.prediction { prediction in
            prediction.departureTime = now.addingTimeInterval(60).toKotlinInstant()
        }
        let connectingRoute = objects.route()
        let stopListEntry = TripDetailsStopList.Entry(
            stop: stop,
            stopSequence: 1,
            alert: nil,
            schedule: nil,
            prediction: prediction,
            vehicle: nil,
            routes: [connectingRoute]
        )
        let exp = expectation(description: "calls onTapLink")
        let sut = TripDetailsStopView(
            stop: stopListEntry,
            now: now.toKotlinInstant(),
            onTapLink: { navStackEntry, actualStopListEntry, connectingRoute in
                XCTAssertEqual(navStackEntry, .stopDetails(stop, nil))
                XCTAssertEqual(stopListEntry, actualStopListEntry)
                XCTAssertNil(connectingRoute)
                exp.fulfill()
            },
            routeType: nil
        )

        try sut.inspect().find(button: "Boylston").tap()
        wait(for: [exp], timeout: 5)
    }
}
