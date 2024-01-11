import Mapbox
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
        let stopsData: Dictionary<String, CLLocationCoordinate2D> = [
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
    }
}
