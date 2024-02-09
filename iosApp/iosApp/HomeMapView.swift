//
//  HomeMapView.swift
//  iosApp
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI
@_spi(Experimental) import MapboxMaps

let defaultCenter: CLLocationCoordinate2D = .init(latitude: 42.356395, longitude: -71.062424)
let defaultZoom: CGFloat = 12

struct HomeMapView: View {
    @State private var viewport: Viewport = .camera(center: defaultCenter, zoom: defaultZoom)
    @StateObject var locationDataManager: LocationDataManager = .init(distanceFilter: 1)

    var body: some View {
        MapReader { proxy in

            Map(viewport: $viewport) {
                if let location = locationDataManager.currentLocation {
                    let annotation = PointAnnotation(point: .init(location.coordinate))
                    annotation.image(named: "location-puck")
                }
            }
            .mapStyle(.light).onChange(of: locationDataManager.currentLocation?.coordinate) { location in
                if let proxyView = proxy.viewport {
                    proxyView.transition(to: proxyView.makeCameraViewportState(camera: .init(center: location, zoom: viewport.camera?.zoom)))
                }
            }
        }
    }
}
