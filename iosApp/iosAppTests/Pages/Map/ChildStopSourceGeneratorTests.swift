//
//  ChildStopSourceGeneratorTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-06-24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import XCTest

final class ChildStopSourceGeneratorTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testContainsCorrectData() {
        let objects = ObjectCollectionBuilder()
        let parent = objects.stop { stop in
            stop.id = "1"
            stop.locationType = .station
            stop.name = "Stop"
        }
        let platform = objects.stop { stop in
            stop.id = "2"
            stop.locationType = .stop
            stop.platformName = "Headsign"
            stop.parentStationId = parent.id
        }
        let entrance = objects.stop { stop in
            stop.id = "3"
            stop.locationType = .entranceExit
            stop.name = "Stop - Entrance"
            stop.parentStationId = parent.id
        }
        let boardingArea = objects.stop { stop in
            stop.id = "4"
            stop.locationType = .boardingArea
            stop.platformName = "Other Headsign"
            stop.parentStationId = parent.id
        }
        let node = objects.stop { stop in
            stop.id = "5"
            stop.locationType = .genericNode
            stop.parentStationId = parent.id
        }

        let stops = [platform.id: platform, entrance.id: entrance, boardingArea.id: boardingArea, node.id: node]

        let source = ChildStopSourceGenerator.generateChildStopSource(childStops: stops)

        if case let .featureCollection(collection) = source.data! {
            XCTAssertEqual(collection.features.count, 3)

            XCTAssertEqual(collection.features[0].identifier, .string(platform.id))
            XCTAssertEqual(collection.features[0].properties, [
                ChildStopSourceGenerator.propNameKey: .string("Headsign"),
                ChildStopSourceGenerator.propLocationTypeKey: .string(String(describing: LocationType.stop)),
                ChildStopSourceGenerator.propSortOrderKey: 0,
            ])

            XCTAssertEqual(collection.features[1].identifier, .string(entrance.id))
            XCTAssertEqual(collection.features[1].properties, [
                ChildStopSourceGenerator.propNameKey: .string("Entrance"),
                ChildStopSourceGenerator.propLocationTypeKey: .string(String(describing: LocationType.entranceExit)),
                ChildStopSourceGenerator.propSortOrderKey: 1,
            ])

            XCTAssertEqual(collection.features[2].identifier, .string(boardingArea.id))
            XCTAssertEqual(collection.features[2].properties, [
                ChildStopSourceGenerator.propNameKey: .string("Other Headsign"),
                ChildStopSourceGenerator.propLocationTypeKey: .string(String(describing: LocationType.boardingArea)),
                ChildStopSourceGenerator.propSortOrderKey: 2,
            ])
        }
    }
}
