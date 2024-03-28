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
        route.routePatternIds = ["Red-1-0", "Red-3-0"]
    }

    static let routeOrange = objects.route { route in
        route.id = "Orange"
        route.color = "ED8B00"
        route.routePatternIds = ["Orange-3-0", "Orange-7-0"]
    }

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
        shape.polyline = "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@jBeDnAiBz@iAf@k@l@g@dAs@fAe@|@WpCe@l@GTCRE\\G??~@O`@ELA|AGf@A\\CjCGrEKz@AdEAxHY|BD~@JjB^fF~AdDbA|InCxCv@zD|@rWfEXDpB`@tANvAHx@AjBIx@M~@S~@a@fAi@HEnA{@fA{@|HuI|DwEbDqDpLkNhCyClEiFhLaN`@c@f@o@RURUbDsDbAiA`AgAv@_AHKHI~E}FdBoBfAgAfD{DxDoE~DcF|BkClAwALODEJOJK|@gATWvAoA`Au@fAs@hAk@n@QpAa@vDeAhA[x@Yh@Wv@a@b@YfAaAjCgCz@aAtByBz@{@??|FaGtCaDbL{LhI{IzHgJdAuAjC{CVYvAwA??JIl@a@NMNM\\[|AuArF_GlPyQrD_ErAwAd@e@nE{ErDuD\\a@nE_FZYPSRUvL{Mv@}@Z[JILKv@m@z@i@fCkAlBmAl@[t@[??h@WxBeAp@]dAi@p@YXIPEXKDALENEbAQl@Gz@ChADtAL~ARnCZbGx@xB`@TDL@PBzAVjIvA^FVDVB|@NjHlAlPnCnCd@vBXhBNv@JtAPL@|BXrAN??`@FRBj@Bp@FbADz@?dAIp@I|@Mx@Q`AWhAYlBs@pDaBzAs@nBgAZQJGJGhAs@RKVMNKTMf@YdHcEzBmApAw@`GmDLI@AHGlEwClAi@hA_@v@Up@ObB]z@Kr@Ir@EZCpA?dCRf@DpAHvANrE`@bDTr@DfMdA`CJvBRn@DnCLnBPfAFV@"
    }

    static let shapeRedC2 = objects.shape { shape in
        shape.id = "canonical-931_0009"
        shape.polyline = "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@zDuF\\c@n@s@VObAw@^Sl@Yj@U\\O|@WdAUxAQRCt@E??xAGrBQZAhAGlAEv@Et@E~@AdAAbCGpCA|BEjCMr@?nBDvANlARdBb@nDbA~@XnBp@\\JRH??|Al@`AZbA^jA^lA\\h@P|@TxAZ|@J~@LN?fBXxHhApDt@b@JXFtAVhALx@FbADtAC`B?z@BHBH@|@f@RN^^T\\h@hANb@HZH`@H^LpADlA@dD@jD@x@@b@Bp@HdAFd@Ll@F^??n@rDBRl@vD^pATp@Rb@b@z@\\l@`@j@p@t@j@h@n@h@n@`@hAh@n@\\t@PzANpAApBGtE}@xBa@??xB_@nOmB`OgBb@IrC[p@MbEmARCV@d@LH?tDyAXM"
    }

    static let shapeOrangeC1 = objects.shape { shape in
        shape.id = "canonical-903_0018"
        shape.polyline = "an_bG|_xpLpBPrCZXBTBP@P@~Dd@dANlATjBd@tDhALDNDhA\\pIbClGjBz@ZhA\\xA`@XJpBl@??ZHpBn@HBfHvBfF~AhDbApA\\bAVND`Er@nALvCVfBLfFDb@?dBAjCEfB?pFDrHF~HFfB?vAC~FFpQJpB?|C@P?`B?V?X?h@B??l@DfEFL@f@BpCDxDFfABfA?lCBdB@fABnC@|C@tO@`ECfCI??|@ElESdCGdCAzA@pEEdBCnBItAAdBA^AT@nAAd@@`A?VAbC?fABzAD??nBDR?Z@v@@`DKtFCn@AZC`@Gr@Q\\MHEb@MzA{@zJeIlBaBhBoBzA}BlAoBrB_EVw@r@oBvAiE??DId@cBxCkGpGmK@Cda@o_@PO??tHkGbCuCh@e@b@Y????`@Sr@IlMg@zGB??T?vC?N@VN`ElDvAvBbBrBtA~A????v@t@hAtAhDbDb@f@t@p@VJj@Nh@@n@Bz@F??RBdD`@|B^XJRLz@`AxBvB|@N??xATNP`HrRJd@BZAf@OzCCbAApFCvG@hC?~F@t@@bB@jAFlAJ~AJf@???@Jb@DLRb@T^v@dA|AxBbB~BnHlJ^f@xAlBJHn@x@rBbClDnEt@fA??Zd@b@n@b@l@nDxEjN`RfAtApCrDb@h@RV??tCxDRTjLzNNRfEnFNN~AhBn@n@NJ`@\\????|@t@dE~CnA|@bAn@n@X`@Vd@R|@^\\Rf@Tp@Xn@R|@T~@ZpA`@l@NZHtEpAHBf@LbHrCvAr@??LFRLf@\\PJvA|@lA~@|AhAvCbCfDpCnA|@hCbBv@b@vAt@lAj@VL??JDd@TtBz@hA^RFvD`AdAZhBl@pA\\??~CfAzAh@vDfBxC`BXNb@T??NJdAj@~CdB|@h@h@Zv@h@fDhBHDnFvCdAn@vC`B~BrAvA~@|CjBrA~@hBhALJl@d@NHNJnDvBhAr@"
    }

    static let shapeOrangeAtypical = objects.shape { shape in shape.id = "40460002" }

    static let routeResponse = RouteResponse(
        routes: [routeRed, routeOrange],
        routePatterns: [
            "Red-1-0": patternRed10,
            "Red-3-0": patternRed30,
            "Orange-3-0": patternOrange30,
            "Orange-7-0": patternOrange70,
        ],
        shapes: [
            "canonical-933_0009": shapeRedC1,
            "canonical-931_0009": shapeRedC2,
            "canonical-903_0018": shapeOrangeC1,
            "40460002": shapeOrangeAtypical,
        ],
        trips: [
            "canonical-Red-C1-0": tripRedC1,
            "canonical-Red-C2-0": tripRedC2,
            "canonical-Orange-C1-0": tripOrangeC1,
            "61746557": tripOrangeAtypical,
        ]
    )
}
