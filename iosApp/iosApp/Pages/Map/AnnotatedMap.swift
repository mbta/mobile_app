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

    var stopMapData: StopMapResponse?
    var filter: StopDetailsFilter?
    var nearbyLocation: CLLocationCoordinate2D?
    var sheetHeight: CGFloat
    var vehicles: [Vehicle]?

    @ObservedObject var viewportProvider: ViewportProvider

    var handleCameraChange: (CameraChanged) -> Void
    var handleStopLayerTap: (QueriedFeature, MapContentGestureContext) -> Bool

    @State private var zoomLevel: CGFloat = 0

    var body: some View {
        map
            .gestureOptions(.init(rotateEnabled: false, pitchEnabled: false))
            .mapStyle(.light)
            .onCameraChanged { change in handleCameraChange(change) }
            .ornamentOptions(.init(scaleBar: .init(visibility: .hidden)))
            .onLayerTapGesture(StopLayerGenerator.getStopLayerId(.stop), perform: handleStopLayerTap)
            .onLayerTapGesture(StopLayerGenerator.getStopLayerId(.station), perform: handleStopLayerTap)
            .additionalSafeAreaInsets(.bottom, sheetHeight)
            .accessibilityIdentifier("transitMap")
            .onReceive(viewportProvider.cameraStateSubject) { newCameraState in
                zoomLevel = newCameraState.zoom
            }
    }

    @ViewBuilder
    var map: Map {
        Map(viewport: $viewportProvider.viewport) {
            Puck2D().pulsing(.none)
            if let nearbyLocation {
                MapViewAnnotation(coordinate: nearbyLocation) {
                    Circle()
                        .strokeBorder(.white, lineWidth: 2.5)
                        .background(Circle().fill(.orange))
                        .frame(width: 22, height: 22)
                }
            }
            if let filter, let vehicles {
                ForEvery(vehicles, id: \.id) { vehicle in
                    if vehicle.routeId == filter.routeId, vehicle.directionId == filter.directionId {
                        MapViewAnnotation(coordinate: vehicle.coordinate) {
                            Circle()
                                .strokeBorder(.white, lineWidth: 2.5)
                                .background(Circle().fill(.black))
                                .frame(width: 16, height: 16)
                        }
                    }
                }
            }
            if let childStops = stopMapData?.childStops.values {
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
                        .visible(zoomLevel >= StopIcons.tombstoneZoomThreshold)
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
                        .visible(zoomLevel >= StopIcons.tombstoneZoomThreshold)
                    default:
                        MapViewAnnotation(coordinate: child.coordinate) {
                            EmptyView()
                        }.visible(false)
                    }
                }
            }
        }
    }
}
