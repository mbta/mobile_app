//
//  RouteSourceGeneratorTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Polyline
import shared
import XCTest
@_spi(Experimental) import MapboxMaps

final class RouteSourceGeneratorTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testRouteSourcesAreCreated() {
        let routeSourceGenerator = RouteSourceGenerator(routeData: MapTestDataHelper.routeResponse,
                                                        stopsById: [MapTestDataHelper.stopAlewife.id: MapTestDataHelper.stopAlewife,
                                                                    MapTestDataHelper.stopDavis.id: MapTestDataHelper.stopDavis,
                                                                    MapTestDataHelper.stopPorter.id: MapTestDataHelper.stopPorter,
                                                                    MapTestDataHelper.stopHarvard.id: MapTestDataHelper.stopHarvard,
                                                                    MapTestDataHelper.stopCentral.id: MapTestDataHelper.stopCentral,
                                                                    MapTestDataHelper.stopAssembly.id: MapTestDataHelper.stopAssembly,
                                                                    MapTestDataHelper.stopSullivan.id: MapTestDataHelper.stopSullivan],
                                                        alertsByStop: [:])

        XCTAssertEqual(routeSourceGenerator.routeSources.count, 2)

        let redSource = routeSourceGenerator.routeSources.first { $0.id == RouteSourceGenerator.getRouteSourceId(MapTestDataHelper.routeRed.id) }
        XCTAssertNotNil(redSource)
        if case let .featureCollection(collection) = redSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 2)
            XCTAssertEqual(
                collection.features[0].geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeRedC2.polyline!).coordinates!)
                    .sliced(from: MapTestDataHelper.stopAlewife.coordinate, to: MapTestDataHelper.stopDavis.coordinate)!)
            )
        } else {
            XCTFail("Red route source had no features")
        }

        let orangeSource = routeSourceGenerator.routeSources.first { $0.id == RouteSourceGenerator.getRouteSourceId(MapTestDataHelper.routeOrange.id) }
        XCTAssertNotNil(orangeSource)
        if case let .featureCollection(collection) = orangeSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 1)
            XCTAssertEqual(
                collection.features[0].geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeOrangeC1.polyline!).coordinates!)
                    .sliced(from: MapTestDataHelper.stopAssembly.coordinate, to: MapTestDataHelper.stopSullivan.coordinate)!)
            )
        } else {
            XCTFail("Orange route source had no features")
        }
    }

    func testAlertingSourcesCreated() {
        let now = Date.now

        let objects = ObjectCollectionBuilder()

        let redAlert = objects.alert { alert in
            alert.id = "a1"
            alert.effect = .shuttle
            alert.activePeriod(start: now.addingTimeInterval(-1).toKotlinInstant(), end: nil)
            alert.informedEntity(activities: [.board], directionId: nil, facility: nil,
                                 route: MapTestDataHelper.routeRed.id, routeType: .heavyRail,
                                 stop: MapTestDataHelper.stopPorter.id, trip: nil)
            alert.informedEntity(activities: [.board], directionId: nil, facility: nil,
                                 route: MapTestDataHelper.routeRed.id, routeType: .heavyRail,
                                 stop: MapTestDataHelper.stopHarvard.id, trip: nil)
        }
        let alertsByStop = [
            MapTestDataHelper.stopPorter.id: AlertAssociatedStop(
                stop: MapTestDataHelper.stopPorter,
                relevantAlerts: [redAlert],
                routePatterns: [MapTestDataHelper.patternRed30],
                childStops: [:],
                childAlerts: [:]
            ),
            MapTestDataHelper.stopHarvard.id: AlertAssociatedStop(
                stop: MapTestDataHelper.stopHarvard,
                relevantAlerts: [redAlert],
                routePatterns: [MapTestDataHelper.patternRed30],
                childStops: [:],
                childAlerts: [:]
            ),
        ]

        let routeSourceGenerator = RouteSourceGenerator(routeData: MapTestDataHelper.routeResponse,
                                                        stopsById: [MapTestDataHelper.stopAlewife.id: MapTestDataHelper.stopAlewife,
                                                                    MapTestDataHelper.stopDavis.id: MapTestDataHelper.stopDavis,
                                                                    MapTestDataHelper.stopPorter.id: MapTestDataHelper.stopPorter,
                                                                    MapTestDataHelper.stopHarvard.id: MapTestDataHelper.stopHarvard,
                                                                    MapTestDataHelper.stopCentral.id: MapTestDataHelper.stopCentral],
                                                        alertsByStop: alertsByStop)

        // RL & OL
        XCTAssertEqual(routeSourceGenerator.routeSources.count, 2)

        let redSource = routeSourceGenerator.routeSources.first
        XCTAssertNotNil(redSource)
        if case let .featureCollection(collection) = redSource!.data.unsafelyUnwrapped {
            // Alewife - Davis (normal), Harvard - Porter (alerting), Porter - Central (normal)
            XCTAssertEqual(collection.features.count, 3)
            XCTAssertEqual(
                collection.features[0].properties![RouteSourceGenerator.propIsAlertingKey]!, JSONValue(Bool(false))
            )
            XCTAssertEqual(
                collection.features[0].geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeRedC1.polyline!).coordinates!)
                    .sliced(from: MapTestDataHelper.stopAlewife.coordinate, to: MapTestDataHelper.stopDavis.coordinate)!)
            )

            XCTAssertEqual(
                collection.features[1].properties![RouteSourceGenerator.propIsAlertingKey]!, JSONValue(Bool(true))
            )
            XCTAssertEqual(
                collection.features[1].geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeRedC1.polyline!).coordinates!)
                    .sliced(from: MapTestDataHelper.stopPorter.coordinate, to: MapTestDataHelper.stopHarvard.coordinate)!)
            )
            XCTAssertEqual(
                collection.features[2].properties![RouteSourceGenerator.propIsAlertingKey]!, JSONValue(Bool(false))
            )
            XCTAssertEqual(
                collection.features[2].geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeRedC1.polyline!).coordinates!)
                    .sliced(from: MapTestDataHelper.stopHarvard.coordinate, to: MapTestDataHelper.stopCentral.coordinate)!)
            )

        } else {
            XCTFail("Red route source had no features")
        }
    }
}
