//
//  ViewportProvider.swift
//  iosApp
//
//  Created by Simon, Emma on 3/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps

class ViewportProvider: ObservableObject {
    static let defaultCenter: CLLocationCoordinate2D = .init(latitude: 42.356395, longitude: -71.062424)
    static let defaultZoom: CGFloat = 14

    @Published var viewport: Viewport

    init(viewport: Viewport? = nil) {
        self.viewport = viewport ?? .camera(center: ViewportProvider.defaultCenter, zoom: ViewportProvider.defaultZoom)
    }

    func updateViewport(nextViewport: Viewport) {
        viewport = nextViewport
    }

    func follow(animation: ViewportAnimation = .easeInOut(duration: 1)) {
        withViewportAnimation(animation) {
            self.viewport = .followPuck(zoom: self.viewport.camera?.zoom ?? ViewportProvider.defaultZoom)
        }
    }
}
