//
//  ViewportProvider.swift
//  iosApp
//
//  Created by Simon, Emma on 3/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@_spi(Experimental) import MapboxMaps

class ViewportProvider: ObservableObject {
    enum Defaults {
        static let animation: ViewportAnimation = .easeInOut(duration: 1)
        static let center: CLLocationCoordinate2D = .init(latitude: 42.356395, longitude: -71.062424)
        static let zoom: CGFloat = MapDefaults.defaultZoomThreshold
    }

    @Published var viewport: Viewport
    var cameraStatePublisher: AnyPublisher<CameraState, Never> {
        cameraStateSubject
            .removeDuplicates(by: { lhs, rhs in
                lhs.center.isRoughlyEqualTo(rhs.center)
            })
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()
    }

    private let cameraStateSubject: CurrentValueSubject<CameraState, Never>

    init(viewport: Viewport? = nil) {
        self.viewport = viewport ?? .camera(center: Defaults.center, zoom: Defaults.zoom)
        let viewportCamera = viewport?.camera
        let initialCameraState = CameraState(
            center: viewportCamera?.center ?? Defaults.center,
            padding: viewportCamera?.padding ?? .zero,
            zoom: viewportCamera?.zoom ?? Defaults.zoom,
            bearing: viewportCamera?.bearing ?? 0.0,
            pitch: viewportCamera?.pitch ?? 0.0
        )
        cameraStateSubject = .init(initialCameraState)
    }

    func follow(animation: ViewportAnimation = Defaults.animation) {
        withViewportAnimation(animation) {
            self.viewport = .followPuck(zoom: cameraStateSubject.value.zoom)
        }
    }

    func animateTo(
        coordinates: CLLocationCoordinate2D,
        animation: ViewportAnimation = Defaults.animation,
        zoom: CGFloat? = nil
    ) {
        withViewportAnimation(animation) {
            self.viewport = .camera(
                center: coordinates,
                zoom: zoom == nil ? self.cameraStateSubject.value.zoom : zoom
            )
        }
    }

    func updateCameraState(_ state: CameraState) {
        cameraStateSubject.send(state)
    }
}
