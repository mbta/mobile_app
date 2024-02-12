//
//  HomeMapView.swift
//  iosApp
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI
@_spi(Experimental) import MapboxMaps

struct HomeMapView: View {
    static let defaultCenter: CLLocationCoordinate2D = .init(latitude: 42.356395, longitude: -71.062424)
    static let defaultZoom: CGFloat = 12

    @State var viewport: Viewport = .camera(center: defaultCenter, zoom: defaultZoom)
    @ObservedObject var locationDataManager: LocationDataManager
    var didAppear: ((Self) -> Void)? // 1.

    var body: some View {
        MapReader { proxy in

            Map(viewport: $viewport) {
                Puck2D().pulsing(.none)
            }
            .mapStyle(.light).onAppear {
                proxy.location?.override(locationProvider: locationDataManager.$currentLocation.map {
                    if let location = $0 {
                        [Location(clLocation: location)]
                    } else { [] }
                }.eraseToSignal())

                viewport = .followPuck(zoom: viewport.camera?.zoom ?? HomeMapView.defaultZoom)
                didAppear?(self)
            }
        }
    }
}
