package com.mbta.tid.mbta_app.android.map

import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse

object MapTestDataHelper {
    val objects = ObjectCollectionBuilder()

    val routeRed =
        objects.route {
            id = "Red"
            color = "DA291C"
            routePatternIds = mutableListOf("Red-1-0", "Red-3-0")
        }

    val routeOrange =
        objects.route {
            id = "Orange"
            color = "ED8800"
            routePatternIds = mutableListOf("Orange-3-0", "Orange-7-0")
        }

    val stopAlewife =
        objects.stop {
            id = "place-alfcl"
            latitude = 42.39583
            longitude = -71.141287
            locationType = LocationType.STATION
        }

    val stopDavis =
        objects.stop {
            id = "place-davis"
            latitude = 42.39674
            longitude = -71.121815
            locationType = LocationType.STATION
        }

    val stopPorter =
        objects.stop {
            id = "place-porter"
            latitude = 42.3884
            longitude = -71.119149
            locationType = LocationType.STATION
        }

    val stopHarvard =
        objects.stop {
            id = "place-harsq"
            latitude = 42.373362
            longitude = -71.118956
            locationType = LocationType.STATION
        }

    val stopCentral =
        objects.stop {
            id = "place-cntsq"
            latitude = 42.365486
            longitude = -71.103802
            locationType = LocationType.STATION
        }

    val stopAssembly =
        objects.stop {
            id = "place-astao"
            latitude = 42.392811
            longitude = -71.077257
            locationType = LocationType.STATION
        }

    val stopSullivan =
        objects.stop {
            id = "place-sull"
            latitude = 42.383975
            longitude = -71.076994
            locationType = LocationType.STATION
        }

    val patternRed10 =
        objects.routePattern(routeRed) {
            id = "Red-1-0"
            typicality = RoutePattern.Typicality.Typical
            representativeTripId = "canonical-Red-C2-0"
        }

    val patternRed30 =
        objects.routePattern(routeRed) {
            id = "Red-3-0"
            typicality = RoutePattern.Typicality.Typical
            representativeTripId = "canonical-Red-C1-0"
        }

    val patternOrange30 =
        objects.routePattern(routeOrange) {
            id = "Orange-3-0"
            typicality = RoutePattern.Typicality.Typical
            representativeTripId = "canonical-Orange-C1-0"
        }

    val patternOrange70 =
        objects.routePattern(routeOrange) {
            id = "Orange-7-0"
            typicality = RoutePattern.Typicality.Typical
            representativeTripId = "61746557"
        }

    val tripRedC1 =
        objects.trip(patternRed30) {
            id = "canonical-Red-C1-0"
            shapeId = "canonical-933_0009"
        }

    val tripRedC2 =
        objects.trip(patternRed10) {
            id = "canonical-Red-C2-0"
            shapeId = "canonical-931_0009"
            stopIds = listOf("70061")
        }

    val tripOrangeC1 =
        objects.trip(patternOrange30) {
            id = "canonical-Orange-C1-0"
            shapeId = "canonical-903_0018"
        }

    val tripOrangeAtypical =
        objects.trip(patternOrange30) {
            id = "61746557"
            shapeId = "40460002"
        }

    val shapeRedC1 =
        objects.shape {
            id = "canonical-933_0009"
            polyline =
                "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@jBeDnAiBz@iAf@k@l@g@dAs@fAe@|@WpCe@l@GTCRE\\G??~@O`@ELA|AGf@A\\CjCGrEKz@AdEAxHY|BD~@JjB^fF~AdDbA|InCxCv@zD|@rWfEXDpB`@tANvAHx@AjBIx@M~@S~@a@fAi@HEnA{@fA{@|HuI|DwEbDqDpLkNhCyClEiFhLaN`@c@f@o@RURUbDsDbAiA`AgAv@_AHKHI~E}FdBoBfAgAfD{DxDoE~DcF|BkClAwALODEJOJK|@gATWvAoA`Au@fAs@hAk@n@QpAa@vDeAhA[x@Yh@Wv@a@b@YfAaAjCgCz@aAtByBz@{@??|FaGtCaDbL{LhI{IzHgJdAuAjC{CVYvAwA??JIl@a@NMNM\\[|AuArF_GlPyQrD_ErAwAd@e@nE{ErDuD\\a@nE_FZYPSRUvL{Mv@}@Z[JILKv@m@z@i@fCkAlBmAl@[t@[??h@WxBeAp@]dAi@p@YXIPEXKDALENEbAQl@Gz@ChADtAL~ARnCZbGx@xB`@TDL@PBzAVjIvA^FVDVB|@NjHlAlPnCnCd@vBXhBNv@JtAPL@|BXrAN??`@FRBj@Bp@FbADz@?dAIp@I|@Mx@Q`AWhAYlBs@pDaBzAs@nBgAZQJGJGhAs@RKVMNKTMf@YdHcEzBmApAw@`GmDLI@AHGlEwClAi@hA_@v@Up@ObB]z@Kr@Ir@EZCpA?dCRf@DpAHvANrE`@bDTr@DfMdA`CJvBRn@DnCLnBPfAFV@"
        }

    val shapeRedC2 =
        objects.shape {
            id = "canonical-931_0009"
            polyline =
                "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@zDuF\\c@n@s@VObAw@^Sl@Yj@U\\O|@WdAUxAQRCt@E??xAGrBQZAhAGlAEv@Et@E~@AdAAbCGpCA|BEjCMr@?nBDvANlARdBb@nDbA~@XnBp@\\JRH??|Al@`AZbA^jA^lA\\h@P|@TxAZ|@J~@LN?fBXxHhApDt@b@JXFtAVhALx@FbADtAC`B?z@BHBH@|@f@RN^^T\\h@hANb@HZH`@H^LpADlA@dD@jD@x@@b@Bp@HdAFd@Ll@F^??n@rDBRl@vD^pATp@Rb@b@z@\\l@`@j@p@t@j@h@n@h@n@`@hAh@n@\\t@PzANpAApBGtE}@xBa@??xB_@nOmB`OgBb@IrC[p@MbEmARCV@d@LH?tDyAXM"
        }

    val shapeOrangeC1 =
        objects.shape {
            id = "canonical-903_0018"
            polyline =
                "an_bG|_xpLpBPrCZXBTBP@P@~Dd@dANlATjBd@tDhALDNDhA\\pIbClGjBz@ZhA\\xA`@XJpBl@??ZHpBn@HBfHvBfF~AhDbApA\\bAVND`Er@nALvCVfBLfFDb@?dBAjCEfB?pFDrHF~HFfB?vAC~FFpQJpB?|C@P?`B?V?X?h@B??l@DfEFL@f@BpCDxDFfABfA?lCBdB@fABnC@|C@tO@`ECfCI??|@ElESdCGdCAzA@pEEdBCnBItAAdBA^AT@nAAd@@`A?VAbC?fABzAD??nBDR?Z@v@@`DKtFCn@AZC`@Gr@Q\\MHEb@MzA{@zJeIlBaBhBoBzA}BlAoBrB_EVw@r@oBvAiE??DId@cBxCkGpGmK@Cda@o_@PO??tHkGbCuCh@e@b@Y????`@Sr@IlMg@zGB??T?vC?N@VN`ElDvAvBbBrBtA~A????v@t@hAtAhDbDb@f@t@p@VJj@Nh@@n@Bz@F??RBdD`@|B^XJRLz@`AxBvB|@N??xATNP`HrRJd@BZAf@OzCCbAApFCvG@hC?~F@t@@bB@jAFlAJ~AJf@???@Jb@DLRb@T^v@dA|AxBbB~BnHlJ^f@xAlBJHn@x@rBbClDnEt@fA??Zd@b@n@b@l@nDxEjN`RfAtApCrDb@h@RV??tCxDRTjLzNNRfEnFNN~AhBn@n@NJ`@\\????|@t@dE~CnA|@bAn@n@X`@Vd@R|@^\\Rf@Tp@Xn@R|@T~@ZpA`@l@NZHtEpAHBf@LbHrCvAr@??LFRLf@\\PJvA|@lA~@|AhAvCbCfDpCnA|@hCbBv@b@vAt@lAj@VL??JDd@TtBz@hA^RFvD`AdAZhBl@pA\\??~CfAzAh@vDfBxC`BXNb@T??NJdAj@~CdB|@h@h@Zv@h@fDhBHDnFvCdAn@vC`B~BrAvA~@|CjBrA~@hBhALJl@d@NHNJnDvBhAr@"
        }

    val shapeOrangeAtypical = objects.shape { id = "40460002" }

    val routeResponse =
        MapFriendlyRouteResponse(
            listOf(
                MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                    routeRed.id,
                    listOf(
                        SegmentedRouteShape(
                            patternRed10.id,
                            patternRed10.routeId,
                            patternRed10.directionId,
                            listOf(
                                RouteSegment(
                                    "segment1",
                                    patternRed10.id,
                                    patternRed10.routeId,
                                    listOf(stopAlewife.id, stopDavis.id),
                                    emptyMap()
                                )
                            ),
                            shapeRedC1
                        ),
                        SegmentedRouteShape(
                            patternRed30.id,
                            patternRed30.routeId,
                            patternRed30.directionId,
                            listOf(
                                RouteSegment(
                                    "segment2",
                                    patternRed30.id,
                                    patternRed30.routeId,
                                    listOf(stopPorter.id, stopHarvard.id, stopCentral.id),
                                    emptyMap()
                                )
                            ),
                            shapeRedC1
                        ),
                    )
                ),
                MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                    routeOrange.id,
                    listOf(
                        SegmentedRouteShape(
                            patternOrange30.id,
                            patternOrange30.routeId,
                            patternOrange30.directionId,
                            listOf(
                                RouteSegment(
                                    "segment3",
                                    patternOrange30.id,
                                    patternOrange30.routeId,
                                    listOf(stopAssembly.id, stopSullivan.id),
                                    emptyMap()
                                )
                            ),
                            shapeOrangeC1
                        )
                    )
                )
            )
        )
}
