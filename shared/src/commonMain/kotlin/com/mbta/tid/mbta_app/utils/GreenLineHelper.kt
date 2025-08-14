package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.StopMapResponse

internal typealias RouteWithSegmentedShapes = MapFriendlyRouteResponse.RouteWithSegmentedShapes

public class GreenLineTestHelper {
    public companion object {
        public val objects: ObjectCollectionBuilder = TestData.clone()
        public val line: Line = objects.getLine("line-Green")
        public val routeB: Route = objects.getRoute("Green-B")
        public val routeC: Route = objects.getRoute("Green-C")
        public val routeE: Route = objects.getRoute("Green-E")

        internal val stopArlington = objects.getStop("place-armnl")
        public val stopEastbound: Stop = objects.getStop("70156")
        public val stopWestbound: Stop = objects.getStop("70157")

        public val rpB0: RoutePattern = objects.getRoutePattern("Green-B-812-0")
        public val rpB1: RoutePattern = objects.getRoutePattern("Green-B-812-1")
        public val rpC0: RoutePattern = objects.getRoutePattern("Green-C-832-0")
        public val rpC1: RoutePattern = objects.getRoutePattern("Green-C-832-1")
        public val rpE0: RoutePattern = objects.getRoutePattern("Green-E-886-0")
        public val rpE1: RoutePattern = objects.getRoutePattern("Green-E-886-1")

        internal val shapeB0 =
            objects.shape {
                id = "canonical-8000012"
                polyline =
                    "oplaGpwjqLJq@B_@@W@}@KmBGmBAyAAqDJ}K@eEDaBHoAJ{AVcCJw@RkABQ??Z}AZwAVw@`@mAd@kAdAsCn@kBLg@Lk@DYLaBCUESKWA???AASYMQEEm@g@YQg@Sg@Uc@So@_@c@Wc@_@a@_@[_@w@gAaA_BO[??[s@a@kA[sAQ}@Go@G_ACcB@k@BiB?}@E}AEi@C]??Eo@Im@Mo@S_A]eA[y@Wk@u@oA_@e@]a@]]o@g@w@i@eAm@??ECuAy@{@m@uCeBm@[QI[U_B{@yAk@q@Sc@IiCUg@Mu@W??[KUKIGQYUe@Kc@Ii@Ew@?g@@]B]Ha@d@_CBE??t@aDNo@Jo@Dc@@a@?_@Ag@Gi@Gk@Om@_@cAOa@??O_@uA{DcAiCs@qB_@_A}@eCGM??q@iBOk@Gc@Eg@Ai@?m@FyALyB@k@?gAAe@Eq@QsAKe@Om@Uo@c@eAe@_AYi@q@kAg@o@QY??W_@IQAO?Yh@sIX{E`@wGBg@??PiCHmABo@J_C\\mFRiD^cG??\\oFLkC\\yFBW@[Fw@Da@?EHgA\\uFj@uIRiD??JkBn@qKFkA??`@uGT_ERgDJyA??B]JsBVyEViF@aBEqGMqF?G???A?oC@S@MFQt@aAzAkBHOL_@FWBQ@QQuR?kABm@??D{@Bm@b@aJ?SAOGa@{Igg@YcB_@aCMs@??cJog@Ga@??wAqIe@cDScCKcBXaLC_@CMEKEE??CEyA]]Ma@Ui@SOQQGa@QSIgBw@q@UYO]Co@]i@c@W[_@c@[_@{@eAU]??q@{@UYWQa@c@o@u@uB_Cu@q@eEmC[Ua@]"
            }

        internal val shapeB1 =
            objects.shape {
                id = "canonical-8000013"
                polyline =
                    "{hpaGdxupLXVZTdElCt@p@tB~Bn@t@`@b@VPTXp@z@??T\\z@dAZ^^b@VZh@b@n@\\\\BXNp@TfBv@RH`@PPFNPh@R`@T\\LB@??tAZHJDJBLB^Y`LJbBRbCd@bDvArI??F^bJng@??Lr@^`CXbBzIfg@F`@@N?Rc@`JCl@Ez@??Cl@?jAPtRAPCPGVM^IN{AjBu@`AGPALAR?nC?D???BLpFDpGA`BWhFWxEKrBEj@??IjASfDU~Da@`H??G~@o@pKM~B??QtCk@tI]tFIfA?DE`@Gv@AZCV]xFMjC_@`G??]pFShD]lFK~BCn@IlASvC??AXa@vGYzEi@rI?X@NHPX`@??NVf@n@p@jAXh@d@~@b@dATn@Nl@Jd@PrADp@@d@?fAAj@MxBGxA?l@@h@Df@Fb@Nj@r@jB??DJ|@dC^~@r@pBbAhCtAzD^`A@@??\\`ANl@Fj@Fh@@f@?^A`@Eb@Kn@On@y@fD????e@~BI`@C\\A\\?f@Dv@Hh@Jb@Td@PXHFTJVJ??x@Vf@LhCTb@Hp@RxAj@~Az@ZTPHl@ZtCdBz@l@tAx@D@??dAn@v@h@n@f@\\\\\\`@^d@t@nAVj@Zx@\\dAR~@Ln@Hl@Dp@??BZDh@D|A?|@ChBAj@BbBF~@Fn@P|@ZrA`@jAj@nABD??|@xAv@fAZ^`@^b@^b@Vn@^b@Rf@Tf@RXPl@f@DDLPRX????B@JVDRBTM`BEXMj@Mf@o@jBeArCe@jAa@lAWv@[vAYvA??EVSjAKv@WbCKzAInAE`BAdEK|K@pD@xAFlBJlBA|@AVC^??"
            }

        internal val shapeC0 =
            objects.shape {
                id = "canonical-8000005"
                polyline =
                    "}wkaGligqL_@uCQcAEYg@kDcAaIGc@??E]Kw@SiB_@wCc@qDQuAEa@Q{A??QwAk@yDg@gD]eC@PGa@??i@iDgAmHUuAIe@EMUu@Mc@AG??Ie@EYGi@KeBEaAAy@@_C?oDA{AAO???YIgBIeBM_BMmA??EY[oBQiAUiAe@sBOi@_@mAI]KWIYa@gBCO]gB??G_@g@_DKw@g@cEcAiGS_AEUCW??Gc@AYEe@[oBSiAc@wBcBaJ]uB??Ki@OaAs@mDo@uDUmAKm@??s@}Da@wBg@oCUmAAK??_AeFYuAUyAMy@{@sEKc@Om@??GQOq@]aBs@cEkFqXgF{ZAS??COAiECuACkB?EQ}B?G???A?oC@S@MFQt@aAzAkBHOL_@FWBQ@QQuR?kABm@??D{@Bm@b@aJ?SAOGa@{Igg@YcB_@aCMs@??cJog@Ga@??wAqIe@cDScCKcBXaLC_@CMEKEE??CEyA]]Ma@Ui@SOQQGa@QSIgBw@q@UYO]Co@]i@c@W[_@c@[_@{@eAU]??q@{@UYWQa@c@o@u@uB_Cu@q@eEmC[Ua@]"
            }

        internal val shapeC1 =
            objects.shape {
                id = "canonical-8000006"
                polyline =
                    "{hpaGdxupLXVZTdElCt@p@tB~Bn@t@`@b@VPTXp@z@??T\\z@dAZ^^b@VZh@b@n@\\\\BXNp@TfBv@RH`@PPFNPh@R`@T\\LB@??tAZHJDJBLB^Y`LJbBRbCd@bDvArI??F^bJng@??Lr@^`CXbBzIfg@F`@@N?Rc@`JCl@Ez@??Cl@?jAPtRAPCPGVM^IN{AjBu@`AGPALAR?nC?D???BP|B?DBjBBtA@hEBN??@RfFzZjFpXr@bE\\`BNp@DP??Pl@Jb@z@rELx@TxAXtA~@dF??@JTlAf@nC`@vB~@jFDR??Nx@n@tDr@lDN`ATlA??RpAbB`Jb@vBRhAZnBDd@@XFd@??BTDTR~@bAhGf@bEJv@f@~CX~A??Jf@BN`@fBHXJVH\\^lANh@d@rBThAPhAPbA??Hj@RfBL~AHdBDv@??Bn@@h@@zA?nDA~B@x@D`AJdBFh@DXJl@FR??DNTt@DLHd@TtAfAlHh@hD??F`@AQ\\dCf@fDj@xDVxB??Jx@D`@PtAb@pD^vCRhBJv@L`AFZ??z@dHf@jDDXPbA^tC"
            }

        internal val shapeE0 =
            objects.shape {
                id = "canonical-8000015"
                polyline =
                    "cgjaGbu_qLCCCAOBGDKLIJW`@QX_@d@e@TgA`@aBn@WF??_Cp@wDbA??SH[FEAEEQYQ[Q]Ua@[i@o@oAM]O_@Yu@Ia@GYAGCW??CUAm@?w@Aw@?w@C_AAuC?qA?[Ee@Mq@Oi@]}@OYKS??Yq@a@eAOi@Ss@??EMQi@k@oBEO}AuFSw@m@mCMk@k@mB]eAOi@IU??m@kBm@}B}B}HcB_Gg@iB??WeA_BaFYaAgAyDUaA_@oAq@}Bq@cCIW{AcFg@_BQi@??s@yBYcA]kAQm@Oe@e@aAWe@gA{A_D}Da@g@k@u@??{MiQSY??_@i@yE{GW]_@Yw@[iHiAOCmBf@QGISYaAYcB_@aCMs@??cJog@Ga@??wAqIe@cDScCKcBXaLC_@CMEKEE??CEyA]]Ma@Ui@SOQQGa@QSIgBw@q@UYO]Co@]i@c@W[_@c@[_@{@eAU]??q@{@UYWQa@c@o@u@uB_Cu@q@eEmC[Ua@]??KIQQq@o@uE}DUOUAg@@aGRa@H[BSDKD??_@RMHi@b@MPyAbBmIjH??ONuCrCGPCN?RBh@BVJZbB~Ef@|ALl@D`@Bb@ANCN]~@_@`CKr@KbAUtASt@Sp@Un@Uh@Qd@e@jAELwAfDw@~B??ELo@zAeBlEkBnEqA~Cw@jBsBfFO^s@nBuAjBo@`@A@??]VWPONUZ[h@Qj@WjAUfAmEjLo@~As@v@y@v@YRe@Ra@FQD_A^YRW\\]h@e@`A[z@IZO^MX_@f@{@~@cAdA_A~@wKdKs@z@aArAe@n@wBvB??_DzC{H|HsDfDiAz@iBnAkApAk@p@u@p@qAvAs@~@k@v@eApBcAvBo@dBaBzEw@|Be@rA??IT{@xBk@xAY|@k@vAaA~Ce@rAiCrHyAvDOd@a@`A[l@m@dAo@dA[h@KPs@~@m@l@{@|@s@t@UPq@f@q@`@ED??UL{@l@cAr@{@j@q@d@a@Vg@\\e@Zg@\\KFQNy@h@i@\\a@XMJgBlAs@d@s@h@cAp@m@`@c@Zw@f@m@b@y@h@_@V_An@w@h@o@b@??IDe@Zk@b@o@b@cAn@_Aj@]VSLa@Xg@\\c@Ze@Xg@\\UNg@\\g@^cAr@{@j@_@T{AfAOJsA|@iD`CqCjBkCdBm@`@OLMJSN"
            }

        internal val shapeE1 =
            objects.shape {
                id = "canonical-8000018"
                polyline =
                    "awyaG|~`qLZSLKNMl@a@jCeBpCkBhDaCrA}@NKzAgA^Uz@k@bAs@f@_@f@]TOf@]d@Yb@[f@]`@YRM\\W~@k@bAo@n@c@j@c@d@[JG??l@a@v@i@~@o@^Wx@i@l@c@v@g@b@[l@a@bAq@r@i@r@e@fBmALK`@Yh@]x@i@POJGf@]d@[f@]`@Wp@e@z@k@bAs@z@m@TM??DEp@a@p@g@TQr@u@z@}@l@m@r@_AJQZi@n@eAl@eAZm@`@aANe@xAwDhCsHd@sA`A_Dj@wAX}@j@yAz@yBDO??h@yAv@}B`B{En@eBbAwBdAqBj@w@r@_ApAwAt@q@j@q@jAqAhBoAhA{@rDgDzH}HzCyC??zByBd@o@`AsAr@{@vKeK~@_AbAeAz@_A^g@LYN_@H[Z{@d@aA\\i@V]XS~@_@PE`@Gd@SXSx@w@r@w@n@_BlEkLTgAVkAPk@Zi@T[NOVQ\\Y??@?n@a@tAkBr@oBN_@rBgFv@kBpA_DjBoEdBmEn@{A@C??z@iCvAgDDMd@kAPe@Ti@To@Rq@Ru@TuAJcAJs@^aC\\_ABO@OCc@Ea@Mm@g@}AcB_FK[CWCi@?SBOFQtCsCNO??lIkHxAcBLQh@c@LIZQ??NGREZC`@I`GSf@AT@TNtE|Dp@n@PPRN??XVZTdElCt@p@tB~Bn@t@`@b@VPTXp@z@??T\\z@dAZ^^b@VZh@b@n@\\\\BXNp@TfBv@RH`@PPFNPh@R`@T\\LB@??tAZHJDJBLB^Y`LJbBRbCd@bDvArI??F^bJng@??Lr@^`CXbBX`AHRPFlBg@NBhHhAv@Z^XV\\xEzG^h@??RXxMfQ??l@v@`@f@~C|DfAzAVd@d@`ANd@Pl@\\jAXbAr@xB??Ph@f@~AzAbFHVp@bCp@|B^nAT`AfAxDX`A~A`FXhA??d@dBbB~F|B|Hl@|Bj@dB??JZNh@\\dAj@lBLj@l@lCRv@|AtFDNj@nBPh@DL??Rr@Nh@`@dAZr@??HPNX\\|@Nh@Lp@Dd@?Z?pA@tCB~@?v@@v@?v@@l@D^??@L@FFXH`@Xt@N^L\\n@nAZh@T`@P\\PZPXDDD@ZGTI??tDcA~Bq@??VG`Bo@fAa@d@Uf@Bd@CT?b@MBa@CYG]"
            }

        internal val stopMapResponse =
            StopMapResponse(
                listOf(
                    RouteWithSegmentedShapes(
                        routeB.id,
                        listOf(
                            SegmentedRouteShape(
                                rpB0.id,
                                rpB0.routeId,
                                rpB0.directionId,
                                listOf(
                                    RouteSegment(
                                        "segmentB0",
                                        rpB0.id,
                                        rpB0.routeId,
                                        listOf(
                                            "place-gover",
                                            "place-pktrm",
                                            stopArlington.id,
                                            "place-lake",
                                        ),
                                        emptyMap(),
                                    )
                                ),
                                shapeB0,
                            ),
                            SegmentedRouteShape(
                                rpB1.id,
                                rpB1.routeId,
                                rpB1.directionId,
                                listOf(
                                    RouteSegment(
                                        "segmentB1",
                                        rpB1.id,
                                        rpB1.routeId,
                                        listOf(
                                            "place-lake",
                                            stopArlington.id,
                                            "place-pktrm",
                                            "place-gover",
                                        ),
                                        emptyMap(),
                                    )
                                ),
                                shapeB1,
                            ),
                        ),
                    ),
                    RouteWithSegmentedShapes(
                        routeC.id,
                        listOf(
                            SegmentedRouteShape(
                                rpC0.id,
                                rpC0.routeId,
                                rpC0.directionId,
                                listOf(
                                    RouteSegment(
                                        "segmentC0",
                                        rpC0.id,
                                        rpC0.routeId,
                                        listOf(
                                            "place-gover",
                                            "place-pktrm",
                                            stopArlington.id,
                                            "place-clmnl",
                                        ),
                                        emptyMap(),
                                    )
                                ),
                                shapeC0,
                            ),
                            SegmentedRouteShape(
                                rpC1.id,
                                rpC1.routeId,
                                rpC1.directionId,
                                listOf(
                                    RouteSegment(
                                        "segmentC1",
                                        rpC1.id,
                                        rpC1.routeId,
                                        listOf(
                                            "place-clmnl",
                                            stopArlington.id,
                                            "place-pktrm",
                                            "place-gover",
                                        ),
                                        emptyMap(),
                                    )
                                ),
                                shapeC1,
                            ),
                        ),
                    ),
                    RouteWithSegmentedShapes(
                        routeE.id,
                        listOf(
                            SegmentedRouteShape(
                                rpE0.id,
                                rpE0.routeId,
                                rpE0.directionId,
                                listOf(
                                    RouteSegment(
                                        "segmentE0",
                                        rpE0.id,
                                        rpE0.routeId,
                                        listOf(
                                            "place-gover",
                                            "place-pktrm",
                                            stopArlington.id,
                                            "place-hsmnl",
                                        ),
                                        emptyMap(),
                                    )
                                ),
                                shapeE0,
                            ),
                            SegmentedRouteShape(
                                rpE1.id,
                                rpE1.routeId,
                                rpE1.directionId,
                                listOf(
                                    RouteSegment(
                                        "segmentE1",
                                        rpE1.id,
                                        rpE1.routeId,
                                        listOf(
                                            "place-hsmnl",
                                            stopArlington.id,
                                            "place-pktrm",
                                            "place-gover",
                                        ),
                                        emptyMap(),
                                    )
                                ),
                                shapeE1,
                            ),
                        ),
                    ),
                ),
                emptyMap(),
            )
    }
}
