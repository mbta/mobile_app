//
//  HomeMap.swift
//  iosApp
//
//  Created by Brady, Kayla on 1/24/24.
//  Copyright © 2024 orgName. All rights reserved.
//


import SwiftUI
import Polyline
@_spi(Experimental) import MapboxMaps


struct HomeMapView: View {
    @ObservedObject var mapVM: HomeMapViewModel
    @State private var viewport: Viewport = .camera(center: CLLocationCoordinate2D(latitude:42.355, longitude: -71.06555), zoom: 12, bearing: 0, pitch: 0)
    var shouldShowStops = false

    @State var rasterTiles: Bool = false






    var body: some View {
        Button("Toggle Tiles") {
            rasterTiles.toggle()
        }


        Map(viewport: $viewport) {



            PolylineAnnotationGroup(mapVM.routes) { route in
                PolylineAnnotation(id: route.id, lineCoordinates: route.coordinates).lineColor(StyleColor(route.color ?? .black)).lineWidth(3)

            }

            if (mapVM.zoom > 14) {

                PointAnnotationGroup(Array(stopsData.values)) { stop in
                    PointAnnotation(coordinate:stop.coordinate).image(named: "t-logo")

                }


            }

            ForEvery(mapVM.vehicles) { vehicle in
                MapViewAnnotation(layerId: "vehicles", featureId: vehicle.id) {
                    Text("HELLO \(vehicle.id)!")
                        .background(.white)
                        .font(.callout)
                        .offset(y:-25)


                }.visible(mapVM.selectedVehicle == vehicle.id).selected(mapVM.selectedVehicle == vehicle.id)

            }



            PointAnnotationGroup(mapVM.vehicles) { vehicle in
                PointAnnotation(id: vehicle.id, coordinate: vehicle.coordinate).image(named: "bus-icon")
                    .onTapGesture {
                        print("TAPPED")
                        mapVM.selectedVehicle = vehicle.id
                        viewport = .camera(center: vehicle.coordinate, zoom: 15)

                }
            }.layerId("vehicles")










        }.mapStyle(rasterTiles ?  MapStyle(json: "{\"version\": 8, \"name\": \"dotcom\",  \"sources\": {\"dotcom\": {\"type\": \"raster\", \"tiles\": [\"https://cdn.mbta.com/osm_tiles/{z}/{x}/{y}.png\"]}}}"): .light)
        // This is continuous - not like .onCameraChangeFinished. Debouncing recommended in docs
        // https://docs.mapbox.com/ios/maps/api/11.1.0/documentation/mapboxmaps/swiftui-user-guide/#Using-Viewport-to-manage-camera
            .onCameraChanged { cameraChanged in
                mapVM.zoom = cameraChanged.cameraState.zoom

            }


    }
}

final class HomeMapViewModel: NSObject, ObservableObject {

    @Published var vehicles: [VehicleAnnotation] = []
    @Published var routes: [CustomPolyline] = Array(routeFeatures.values)
    @Published var zoom: CGFloat = 12
    @Published var selectedVehicle: String? = nil
     var shouldShowStops = false
    var vehiclesTimer: Timer? = nil


    override init() {
        super.init()
        vehicles = [VehicleAnnotation(id: "1", coordinate: .init(latitude: 42.347348, longitude: -71.068556)), VehicleAnnotation(id: "2", coordinate: .init(latitude: 42.358863, longitude: -71.057481))];
        vehiclesTimer = Timer.scheduledTimer(withTimeInterval: 5, repeats: true) { _ in

            self.vehicles = self.vehicles.map { v in
                // keep one vehicle that doesn't move
                if (v.id == "1") {
                    return VehicleAnnotation(id: v.id, coordinate: .init(latitude: v.coordinate.latitude, longitude: v.coordinate.longitude))
                }
                else {
                    return VehicleAnnotation(id: v.id, coordinate: .init(latitude: v.coordinate.latitude + 0.0002, longitude: v.coordinate.longitude))
                }
            }

       }
    }
}


class VehicleAnnotation: NSObject, Identifiable {

    let id: String
    let title: String?
    dynamic var coordinate: CLLocationCoordinate2D
    init(id: String, coordinate: CLLocationCoordinate2D) {
        self.id = id
        self.title = id
        self.coordinate = coordinate
    }
}


private let routeFeatures = [
    "Orange-3-0": routeFeature(encodedPolyline: """
an_bG|_xpLpBPrCZXBTBP@P@~Dd@dANlATjBd@tDhALDNDhA\\pIbClGjBz@ZhA\\xA`@XJpBl@??ZHpBn@HBfHvBfF~AhDbApA\\bAVND`Er@nALvCVfBLfFDb@?dBAjCEfB?pFDrHF~HFfB?vAC~FFpQJpB?|C@P?`B?V?X?h@B??l@DfEFL@f@BpCDxDFfABfA
?lCBdB@fABnC@|C@tO@`ECfCI??|@ElESdCGdCAzA@pEEdBCnBItAAdBA^AT@nAAd@@`A?VAbC?fABzAD??nBDR?Z@v@@`DKtFCn@AZC`@Gr@Q\\MHEb@MzA{@zJeIlBaBhBoBzA}BlAoBrB_EVw@r@oBvAiE??DId@cBxCkGpGmK@Cda@o_@PO??tHkGbCuCh@e@
b@Y????`@Sr@IlMg@zGB??T?vC?N@VN`ElDvAvBbBrBtA~A????v@t@hAtAhDbDb@f@t@p@VJj@Nh@@n@Bz@F??RBdD`@|B^XJRLz@`AxBvB|@N??xATNP`HrRJd@BZAf@OzCCbAApFCvG@hC?~F@t@@bB@jAFlAJ~AJf@???@Jb@DLRb@T^v@dA|AxBbB~BnHlJ^
f@xAlBJHn@x@rBbClDnEt@fA??Zd@b@n@b@l@nDxEjN`RfAtApCrDb@h@RV??tCxDRTjLzNNRfEnFNN~AhBn@n@NJ`@\\????|@t@dE~CnA|@bAn@n@X`@Vd@R|@^\\Rf@Tp@Xn@R|@T~@ZpA`@l@NZHtEpAHBf@LbHrCvAr@??LFRLf@\\PJvA|@lA~@|AhAvCbC
fDpCnA|@hCbBv@b@vAt@lAj@VL??JDd@TtBz@hA^RFvD`AdAZhBl@pA\\??~CfAzAh@vDfBxC`BXNb@T??NJdAj@~CdB|@h@h@Zv@h@fDhBHDnFvCdAn@vC`B~BrAvA~@|CjBrA~@hBhALJl@d@NHNJnDvBhAr@
""".replacingOccurrences(of: "\n", with: ""), color: UIColor.orange),
    "Orange-3-1": routeFeature(encodedPolyline: """
qzdaGbn`qLiAs@oDwBOKOIm@e@MKiBiAsA_A}CkBwA_A_CsAwCaBeAo@oFwCIEgDiBw@i@i@[}@i@_DeBeAk@QK??a@UYOyCaBwDgB{Ai@_DgA??qA]iBm@eA[wDaASGiA_@uB{@e@UKE??WMmAk@wAu@w@c@iCcBoA}@gDqCwCcC}AiAmA_AwA}@QKg@]SMMG??w
As@cHsCg@MICuEqA[Im@OqAa@_A[}@Uo@Sq@Yg@U]S}@_@e@Sa@Wo@YcAo@oA}@eE_D}@u@????a@]OKo@o@_BiBOOgEoFOSkL{NSUuCyD??SWc@i@qCsDgAuAkNaRoDyEc@m@c@o@]g@??s@eAmDoEsBcCo@y@KIyAmB_@g@oHmJcB_C}AyBw@eAU_@Sc@EMKc@?
A??Kg@K_BGmAAkAAcBAu@?_GAiCBwG@qFBcAN{C@g@C[Ke@aHsROQyAU??}@OyBwB{@aASMYK}B_@eDa@UC??y@Go@Ci@Ak@OWKu@q@c@g@iDcDiAuAw@u@AA??sA}AcBsBwAwBaEmDWOOAwC?U???{GCmMf@s@Ha@R??c@Xi@d@cCtCuHlG??QLea@n_@ABqGlKy
CjGe@bBCD??yAlEs@nBWv@sB~DmAnB{A|BiBnBmB`B{JdI{Az@c@LID]Ls@Pa@F[Bo@@uFBaDJw@A[AS?qBE??yAEgACcC?W@aA?e@AoA@UA_@@eB@uA@oBHeBBqED{AAeC@eCFmER}@D??gCHaEBuOA}CAoCAgACeBAmCCgA?gACyDGqCEg@CMAgEGo@E??g@CY?
W?aB?Q?}CAqB?qQK_GGwABgB?_IGsHGqFEgB?kCDeB@c@?gFEgBMwCWoAMaEs@OEcAWqA]iDcAgF_BgHwBICqBo@[I??qBm@YKyAa@iA]{@[mGkBqIcCiA]OEMEuDiAkBe@mAUeAO_Ee@QAQAUCYCsC[qBQ
""".replacingOccurrences(of: "\n", with: ""), color: UIColor.orange),
    "Red-1-0": routeFeature(encodedPolyline: """
}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\t
AP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrC
yDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@zDuF\\c@n@s@VObAw@^Sl@Yj@U\\O|@WdAU
xAQRCt@E??xAGrBQZAhAGlAEv@Et@E~@AdAAbCGpCA|BEjCMr@?nBDvANlARdBb@nDbA~@XnBp@\\JRH??|Al@`AZbA^jA^lA\\h@P|@TxAZ|@J~@LN?fBXxHhApDt@b@JXFtAVhALx@FbADtAC`B?z@BHBH@|@f@RN^^T\\h@hANb@HZH`@H^LpADlA@dD@jD@x@
@b@Bp@HdAFd@Ll@F^??n@rDBRl@vD^pATp@Rb@b@z@\\l@`@j@p@t@j@h@n@h@n@`@hAh@n@\\t@PzANpAApBGtE}@xBa@??xB_@nOmB`OgBb@IrC[p@MbEmARCV@d@LH?tDyAXM
""".replacingOccurrences(of: "\n", with: ""), color: .red),
    "Red-1-1": routeFeature(encodedPolyline: """
qsaaGfrvpLYLuDxAI?e@MWASBcElAq@LsCZc@HaOfBoOlByB^??yB`@uE|@qBFqA@{AOu@Qo@]iAi@o@a@o@i@k@i@q@u@a@k@]m@c@{@Sc@Uq@_@qAm@wDCSo@sD??G_@Mm@Ge@IeACq@Ac@Ay@AkDAeDEmAMqAI_@Ia@I[Oc@i@iAU]_@_@SO}@g@IAIC{@CaB?
uABcAEy@GiAMuAWYGc@KqDu@yHiAgBYO?_AM}@KyA[}@Ui@QmA]kA_@cA_@aA[}Am@??SI]KoBq@_AYoDcAeBc@mASwAOoBEs@?kCL}BDqC@cCFeA@_A@u@Dw@DmADiAF[@sBPyAF??u@DSByAPeAT}@V]Nk@Tm@X_@RcAv@WNo@r@]b@{DtF[h@}A|Bq@hAUZ}B|
Cm@d@m@\\{@XY@MC_@QqBq@{F_C??g@ScnAl@??qCBie@i[y@c@m@QYIu@Gc@?i@DiARm@TQNa@f@uDbNIX??k@hBq@jBs@vBk@bA_JrQUj@??aDfI??[v@Wn@w@pBa@`Aa@fAwBtFc@~@k@hAsCxDwJ~N]|@Mb@K^CFKn@Gb@C\\ATCjAEt@GhBEbA??AXGlAAPM
`DYjIM|CO|GM|DG|AGxCa@`MUbHs@fJOfD??ATIrB_Ap^s@jRWlJYdHOhGIpBKjAk@bBe@bAaDnGkBzD??m@pAwMpVgArCsBdGkEbPc@nAYb@a@d@eChDwG|HMPEFwCzNK^KVMJIB_@?iAO??kAOmB]gBGm@A[HSL[^a@|@e@fAo@r@iBh@aB^}@BkAGaNq@am@iD
Q???sMUyQ~@uCJsBDo@Pc@Xm@l@m@bAaAvBu@pB]hAOx@UdAKr@??i@dD_@lDKlBOpCQjESvHKtEYbJg@rUGjCRnDFv@NbAPjAHRHLLFt@Nt@Vt@j@f@|@Vt@@DR~B@b@HpAFxN
""".replacingOccurrences(of: "\n", with: ""), color: .red),
    "Red-3-0": routeFeature(encodedPolyline: """
}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\t
AP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrC
yDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@jBeDnAiBz@iAf@k@l@g@dAs@fAe@|@WpCe@
l@GTCRE\\G??~@O`@ELA|AGf@A\\CjCGrEKz@AdEAxHY|BD~@JjB^fF~AdDbA|InCxCv@zD|@rWfEXDpB`@tANvAHx@AjBIx@M~@S~@a@fAi@HEnA{@fA{@|HuI|DwEbDqDpLkNhCyClEiFhLaN`@c@f@o@RURUbDsDbAiA`AgAv@_AHKHI~E}FdBoBfAgAfD{DxD
oE~DcF|BkClAwALODEJOJK|@gATWvAoA`Au@fAs@hAk@n@QpAa@vDeAhA[x@Yh@Wv@a@b@YfAaAjCgCz@aAtByBz@{@??|FaGtCaDbL{LhI{IzHgJdAuAjC{CVYvAwA??JIl@a@NMNM\\[|AuArF_GlPyQrD_ErAwAd@e@nE{ErDuD\\a@nE_FZYPSRUvL{Mv@}@Z
[JILKv@m@z@i@fCkAlBmAl@[t@[??h@WxBeAp@]dAi@p@YXIPEXKDALENEbAQl@Gz@ChADtAL~ARnCZbGx@xB`@TDL@PBzAVjIvA^FVDVB|@NjHlAlPnCnCd@vBXhBNv@JtAPL@|BXrAN??`@FRBj@Bp@FbADz@?dAIp@I|@Mx@Q`AWhAYlBs@pDaBzAs@nBgAZQJ
GJGhAs@RKVMNKTMf@YdHcEzBmApAw@`GmDLI@AHGlEwClAi@hA_@v@Up@ObB]z@Kr@Ir@EZCpA?dCRf@DpAHvANrE`@bDTr@DfMdA`CJvBRn@DnCLnBPfAFV@
""".replacingOccurrences(of: "\n", with: ""), color: .red),
    "Red-3-1": routeFeature(encodedPolyline: """
iyr`GzmjpLWAgAGoBQoCMo@EwBSaCKgMeAs@EcDUsEa@wAOqAIg@EeCSqA?[Bs@Ds@H{@JcB\\q@Nw@TiA^mAh@mEvCIFA@MHaGlDqAv@{BlAeHbEg@XULOJWLSJiAr@KFKF[PoBfA{Ar@qD`BmBr@iAXaAVy@P}@Lq@HeAH{@?cAEq@Gk@CSCc@G??qAO}BYMAuA
Qw@KiBOwBYoCe@mPoCkHmA}@OWCWE_@GkIwA{AWQCMAUEyBa@cGy@oC[_BSuAMiAE{@Bm@FcAPODMDE@YJQDYHq@XeAh@q@\\yBdAi@T??u@\\m@ZmBlAgCjA{@h@w@l@MJKH[Zw@|@wLzMSTQR[XoE~E]`@sDtDoEzEe@d@sAvAsD~DmPxQsF~F}AtA]ZOLOLm@`
@IF??yAxAWXkCzCeAtA{HfJiIzIcLzLuC`D}F`G??{@z@uBxB{@`AkCfCgA`Ac@Xw@`@i@Vy@XiAZwDdAqA`@o@PiAj@gAr@aAt@wAnAUV}@fAKJKNEDMNmAvA}BjC_EbFyDnEgDzDgAfAeBnB_F|FIHIJw@~@aAfAcAhAcDrDSTSTg@n@a@b@iL`NmEhFiCxCqLj
NcDpD}DvE}HtIgAz@oAz@IDgAh@_A`@_ARy@LkBHy@@wAIuAOqBa@YEsWgE{D}@yCw@}IoCeDcAgF_BkB_@_AK}BEyHXeE@{@@sEJkCF]Bg@@}AFM@a@D_AN??]FSDUBm@FqCd@}@VgAd@eAr@m@f@g@j@{@hAoAhBkBdD[h@}A|Bq@hAUZ}B|Cm@d@m@\\{@XY@M
C_@QqBq@{F_C??g@ScnAl@??qCBie@i[y@c@m@QYIu@Gc@?i@DiARm@TQNa@f@uDbNIX??k@hBq@jBs@vBk@bA_JrQUj@??aDfI??[v@Wn@w@pBa@`Aa@fAwBtFc@~@k@hAsCxDwJ~N]|@Mb@K^CFKn@Gb@C\\ATCjAEt@GhBEbA??AXGlAAPM`DYjIM|CO|GM|DG
|AGxCa@`MUbHs@fJOfD??ATIrB_Ap^s@jRWlJYdHOhGIpBKjAk@bBe@bAaDnGkBzD??m@pAwMpVgArCsBdGkEbPc@nAYb@a@d@eChDwG|HMPEFwCzNK^KVMJIB_@?iAO??kAOmB]gBGm@A[HSL[^a@|@e@fAo@r@iBh@aB^}@BkAGaNq@am@iDQ???sMUyQ~@uCJs
BDo@Pc@Xm@l@m@bAaAvBu@pB]hAOx@UdAKr@??i@dD_@lDKlBOpCQjESvHKtEYbJg@rUGjCRnDFv@NbAPjAHRHLLFt@Nt@Vt@j@f@|@Vt@@DR~B@b@HpAFxN
""".replacingOccurrences(of: "\n", with: ""), color: .red),
    "749-_-0": routeFeature(encodedPolyline: """
sooaGhjvpLaAzBXXb@`@d@\\NJz@`@|CnArBp@\\J??b@LjCf@`C^zBd@VoGHgBx@\\f@XjBv@??vAj@hAZfCd@rB\\~ATpCd@\\D??F@v@Np@N\\NzCvAr@^tC`Bj@XDB??h@ZRRd@l@b@l@h@p@hCjDT^Xf@LXPh@J\\|@jDNd@Tn@hA~Bp@jAR\\??lAtBvAfC
rC~ET\\??bAdBbBhCzAdC\\d@??bA~AvAzBJNV^??bAxA~DjFRT??~@nAFF~@|ANTd@p@l@r@XXTRlBnALHXJ??l@Nt@Z`A|@d@h@jAvA|@dAn@t@\\h@NZXjA^`AX`@TR\\XJqB
""".replacingOccurrences(of: "\n", with: ""), color: .gray),
    "749-_-1": routeFeature(encodedPolyline: """
qpjaGfqzpLJyB{@Mi@Ge@I]Gc@Oo@u@}@eAkAwAe@i@aA}@a@_@MKYOc@UOG[S??uAy@o@i@[a@s@cA}AaBGGsAeBa@k@??}C_EkAcB??OUKOwA{BaBeCQ]??iAgBcBiCmAqB??KQsC_FwAgCaBsCq@kAiA_CUo@Oe@K_@??q@kCK]Qi@MYYg@U_@iCkDi@q@c@m@
e@m@SSo@_@k@Yu@c@??_B}@s@_@cCkA??WK]Oq@Ow@Oe@GqCe@_BUsB]gCe@iA[oBy@??sAi@g@Yy@]g@OiAYSG_AOaBUMC??UC{AAKAc@Ca@KYMo@_@GG}@y@a@a@A???uA}AiBlE
""".replacingOccurrences(of: "\n", with: ""), color: .gray)
]

private let stopsData  = [
           "place-chmnl": pointFeature(latitude: 42.361166, longitude: -71.070628),
           "place-armnl": pointFeature(latitude: 42.351902, longitude: -71.070893),
           "place-boyls": pointFeature(latitude: 42.35302, longitude: -71.06459),
           "place-chncl": pointFeature(latitude: 42.352547, longitude: -71.062752),
           "place-bomnl": pointFeature(latitude: 42.361365, longitude: -71.062037),
           "place-gover": pointFeature(latitude: 42.359705, longitude: -71.059215),
           "place-pktrm": pointFeature(latitude: 42.356395, longitude: -71.062424),
           "place-state": pointFeature(latitude: 42.358978, longitude: -71.057598),
           "place-dwnxg": pointFeature(latitude: 42.355518, longitude: -71.060225),
           "place-tumnl": pointFeature(latitude: 42.349662, longitude: -71.063917),
           "177": pointFeature(latitude: 42.351039, longitude: -71.070502),
           "9981": pointFeature(latitude: 42.356954, longitude: -71.065766),
           "9983": pointFeature(latitude: 42.351039, longitude: -71.066798),
           "8281": pointFeature(latitude: 42.349606, longitude: -71.065439),
           "146": pointFeature(latitude: 42.34881, longitude: -71.069533),
           "171": pointFeature(latitude: 42.348569, longitude: -71.071717),
           "36540": pointFeature(latitude: 42.347293, longitude: -71.071004),
           "168": pointFeature(latitude: 42.346361, longitude: -71.069294),
           "9980": pointFeature(latitude: 42.356083, longitude: -71.06926),
           "8279": pointFeature(latitude: 42.353247, longitude: -71.064353),
           "9982": pointFeature(latitude: 42.357656, longitude: -71.06328),
           "10000": pointFeature(latitude: 42.355692, longitude: -71.062911),
           "6550": pointFeature(latitude: 42.352188, longitude: -71.057992),
           "16535": pointFeature(latitude: 42.354243, longitude: -71.058557),
           "16538": pointFeature(latitude: 42.354271, longitude: -71.059508),
           "6567": pointFeature(latitude: 42.352461, longitude: -71.062552),
           "6537": pointFeature(latitude: 42.352288, longitude: -71.062581),
           "4511": pointFeature(latitude: 42.362126, longitude: -71.058975),
           "4510": pointFeature(latitude: 42.360043, longitude: -71.0598),
           "191": pointFeature(latitude: 42.360387, longitude: -71.057129),
           "204": pointFeature(latitude: 42.359546, longitude: -71.057192),
           "190": pointFeature(latitude: 42.358789, longitude: -71.056733),
           "65": pointFeature(latitude: 42.358863, longitude: -71.057481),
           "49704": pointFeature(latitude: 42.358108, longitude: -71.060394),
           "11891": pointFeature(latitude: 42.357495, longitude: -71.056251),
           "6551": pointFeature(latitude: 42.355286, longitude: -71.056411),
           "6535": pointFeature(latitude: 42.355521, longitude: -71.057253),
           "65471": pointFeature(latitude: 42.357732, longitude: -71.057373),
           "6548": pointFeature(latitude: 42.356837, longitude: -71.057414),
           "16539": pointFeature(latitude: 42.356727, longitude: -71.05748),
           "49001": pointFeature(latitude: 42.355385, longitude: -71.062211),
           "16551": pointFeature(latitude: 42.35555, longitude: -71.055682),
           "1242": pointFeature(latitude: 42.348777, longitude: -71.066481),
           "1239": pointFeature(latitude: 42.34818, longitude: -71.067595),
           "147": pointFeature(latitude: 42.347374, longitude: -71.068436),
           "1238": pointFeature(latitude: 42.347348, longitude: -71.068556),
           "1241": pointFeature(latitude: 42.351748, longitude: -71.067101),
           "145": pointFeature(latitude: 42.351105, longitude: -71.070442),
           "144": pointFeature(latitude: 42.351234, longitude: -71.07295),
           "6555": pointFeature(latitude: 42.35068, longitude: -71.059655),
           "6542": pointFeature(latitude: 42.350845, longitude: -71.062868),
           "148": pointFeature(latitude: 42.346575, longitude: -71.064724),
           "49003": pointFeature(latitude: 42.346457, longitude: -71.064695),
           "11481": pointFeature(latitude: 42.346122, longitude: -71.062369),
           "15095": pointFeature(latitude: 42.345582, longitude: -71.064848),
           "6565": pointFeature(latitude: 42.34997, longitude: -71.063413),
           "49002": pointFeature(latitude: 42.349908, longitude: -71.063684),
       ]


struct CustomPolyline: Identifiable {
    var id: String
    var coordinates: [LocationCoordinate2D]
    var color: UIColor?

}


private func routeFeature(encodedPolyline: String, color: UIColor) -> CustomPolyline {
    let routePolyline = Polyline(encodedPolyline: encodedPolyline)
    let mkPolyline = CustomPolyline(id: encodedPolyline, coordinates:
                                        (routePolyline.coordinates ?? []), color: color)
    return mkPolyline
}
struct StopAnnotation: Identifiable{
    var id: String

     var coordinate: CLLocationCoordinate2D
}

private func pointFeature(latitude: CLLocationDegrees, longitude: CLLocationDegrees) -> StopAnnotation {
    return StopAnnotation.init(id: "\(latitude) \(longitude)", coordinate: .init(latitude: latitude, longitude: longitude))
}
