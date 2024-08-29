//
//  GreenLineHelper.swift
//  iosAppTests
//
//  Created by Simon, Emma on 6/27/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import XCTest
@_spi(Experimental) import MapboxMaps

enum GreenLineHelper {
    static let objects = ObjectCollectionBuilder()

    static let line = objects.line { line in
        line.id = "line-Green"
    }

    static let routeB = objects.route { route in
        route.id = "Green-B"
        route.type = .lightRail
        route.shortName = "B"
        route.lineId = "line-Green"
        route.directionNames = ["West", "East"]
        route.directionDestinations = ["Kenmore & West", "Park St & North"]
    }

    static let routeC = objects.route { route in
        route.id = "Green-C"
        route.type = .lightRail
        route.shortName = "C"
        route.lineId = "line-Green"
        route.directionNames = ["West", "East"]
        route.directionDestinations = ["Kenmore & West", "Park St & North"]
    }

    static let routeE = objects.route { route in
        route.id = "Green-E"
        route.type = .lightRail
        route.shortName = "E"
        route.lineId = "line-Green"
        route.directionNames = ["West", "East"]
        route.directionDestinations = ["Heath Street", "Park St & North"]
    }

    static let stopArlington = objects.stop { stop in
        stop.id = "place-armnl"
        stop.name = "Arlington"
        stop.childStopIds = ["70156", "70157"]
    }

    static let stopEastbound = objects.stop { stop in
        stop.id = "70156"
        stop.name = "Arlington"
        stop.description_ = "Arlington - Green Line - Park Street & North"
        stop.parentStationId = "place-armnl"
    }

    static let stopWestbound = objects.stop { stop in
        stop.id = "70157"
        stop.name = "Arlington"
        stop.description_ = "Arlington - Green Line - Copley & West"
        stop.parentStationId = "place-armnl"
    }

    static let rpB0 = objects.routePattern(route: routeB) { pattern in
        pattern.id = "Green-B-812-0"
        pattern.sortOrder = 100_320_000
        pattern.typicality = .typical
        pattern.directionId = 0
        pattern.representativeTrip { trip in
            trip.headsign = "Boston College"
            trip.directionId = 0
        }
    }

    static let rpB1 = objects.routePattern(route: routeB) { pattern in
        pattern.id = "Green-B-812-1"
        pattern.sortOrder = 100_321_000
        pattern.typicality = .typical
        pattern.directionId = 1
        pattern.representativeTrip { trip in
            trip.headsign = "Government Center"
            trip.directionId = 1
        }
    }

    static let rpC0 = objects.routePattern(route: routeC) { pattern in
        pattern.id = "Green-C-832-0"
        pattern.sortOrder = 100_330_000
        pattern.typicality = .typical
        pattern.directionId = 0
        pattern.representativeTrip { trip in
            trip.headsign = "Cleveland Circle"
            trip.directionId = 0
        }
    }

    static let rpC1 = objects.routePattern(route: routeC) { pattern in
        pattern.id = "Green-C-832-1"
        pattern.sortOrder = 100_331_000
        pattern.typicality = .typical
        pattern.directionId = 1
        pattern.representativeTrip { trip in
            trip.headsign = "Government Center"
            trip.directionId = 1
        }
    }

    static let rpE0 = objects.routePattern(route: routeE) { pattern in
        pattern.id = "Green-E-886-0"
        pattern.sortOrder = 100_350_000
        pattern.typicality = .typical
        pattern.directionId = 0
        pattern.representativeTrip { trip in
            trip.headsign = "Heath Street"
            trip.directionId = 0
            trip.routePatternId = pattern.id
        }
    }

    static let rpE1 = objects.routePattern(route: routeE) { pattern in
        pattern.id = "Green-E-886-1"
        pattern.sortOrder = 100_351_000
        pattern.typicality = .typical
        pattern.directionId = 1
        pattern.representativeTrip { trip in
            trip.headsign = "Medford/Tufts"
            trip.directionId = 1
        }
    }

    static let shapeB0 = objects.shape { shape in
        shape.id = "canonical-8000012"
        shape.polyline =
            // swiftlint:disable:next line_length
            "oplaGpwjqLJq@B_@@W@}@KmBGmBAyAAqDJ}K@eEDaBHoAJ{AVcCJw@RkABQ??Z}AZwAVw@`@mAd@kAdAsCn@kBLg@Lk@DYLaBCUESKWA???AASYMQEEm@g@YQg@Sg@Uc@So@_@c@Wc@_@a@_@[_@w@gAaA_BO[??[s@a@kA[sAQ}@Go@G_ACcB@k@BiB?}@E}AEi@C]??Eo@Im@Mo@S_A]eA[y@Wk@u@oA_@e@]a@]]o@g@w@i@eAm@??ECuAy@{@m@uCeBm@[QI[U_B{@yAk@q@Sc@IiCUg@Mu@W??[KUKIGQYUe@Kc@Ii@Ew@?g@@]B]Ha@d@_CBE??t@aDNo@Jo@Dc@@a@?_@Ag@Gi@Gk@Om@_@cAOa@??O_@uA{DcAiCs@qB_@_A}@eCGM??q@iBOk@Gc@Eg@Ai@?m@FyALyB@k@?gAAe@Eq@QsAKe@Om@Uo@c@eAe@_AYi@q@kAg@o@QY??W_@IQAO?Yh@sIX{E`@wGBg@??PiCHmABo@J_C\\mFRiD^cG??\\oFLkC\\yFBW@[Fw@Da@?EHgA\\uFj@uIRiD??JkBn@qKFkA??`@uGT_ERgDJyA??B]JsBVyEViF@aBEqGMqF?G???A?oC@S@MFQt@aAzAkBHOL_@FWBQ@QQuR?kABm@??D{@Bm@b@aJ?SAOGa@{Igg@YcB_@aCMs@??cJog@Ga@??wAqIe@cDScCKcBXaLC_@CMEKEE??CEyA]]Ma@Ui@SOQQGa@QSIgBw@q@UYO]Co@]i@c@W[_@c@[_@{@eAU]??q@{@UYWQa@c@o@u@uB_Cu@q@eEmC[Ua@]"
    }

    static let shapeB1 = objects.shape { shape in
        shape.id = "canonical-8000013"
        shape.polyline =
            // swiftlint:disable:next line_length
            "{hpaGdxupLXVZTdElCt@p@tB~Bn@t@`@b@VPTXp@z@??T\\z@dAZ^^b@VZh@b@n@\\\\BXNp@TfBv@RH`@PPFNPh@R`@T\\LB@??tAZHJDJBLB^Y`LJbBRbCd@bDvArI??F^bJng@??Lr@^`CXbBzIfg@F`@@N?Rc@`JCl@Ez@??Cl@?jAPtRAPCPGVM^IN{AjBu@`AGPALAR?nC?D???BLpFDpGA`BWhFWxEKrBEj@??IjASfDU~Da@`H??G~@o@pKM~B??QtCk@tI]tFIfA?DE`@Gv@AZCV]xFMjC_@`G??]pFShD]lFK~BCn@IlASvC??AXa@vGYzEi@rI?X@NHPX`@??NVf@n@p@jAXh@d@~@b@dATn@Nl@Jd@PrADp@@d@?fAAj@MxBGxA?l@@h@Df@Fb@Nj@r@jB??DJ|@dC^~@r@pBbAhCtAzD^`A@@??\\`ANl@Fj@Fh@@f@?^A`@Eb@Kn@On@y@fD????e@~BI`@C\\A\\?f@Dv@Hh@Jb@Td@PXHFTJVJ??x@Vf@LhCTb@Hp@RxAj@~Az@ZTPHl@ZtCdBz@l@tAx@D@??dAn@v@h@n@f@\\\\\\`@^d@t@nAVj@Zx@\\dAR~@Ln@Hl@Dp@??BZDh@D|A?|@ChBAj@BbBF~@Fn@P|@ZrA`@jAj@nABD??|@xAv@fAZ^`@^b@^b@Vn@^b@Rf@Tf@RXPl@f@DDLPRX????B@JVDRBTM`BEXMj@Mf@o@jBeArCe@jAa@lAWv@[vAYvA??EVSjAKv@WbCKzAInAE`BAdEK|K@pD@xAFlBJlBA|@AVC^??"
    }

    static let shapeC0 = objects.shape { shape in
        shape.id = "canonical-8000005"
        shape.polyline =
            // swiftlint:disable:next line_length
            "}wkaGligqL_@uCQcAEYg@kDcAaIGc@??E]Kw@SiB_@wCc@qDQuAEa@Q{A??QwAk@yDg@gD]eC@PGa@??i@iDgAmHUuAIe@EMUu@Mc@AG??Ie@EYGi@KeBEaAAy@@_C?oDA{AAO???YIgBIeBM_BMmA??EY[oBQiAUiAe@sBOi@_@mAI]KWIYa@gBCO]gB??G_@g@_DKw@g@cEcAiGS_AEUCW??Gc@AYEe@[oBSiAc@wBcBaJ]uB??Ki@OaAs@mDo@uDUmAKm@??s@}Da@wBg@oCUmAAK??_AeFYuAUyAMy@{@sEKc@Om@??GQOq@]aBs@cEkFqXgF{ZAS??COAiECuACkB?EQ}B?G???A?oC@S@MFQt@aAzAkBHOL_@FWBQ@QQuR?kABm@??D{@Bm@b@aJ?SAOGa@{Igg@YcB_@aCMs@??cJog@Ga@??wAqIe@cDScCKcBXaLC_@CMEKEE??CEyA]]Ma@Ui@SOQQGa@QSIgBw@q@UYO]Co@]i@c@W[_@c@[_@{@eAU]??q@{@UYWQa@c@o@u@uB_Cu@q@eEmC[Ua@]"
    }

    static let shapeC1 = objects.shape { shape in
        shape.id = "canonical-8000006"
        shape.polyline =
            // swiftlint:disable:next line_length
            "{hpaGdxupLXVZTdElCt@p@tB~Bn@t@`@b@VPTXp@z@??T\\z@dAZ^^b@VZh@b@n@\\\\BXNp@TfBv@RH`@PPFNPh@R`@T\\LB@??tAZHJDJBLB^Y`LJbBRbCd@bDvArI??F^bJng@??Lr@^`CXbBzIfg@F`@@N?Rc@`JCl@Ez@??Cl@?jAPtRAPCPGVM^IN{AjBu@`AGPALAR?nC?D???BP|B?DBjBBtA@hEBN??@RfFzZjFpXr@bE\\`BNp@DP??Pl@Jb@z@rELx@TxAXtA~@dF??@JTlAf@nC`@vB~@jFDR??Nx@n@tDr@lDN`ATlA??RpAbB`Jb@vBRhAZnBDd@@XFd@??BTDTR~@bAhGf@bEJv@f@~CX~A??Jf@BN`@fBHXJVH\\^lANh@d@rBThAPhAPbA??Hj@RfBL~AHdBDv@??Bn@@h@@zA?nDA~B@x@D`AJdBFh@DXJl@FR??DNTt@DLHd@TtAfAlHh@hD??F`@AQ\\dCf@fDj@xDVxB??Jx@D`@PtAb@pD^vCRhBJv@L`AFZ??z@dHf@jDDXPbA^tC"
    }

    static let shapeE0 = objects.shape { shape in
        shape.id = "canonical-8000015"
        shape.polyline =
            // swiftlint:disable:next line_length
            "cgjaGbu_qLCCCAOBGDKLIJW`@QX_@d@e@TgA`@aBn@WF??_Cp@wDbA??SH[FEAEEQYQ[Q]Ua@[i@o@oAM]O_@Yu@Ia@GYAGCW??CUAm@?w@Aw@?w@C_AAuC?qA?[Ee@Mq@Oi@]}@OYKS??Yq@a@eAOi@Ss@??EMQi@k@oBEO}AuFSw@m@mCMk@k@mB]eAOi@IU??m@kBm@}B}B}HcB_Gg@iB??WeA_BaFYaAgAyDUaA_@oAq@}Bq@cCIW{AcFg@_BQi@??s@yBYcA]kAQm@Oe@e@aAWe@gA{A_D}Da@g@k@u@??{MiQSY??_@i@yE{GW]_@Yw@[iHiAOCmBf@QGISYaAYcB_@aCMs@??cJog@Ga@??wAqIe@cDScCKcBXaLC_@CMEKEE??CEyA]]Ma@Ui@SOQQGa@QSIgBw@q@UYO]Co@]i@c@W[_@c@[_@{@eAU]??q@{@UYWQa@c@o@u@uB_Cu@q@eEmC[Ua@]??KIQQq@o@uE}DUOUAg@@aGRa@H[BSDKD??_@RMHi@b@MPyAbBmIjH??ONuCrCGPCN?RBh@BVJZbB~Ef@|ALl@D`@Bb@ANCN]~@_@`CKr@KbAUtASt@Sp@Un@Uh@Qd@e@jAELwAfDw@~B??ELo@zAeBlEkBnEqA~Cw@jBsBfFO^s@nBuAjBo@`@A@??]VWPONUZ[h@Qj@WjAUfAmEjLo@~As@v@y@v@YRe@Ra@FQD_A^YRW\\]h@e@`A[z@IZO^MX_@f@{@~@cAdA_A~@wKdKs@z@aArAe@n@wBvB??_DzC{H|HsDfDiAz@iBnAkApAk@p@u@p@qAvAs@~@k@v@eApBcAvBo@dBaBzEw@|Be@rA??IT{@xBk@xAY|@k@vAaA~Ce@rAiCrHyAvDOd@a@`A[l@m@dAo@dA[h@KPs@~@m@l@{@|@s@t@UPq@f@q@`@ED??UL{@l@cAr@{@j@q@d@a@Vg@\\e@Zg@\\KFQNy@h@i@\\a@XMJgBlAs@d@s@h@cAp@m@`@c@Zw@f@m@b@y@h@_@V_An@w@h@o@b@??IDe@Zk@b@o@b@cAn@_Aj@]VSLa@Xg@\\c@Ze@Xg@\\UNg@\\g@^cAr@{@j@_@T{AfAOJsA|@iD`CqCjBkCdBm@`@OLMJSN"
    }

    static let shapeE1 = objects.shape { shape in
        shape.id = "canonical-8000018"
        shape.polyline =
            // swiftlint:disable:next line_length
            "awyaG|~`qLZSLKNMl@a@jCeBpCkBhDaCrA}@NKzAgA^Uz@k@bAs@f@_@f@]TOf@]d@Yb@[f@]`@YRM\\W~@k@bAo@n@c@j@c@d@[JG??l@a@v@i@~@o@^Wx@i@l@c@v@g@b@[l@a@bAq@r@i@r@e@fBmALK`@Yh@]x@i@POJGf@]d@[f@]`@Wp@e@z@k@bAs@z@m@TM??DEp@a@p@g@TQr@u@z@}@l@m@r@_AJQZi@n@eAl@eAZm@`@aANe@xAwDhCsHd@sA`A_Dj@wAX}@j@yAz@yBDO??h@yAv@}B`B{En@eBbAwBdAqBj@w@r@_ApAwAt@q@j@q@jAqAhBoAhA{@rDgDzH}HzCyC??zByBd@o@`AsAr@{@vKeK~@_AbAeAz@_A^g@LYN_@H[Z{@d@aA\\i@V]XS~@_@PE`@Gd@SXSx@w@r@w@n@_BlEkLTgAVkAPk@Zi@T[NOVQ\\Y??@?n@a@tAkBr@oBN_@rBgFv@kBpA_DjBoEdBmEn@{A@C??z@iCvAgDDMd@kAPe@Ti@To@Rq@Ru@TuAJcAJs@^aC\\_ABO@OCc@Ea@Mm@g@}AcB_FK[CWCi@?SBOFQtCsCNO??lIkHxAcBLQh@c@LIZQ??NGREZC`@I`GSf@AT@TNtE|Dp@n@PPRN??XVZTdElCt@p@tB~Bn@t@`@b@VPTXp@z@??T\\z@dAZ^^b@VZh@b@n@\\\\BXNp@TfBv@RH`@PPFNPh@R`@T\\LB@??tAZHJDJBLB^Y`LJbBRbCd@bDvArI??F^bJng@??Lr@^`CXbBX`AHRPFlBg@NBhHhAv@Z^XV\\xEzG^h@??RXxMfQ??l@v@`@f@~C|DfAzAVd@d@`ANd@Pl@\\jAXbAr@xB??Ph@f@~AzAbFHVp@bCp@|B^nAT`AfAxDX`A~A`FXhA??d@dBbB~F|B|Hl@|Bj@dB??JZNh@\\dAj@lBLj@l@lCRv@|AtFDNj@nBPh@DL??Rr@Nh@`@dAZr@??HPNX\\|@Nh@Lp@Dd@?Z?pA@tCB~@?v@@v@?v@@l@D^??@L@FFXH`@Xt@N^L\\n@nAZh@T`@P\\PZPXDDD@ZGTI??tDcA~Bq@??VG`Bo@fAa@d@Uf@Bd@CT?b@MBa@CYG]"
    }

    static let nearbyData = NearbyStaticData.companion.build { builder in
        builder.line(line: line, routes: [routeB, routeC, routeE]) { builder in
            builder.stop(
                stop: stopArlington,
                routes: [routeB, routeC, routeE],
                childStopIds: [stopEastbound.id, stopWestbound.id]
            ) { builder in
                builder.direction(
                    direction: Direction(name: "West", destination: "Kenmore & West", id: 0),
                    routes: [routeB, routeC], patterns: [rpB0, rpC0]
                )
                builder.headsign(
                    route: routeE,
                    headsign: "Heath Street",
                    patterns: [rpE0],
                    routePatternId: "Green-E-886-0"
                )
                builder.direction(
                    direction: Direction(name: "East", destination: "Park St & North", id: 1),
                    routes: [routeB, routeC, routeE], patterns: [rpB1, rpC1, rpE1]
                )
            }
        }
    }

    static let state = NearbyViewModel.NearbyTransitState(
        loadedLocation: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
        nearbyByRouteAndStop: nearbyData
    )

    static let stopMapResponse = StopMapResponse(
        routeShapes: [
            MapFriendlyRouteResponse.RouteWithSegmentedShapes(routeId: routeB.id, segmentedShapes: [
                SegmentedRouteShape(
                    sourceRoutePatternId: rpB0.id, sourceRouteId: rpB0.routeId,
                    directionId: rpB0.directionId,
                    routeSegments: [RouteSegment(
                        id: "segmentB0",
                        sourceRoutePatternId: rpB0.id,
                        sourceRouteId: rpB0.routeId,
                        stopIds: ["place-gover", "place-pktrm", stopArlington.id, "place-lake"],
                        otherPatternsByStopId: [:]
                    )],
                    shape: shapeB0
                ),
                SegmentedRouteShape(
                    sourceRoutePatternId: rpB1.id, sourceRouteId: rpB1.routeId,
                    directionId: rpB1.directionId,
                    routeSegments: [RouteSegment(
                        id: "segmentB1",
                        sourceRoutePatternId: rpB1.id,
                        sourceRouteId: rpB1.routeId,
                        stopIds: ["place-lake", stopArlington.id, "place-pktrm", "place-gover"],
                        otherPatternsByStopId: [:]
                    )],
                    shape: shapeB1
                ),
            ]),
            MapFriendlyRouteResponse.RouteWithSegmentedShapes(routeId: routeC.id, segmentedShapes: [
                SegmentedRouteShape(
                    sourceRoutePatternId: rpC0.id, sourceRouteId: rpC0.routeId,
                    directionId: rpC0.directionId,
                    routeSegments: [RouteSegment(
                        id: "segmentC0",
                        sourceRoutePatternId: rpC0.id,
                        sourceRouteId: rpC0.routeId,
                        stopIds: ["place-gover", "place-pktrm", stopArlington.id, "place-clmnl"],
                        otherPatternsByStopId: [:]
                    )],
                    shape: shapeC0
                ),
                SegmentedRouteShape(
                    sourceRoutePatternId: rpC1.id, sourceRouteId: rpC1.routeId,
                    directionId: rpC1.directionId,
                    routeSegments: [RouteSegment(
                        id: "segmentC1",
                        sourceRoutePatternId: rpC1.id,
                        sourceRouteId: rpC1.routeId,
                        stopIds: ["place-clmnl", stopArlington.id, "place-pktrm", "place-gover"],
                        otherPatternsByStopId: [:]
                    )],
                    shape: shapeC1
                ),
            ]),
            MapFriendlyRouteResponse.RouteWithSegmentedShapes(routeId: routeE.id, segmentedShapes: [
                SegmentedRouteShape(
                    sourceRoutePatternId: rpE0.id, sourceRouteId: rpE0.routeId,
                    directionId: rpE0.directionId,
                    routeSegments: [RouteSegment(
                        id: "segmentE0",
                        sourceRoutePatternId: rpE0.id,
                        sourceRouteId: rpE0.routeId,
                        stopIds: ["place-gover", "place-pktrm", stopArlington.id, "place-hsmnl"],
                        otherPatternsByStopId: [:]
                    )],
                    shape: shapeE0
                ),
                SegmentedRouteShape(
                    sourceRoutePatternId: rpE1.id, sourceRouteId: rpE1.routeId,
                    directionId: rpE1.directionId,
                    routeSegments: [RouteSegment(
                        id: "segmentE1",
                        sourceRoutePatternId: rpE1.id,
                        sourceRouteId: rpE1.routeId,
                        stopIds: ["place-hsmnl", stopArlington.id, "place-pktrm", "place-gover"],
                        otherPatternsByStopId: [:]
                    )],
                    shape: shapeE1
                ),
            ]),
        ],
        childStops: [:]
    )
}
