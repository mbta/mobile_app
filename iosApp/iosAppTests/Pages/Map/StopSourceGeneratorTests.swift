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

        let stopSourceGenerator = StopSourceGenerator(stops: [
            stop1.id: .init(stop: stop1, routes: [:], routeTypes: [MapStopRoute.blue]),
            stop2.id: .init(stop: stop2, routes: [:], routeTypes: [MapStopRoute.green]),
            stop3.id: .init(
                stop: stop3,
                routes: [:],
                routeTypes: [MapStopRoute.red, MapStopRoute.mattapan, MapStopRoute.bus]
            ),
            stop4.id: .init(stop: stop4, routes: [:], routeTypes: [MapStopRoute.bus]),
            stop5.id: .init(stop: stop5, routes: [:], routeTypes: [MapStopRoute.bus]),
            stop6.id: .init(stop: stop6, routes: [:], routeTypes: [MapStopRoute.bus]),
        ])
        let source = stopSourceGenerator.stopSource

        XCTAssertEqual(source.id, StopSourceGenerator.stopSourceId)

        XCTAssertNotNil(source)
        if case let .featureCollection(collection) = source.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 5)
            XCTAssertTrue(collection.features.contains(where: { $0.geometry == .point(Point(stop1.coordinate)) }))
        } else {
            XCTFail("Station source had no features")
        }
    }

    func testStopsAreSnappedToRoutes() {
        let stops = [
            MapTestDataHelper.stopAssembly.id: MapTestDataHelper.mapStopAssembly,
            MapTestDataHelper.stopSullivan.id: MapTestDataHelper.mapStopSullivan,
            MapTestDataHelper.stopAlewife.id: MapTestDataHelper.mapStopAlewife,
            MapTestDataHelper.stopDavis.id: MapTestDataHelper.mapStopDavis,
        ]

        let routeSourceGenerator = RouteSourceGenerator(
            routeData: MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
            routesById: MapTestDataHelper.routesById,
            stopsById: stops.mapValues { mapStop in mapStop.stop },
            alertsByStop: [:]
        )
        let stopSourceGenerator = StopSourceGenerator(
            stops: stops,
            routeLines: routeSourceGenerator.routeLines
        )
        let source = stopSourceGenerator.stopSource
        let snappedStopCoordinates = CLLocationCoordinate2D(latitude: 42.3961623851223, longitude: -71.14129664101432)

        if case let .featureCollection(collection) = source.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 4)
            if case let .point(point) = collection.features.first(where: {
                $0.identifier == FeatureIdentifier(MapTestDataHelper.stopAlewife.id)
            })!.geometry {
                XCTAssertEqual(point.coordinates, snappedStopCoordinates)
            } else {
                XCTFail("Source feature was not a point")
            }
        } else {
            XCTFail("Station source had no features")
        }
    }

    func testSelectedStopHasPropSet() {
        let objects = ObjectCollectionBuilder()
        let selectedStop = objects.stop { stop in
            stop.id = "place-alfcl"
            stop.name = "Alewife"
            stop.latitude = 42.39583
            stop.longitude = -71.141287
            stop.locationType = .station
            stop.childStopIds = ["70061"]
        }

        let otherStop = objects.stop { stop in
            stop.id = "place-davis"
            stop.name = "Davis"
            stop.locationType = .station
            stop.childStopIds = []
        }

        let stopSourceGenerator = StopSourceGenerator(
            stops: [selectedStop.id: selectedStop, otherStop.id: otherStop].mapValues { stop in
                MapStop(stop: stop, routes: [.red: [MapTestDataHelper.routeRed]], routeTypes: [.red])
            },
            selectedStop: selectedStop,
            routeLines: []
        )

        let source = stopSourceGenerator.stopSource
        if case let .featureCollection(collection) = source.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 2)

            let selectedFeature = collection.features.first { feat in
                feat.identifier == FeatureIdentifier(selectedStop.id)
            }
            let otherFeature = collection.features.first { feat in
                feat.identifier == FeatureIdentifier(otherStop.id)
            }
            XCTAssertNotNil(selectedFeature)
            if case let .boolean(isSelected) = selectedFeature!.properties![StopSourceGenerator.propIsSelectedKey] {
                XCTAssertTrue(isSelected)
            } else {
                XCTFail("Selected stop doesn't have isSelected prop set")
            }

            if case let .boolean(isOtherSelected) = otherFeature!.properties![StopSourceGenerator.propIsSelectedKey] {
                XCTAssertFalse(isOtherSelected)
            } else {
                XCTFail("Selected stop doesn't have isSelected prop set")
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
            alert.informedEntity(
                activities: [.board],
                directionId: nil,
                facility: nil,
                route: "Red",
                routeType: .heavyRail,
                stop: "70061",
                trip: nil
            )
        }
        let orangeAlert = objects.alert { alert in
            alert.id = "a2"
            alert.effect = .stationClosure
            alert.activePeriod(start: now.addingTimeInterval(-1).toKotlinInstant(), end: nil)
            alert.informedEntity(
                activities: [.board],
                directionId: nil,
                facility: nil,
                route: "Orange",
                routeType: .heavyRail,
                stop: "place-astao",
                trip: nil
            )
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
        let stopSourceGenerator = StopSourceGenerator(
            stops: [
                "70061": .init(
                    stop: stops["70061"]!,
                    routes: [.red: [MapTestDataHelper.routeRed]],
                    routeTypes: [.red]
                ),
                "place-alfcl": .init(
                    stop: stops["place-alfcl"]!,
                    routes: [.red: [MapTestDataHelper.routeRed]],
                    routeTypes: [.red]
                ),
                "place-astao": .init(
                    stop: stops["place-astao"]!,
                    routes: [.orange: [MapTestDataHelper.routeOrange]],
                    routeTypes: [.orange]
                ),
            ],
            alertsByStop: alertsByStop
        )
        let source = stopSourceGenerator.stopSource

        if case let .featureCollection(collection) = source.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 2)

            let alewifeFeature = collection.features.first { feat in propId(from: feat) == "place-alfcl" }
            XCTAssertNotNil(alewifeFeature)
            if let serviceStatus = propString(prop: StopSourceGenerator.propServiceStatusKey, from: alewifeFeature!) {
                XCTAssertEqual(serviceStatus, StopServiceStatus.partialService.name)
            } else {
                XCTFail("Disrupted source status property was not set correctly")
            }

            let assemblyFeature = collection.features.first { feat in propId(from: feat) == "place-astao" }
            XCTAssertNotNil(assemblyFeature)
            if let serviceStatus = propString(prop: StopSourceGenerator.propServiceStatusKey, from: assemblyFeature!) {
                XCTAssertEqual(serviceStatus, StopServiceStatus.noService.name)
            } else {
                XCTFail("No service source status property was not set correctly")
            }
        } else {
            XCTFail("Station source had no features")
        }
    }

    func testStopFeaturesHaveRoutes() {
        let stops = [
            MapTestDataHelper.stopAssembly.id: MapTestDataHelper.mapStopAssembly,
            MapTestDataHelper.stopSullivan.id: MapTestDataHelper.mapStopSullivan,
            MapTestDataHelper.stopAlewife.id: MapTestDataHelper.mapStopAlewife,
            MapTestDataHelper.stopDavis.id: MapTestDataHelper.mapStopDavis,
        ]

        let routeSourceGenerator = RouteSourceGenerator(
            routeData: MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
            routesById: MapTestDataHelper.routesById,
            stopsById: stops.mapValues { mapStop in mapStop.stop },
            alertsByStop: [:]
        )
        let stopSourceGenerator = StopSourceGenerator(
            stops: stops,
            routeLines: routeSourceGenerator.routeLines
        )

        let source = stopSourceGenerator.stopSource
        if case let .featureCollection(collection) = source.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 4)
            guard let assemblyFeature = collection.features.first(where: { feat in
                propId(from: feat) == MapTestDataHelper.stopAssembly.id
            }) else {
                XCTFail("Assembly stop feature was not present in the source")
                return
            }

            if let assemblyRoutes = propRoutesArray(from: assemblyFeature) {
                XCTAssertEqual(assemblyRoutes, [MapStopRoute.orange])
            } else {
                XCTFail("Assembly route property was not set correctly")
            }

            guard let alewifeFeature = collection.features.first(where: { feat in
                propId(from: feat) == MapTestDataHelper.stopAlewife.id
            }) else {
                XCTFail("Alewife stop feature was not present in the source")
                return
            }

            if let alewifeRoutes = propRoutesArray(from: alewifeFeature) {
                XCTAssertEqual(alewifeRoutes, [MapStopRoute.red, MapStopRoute.bus])
            } else {
                XCTFail("Alewife route property was not set correctly")
            }
        } else {
            XCTFail("Station source had no features")
        }
    }

    func testStopFeaturesHaveNames() {
        let stops = [
            MapTestDataHelper.stopAssembly.id: MapTestDataHelper.mapStopAssembly,
            MapTestDataHelper.stopAlewife.id: MapTestDataHelper.mapStopAlewife,
        ]

        let stopSourceGenerator = StopSourceGenerator(stops: stops)

        let source = stopSourceGenerator.stopSource
        if case let .featureCollection(collection) = source.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 2)
            guard let assemblyFeature = collection.features.first(where: { feat in
                propId(from: feat) == MapTestDataHelper.stopAssembly.id
            }) else {
                XCTFail("Assembly stop feature was not present in the source")
                return
            }

            if let assemblyName = propString(prop: StopSourceGenerator.propNameKey, from: assemblyFeature) {
                XCTAssertEqual(assemblyName, MapTestDataHelper.stopAssembly.name)
            } else {
                XCTFail("Assembly name property was not set correctly")
            }

            guard let alewifeFeature = collection.features.first(where: { feat in
                propId(from: feat) == MapTestDataHelper.stopAlewife.id
            }) else {
                XCTFail("Alewife stop feature was not present in the source")
                return
            }

            if let alewifeName = propString(prop: StopSourceGenerator.propNameKey, from: alewifeFeature) {
                XCTAssertEqual(alewifeName, MapTestDataHelper.stopAlewife.name)
            } else {
                XCTFail("Alewife name property was not set correctly")
            }
        } else {
            XCTFail("Station source had no features")
        }
    }

    private func asString(_ wrapped: JSONValue) -> String? {
        if case let .string(value) = wrapped { value } else { nil }
    }

    private func propId(from feat: Feature) -> String? {
        propString(prop: StopSourceGenerator.propIdKey, from: feat)
    }

    private func propRoutesArray(
        prop: String = StopSourceGenerator.propMapRoutesKey,
        from feat: Feature
    ) -> [MapStopRoute]? {
        guard case let .array(routes) = feat.properties![prop] else { return nil }
        return routes.compactMap { wrappedValue in
            MapStopRoute.allCases.first { enumCase in enumCase.name == asString(wrappedValue ?? "") }
        }
    }

    private func propString(prop: String, from feat: Feature) -> String? {
        guard let value: JSONValue = feat.properties?[prop] ?? nil else { return nil }
        return asString(value)
    }
}
