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

        let stopSourceGenerator = StopSourceGenerator(stops: [stop1, stop2, stop3, stop4, stop5, stop6])
        let sources = stopSourceGenerator.stopSources
        XCTAssertEqual(sources.count, 2)

        let sourceIds = sources.map(\.id)
        XCTAssert(sourceIds.contains(StopSourceGenerator.getStopSourceId(.station)))
        XCTAssert(sourceIds.contains(StopSourceGenerator.getStopSourceId(.stop)))

        let stationSource = sources.first { $0.id == StopSourceGenerator.getStopSourceId(.station) }
        XCTAssertNotNil(stationSource)
        if case let .featureCollection(collection) = stationSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 3)
            XCTAssertEqual(collection.features[0].geometry, .point(Point(stop1.coordinate)))
        } else {
            XCTFail("Station source had no features")
        }

        let stopSource = sources.first { $0.id == StopSourceGenerator.getStopSourceId(.stop) }
        XCTAssertNotNil(stopSource)
        if case let .featureCollection(collection) = stopSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 2)
            XCTAssertEqual(collection.features[0].geometry, .point(Point(stop4.coordinate)))
        } else {
            XCTFail("Stop source had no features")
        }
    }

    func testStopsAreSnappedToRoutes() {
        let objects = MapTestDataHelper.objects

        let stops = [
            objects.stop { stop in
                stop.id = "70061"
                stop.name = "Alewife"
                stop.latitude = 42.396158
                stop.longitude = -71.139971
                stop.locationType = .stop
                stop.parentStationId = "place-alfcl"
            },
            objects.stop { stop in
                stop.id = "place-alfcl"
                stop.name = "Alewife"
                stop.latitude = 42.39583
                stop.longitude = -71.141287
                stop.locationType = .station
            },
            objects.stop { stop in
                stop.id = "place-astao"
                stop.name = "Assembly"
                stop.latitude = 42.392811
                stop.longitude = -71.077257
                stop.locationType = .station
            },
        ]

        let routeSourceGenerator = RouteSourceGenerator(routeData: MapTestDataHelper.routeResponse)
        let stopSourceGenerator = StopSourceGenerator(stops: stops, routeSourceDetails: routeSourceGenerator.routeSourceDetails)
        let sources = stopSourceGenerator.stopSources
        let snappedStopCoordinates = CLLocationCoordinate2D(latitude: 42.39616238508952, longitude: -71.14129664308807)

        let stationSource = sources.first { $0.id == StopSourceGenerator.getStopSourceId(.station) }
        XCTAssertNotNil(stationSource)
        if case let .featureCollection(collection) = stationSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 2)
            if case let .point(point) = collection.features[0].geometry {
                XCTAssertEqual(point.coordinates, snappedStopCoordinates)
            } else {
                XCTFail("Source feature was not a point")
            }
        } else {
            XCTFail("Station source had no features")
        }
    }
}

func getSnapTestRouteResponse(_ objects: ObjectCollectionBuilder) -> RouteResponse {
    let routeRed = objects.route { route in
        route.id = "Red"
        route.routePatternIds = ["Red-1-0"]
    }

    let patternRed10 = objects.routePattern(route: routeRed) { pattern in
        pattern.id = "Red-1-0"
        pattern.typicality = .typical
        pattern.representativeTripId = "canonical-Red-C2-0"
    }

    let tripRedC2 = objects.trip(routePattern: patternRed10) { trip in
        trip.id = "canonical-Red-C2-0"
        trip.shapeId = "canonical-931_0009"
        trip.stopIds = ["70061"]
    }

    let shapeRedC2 = objects.shape { shape in
        shape.id = "canonical-931_0009"
        shape.polyline = "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@zDuF\\c@n@s@VObAw@^Sl@Yj@U\\O|@WdAUxAQRCt@E??xAGrBQZAhAGlAEv@Et@E~@AdAAbCGpCA|BEjCMr@?nBDvANlARdBb@nDbA~@XnBp@\\JRH??|Al@`AZbA^jA^lA\\h@P|@TxAZ|@J~@LN?fBXxHhApDt@b@JXFtAVhALx@FbADtAC`B?z@BHBH@|@f@RN^^T\\h@hANb@HZH`@H^LpADlA@dD@jD@x@@b@Bp@HdAFd@Ll@F^??n@rDBRl@vD^pATp@Rb@b@z@\\l@`@j@p@t@j@h@n@h@n@`@hAh@n@\\t@PzANpAApBGtE}@xBa@??xB_@nOmB`OgBb@IrC[p@MbEmARCV@d@LH?tDyAXM"
    }

    let stops = [
        objects.stop { stop in
            stop.id = "70061"
            stop.name = "Alewife"
            stop.latitude = 42.396158
            stop.longitude = -71.139971
            stop.locationType = .stop
            stop.parentStationId = "place-alfcl"
        },
        objects.stop { stop in
            stop.id = "place-alfcl"
            stop.name = "Alewife"
            stop.latitude = 42.39583
            stop.longitude = -71.141287
            stop.locationType = .station
        },
        objects.stop { stop in
            stop.id = "place-astao"
            stop.name = "Assembly"
            stop.latitude = 42.392811
            stop.longitude = -71.077257
            stop.locationType = .station
        },
    ]

    return RouteResponse(
        routes: [routeRed],
        routePatterns: ["Red-1-0": patternRed10],
        shapes: ["canonical-931_0009": shapeRedC2],
        trips: ["canonical-Red-C2-0": tripRedC2]
    )
}
