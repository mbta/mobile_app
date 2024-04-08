//
//  StopSourceGeneratorTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import XCTest
@_spi(Experimental) import MapboxMaps

final class StopSourceGeneratorTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testStopSourcesAreCreated() {
        let objects = ObjectCollectionBuilder()

        let stop1 = objects.stop { stop in
            stop.id = "place-aqucl"
            stop.name = "Aquarium"
            stop.latitude = 42.359784
            stop.longitude = -71.051652
            stop.locationType = .station
        }
        let stop2 = objects.stop { stop in
            stop.id = "place-armnl"
            stop.name = "Arlington"
            stop.latitude = 42.351902
            stop.longitude = -71.070893
            stop.locationType = .station
        }
        let stop3 = objects.stop { stop in
            stop.id = "place-asmnl"
            stop.name = "Ashmont"
            stop.latitude = 42.28452
            stop.longitude = -71.063777
            stop.locationType = .station
        }
        let stop4 = objects.stop { stop in
            stop.id = "1432"
            stop.name = "Arsenal St @ Irving St"
            stop.latitude = 42.364737
            stop.longitude = -71.178564
            stop.locationType = .stop
        }
        let stop5 = objects.stop { stop in
            stop.id = "14320"
            stop.name = "Adams St @ Whitwell St"
            stop.latitude = 42.253069
            stop.longitude = -71.017292
            stop.locationType = .stop
        }
        let stop6 = objects.stop { stop in
            stop.id = "13"
            stop.name = "Andrew"
            stop.latitude = 42.329962
            stop.longitude = -71.057625
            stop.locationType = .stop
            stop.parentStationId = "place-andrw"
        }

        let stopSourceGenerator = StopSourceGenerator(stops: [stop1.id: stop1, stop2.id: stop2, stop3.id: stop3, stop4.id: stop4, stop5.id: stop5, stop6.id: stop6])
        let sources = stopSourceGenerator.stopSources
        XCTAssertEqual(sources.count, 2)

        let sourceIds = sources.map(\.id)
        XCTAssert(sourceIds.contains(StopSourceGenerator.getStopSourceId(.station)))
        XCTAssert(sourceIds.contains(StopSourceGenerator.getStopSourceId(.stop)))

        let stationSource = sources.first { $0.id == StopSourceGenerator.getStopSourceId(.station) }
        XCTAssertNotNil(stationSource)
        if case let .featureCollection(collection) = stationSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 3)
            XCTAssertTrue(collection.features.contains(where: { $0.geometry == .point(Point(stop1.coordinate)) }))
        } else {
            XCTFail("Station source had no features")
        }

        let stopSource = sources.first { $0.id == StopSourceGenerator.getStopSourceId(.stop) }
        XCTAssertNotNil(stopSource)
        if case let .featureCollection(collection) = stopSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 2)
            if case let .string(serviceStatus) = collection.features[0].properties![StopSourceGenerator.propServiceStatusKey] {
                XCTAssertEqual(serviceStatus, String(describing: StopServiceStatus.normal))
            } else {
                XCTFail("Source status property was not set correctly")
            }
            XCTAssertTrue(collection.features.contains(where: { $0.geometry == .point(Point(stop4.coordinate)) }))
        } else {
            XCTFail("Stop source had no features")
        }
    }

    func testStopsAreSnappedToRoutes() {
        let objects = MapTestDataHelper.objects

        let stops = [
            MapTestDataHelper.stopAssembly.id: MapTestDataHelper.stopAssembly,
            MapTestDataHelper.stopSullivan.id: MapTestDataHelper.stopSullivan,
            MapTestDataHelper.stopAlewife.id: MapTestDataHelper.stopAlewife,
            MapTestDataHelper.stopDavis.id: MapTestDataHelper.stopDavis,
        ]

        let routeSourceGenerator = RouteSourceGenerator(routeData: MapTestDataHelper.routeResponse, stopsById: stops)
        let stopSourceGenerator = StopSourceGenerator(stops: stops, routeSourceDetails: routeSourceGenerator.routeSourceDetails)
        let sources = stopSourceGenerator.stopSources
        let snappedStopCoordinates = CLLocationCoordinate2D(latitude: 42.3961623851223, longitude: -71.14129664101432)

        let stationSource = sources.first { $0.id == StopSourceGenerator.getStopSourceId(.station) }
        XCTAssertNotNil(stationSource)
        if case let .featureCollection(collection) = stationSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 4)
            if case let .point(point) = collection.features.first(where: { $0.identifier ==
                    FeatureIdentifier(MapTestDataHelper.stopAlewife.id)
            })!
                .geometry {
                XCTAssertEqual(point.coordinates, snappedStopCoordinates)
            } else {
                XCTFail("Source feature was not a point")
            }
        } else {
            XCTFail("Station source had no features")
        }
    }

    func testStopsFeaturesHaveServiceStatus() {
        let objects = MapTestDataHelper.objects

        let stops = [
            "70061": objects.stop { stop in
                stop.id = "70061"
                stop.name = "Alewife"
                stop.latitude = 42.396158
                stop.longitude = -71.139971
                stop.locationType = .stop
                stop.parentStationId = "place-alfcl"
            },
            "place-alfcl": objects.stop { stop in
                stop.id = "place-alfcl"
                stop.name = "Alewife"
                stop.latitude = 42.39583
                stop.longitude = -71.141287
                stop.locationType = .station
                stop.childStopIds = ["70061"]
            },
            "place-astao": objects.stop { stop in
                stop.id = "place-astao"
                stop.name = "Assembly"
                stop.latitude = 42.392811
                stop.longitude = -71.077257
                stop.locationType = .station
            },
        ]

        let now = Date.now

        let redAlert = objects.alert { alert in
            alert.id = "a1"
            alert.effect = .shuttle
            alert.activePeriod(start: now.addingTimeInterval(-1).toKotlinInstant(), end: nil)
            alert.informedEntity(activities: [.board], directionId: nil, facility: nil, route: "Red", routeType: .heavyRail, stop: "70061", trip: nil)
        }
        let orangeAlert = objects.alert { alert in
            alert.id = "a2"
            alert.effect = .stationClosure
            alert.activePeriod(start: now.addingTimeInterval(-1).toKotlinInstant(), end: nil)
            alert.informedEntity(activities: [.board], directionId: nil, facility: nil, route: "Orange", routeType: .heavyRail, stop: "place-astao", trip: nil)
        }

        let alertsByStop = [
            "place-alfcl": AlertAssociatedStop(
                stop: stops["place-alfcl"]!,
                relevantAlerts: [],
                routePatterns: [MapTestDataHelper.patternRed30],
                childStops: ["70061": stops["70061"]!],
                childAlerts: ["70061": AlertAssociatedStop(
                    stop: stops["70061"]!,
                    relevantAlerts: [redAlert],
                    routePatterns: [MapTestDataHelper.patternRed10],
                    childStops: [:],
                    childAlerts: [:]
                )]
            ),
            "place-astao": AlertAssociatedStop(
                stop: stops["place-astao"]!,
                relevantAlerts: [orangeAlert],
                routePatterns: [MapTestDataHelper.patternOrange30],
                childStops: [:],
                childAlerts: [:]
            ),
        ]
        let stopSourceGenerator = StopSourceGenerator(stops: stops, alertsByStop: alertsByStop)
        let sources = stopSourceGenerator.stopSources

        let stationSource = sources.first { $0.id == StopSourceGenerator.getStopSourceId(.station) }
        XCTAssertNotNil(stationSource)
        if case let .featureCollection(collection) = stationSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 2)

            let alewifeFeature = collection.features.first { feat in
                if case let .string(id) = feat.properties![StopSourceGenerator.propIdKey] { id == "place-alfcl" } else { false }
            }
            XCTAssertNotNil(alewifeFeature)
            if case let .string(serviceStatus) = alewifeFeature!.properties![StopSourceGenerator.propServiceStatusKey] {
                XCTAssertEqual(serviceStatus, String(describing: StopServiceStatus.partialService))
            } else {
                XCTFail("Disrupted source status property was not set correctly")
            }

            let assemblyFeature = collection.features.first { feat in
                if case let .string(id) = feat.properties![StopSourceGenerator.propIdKey] { id == "place-astao" } else { false }
            }
            XCTAssertNotNil(assemblyFeature)
            if case let .string(serviceStatus) = assemblyFeature!.properties![StopSourceGenerator.propServiceStatusKey] {
                XCTAssertEqual(serviceStatus, String(describing: StopServiceStatus.noService))
            } else {
                XCTFail("No service source status property was not set correctly")
            }
        } else {
            XCTFail("Station source had no features")
        }
    }
}
