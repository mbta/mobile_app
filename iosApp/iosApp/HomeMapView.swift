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

    var body: some View {
            Map(viewport: $viewport)
                .mapStyle(.light)
        
    }
}
