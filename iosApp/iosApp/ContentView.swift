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

        mapView.setZoomLevel(13, animated: false)

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
        let source = MGLRasterTileSource(
            identifier: "dotcom",
            tileURLTemplates: ["https://cdn.mbta.com/osm_tiles/{z}/{x}/{y}.png"],
            options: [
                .attributionInfos: [MGLAttributionInfo(
                    title: .init(string: "© OpenStreetMap contributors"),
                    url: .init(string: "http://osm.org/copyright"))],
                .tileSize: 256])
        let layer = MGLRasterStyleLayer(identifier: "dotcom", source: source)

        style.addSource(source)
        style.layers = [layer]
    }
}
