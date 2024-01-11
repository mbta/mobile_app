import Mapbox
import Polyline
import SwiftUI
import shared

struct ContentView: View {
    var body: some View {
        MapView().edgesIgnoringSafeArea(.all)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

struct MapView: UIViewRepresentable {
    let delegate = MapViewDelegate()

    func makeUIView(context: Context) -> some UIView {
        // Create the map view
        let mapView = MGLMapView(frame: .zero)

        mapView.logoView.isHidden = true

        mapView.setCenter(
            CLLocationCoordinate2D(
                latitude: 42.355, longitude: -71.06555), animated: false)

        mapView.setZoomLevel(14, animated: false)

        mapView.delegate = delegate

        return mapView
    }

    func updateUIView(_ uiView: UIViewType, context: Context) {
        //
    }
}

private let routeFeatures = [
    "Orange-3-0": routeFeature(polyline: """
an_bG|_xpLpBPrCZXBTBP@P@~Dd@dANlATjBd@tDhALDNDhA\\pIbClGjBz@ZhA\\xA`@XJpBl@??ZHpBn@HBfHvBfF~AhDbApA\\bAVND`Er@nALvCVfBLfFDb@?dBAjCEfB?pFDrHF~HFfB?vAC~FFpQJpB?|C@P?`B?V?X?h@B??l@DfEFL@f@BpCDxDFfABfA
?lCBdB@fABnC@|C@tO@`ECfCI??|@ElESdCGdCAzA@pEEdBCnBItAAdBA^AT@nAAd@@`A?VAbC?fABzAD??nBDR?Z@v@@`DKtFCn@AZC`@Gr@Q\\MHEb@MzA{@zJeIlBaBhBoBzA}BlAoBrB_EVw@r@oBvAiE??DId@cBxCkGpGmK@Cda@o_@PO??tHkGbCuCh@e@
b@Y????`@Sr@IlMg@zGB??T?vC?N@VN`ElDvAvBbBrBtA~A????v@t@hAtAhDbDb@f@t@p@VJj@Nh@@n@Bz@F??RBdD`@|B^XJRLz@`AxBvB|@N??xATNP`HrRJd@BZAf@OzCCbAApFCvG@hC?~F@t@@bB@jAFlAJ~AJf@???@Jb@DLRb@T^v@dA|AxBbB~BnHlJ^
f@xAlBJHn@x@rBbClDnEt@fA??Zd@b@n@b@l@nDxEjN`RfAtApCrDb@h@RV??tCxDRTjLzNNRfEnFNN~AhBn@n@NJ`@\\????|@t@dE~CnA|@bAn@n@X`@Vd@R|@^\\Rf@Tp@Xn@R|@T~@ZpA`@l@NZHtEpAHBf@LbHrCvAr@??LFRLf@\\PJvA|@lA~@|AhAvCbC
fDpCnA|@hCbBv@b@vAt@lAj@VL??JDd@TtBz@hA^RFvD`AdAZhBl@pA\\??~CfAzAh@vDfBxC`BXNb@T??NJdAj@~CdB|@h@h@Zv@h@fDhBHDnFvCdAn@vC`B~BrAvA~@|CjBrA~@hBhALJl@d@NHNJnDvBhAr@
""".replacingOccurrences(of: "\n", with: ""), color: UIColor.orange),
    "Orange-3-1": routeFeature(polyline: """
qzdaGbn`qLiAs@oDwBOKOIm@e@MKiBiAsA_A}CkBwA_A_CsAwCaBeAo@oFwCIEgDiBw@i@i@[}@i@_DeBeAk@QK??a@UYOyCaBwDgB{Ai@_DgA??qA]iBm@eA[wDaASGiA_@uB{@e@UKE??WMmAk@wAu@w@c@iCcBoA}@gDqCwCcC}AiAmA_AwA}@QKg@]SMMG??w
As@cHsCg@MICuEqA[Im@OqAa@_A[}@Uo@Sq@Yg@U]S}@_@e@Sa@Wo@YcAo@oA}@eE_D}@u@????a@]OKo@o@_BiBOOgEoFOSkL{NSUuCyD??SWc@i@qCsDgAuAkNaRoDyEc@m@c@o@]g@??s@eAmDoEsBcCo@y@KIyAmB_@g@oHmJcB_C}AyBw@eAU_@Sc@EMKc@?
A??Kg@K_BGmAAkAAcBAu@?_GAiCBwG@qFBcAN{C@g@C[Ke@aHsROQyAU??}@OyBwB{@aASMYK}B_@eDa@UC??y@Go@Ci@Ak@OWKu@q@c@g@iDcDiAuAw@u@AA??sA}AcBsBwAwBaEmDWOOAwC?U???{GCmMf@s@Ha@R??c@Xi@d@cCtCuHlG??QLea@n_@ABqGlKy
CjGe@bBCD??yAlEs@nBWv@sB~DmAnB{A|BiBnBmB`B{JdI{Az@c@LID]Ls@Pa@F[Bo@@uFBaDJw@A[AS?qBE??yAEgACcC?W@aA?e@AoA@UA_@@eB@uA@oBHeBBqED{AAeC@eCFmER}@D??gCHaEBuOA}CAoCAgACeBAmCCgA?gACyDGqCEg@CMAgEGo@E??g@CY?
W?aB?Q?}CAqB?qQK_GGwABgB?_IGsHGqFEgB?kCDeB@c@?gFEgBMwCWoAMaEs@OEcAWqA]iDcAgF_BgHwBICqBo@[I??qBm@YKyAa@iA]{@[mGkBqIcCiA]OEMEuDiAkBe@mAUeAO_Ee@QAQAUCYCsC[qBQ
""".replacingOccurrences(of: "\n", with: ""), color: UIColor.orange),
    "Red-1-0": routeFeature(polyline: """
}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\t
AP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrC
yDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@zDuF\\c@n@s@VObAw@^Sl@Yj@U\\O|@WdAU
xAQRCt@E??xAGrBQZAhAGlAEv@Et@E~@AdAAbCGpCA|BEjCMr@?nBDvANlARdBb@nDbA~@XnBp@\\JRH??|Al@`AZbA^jA^lA\\h@P|@TxAZ|@J~@LN?fBXxHhApDt@b@JXFtAVhALx@FbADtAC`B?z@BHBH@|@f@RN^^T\\h@hANb@HZH`@H^LpADlA@dD@jD@x@
@b@Bp@HdAFd@Ll@F^??n@rDBRl@vD^pATp@Rb@b@z@\\l@`@j@p@t@j@h@n@h@n@`@hAh@n@\\t@PzANpAApBGtE}@xBa@??xB_@nOmB`OgBb@IrC[p@MbEmARCV@d@LH?tDyAXM
""".replacingOccurrences(of: "\n", with: ""), color: .red),
    "Red-1-1": routeFeature(polyline: """
qsaaGfrvpLYLuDxAI?e@MWASBcElAq@LsCZc@HaOfBoOlByB^??yB`@uE|@qBFqA@{AOu@Qo@]iAi@o@a@o@i@k@i@q@u@a@k@]m@c@{@Sc@Uq@_@qAm@wDCSo@sD??G_@Mm@Ge@IeACq@Ac@Ay@AkDAeDEmAMqAI_@Ia@I[Oc@i@iAU]_@_@SO}@g@IAIC{@CaB?
uABcAEy@GiAMuAWYGc@KqDu@yHiAgBYO?_AM}@KyA[}@Ui@QmA]kA_@cA_@aA[}Am@??SI]KoBq@_AYoDcAeBc@mASwAOoBEs@?kCL}BDqC@cCFeA@_A@u@Dw@DmADiAF[@sBPyAF??u@DSByAPeAT}@V]Nk@Tm@X_@RcAv@WNo@r@]b@{DtF[h@}A|Bq@hAUZ}B|
Cm@d@m@\\{@XY@MC_@QqBq@{F_C??g@ScnAl@??qCBie@i[y@c@m@QYIu@Gc@?i@DiARm@TQNa@f@uDbNIX??k@hBq@jBs@vBk@bA_JrQUj@??aDfI??[v@Wn@w@pBa@`Aa@fAwBtFc@~@k@hAsCxDwJ~N]|@Mb@K^CFKn@Gb@C\\ATCjAEt@GhBEbA??AXGlAAPM
`DYjIM|CO|GM|DG|AGxCa@`MUbHs@fJOfD??ATIrB_Ap^s@jRWlJYdHOhGIpBKjAk@bBe@bAaDnGkBzD??m@pAwMpVgArCsBdGkEbPc@nAYb@a@d@eChDwG|HMPEFwCzNK^KVMJIB_@?iAO??kAOmB]gBGm@A[HSL[^a@|@e@fAo@r@iBh@aB^}@BkAGaNq@am@iD
Q???sMUyQ~@uCJsBDo@Pc@Xm@l@m@bAaAvBu@pB]hAOx@UdAKr@??i@dD_@lDKlBOpCQjESvHKtEYbJg@rUGjCRnDFv@NbAPjAHRHLLFt@Nt@Vt@j@f@|@Vt@@DR~B@b@HpAFxN
""".replacingOccurrences(of: "\n", with: ""), color: .red),
    "Red-3-0": routeFeature(polyline: """
}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\t
AP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrC
yDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@jBeDnAiBz@iAf@k@l@g@dAs@fAe@|@WpCe@
l@GTCRE\\G??~@O`@ELA|AGf@A\\CjCGrEKz@AdEAxHY|BD~@JjB^fF~AdDbA|InCxCv@zD|@rWfEXDpB`@tANvAHx@AjBIx@M~@S~@a@fAi@HEnA{@fA{@|HuI|DwEbDqDpLkNhCyClEiFhLaN`@c@f@o@RURUbDsDbAiA`AgAv@_AHKHI~E}FdBoBfAgAfD{DxD
oE~DcF|BkClAwALODEJOJK|@gATWvAoA`Au@fAs@hAk@n@QpAa@vDeAhA[x@Yh@Wv@a@b@YfAaAjCgCz@aAtByBz@{@??|FaGtCaDbL{LhI{IzHgJdAuAjC{CVYvAwA??JIl@a@NMNM\\[|AuArF_GlPyQrD_ErAwAd@e@nE{ErDuD\\a@nE_FZYPSRUvL{Mv@}@Z
[JILKv@m@z@i@fCkAlBmAl@[t@[??h@WxBeAp@]dAi@p@YXIPEXKDALENEbAQl@Gz@ChADtAL~ARnCZbGx@xB`@TDL@PBzAVjIvA^FVDVB|@NjHlAlPnCnCd@vBXhBNv@JtAPL@|BXrAN??`@FRBj@Bp@FbADz@?dAIp@I|@Mx@Q`AWhAYlBs@pDaBzAs@nBgAZQJ
GJGhAs@RKVMNKTMf@YdHcEzBmApAw@`GmDLI@AHGlEwClAi@hA_@v@Up@ObB]z@Kr@Ir@EZCpA?dCRf@DpAHvANrE`@bDTr@DfMdA`CJvBRn@DnCLnBPfAFV@
""".replacingOccurrences(of: "\n", with: ""), color: .red),
    "Red-3-1": routeFeature(polyline: """
iyr`GzmjpLWAgAGoBQoCMo@EwBSaCKgMeAs@EcDUsEa@wAOqAIg@EeCSqA?[Bs@Ds@H{@JcB\\q@Nw@TiA^mAh@mEvCIFA@MHaGlDqAv@{BlAeHbEg@XULOJWLSJiAr@KFKF[PoBfA{Ar@qD`BmBr@iAXaAVy@P}@Lq@HeAH{@?cAEq@Gk@CSCc@G??qAO}BYMAuA
Qw@KiBOwBYoCe@mPoCkHmA}@OWCWE_@GkIwA{AWQCMAUEyBa@cGy@oC[_BSuAMiAE{@Bm@FcAPODMDE@YJQDYHq@XeAh@q@\\yBdAi@T??u@\\m@ZmBlAgCjA{@h@w@l@MJKH[Zw@|@wLzMSTQR[XoE~E]`@sDtDoEzEe@d@sAvAsD~DmPxQsF~F}AtA]ZOLOLm@`
@IF??yAxAWXkCzCeAtA{HfJiIzIcLzLuC`D}F`G??{@z@uBxB{@`AkCfCgA`Ac@Xw@`@i@Vy@XiAZwDdAqA`@o@PiAj@gAr@aAt@wAnAUV}@fAKJKNEDMNmAvA}BjC_EbFyDnEgDzDgAfAeBnB_F|FIHIJw@~@aAfAcAhAcDrDSTSTg@n@a@b@iL`NmEhFiCxCqLj
NcDpD}DvE}HtIgAz@oAz@IDgAh@_A`@_ARy@LkBHy@@wAIuAOqBa@YEsWgE{D}@yCw@}IoCeDcAgF_BkB_@_AK}BEyHXeE@{@@sEJkCF]Bg@@}AFM@a@D_AN??]FSDUBm@FqCd@}@VgAd@eAr@m@f@g@j@{@hAoAhBkBdD[h@}A|Bq@hAUZ}B|Cm@d@m@\\{@XY@M
C_@QqBq@{F_C??g@ScnAl@??qCBie@i[y@c@m@QYIu@Gc@?i@DiARm@TQNa@f@uDbNIX??k@hBq@jBs@vBk@bA_JrQUj@??aDfI??[v@Wn@w@pBa@`Aa@fAwBtFc@~@k@hAsCxDwJ~N]|@Mb@K^CFKn@Gb@C\\ATCjAEt@GhBEbA??AXGlAAPM`DYjIM|CO|GM|DG
|AGxCa@`MUbHs@fJOfD??ATIrB_Ap^s@jRWlJYdHOhGIpBKjAk@bBe@bAaDnGkBzD??m@pAwMpVgArCsBdGkEbPc@nAYb@a@d@eChDwG|HMPEFwCzNK^KVMJIB_@?iAO??kAOmB]gBGm@A[HSL[^a@|@e@fAo@r@iBh@aB^}@BkAGaNq@am@iDQ???sMUyQ~@uCJs
BDo@Pc@Xm@l@m@bAaAvBu@pB]hAOx@UdAKr@??i@dD_@lDKlBOpCQjESvHKtEYbJg@rUGjCRnDFv@NbAPjAHRHLLFt@Nt@Vt@j@f@|@Vt@@DR~B@b@HpAFxN
""".replacingOccurrences(of: "\n", with: ""), color: .red),
    "749-_-0": routeFeature(polyline: """
sooaGhjvpLaAzBXXb@`@d@\\NJz@`@|CnArBp@\\J??b@LjCf@`C^zBd@VoGHgBx@\\f@XjBv@??vAj@hAZfCd@rB\\~ATpCd@\\D??F@v@Np@N\\NzCvAr@^tC`Bj@XDB??h@ZRRd@l@b@l@h@p@hCjDT^Xf@LXPh@J\\|@jDNd@Tn@hA~Bp@jAR\\??lAtBvAfC
rC~ET\\??bAdBbBhCzAdC\\d@??bA~AvAzBJNV^??bAxA~DjFRT??~@nAFF~@|ANTd@p@l@r@XXTRlBnALHXJ??l@Nt@Z`A|@d@h@jAvA|@dAn@t@\\h@NZXjA^`AX`@TR\\XJqB
""".replacingOccurrences(of: "\n", with: ""), color: .gray),
    "749-_-1": routeFeature(polyline: """
qpjaGfqzpLJyB{@Mi@Ge@I]Gc@Oo@u@}@eAkAwAe@i@aA}@a@_@MKYOc@UOG[S??uAy@o@i@[a@s@cA}AaBGGsAeBa@k@??}C_EkAcB??OUKOwA{BaBeCQ]??iAgBcBiCmAqB??KQsC_FwAgCaBsCq@kAiA_CUo@Oe@K_@??q@kCK]Qi@MYYg@U_@iCkDi@q@c@m@
e@m@SSo@_@k@Yu@c@??_B}@s@_@cCkA??WK]Oq@Ow@Oe@GqCe@_BUsB]gCe@iA[oBy@??sAi@g@Yy@]g@OiAYSG_AOaBUMC??UC{AAKAc@Ca@KYMo@_@GG}@y@a@a@A???uA}AiBlE
""".replacingOccurrences(of: "\n", with: ""), color: .gray)
]

class MapViewDelegate: NSObject, MGLMapViewDelegate {
    func mapView(_ mapView: MGLMapView, didFinishLoading style: MGLStyle) {
        // Set the tiles
        let tileSource = MGLRasterTileSource(
            identifier: "dotcom",
            tileURLTemplates: ["https://cdn.mbta.com/osm_tiles/{z}/{x}/{y}.png"],
            options: [
                .attributionInfos: [MGLAttributionInfo(
                    title: .init(string: "© OpenStreetMap contributors"),
                    url: .init(string: "http://osm.org/copyright"))],
                .tileSize: 256])
        let tileLayer = MGLRasterStyleLayer(identifier: "dotcom", source: tileSource)

        style.addSource(tileSource)
        style.layers = [tileLayer]

        // Add some stops
        let stopsData: [String: CLLocationCoordinate2D] = [
            "place-chmnl": .init(latitude: 42.361166, longitude: -71.070628),
            "place-armnl": .init(latitude: 42.351902, longitude: -71.070893),
            "place-boyls": .init(latitude: 42.35302, longitude: -71.06459),
            "place-chncl": .init(latitude: 42.352547, longitude: -71.062752),
            "place-bomnl": .init(latitude: 42.361365, longitude: -71.062037),
            "place-gover": .init(latitude: 42.359705, longitude: -71.059215),
            "place-pktrm": .init(latitude: 42.356395, longitude: -71.062424),
            "place-state": .init(latitude: 42.358978, longitude: -71.057598),
            "place-dwnxg": .init(latitude: 42.355518, longitude: -71.060225),
            "place-tumnl": .init(latitude: 42.349662, longitude: -71.063917),
            "177": .init(latitude: 42.351039, longitude: -71.070502),
            "9981": .init(latitude: 42.356954, longitude: -71.065766),
            "9983": .init(latitude: 42.351039, longitude: -71.066798),
            "8281": .init(latitude: 42.349606, longitude: -71.065439),
            "146": .init(latitude: 42.34881, longitude: -71.069533),
            "171": .init(latitude: 42.348569, longitude: -71.071717),
            "36540": .init(latitude: 42.347293, longitude: -71.071004),
            "168": .init(latitude: 42.346361, longitude: -71.069294),
            "9980": .init(latitude: 42.356083, longitude: -71.06926),
            "8279": .init(latitude: 42.353247, longitude: -71.064353),
            "9982": .init(latitude: 42.357656, longitude: -71.06328),
            "10000": .init(latitude: 42.355692, longitude: -71.062911),
            "6550": .init(latitude: 42.352188, longitude: -71.057992),
            "16535": .init(latitude: 42.354243, longitude: -71.058557),
            "16538": .init(latitude: 42.354271, longitude: -71.059508),
            "6567": .init(latitude: 42.352461, longitude: -71.062552),
            "6537": .init(latitude: 42.352288, longitude: -71.062581),
            "4511": .init(latitude: 42.362126, longitude: -71.058975),
            "4510": .init(latitude: 42.360043, longitude: -71.0598),
            "191": .init(latitude: 42.360387, longitude: -71.057129),
            "204": .init(latitude: 42.359546, longitude: -71.057192),
            "190": .init(latitude: 42.358789, longitude: -71.056733),
            "65": .init(latitude: 42.358863, longitude: -71.057481),
            "49704": .init(latitude: 42.358108, longitude: -71.060394),
            "11891": .init(latitude: 42.357495, longitude: -71.056251),
            "6551": .init(latitude: 42.355286, longitude: -71.056411),
            "6535": .init(latitude: 42.355521, longitude: -71.057253),
            "65471": .init(latitude: 42.357732, longitude: -71.057373),
            "6548": .init(latitude: 42.356837, longitude: -71.057414),
            "16539": .init(latitude: 42.356727, longitude: -71.05748),
            "49001": .init(latitude: 42.355385, longitude: -71.062211),
            "16551": .init(latitude: 42.35555, longitude: -71.055682),
            "1242": .init(latitude: 42.348777, longitude: -71.066481),
            "1239": .init(latitude: 42.34818, longitude: -71.067595),
            "147": .init(latitude: 42.347374, longitude: -71.068436),
            "1238": .init(latitude: 42.347348, longitude: -71.068556),
            "1241": .init(latitude: 42.351748, longitude: -71.067101),
            "145": .init(latitude: 42.351105, longitude: -71.070442),
            "144": .init(latitude: 42.351234, longitude: -71.07295),
            "6555": .init(latitude: 42.35068, longitude: -71.059655),
            "6542": .init(latitude: 42.350845, longitude: -71.062868),
            "148": .init(latitude: 42.346575, longitude: -71.064724),
            "49003": .init(latitude: 42.346457, longitude: -71.064695),
            "11481": .init(latitude: 42.346122, longitude: -71.062369),
            "15095": .init(latitude: 42.345582, longitude: -71.064848),
            "6565": .init(latitude: 42.34997, longitude: -71.063413),
            "49002": .init(latitude: 42.349908, longitude: -71.063684),
        ]
        let stopsSource = MGLShapeSource(identifier: "stops", shapes: stopsData.values.map({loc in
            let result = MGLPointAnnotation()
            result.coordinate = loc
            return result
        }))

        let stopsLayer = MGLSymbolStyleLayer(identifier: "stops", source: stopsSource)
        stopsLayer.iconImageName = NSExpression(forConstantValue: "stop")
        stopsLayer.iconScale = NSExpression(forConstantValue: 0.5)

        if let image = UIImage(named: "house-icon") {
            style.setImage(image, forName: "stop")
        }

        style.addSource(stopsSource)
        style.addLayer(stopsLayer)

        // Add some routes
        let routesSource = MGLShapeSource(identifier: "routes", shapes: Array(routeFeatures.values))

        let routesLayer = MGLLineStyleLayer(identifier: "routes", source: routesSource)
        routesLayer.lineColor = NSExpression(forKeyPath: "color")

        style.addSource(routesSource)
        style.addLayer(routesLayer)
    }
}

private func routeFeature(polyline: String, color: UIColor) -> MGLPolylineFeature {
    let coordinates: [CLLocationCoordinate2D] = decodePolyline(polyline)!
    let result = MGLPolylineFeature(coordinates: coordinates, count: UInt(coordinates.count))
    result.attributes["color"] = color
    return result
}
