//
//  ViewportProvider.swift
//  iosApp
//
//  Created by Simon, Emma on 3/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@_spi(Experimental) import MapboxMaps
import shared
import SwiftUI

class ViewportProvider: ObservableObject {
    enum Defaults {
        static let animation: ViewportAnimation = .easeInOut(duration: 1)
        static let center: CLLocationCoordinate2D = .init(latitude: 42.3575, longitude: -71.0601)
        static let zoom: CGFloat = MapDefaults.shared.defaultZoomThreshold
        static let overviewPadding: EdgeInsets = .init(top: 75, leading: 50, bottom: 75, trailing: 50)
    }

    @Published private(set) var isManuallyCentering: Bool
    @Published private(set) var isFollowingPuck: Bool = false

    @Published var viewport: Viewport
    private var savedNearbyTransitViewport: Viewport?
    var cameraStatePublisher: AnyPublisher<CameraState, Never> {
        cameraStateSubject
            .removeDuplicates(by: { lhs, rhs in lhs.center.isRoughlyEqualTo(rhs.center) })
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()
    }

    private let cameraStateSubject: CurrentValueSubject<CameraState, Never>

    init(viewport: Viewport? = nil, isManuallyCentering: Bool = false) {
        self.viewport = viewport ?? .camera(center: Defaults.center, zoom: Defaults.zoom)
        isFollowingPuck = viewport?.isFollowing ?? false
        let viewportCamera = viewport?.camera
        let initialCameraState = CameraState(
            center: viewportCamera?.center ?? Defaults.center,
            padding: viewportCamera?.padding ?? .zero,
            zoom: viewportCamera?.zoom ?? Defaults.zoom,
            bearing: viewportCamera?.bearing ?? 0.0,
            pitch: viewportCamera?.pitch ?? 0.0
        )
        cameraStateSubject = .init(initialCameraState)
        self.isManuallyCentering = isManuallyCentering
    }

    func follow(animation: ViewportAnimation = Defaults.animation) {
        isFollowingPuck = true
        withViewportAnimation(animation) {
            self.viewport = .followPuck(zoom: cameraStateSubject.value.zoom)
        }
    }

    func vehicleOverview(vehicle: Vehicle, stop: Stop) {
        animateTo(viewport: .overview(
            geometry: MultiPoint([vehicle.coordinate, stop.coordinate]),
            geometryPadding: Defaults.overviewPadding,
            maxZoom: 16
        ))
    }

    func isDefault() -> Bool {
        viewport.camera?.center?.isRoughlyEqualTo(Defaults.center) ?? false
    }

    func animateTo(
        coordinates: CLLocationCoordinate2D,
        animation: ViewportAnimation = Defaults.animation,
        zoom: CGFloat? = nil
    ) {
        animateTo(
            viewport: .camera(
                center: coordinates,
                zoom: zoom == nil ? cameraStateSubject.value.zoom : zoom
            ),
            animation: animation
        )
    }

    func animateTo(viewport: Viewport, animation: ViewportAnimation = Defaults.animation) {
        withViewportAnimation(animation) {
            self.viewport = viewport
        } completion: { _ in
            Task { @MainActor in self.isManuallyCentering = false }
        }
    }

    func updateCameraState(_ location: CLLocation?) {
        guard let coordinate = location?.coordinate else { return }
        updateCameraState(
            .init(center: coordinate, padding: .zero, zoom: 0, bearing: 0, pitch: 0)
        )
    }

    func updateCameraState(_ state: CameraState) {
        cameraStateSubject.send(state)
    }

    func saveCurrentViewport() {
        let camera = cameraStateSubject.value
        if viewport.isFollowing {
            viewport = .followPuck(zoom: camera.zoom)
        } else {
            viewport = .camera(center: camera.center, zoom: camera.zoom)
        }
    }

    func saveNearbyTransitViewport() {
        savedNearbyTransitViewport = viewport
        // When the user is panning the map, the viewport is idle,
        // and to actually restore anything, we need to save the values from the most recent camera state.
        if savedNearbyTransitViewport == .idle {
            let cameraState = cameraStateSubject.value
            savedNearbyTransitViewport = .camera(center: cameraState.center, zoom: cameraState.zoom)
        }
    }

    func restoreNearbyTransitViewport() {
        if let saved = savedNearbyTransitViewport {
            withViewportAnimation(Defaults.animation) {
                let currentZoom = cameraStateSubject.value.zoom
                self.viewport = if let camera = saved.camera {
                    .camera(center: camera.center, zoom: currentZoom)
                } else if let _ = saved.followPuck {
                    .followPuck(zoom: currentZoom)
                } else {
                    saved
                }
            }
        }
        savedNearbyTransitViewport = nil
    }

    func setIsManuallyCentering(_ isManuallyCentering: Bool) {
        self.isManuallyCentering = isManuallyCentering
        if isManuallyCentering {
            isFollowingPuck = false
        }
    }

    static func viewportAround(
        center: CLLocationCoordinate2D,
        inView: [CLLocationCoordinate2D],
        // Insets with different horizontal/vertical values will result in the center point being off center
        padding: EdgeInsets = Defaults.overviewPadding
    ) -> Viewport {
        let reflectedPoints = inView.map { point in
            CLLocationCoordinate2D(
                latitude: reflect(point: center.latitude, reflected: point.latitude),
                longitude: reflect(point: center.longitude, reflected: point.longitude)
            )
        }
        return .overview(
            geometry: MultiPoint([center] + inView + reflectedPoints),
            geometryPadding: padding
        )
    }

    private static func reflect(point: Double, reflected: Double) -> Double { (2 * point) - reflected }
}
