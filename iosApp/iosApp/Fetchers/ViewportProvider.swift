//
//  ViewportProvider.swift
//  iosApp
//
//  Created by Simon, Emma on 3/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@_spi(Experimental) import MapboxMaps
import Shared
import SwiftUI

class ViewportProvider: ObservableObject, Shared.ViewportManager {
    enum Defaults {
        static let animation: ViewportAnimation = .easeInOut(duration: 1)
        static let overviewPadding: SwiftUI.EdgeInsets = .init(top: 75, leading: 50, bottom: 75, trailing: 50)
    }

    @Published private(set) var state: MapViewportState
    var isCurrentlyPanning: Bool { (state as? MapViewportState.ManuallyPanned)?.currentlyPanning == true }
    var isFollowingPuck: Bool { state == .FollowingPuck.shared }
    var isVehicleOverview: Bool { state is MapViewportState.VehicleOverview }
    @Published var isTargeting: Bool = false
    @Published var lastLoadedLocation: CLLocationCoordinate2D? = nil

    @Published var viewport: Viewport?
    private var savedNearbyTransitViewport: Viewport?
    let cameraStatePublisher: AnyPublisher<CameraState, Never>
    let cameraStatePublisherThrottled: AnyPublisher<CameraState, Never>

    private let cameraStateSubject: CurrentValueSubject<CameraState?, Never>

    init(viewport: Viewport? = nil) {
        self.viewport = viewport
        let viewportCamera = viewport?.camera
        let initialCameraState = viewport == nil ? nil : CameraState(
            center: viewportCamera?.center ?? MapDefaults.shared.center.toMapbox(),
            padding: viewportCamera?.padding ?? .zero,
            zoom: viewportCamera?.zoom ?? MapDefaults.shared.zoom,
            bearing: viewportCamera?.bearing ?? 0.0,
            pitch: viewportCamera?.pitch ?? 0.0
        )

        state = (viewport?.isFollowing ?? false) ? .FollowingPuck.shared : .ManuallyPanned(
            toDefault: true,
            currentlyPanning: false
        )
        cameraStateSubject = .init(initialCameraState)

        cameraStatePublisher =
            cameraStateSubject
                .compactMap { $0 }
                .removeDuplicates(by: { lhs, rhs in lhs.center.isRoughlyEqualTo(rhs.center) })
                .receive(on: DispatchQueue.main)
                .eraseToAnyPublisher()

        cameraStatePublisherThrottled = cameraStatePublisher.throttle(
            for: .seconds(0.5),
            scheduler: DispatchQueue.main,
            latest: true
        ).eraseToAnyPublisher()
    }

    func follow(instantly: Bool = false) {
        Task { try await applyState(state: .FollowingPuck.shared, instantly: instantly) }
    }

    func vehicleOverview(vehicle: Vehicle, stop: Stop?) {
        Task { try await applyState(
            state: .VehicleOverview(
                vehiclePosition: vehicle.position, targetStopPosition: stop?.position,
                density: nil
            ),
            instantly: false
        ) }
    }

    func isDefault() -> Bool {
        (state as? MapViewportState.ManuallyPanned)?.toDefault ?? false
    }

    func stopCenter(stop: Stop) {
        Task { try await applyState(state: .StopCenter(stopPosition: stop.position), instantly: false) }
    }

    func panToDefaultCenter() {
        Task { try await applyState(state: .ManuallyPanned(toDefault: true, currentlyPanning: true), instantly: false) }
    }

    func initViewport(location: CLLocation) {
        if viewport == nil {
            let cameraState: CameraState = .init(
                center: location.coordinate,
                padding: .zero,
                zoom: MapDefaults.shared.zoom,
                bearing: 0,
                pitch: 0
            )
            viewport = .camera(center: cameraState.center, zoom: cameraState.zoom)
            updateCameraState(cameraState)
        }
    }

    /**
        For use when map display is turned off and $viewport is never updated directly.
     */
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
        if viewport?.isFollowing ?? false {
            viewport = .followPuck(zoom: currentSubjectZoomSafe())
        } else {
            viewport = .camera(
                center: camera?.center ?? MapDefaults.shared.center.toMapbox(),
                zoom: currentSubjectZoomSafe()
            )
        }
    }

    func setIsCurrentlyPanning(_ isCurrentlyPanning: Bool) {
        state = .ManuallyPanned(toDefault: false, currentlyPanning: isCurrentlyPanning)
    }

    func currentSubjectZoomSafe() -> CGFloat {
        cameraStateSubject.value?.zoom ?? CGFloat(MapDefaults.shared.zoom)
    }

    @MainActor func animateTo(
        coordinates: CLLocationCoordinate2D,
        animation: ViewportAnimation = Defaults.animation,
        zoom: CGFloat? = nil,
        completion: @escaping () -> Void = {},
    ) {
        animateTo(
            viewport: .camera(
                center: coordinates,
                zoom: zoom == nil ? currentSubjectZoomSafe() : zoom
            ),
            animation: animation,
            completion: completion,
        )
    }

    @MainActor private func animateTo(
        viewport: Viewport,
        animation: ViewportAnimation = Defaults.animation,
        completion: @escaping () -> Void = {},
    ) {
        withViewportAnimation(animation) {
            self.viewport = viewport
        } completion: { _ in
            completion()
        }
    }

    static func viewportAround(
        center: CLLocationCoordinate2D,
        inView: [CLLocationCoordinate2D],
        // Insets with different horizontal/vertical values will result in the center point being off center
        padding: SwiftUI.EdgeInsets = Defaults.overviewPadding
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

    // MARK: ViewportManager Conformance

    // swiftlint:disable:next identifier_name
    func __isDefault() async throws -> KotlinBoolean {
        KotlinBoolean(bool: isDefault())
    }

    // swiftlint:disable:next identifier_name
    @MainActor func __restoreNearbyTransitViewport() async throws {
        if let saved = savedNearbyTransitViewport {
            withViewportAnimation(Defaults.animation) {
                let currentZoom = currentSubjectZoomSafe()
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

    // swiftlint:disable:next identifier_name
    func __saveNearbyTransitViewport() async throws {
        savedNearbyTransitViewport = viewport
        // When the user is panning the map, the viewport is idle,
        // and to actually restore anything, we need to save the values from the most recent camera state.
        if savedNearbyTransitViewport == .idle {
            let cameraState = cameraStateSubject.value
            savedNearbyTransitViewport = .camera(
                center: cameraState?.center ?? MapDefaults.shared.center.toMapbox(),
                zoom: currentSubjectZoomSafe()
            )
        }
    }

    // swiftlint:disable:next identifier_name
    @MainActor func __applyState(state: MapViewportState, instantly: Bool) async throws {
        self.state = state
        switch onEnum(of: state) {
        case .followingPuck:
            let animation = instantly ? ViewportAnimation.default(maxDuration: .zero) : ViewportAnimation.default
            withViewportAnimation(animation) {
                self.viewport = .followPuck(zoom: currentSubjectZoomSafe())
            }
        case let .manuallyPanned(state):
            if state.toDefault {
                animateTo(
                    coordinates: MapDefaults.shared.center.toMapbox(),
                    zoom: 13.75,
                    completion: { self.state = .ManuallyPanned(toDefault: true, currentlyPanning: false) }
                )
            }
        case let .stopCenter(state):
            animateTo(coordinates: state.stopPosition.toMapbox())
        case let .vehicleOverview(state):
            let vehicle = state.vehiclePosition
            let geometry: GeometryConvertible = if let stop = state.targetStopPosition {
                MultiPoint([vehicle.toMapbox(), stop.toMapbox()])
            } else {
                Point(vehicle.toMapbox())
            }

            animateTo(viewport: .overview(
                geometry: geometry,
                geometryPadding: Defaults.overviewPadding,
                maxZoom: 16
            ))
        }
    }
}
