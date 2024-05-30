//
//  MapTestDataHelper.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared

enum MapTestDataHelper {
    static let objects = ObjectCollectionBuilder()
    static let routeRed = objects.route { route in
        route.id = "Red"
        route.color = "DA291C"
        route.type = RouteType.heavyRail
        route.routePatternIds = ["Red-1-0", "Red-3-0"]
        route.sortOrder = 10010
    }

    static let routeOrange = objects.route { route in
        route.id = "Orange"
        route.color = "ED8B00"
        route.routePatternIds = ["Orange-3-0", "Orange-7-0"]
    }

    static let routesById = [routeRed.id: routeRed, routeOrange.id: routeOrange]

    static let stopAlewife = objects.stop { stop in
        stop.id = "place-alfcl"
        stop.latitude = 42.39583
        stop.longitude = -71.141287
        stop.locationType = LocationType.station
    }

    static let mapStopAlewife: MapStop = .init(
        stop: stopAlewife,
        routes: [MapStopRoute.red: [routeRed]],
        routeTypes: [MapStopRoute.red]
    )

    static let stopDavis = objects.stop { stop in
        stop.id = "place-davis"
        stop.latitude = 42.39674
        stop.longitude = -71.121815
        stop.locationType = LocationType.station
    }

    static let mapStopDavis: MapStop = .init(
        stop: stopDavis,
        routes: [MapStopRoute.red: [routeRed]],
        routeTypes: [MapStopRoute.red]
    )

    static let stopPorter = objects.stop { stop in
        stop.id = "place-porter"
        stop.latitude = 42.3884
        stop.longitude = -71.119149
        stop.locationType = LocationType.station
    }

    static let mapStopPorter: MapStop = .init(
        stop: stopPorter,
        routes: [MapStopRoute.red: [routeRed]],
        routeTypes: [MapStopRoute.red]
    )

    static let stopHarvard = objects.stop { stop in
        stop.id = "place-harsq"
        stop.latitude = 42.373362
        stop.longitude = -71.118956
        stop.locationType = LocationType.station
    }

    static let mapStopHarvard: MapStop = .init(
        stop: stopHarvard,
        routes: [MapStopRoute.red: [routeRed]],
        routeTypes: [MapStopRoute.red]
    )

    static let stopCentral = objects.stop { stop in
        stop.id = "place-cntsq"
        stop.latitude = 42.365486
        stop.longitude = -71.103802
        stop.locationType = LocationType.station
    }

    static let mapStopCentral: MapStop = .init(
        stop: stopCentral,
        routes: [MapStopRoute.red: [routeRed]],
        routeTypes: [MapStopRoute.red]
    )

    static let stopAssembly = objects.stop { stop in
        stop.id = "place-astao"
        stop.latitude = 42.392811
        stop.longitude = -71.077257
        stop.locationType = LocationType.station
    }

    static let mapStopAssembly: MapStop = .init(
        stop: stopAssembly,
        routes: [MapStopRoute.orange: [routeOrange]],
        routeTypes: [MapStopRoute.orange]
    )

    static let stopSullivan = objects.stop { stop in
        stop.id = "place-sull"
        stop.latitude = 42.383975
        stop.longitude = -71.076994
        stop.locationType = LocationType.station
    }

    static let mapStopSullivan: MapStop = .init(
        stop: stopSullivan,
        routes: [MapStopRoute.orange: [routeOrange]],
        routeTypes: [MapStopRoute.orange]
    )

    static let patternRed10 = objects.routePattern(route: routeRed) { pattern in
        pattern.id = "Red-1-0"
        pattern.typicality = .typical
        pattern.representativeTripId = "canonical-Red-C2-0"
    }

    static let patternRed30 = objects.routePattern(route: routeRed) { pattern in
        pattern.id = "Red-3-0"
        pattern.typicality = .typical
        pattern.representativeTripId = "canonical-Red-C1-0"
    }

    static let patternOrange30 = objects.routePattern(route: routeOrange) { pattern in
        pattern.id = "Orange-3-0"
        pattern.typicality = .typical
        pattern.representativeTripId = "canonical-Orange-C1-0"
    }

    static let patternOrange70 = objects.routePattern(route: routeOrange) { pattern in
        pattern.id = "Orange-7-0"
        pattern.typicality = .atypical
        pattern.representativeTripId = "61746557"
    }

    static let tripRedC1 = objects.trip(routePattern: patternRed30) { trip in
        trip.id = "canonical-Red-C1-0"
        trip.shapeId = "canonical-933_0009"
    }

    static let tripRedC2 = objects.trip(routePattern: patternRed10) { trip in
        trip.id = "canonical-Red-C2-0"
        trip.shapeId = "canonical-931_0009"
        trip.stopIds = ["70061"]
    }

    static let tripOrangeC1 = objects.trip(routePattern: patternOrange30) { trip in
        trip.id = "canonical-Orange-C1-0"
        trip.shapeId = "canonical-903_0018"
    }

    static let tripOrangeAtypical = objects.trip(routePattern: patternOrange30) { trip in
        trip.id = "61746557"
        trip.shapeId = "40460002"
    }

    static let shapeRedC1 = objects.shape { shape in
        shape.id = "canonical-933_0009"
        shape.polyline =
            // swiftlint:disable:next line_length
            "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@jBeDnAiBz@iAf@k@l@g@dAs@fAe@|@WpCe@l@GTCRE\\G??~@O`@ELA|AGf@A\\CjCGrEKz@AdEAxHY|BD~@JjB^fF~AdDbA|InCxCv@zD|@rWfEXDpB`@tANvAHx@AjBIx@M~@S~@a@fAi@HEnA{@fA{@|HuI|DwEbDqDpLkNhCyClEiFhLaN`@c@f@o@RURUbDsDbAiA`AgAv@_AHKHI~E}FdBoBfAgAfD{DxDoE~DcF|BkClAwALODEJOJK|@gATWvAoA`Au@fAs@hAk@n@QpAa@vDeAhA[x@Yh@Wv@a@b@YfAaAjCgCz@aAtByBz@{@??|FaGtCaDbL{LhI{IzHgJdAuAjC{CVYvAwA??JIl@a@NMNM\\[|AuArF_GlPyQrD_ErAwAd@e@nE{ErDuD\\a@nE_FZYPSRUvL{Mv@}@Z[JILKv@m@z@i@fCkAlBmAl@[t@[??h@WxBeAp@]dAi@p@YXIPEXKDALENEbAQl@Gz@ChADtAL~ARnCZbGx@xB`@TDL@PBzAVjIvA^FVDVB|@NjHlAlPnCnCd@vBXhBNv@JtAPL@|BXrAN??`@FRBj@Bp@FbADz@?dAIp@I|@Mx@Q`AWhAYlBs@pDaBzAs@nBgAZQJGJGhAs@RKVMNKTMf@YdHcEzBmApAw@`GmDLI@AHGlEwClAi@hA_@v@Up@ObB]z@Kr@Ir@EZCpA?dCRf@DpAHvANrE`@bDTr@DfMdA`CJvBRn@DnCLnBPfAFV@"
    }

    static let shapeRedC2 = objects.shape { shape in
        shape.id = "canonical-931_0009"
        shape.polyline =
            // swiftlint:disable:next line_length
            "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@zDuF\\c@n@s@VObAw@^Sl@Yj@U\\O|@WdAUxAQRCt@E??xAGrBQZAhAGlAEv@Et@E~@AdAAbCGpCA|BEjCMr@?nBDvANlARdBb@nDbA~@XnBp@\\JRH??|Al@`AZbA^jA^lA\\h@P|@TxAZ|@J~@LN?fBXxHhApDt@b@JXFtAVhALx@FbADtAC`B?z@BHBH@|@f@RN^^T\\h@hANb@HZH`@H^LpADlA@dD@jD@x@@b@Bp@HdAFd@Ll@F^??n@rDBRl@vD^pATp@Rb@b@z@\\l@`@j@p@t@j@h@n@h@n@`@hAh@n@\\t@PzANpAApBGtE}@xBa@??xB_@nOmB`OgBb@IrC[p@MbEmARCV@d@LH?tDyAXM"
    }

    static let shapeOrangeC1 = objects.shape { shape in
        shape.id = "canonical-903_0018"
        shape.polyline =
            // swiftlint:disable:next line_length
            "an_bG|_xpLpBPrCZXBTBP@P@~Dd@dANlATjBd@tDhALDNDhA\\pIbClGjBz@ZhA\\xA`@XJpBl@??ZHpBn@HBfHvBfF~AhDbApA\\bAVND`Er@nALvCVfBLfFDb@?dBAjCEfB?pFDrHF~HFfB?vAC~FFpQJpB?|C@P?`B?V?X?h@B??l@DfEFL@f@BpCDxDFfABfA?lCBdB@fABnC@|C@tO@`ECfCI??|@ElESdCGdCAzA@pEEdBCnBItAAdBA^AT@nAAd@@`A?VAbC?fABzAD??nBDR?Z@v@@`DKtFCn@AZC`@Gr@Q\\MHEb@MzA{@zJeIlBaBhBoBzA}BlAoBrB_EVw@r@oBvAiE??DId@cBxCkGpGmK@Cda@o_@PO??tHkGbCuCh@e@b@Y????`@Sr@IlMg@zGB??T?vC?N@VN`ElDvAvBbBrBtA~A????v@t@hAtAhDbDb@f@t@p@VJj@Nh@@n@Bz@F??RBdD`@|B^XJRLz@`AxBvB|@N??xATNP`HrRJd@BZAf@OzCCbAApFCvG@hC?~F@t@@bB@jAFlAJ~AJf@???@Jb@DLRb@T^v@dA|AxBbB~BnHlJ^f@xAlBJHn@x@rBbClDnEt@fA??Zd@b@n@b@l@nDxEjN`RfAtApCrDb@h@RV??tCxDRTjLzNNRfEnFNN~AhBn@n@NJ`@\\????|@t@dE~CnA|@bAn@n@X`@Vd@R|@^\\Rf@Tp@Xn@R|@T~@ZpA`@l@NZHtEpAHBf@LbHrCvAr@??LFRLf@\\PJvA|@lA~@|AhAvCbCfDpCnA|@hCbBv@b@vAt@lAj@VL??JDd@TtBz@hA^RFvD`AdAZhBl@pA\\??~CfAzAh@vDfBxC`BXNb@T??NJdAj@~CdB|@h@h@Zv@h@fDhBHDnFvCdAn@vC`B~BrAvA~@|CjBrA~@hBhALJl@d@NHNJnDvBhAr@"
    }

    static let shapeOrangeAtypical = objects.shape { shape in shape.id = "40460002" }

    static let routeResponse = MapFriendlyRouteResponse(
        routesWithSegmentedShapes: [
            MapFriendlyRouteResponse.RouteWithSegmentedShapes(routeId: routeRed.id, segmentedShapes: [
                SegmentedRouteShape(sourceRoutePatternId: patternRed10.id, sourceRouteId: patternRed10.routeId,
                                    directionId: patternRed10.directionId,
                                    routeSegments: [RouteSegment(id: "segment1",
                                                                 sourceRoutePatternId: patternRed10.id,
                                                                 sourceRouteId: patternRed10.routeId,
                                                                 stopIds: [stopAlewife.id, stopDavis.id],
                                                                 otherPatternsByStopId: [:])],
                                    shape: shapeRedC1),
                SegmentedRouteShape(sourceRoutePatternId: patternRed30.id, sourceRouteId: patternRed30.routeId,
                                    directionId: patternRed30.directionId,
                                    routeSegments: [RouteSegment(id: "segment2",
                                                                 sourceRoutePatternId: patternRed30.id,
                                                                 sourceRouteId: patternRed30.routeId,
                                                                 stopIds: [
                                                                     stopPorter.id,
                                                                     stopHarvard.id,
                                                                     stopCentral.id,
                                                                 ],
                                                                 otherPatternsByStopId: [:])],
                                    shape: shapeRedC1),
            ]),
            MapFriendlyRouteResponse.RouteWithSegmentedShapes(routeId: routeOrange.id, segmentedShapes: [
                SegmentedRouteShape(sourceRoutePatternId: patternOrange30.id, sourceRouteId: patternOrange30.routeId,
                                    directionId: patternOrange30.directionId,
                                    routeSegments: [RouteSegment(id: "segment3",
                                                                 sourceRoutePatternId: patternOrange30.id,
                                                                 sourceRouteId: patternOrange30.routeId,
                                                                 stopIds: [stopAssembly.id, stopSullivan.id],
                                                                 otherPatternsByStopId: [:])],
                                    shape: shapeOrangeC1),
            ]),
        ]
    )
}
