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
    static let defaultZoom: CGFloat = StopIcons.stopZoomThreshold + 0.25
    static let defaultAnimation: ViewportAnimation = .easeInOut(duration: 1)

    @Published var viewport: Viewport
    @Published var cameraState: CameraState

    init(viewport: Viewport? = nil) {
        self.viewport = viewport ?? .camera(center: Self.defaultCenter, zoom: Self.defaultZoom)
        let viewportCamera = viewport?.camera
        cameraState = .init(
            center: viewportCamera?.center ?? Self.defaultCenter,
            padding: viewportCamera?.padding ?? .zero,
            zoom: viewportCamera?.zoom ?? Self.defaultZoom,
            bearing: viewportCamera?.bearing ?? 0.0,
            pitch: viewportCamera?.pitch ?? 0.0
        )
    }

    func follow(animation: ViewportAnimation = defaultAnimation) {
        withViewportAnimation(animation) {
            self.viewport = .followPuck(zoom: self.viewport.camera?.zoom ?? Self.defaultZoom)
        }
    }

    func animateTo(coordinates: CLLocationCoordinate2D, animation: ViewportAnimation = defaultAnimation) {
        withViewportAnimation(animation) {
            self.viewport = .camera(center: coordinates, zoom: Self.defaultZoom)
        }
    }
}
