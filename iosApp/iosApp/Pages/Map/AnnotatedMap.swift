//
//  AnnotatedMap.swift
//  iosApp
//
//  Created by Simon, Emma on 4/30/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import Shared
import SwiftUI

struct AnnotatedMap: View {
    static let annotationTextZoomThreshold = 19.0

    private let centerMovingGestures: Set<GestureType> = [.pan, .pinch, .doubleTapToZoomIn, .quickZoom]

    var stopMapData: StopMapResponse?
    var filter: StopDetailsFilter?
    var nearbyLocation: CLLocationCoordinate2D?
    var globalData: GlobalResponse?
    var selectedVehicle: Vehicle?
    var sheetHeight: CGFloat
    var vehicles: [Vehicle]?

    @EnvironmentObject var settingsCache: SettingsCache

    @ObservedObject var viewportProvider: ViewportProvider
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.scenePhase) var scenePhase

    var handleCameraChange: (CameraChanged) -> Void
    var handleStyleLoaded: () -> Void
    var handleTapStopLayer: (QueriedFeature, InteractionContext) -> Bool
    var handleTapVehicle: (Vehicle) -> Void

    @State private var zoomLevel: CGFloat = 0

    var body: some View {
        map
            .gestureOptions(.init(
                rotateEnabled: false,
                pitchEnabled: false,
                panDecelerationFactor: 0.99
            ))
            .mapStyle(.init(uri: appVariant.styleUri(colorScheme: colorScheme)))
            .debugOptions(settingsCache.get(.devDebugMode) ? .camera : [])
            .cameraBounds(.init(maxZoom: 18, minZoom: 6))
            .onCameraChanged { change in handleCameraChange(change) }
            .ornamentOptions(.init(
                scaleBar: .init(visibility: .hidden),
                compass: .init(visibility: .hidden),
                attributionButton: .init(margins: .init(x: -3, y: 6))
            ))
            .onLayerTapGesture(StopLayerGenerator.shared.stopLayerId, perform: handleTapStopLayer)
            .onLayerTapGesture(StopLayerGenerator.shared.stopTouchTargetLayerId, perform: handleTapStopLayer)
            .onStyleLoaded { _ in
                // The initial run of this happens before any required data is loaded, so it does nothing and
                // handleTryLayerInit always performs the first layer creation, but once the data is in place,
                // this handles any time the map is reloaded again, like for a light/dark mode switch.
                if scenePhase == .active {
                    // onStyleLoaded was unexpectedly called when app moved to background because the colorScheme
                    // changes twice while backgrounding. Ensure it is only called when the app is active.
                    handleStyleLoaded()
                }
            }
            .additionalSafeAreaInsets(.bottom, sheetHeight)
            .additionalSafeAreaInsets(.top, 20)
            .ignoresSafeArea(.all)
            .accessibilityIdentifier("transitMap")
            .onReceive(viewportProvider.cameraStatePublisher) { newCameraState in
                zoomLevel = newCameraState.zoom
            }
            .withScenePhaseHandlers(onActive: onActive)
    }

    func onActive() {
        // re-load styles in case the colorScheme changed while in the background
        handleStyleLoaded()
    }

    private var allVehicles: [Vehicle]? {
        switch (vehicles, selectedVehicle) {
        case (.none, .none): nil
        case let (.none, .some(vehicle)): [vehicle]
        case let (.some(vehicles), .none): vehicles
        case let (.some(vehicles), .some(vehicle)): vehicles + [vehicle]
        }
    }

    @ViewBuilder
    var map: Map {
        Map(viewport: $viewportProvider.viewport) {
            Puck2D()
                .topImage(.init(resource: .locationDot))
                .shadowImage(.init(resource: .locationHalo))
                .pulsing(.init(color: .keyInverse, radius: .constant(24)))
                .showsAccuracyRing(true)
                .accuracyRingColor(.deemphasized.withAlphaComponent(0.1))
                .accuracyRingBorderColor(.halo)
            if let nearbyLocation {
                MapViewAnnotation(coordinate: nearbyLocation) {
                    Image(.mapNearbyLocationCursor).frame(width: 26, height: 26)
                }.allowHitTesting(false)
            }
            if let allVehicles {
                ForEvery(allVehicles, id: \.id) { vehicle in
                    if let routeId = vehicle.routeId,
                       let route = globalData?.getRoute(routeId: routeId) {
                        let isSelected = vehicle.id == selectedVehicle?.id
                        MapViewAnnotation(coordinate: vehicle.coordinate) {
                            VehicleMarkerView(
                                route: route,
                                vehicle: vehicle,
                                isSelected: isSelected,
                                onTap: { handleTapVehicle(vehicle) }
                            )
                        }
                        .selected(isSelected)
                        .allowOverlap(true)
                        .allowOverlapWithPuck(true)
                        .visible(zoomLevel >= StopLayerGenerator.shared.stopZoomThreshold || isSelected)
                    }
                }
            }
        }
        .gestureHandlers(.init(
            onBegin: { gestureType in
                if centerMovingGestures.contains(gestureType) {
                    viewportProvider.setIsManuallyCentering(true)
                }
            },
            onEnd: { gestureType, willAnimate in
                if centerMovingGestures.contains(gestureType), !willAnimate {
                    viewportProvider.setIsManuallyCentering(false)
                }
            },
            onEndAnimation: { gestureType in
                if centerMovingGestures.contains(gestureType) {
                    viewportProvider.setIsManuallyCentering(false)
                }
            }
        ))
    }
}
