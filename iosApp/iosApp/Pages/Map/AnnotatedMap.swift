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
    var filter: StopDetailsFilter?
    var nearbyLocation: CLLocationCoordinate2D?
    var sheetHeight: CGFloat
    var vehicles: [Vehicle]?

    @ObservedObject var viewportProvider: ViewportProvider

    var handleCameraChange: (CameraChanged) -> Void
    var handleStopLayerTap: (QueriedFeature, MapContentGestureContext) -> Bool

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
        }
    }
}
