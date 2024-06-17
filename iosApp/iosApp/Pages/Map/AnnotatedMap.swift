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

    private let centerMovingGestures: Set<GestureType> = [.pan, .doubleTapToZoomIn]

    var stopMapData: StopMapResponse?
    var filter: StopDetailsFilter?
    var nearbyLocation: CLLocationCoordinate2D?
    var routes: [String: Route]
    var sheetHeight: CGFloat
    var vehicles: [Vehicle]?

    var settingsRepository: ISettingsRepository = RepositoryDI().settings
    @State var mapDebug = false

    @ObservedObject var viewportProvider: ViewportProvider
    @Environment(\.colorScheme) var colorScheme

    var handleCameraChange: (CameraChanged) -> Void
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
            .onLayerTapGesture(StopLayerGenerator.stopLayerId, perform: handleTapStopLayer)
            .onLayerTapGesture(StopLayerGenerator.stopTouchTargetLayerId, perform: handleTapStopLayer)
            .additionalSafeAreaInsets(.bottom, sheetHeight)
            .accessibilityIdentifier("transitMap")
            .onReceive(viewportProvider.cameraStatePublisher) { newCameraState in
                zoomLevel = newCameraState.zoom
            }
            .task {
                do {
                    mapDebug = try await settingsRepository.getMapDebug().boolValue
                } catch {
                    debugPrint("Failed to load map debug", error)
                }
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
                }
            }
            if let filter, let vehicles {
                ForEvery(vehicles, id: \.id) { vehicle in
                    if let routeId = vehicle.routeId,
                       let route = routes[routeId],
                       routeId == filter.routeId,
                       vehicle.directionId == filter.directionId {
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
                            .padding(6)
                            .onTapGesture { handleTapVehicle(vehicle) }
                        }
                        .allowOverlap(true)
                        .allowOverlapWithPuck(true)
                        .visible(zoomLevel >= StopLayerGenerator.stopZoomThreshold)
                    }
                }
            }
            if let childStops = stopMapData?.childStops.values {
                /* TODO: Switch these to PointAnnotations once we have finalized icons and have decided
                    how to handle child stops in general. We should be taking advantage of the text label
                    functionality, and possibly the PointAnnotationGroup clustering behavior to
                    handle stations with overlapping child stops. They may even need to be SymbolLayers
                    if we need to do something really complicated with clustering.
                 */
                ForEvery(Array(childStops), id: \.id) { child in
                    switch child.locationType {
                    case .entranceExit:
                        MapViewAnnotation(coordinate: child.coordinate) {
                            Image(systemName: "door.left.hand.open").annotationLabel(
                                Text(child.name.split(separator: " - ").last ?? "")
                                    .font(.caption)
                                    .italic()
                                    .foregroundStyle(.gray)
                                    .opacity(zoomLevel >= Self.annotationTextZoomThreshold ? 1 : 0)
                            )
                        }
                        .allowHitTesting(false)
                        .ignoreCameraPadding(true)
                        .visible(zoomLevel >= MapDefaults.closeZoomThreshold)
                    case .boardingArea, .stop:
                        MapViewAnnotation(coordinate: child.coordinate) {
                            Circle()
                                .strokeBorder(.white, lineWidth: 2.5)
                                .background(Circle().fill(.gray))
                                .frame(width: 12, height: 12)
                                .annotationLabel(
                                    Text(child.platformName ?? child.name)
                                        .font(.caption)
                                        .italic()
                                        .foregroundStyle(.gray)
                                        .opacity(zoomLevel >= Self.annotationTextZoomThreshold ? 1 : 0)
                                )
                        }
                        .allowHitTesting(false)
                        .ignoreCameraPadding(true)
                        .visible(zoomLevel >= MapDefaults.closeZoomThreshold)
                    default:
                        MapViewAnnotation(coordinate: child.coordinate) {
                            EmptyView()
                        }.visible(false)
                    }
                }
            }
        }
        .gestureHandlers(.init(onBegin: { gestureType in
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
                               }))
    }
}
