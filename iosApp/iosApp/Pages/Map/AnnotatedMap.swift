//
//  AnnotatedMap.swift
//  iosApp
//
//  Created by Simon, Emma on 4/30/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

struct AnnotatedMap: View {
    static let annotationTextZoomThreshold = 19.0

    private let centerMovingGestures: Set<GestureType> = [.pan, .pinch, .doubleTapToZoomIn, .quickZoom]

    var stopMapData: StopMapResponse?
    var filter: StopDetailsFilter?
    var nearbyLocation: CLLocationCoordinate2D?
    var routes: [String: Route]?
    var selectedVehicle: Vehicle?
    var sheetHeight: CGFloat
    var vehicles: [Vehicle]?

    var getSettingUsecase = UsecaseDI().getSettingUsecase
    @State var mapDebug = false

    @ObservedObject var viewportProvider: ViewportProvider
    @Environment(\.colorScheme) var colorScheme

    var handleCameraChange: (CameraChanged) -> Void
    var handleMapLoaded: () -> Void
    var handleTapStopLayer: (QueriedFeature, MapContentGestureContext) -> Bool
    var handleTapVehicle: (Vehicle) -> Void

    @State private var zoomLevel: CGFloat = 0

    var body: some View {
        map
            .gestureOptions(.init(rotateEnabled: false, pitchEnabled: false))
            .mapStyle(colorScheme == .light ? .light : .dark)
            .debugOptions(mapDebug ? .camera : [])
            .onCameraChanged { change in handleCameraChange(change) }
            .ornamentOptions(.init(scaleBar: .init(visibility: .hidden)))
            .onLayerTapGesture(StopLayerGenerator.shared.stopLayerId, perform: handleTapStopLayer)
            .onLayerTapGesture(StopLayerGenerator.shared.stopTouchTargetLayerId, perform: handleTapStopLayer)
            .onMapLoaded { _ in
                // The initial run of this happens before any required data is loaded, so it does nothing and
                // handleTryLayerInit always performs the first layer creation, but once the data is in place,
                // this handles any time the map is reloaded again, like for a light/dark mode switch.
                handleMapLoaded()
            }
            .additionalSafeAreaInsets(.bottom, sheetHeight)
            .accessibilityIdentifier("transitMap")
            .onReceive(viewportProvider.cameraStatePublisher) { newCameraState in
                zoomLevel = newCameraState.zoom
            }
            .task {
                do {
                    mapDebug = try await getSettingUsecase.execute(setting: .map).boolValue
                } catch {
                    debugPrint("Failed to load map debug", error)
                }
            }
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
                       let route = routes?[routeId] {
                        let isSelected = vehicle.id == selectedVehicle?.id
                        MapViewAnnotation(coordinate: vehicle.coordinate) {
                            ZStack {
                                ZStack {
                                    Image(.vehicleHalo)
                                    Image(.vehiclePuck).foregroundStyle(Color(hex: route.color))
                                }
                                .frame(width: 32, height: 32)
                                .rotationEffect(.degrees(45))
                                .rotationEffect(.degrees(vehicle.bearing?.doubleValue ?? 0))
                                routeIcon(route)
                                    .foregroundStyle(Color(hex: route.textColor))
                                    .frame(height: 18)
                            }
                            .scaleEffect(isSelected ? 2 : 1, anchor: .center)
                            .padding(6)
                            .onTapGesture { handleTapVehicle(vehicle) }
                        }
                        .selected(isSelected)
                        .allowOverlap(true)
                        .allowOverlapWithPuck(true)
                        .visible(zoomLevel >= StopLayerGenerator.shared.stopZoomThreshold)
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
