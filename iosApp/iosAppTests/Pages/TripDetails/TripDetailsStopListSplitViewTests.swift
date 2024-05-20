//
//  TripDetailsStopListSplitViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-05-17.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import ViewInspector
import XCTest

final class TripDetailsStopListSplitViewTests: XCTestCase {
    static let objects = ObjectCollectionBuilder()
    let stop1 = objects.stop { $0.name = "A" }
    let stop2 = objects.stop { $0.name = "B" }
    let stop3 = objects.stop { $0.name = "C" }

    let pred1 = objects.prediction { $0.departureTime = Date.now.toKotlinInstant() }
    let pred2 = objects.prediction { $0.departureTime = Date.now.addingTimeInterval(60).toKotlinInstant() }
    let pred3 = objects.prediction { $0.departureTime = Date.now.addingTimeInterval(2 * 60).toKotlinInstant() }

    func entry(_ stop: Stop, _ stopSequence: Int, _ prediction: Prediction) -> TripDetailsStopList.Entry {
        TripDetailsStopList.Entry(
            stop: stop,
            stopSequence: Int32(stopSequence),
            schedule: nil,
            prediction: prediction,
            vehicle: nil
        )
    }

    func testNoAccordionIfFirstStop() throws {
        let sut = TripDetailsStopListSplitView(
            splitStops: .init(
                collapsedStops: [],
                targetStop: entry(stop1, 10, pred1),
                followingStops: [entry(stop2, 20, pred2), entry(stop3, 30, pred3)]
            ),
            now: Date.now.toKotlinInstant()
        )

        XCTAssertNil(try? sut.inspect().find(ViewType.DisclosureGroup.self))
    }

    func testCollapsedStopsInAccordion() throws {
        let sut = TripDetailsStopListSplitView(
            splitStops: .init(
                collapsedStops: [entry(stop1, 10, pred1), entry(stop2, 20, pred2)],
                targetStop: entry(stop3, 30, pred3),
                followingStops: []
            ),
            now: Date.now.toKotlinInstant()
        )

        XCTAssertNotNil(try sut.inspect().find(ViewType.DisclosureGroup.self))

        XCTAssertFalse(try sut.inspect().find(ViewType.DisclosureGroup.self).isExpanded())
        XCTAssertEqual(try sut.inspect().find(ViewType.DisclosureGroup.self).labelView().text().string(), "2 stops")

        XCTAssertNotNil(try sut.inspect()
            .find(TripDetailsStopView.self, where: { try $0.actualView().stop.stop == stop1 })
            .find(ViewType.DisclosureGroup.self, relation: .parent)
        )
        XCTAssertNotNil(try sut.inspect()
            .find(TripDetailsStopView.self, where: { try $0.actualView().stop.stop == stop2 })
            .find(ViewType.DisclosureGroup.self, relation: .parent)
        )
        XCTAssertNil(try? sut.inspect()
            .find(TripDetailsStopView.self, where: { try $0.actualView().stop.stop == stop3 })
            .find(ViewType.DisclosureGroup.self, relation: .parent)
        )
    }
}
