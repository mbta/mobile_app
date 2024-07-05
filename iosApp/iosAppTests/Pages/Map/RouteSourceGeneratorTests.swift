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

    func testRouteSourceIsCreated() {
        let routeSource = RouteSourceGenerator.generateSource(
            routeData: MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
            routesById: MapTestDataHelper.routesById,
            stopsById: [MapTestDataHelper.stopAlewife.id: MapTestDataHelper.stopAlewife,
                        MapTestDataHelper.stopDavis.id: MapTestDataHelper
                            .stopDavis,
                        MapTestDataHelper.stopPorter.id: MapTestDataHelper
                            .stopPorter,
                        MapTestDataHelper.stopHarvard.id: MapTestDataHelper
                            .stopHarvard,
                        MapTestDataHelper.stopCentral.id: MapTestDataHelper
                            .stopCentral,
                        MapTestDataHelper.stopAssembly.id: MapTestDataHelper
                            .stopAssembly,
                        MapTestDataHelper.stopSullivan.id: MapTestDataHelper
                            .stopSullivan],
            alertsByStop: [:]
        )

        if case let .featureCollection(collection) = routeSource.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 3) // 2 red, 1 orange
            XCTAssertEqual(collection.features.filter {
                $0.properties![RouteSourceGenerator.propRouteId] == JSONValue(String(MapTestDataHelper.routeRed.id))
            }.count, 2)
            XCTAssertEqual(collection.features.filter {
                $0.properties![RouteSourceGenerator.propRouteId] == JSONValue(String(MapTestDataHelper.routeOrange.id))
            }.count, 1)

            XCTAssertEqual(
                collection.features.first {
                    $0.properties![RouteSourceGenerator.propRouteId] == JSONValue(String(MapTestDataHelper.routeRed.id))
                }!.geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeRedC2.polyline!).coordinates!)
                    .sliced(
                        from: MapTestDataHelper.stopAlewife.coordinate,
                        to: MapTestDataHelper.stopDavis.coordinate
                    )!)
            )

            XCTAssertEqual(
                collection.features.first {
                    $0
                        .properties![RouteSourceGenerator.propRouteId] ==
                        JSONValue(String(MapTestDataHelper.routeOrange.id))
                }!.geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeOrangeC1.polyline!)
                        .coordinates!)
                    .sliced(
                        from: MapTestDataHelper.stopAssembly.coordinate,
                        to: MapTestDataHelper.stopSullivan.coordinate
                    )!)
            )
        } else {
            XCTFail("Route source had no features")
        }
    }

    func testRouteSourcePreservesRouteProps() {
        let routeSource = RouteSourceGenerator.generateSource(
            routeData: MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
            routesById: MapTestDataHelper.routesById,
            stopsById: [MapTestDataHelper.stopAlewife.id: MapTestDataHelper.stopAlewife,
                        MapTestDataHelper.stopDavis.id: MapTestDataHelper
                            .stopDavis,
                        MapTestDataHelper.stopPorter.id: MapTestDataHelper
                            .stopPorter,
                        MapTestDataHelper.stopHarvard.id: MapTestDataHelper
                            .stopHarvard,
                        MapTestDataHelper.stopCentral.id: MapTestDataHelper
                            .stopCentral,
                        MapTestDataHelper.stopAssembly.id: MapTestDataHelper
                            .stopAssembly,
                        MapTestDataHelper.stopSullivan.id: MapTestDataHelper
                            .stopSullivan],
            alertsByStop: [:]
        )

        if case let .featureCollection(collection) = routeSource.data.unsafelyUnwrapped {
            let firstRedFeature = collection.features.filter {
                $0.properties![RouteSourceGenerator.propRouteId] == JSONValue(String(MapTestDataHelper.routeRed.id))
            }[0]

            XCTAssertEqual(
                firstRedFeature.properties![RouteSourceGenerator.propRouteColor],
                JSONValue(String("#DA291C"))
            )
            XCTAssertEqual(
                firstRedFeature.properties![RouteSourceGenerator.propRouteType],
                JSONValue(String("HEAVY_RAIL"))
            )
            XCTAssertEqual(firstRedFeature.properties![RouteSourceGenerator.propRouteSortKey], JSONValue(Int(-10010)))

        } else {
            XCTFail("Route source had no features")
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
                stateByRoute: [.red: .shuttle]
            ),
            MapTestDataHelper.stopHarvard.id: AlertAssociatedStop(
                stop: MapTestDataHelper.stopHarvard,
                relevantAlerts: [redAlert],
                stateByRoute: [.red: .shuttle]
            ),
        ]

        let routeSource = RouteSourceGenerator.generateSource(
            routeData: MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
            routesById: MapTestDataHelper.routesById,
            stopsById: [MapTestDataHelper.stopAlewife.id: MapTestDataHelper.stopAlewife,
                        MapTestDataHelper.stopDavis.id: MapTestDataHelper
                            .stopDavis,
                        MapTestDataHelper.stopPorter.id: MapTestDataHelper
                            .stopPorter,
                        MapTestDataHelper.stopHarvard.id: MapTestDataHelper
                            .stopHarvard,
                        MapTestDataHelper.stopCentral.id: MapTestDataHelper
                            .stopCentral],
            alertsByStop: alertsByStop
        )

        if case let .featureCollection(collection) = routeSource.data.unsafelyUnwrapped {
            let redFeatures = collection.features.filter {
                $0.properties![RouteSourceGenerator.propRouteId] == JSONValue(String(MapTestDataHelper.routeRed.id))
            }

            XCTAssertEqual(redFeatures.count, 3)
            XCTAssertEqual(
                redFeatures[0].properties![RouteSourceGenerator.propAlertStateKey]!,
                JSONValue(String(describing: SegmentAlertState.normal))
            )
            XCTAssertEqual(
                redFeatures[0].geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeRedC1.polyline!).coordinates!)
                    .sliced(
                        from: MapTestDataHelper.stopAlewife.coordinate,
                        to: MapTestDataHelper.stopDavis.coordinate
                    )!)
            )

            XCTAssertEqual(
                redFeatures[1].properties![RouteSourceGenerator.propAlertStateKey]!,
                JSONValue(String(describing: SegmentAlertState.shuttle))
            )
            XCTAssertEqual(
                redFeatures[1].geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeRedC1.polyline!).coordinates!)
                    .sliced(
                        from: MapTestDataHelper.stopPorter.coordinate,
                        to: MapTestDataHelper.stopHarvard.coordinate
                    )!)
            )
            XCTAssertEqual(
                redFeatures[2].properties![RouteSourceGenerator.propAlertStateKey]!,
                JSONValue(String(describing: SegmentAlertState.normal))
            )
            XCTAssertEqual(
                redFeatures[2].geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeRedC1.polyline!).coordinates!)
                    .sliced(
                        from: MapTestDataHelper.stopHarvard.coordinate,
                        to: MapTestDataHelper.stopCentral.coordinate
                    )!)
            )

        } else {
            XCTFail("Red route source had no features")
        }
    }

    func testShapeWithStopsToMapFriendly() {
        let now = Date.now

        let objects = ObjectCollectionBuilder()

        let redAlert = objects.alert { alert in
            alert.id = "a1"
            alert.effect = .shuttle
            alert.activePeriod(start: now.addingTimeInterval(-1).toKotlinInstant(), end: nil)
            alert.informedEntity(activities: [.board], directionId: nil, facility: nil,
                                 route: MapTestDataHelper.routeRed.id, routeType: .heavyRail,
                                 stop: MapTestDataHelper.stopAlewife.id, trip: nil)
            alert.informedEntity(activities: [.board], directionId: nil, facility: nil,
                                 route: MapTestDataHelper.routeRed.id, routeType: .heavyRail,
                                 stop: MapTestDataHelper.stopDavis.id, trip: nil)
        }
        let alertsByStop = [
            MapTestDataHelper.stopAlewife.id: AlertAssociatedStop(
                stop: MapTestDataHelper.stopAlewife,
                relevantAlerts: [redAlert],
                stateByRoute: [.red: .shuttle]
            ),
            MapTestDataHelper.stopDavis.id: AlertAssociatedStop(
                stop: MapTestDataHelper.stopDavis,
                relevantAlerts: [redAlert],
                stateByRoute: [.red: .shuttle]
            ),
        ]

        let shapeWithStops: ShapeWithStops = .init(directionId: MapTestDataHelper.patternRed10.directionId,
                                                   routeId: MapTestDataHelper.routeRed.id,
                                                   routePatternId: MapTestDataHelper.patternRed10.id,
                                                   shape: MapTestDataHelper.shapeRedC1,
                                                   stopIds: [
                                                       MapTestDataHelper.stopAlewifeChild.id,
                                                       MapTestDataHelper.stopDavisChild.id,
                                                   ])

        let transformedShapes: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] =
            RouteSourceGenerator.shapesWithStopsToMapFriendly([shapeWithStops],
                                                              [MapTestDataHelper.stopAlewife.id:
                                                                  MapTestDataHelper.stopAlewife,
                                                                  MapTestDataHelper.stopDavis.id:
                                                                      MapTestDataHelper.stopDavis,
                                                                  MapTestDataHelper.stopAlewifeChild.id:
                                                                      MapTestDataHelper.stopAlewifeChild,
                                                                  MapTestDataHelper.stopDavisChild.id:
                                                                      MapTestDataHelper.stopDavisChild])

        XCTAssertEqual([MapFriendlyRouteResponse
                .RouteWithSegmentedShapes(routeId: shapeWithStops.routeId,
                                          segmentedShapes: [
                                              .init(sourceRoutePatternId: shapeWithStops.routeId,
                                                    sourceRouteId: shapeWithStops.routeId,
                                                    directionId: shapeWithStops.directionId,
                                                    routeSegments:
                                                    [
                                                        .init(id: shapeWithStops.shape!.id,
                                                              sourceRoutePatternId: shapeWithStops
                                                                  .routePatternId,
                                                              sourceRouteId: shapeWithStops.routeId,
                                                              stopIds: [MapTestDataHelper.stopAlewife.id,
                                                                        MapTestDataHelper.stopDavis.id],
                                                              otherPatternsByStopId: [:]),
                                                    ],
                                                    shape: shapeWithStops.shape!),
                                          ])], transformedShapes)
    }
}
