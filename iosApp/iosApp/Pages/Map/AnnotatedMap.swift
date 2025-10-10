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

    var filter: StopDetailsFilter?
    var targetedLocation: CLLocationCoordinate2D?
    var globalData: GlobalResponse?
    var selectedVehicle: Vehicle?
    var sheetHeight: CGFloat
    var showPuck: Bool
    var vehicles: [Vehicle]?
    var handleCameraChange: (CameraChanged) -> Void
    var handleStyleLoaded: () -> Void
    var handleTapStopLayer: (FeaturesetFeature, InteractionContext) -> Bool
    var handleTapVehicle: (Vehicle) -> Void

    @ObservedObject var viewportProvider: ViewportProvider

    @State private var zoomLevel: CGFloat = 0

    @EnvironmentObject var settingsCache: SettingsCache
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.scenePhase) var scenePhase

    init(
        filter: StopDetailsFilter? = nil,
        targetedLocation: CLLocationCoordinate2D? = nil,
        globalData: GlobalResponse? = nil,
        selectedVehicle: Vehicle? = nil,
        sheetHeight: CGFloat,
        showPuck: Bool,
        vehicles: [Vehicle]? = nil,
        handleCameraChange: @escaping (CameraChanged) -> Void,
        handleStyleLoaded: @escaping () -> Void,
        handleTapStopLayer: @escaping (FeaturesetFeature, InteractionContext) -> Bool,
        handleTapVehicle: @escaping (Vehicle) -> Void,
        viewportProvider: ViewportProvider,
    ) {
        self.filter = filter
        self.targetedLocation = targetedLocation
        self.globalData = globalData
        self.selectedVehicle = selectedVehicle
        self.sheetHeight = sheetHeight
        self.showPuck = showPuck
        self.vehicles = vehicles
        self.handleCameraChange = handleCameraChange
        self.handleStyleLoaded = handleStyleLoaded
        self.handleTapStopLayer = handleTapStopLayer
        self.handleTapVehicle = handleTapVehicle
        self.viewportProvider = viewportProvider
    }

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
            .onChange(of: showPuck) { _ in handleStyleLoaded() }
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
            TapInteraction(.layer(StopLayerGenerator.shared.stopLayerId), action: handleTapStopLayer)
            TapInteraction(.layer(StopLayerGenerator.shared.stopTouchTargetLayerId), action: handleTapStopLayer)
            if showPuck {
                Puck2D()
                    .topImage(.init(resource: .locationDot))
                    .shadowImage(.init(resource: .locationHalo))
                    .pulsing(.init(color: .keyInverse, radius: .constant(24)))
                    .showsAccuracyRing(true)
                    .accuracyRingColor(.deemphasized.withAlphaComponent(0.1))
                    .accuracyRingBorderColor(.halo)
            }
            if let targetedLocation {
                MapViewAnnotation(coordinate: targetedLocation) {
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
                        .priority(isSelected ? 1 : 0)
                        .allowOverlap(true)
                        .allowOverlapWithPuck(true)
                        .ignoreCameraPadding(true)
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
