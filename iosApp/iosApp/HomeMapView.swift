//
//  HomeMapView.swift
//  iosApp
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI
import shared
@_spi(Experimental) import MapboxMaps

struct HomeMapView: View {

    static let defaultCenter: CLLocationCoordinate2D = .init(latitude: 42.356395, longitude: -71.062424)
    static let defaultZoom: CGFloat = 12

    private let stopLayerId = "stop-layer"
    private let stopSourceId = "stop-source"
    private let stopIconId = "t-logo"

    @State var viewport: Viewport = .camera(center: defaultCenter, zoom: defaultZoom)
    @StateObject private var locationDataManager: LocationDataManager
    @ObservedObject var globalFetcher: GlobalFetcher

    init(globalFetcher: GlobalFetcher, locationDataManager: LocationDataManager = .init(distanceFilter: 1)) {
        self.globalFetcher = globalFetcher
        _locationDataManager = StateObject(wrappedValue: locationDataManager)
    }

    var didAppear: ((Self) -> Void)?
    
    func updateStopOpacity(map: MapboxMap?, opacity: Double) {
        try? map?.updateLayer(withId: stopLayerId, type: SymbolLayer.self) { layer in
            if (layer.iconOpacity != .constant(opacity)) {
                layer.iconOpacity = .constant(opacity)
            }
        }
    }
    
    func createStopSourceData(stops : [Stop]) -> GeoJSONSourceData {
        let stopFeatures = stops.map{ stop in
            var stopFeature = Feature(
                geometry: Point(CLLocationCoordinate2D(latitude: stop.latitude, longitude: stop.longitude))
            )
            stopFeature.identifier = FeatureIdentifier(stop.id)
            return stopFeature
        }

        return .featureCollection(FeatureCollection(features: stopFeatures))
    }
    
    func createStopLayer() -> Layer {
        var stopLayer = SymbolLayer(id: stopLayerId, source: stopSourceId)
        stopLayer.iconImage = .constant(.name(stopIconId))
        stopLayer.iconAllowOverlap = .constant(true)
        stopLayer.minZoom = 13.75
        stopLayer.iconOpacity = .constant(0)
        stopLayer.iconOpacityTransition = StyleTransition(duration: 1, delay: 0)

        return stopLayer
    }

    var body: some View {
        MapReader { proxy in
            Map(viewport: $viewport) {
                Puck2D().pulsing(.none)
            }
            .gestureOptions(.init(rotateEnabled: false))
            .mapStyle(.light)
            .onCameraChanged { change in
                updateStopOpacity(map: proxy.map, opacity: change.cameraState.zoom > 14.0 ? 1 : 0)
            }
            .ornamentOptions(.init(scaleBar: .init(visibility: .hidden)))
            .onLayerTapGesture(stopLayerId) { feature, context in
                // Each stop feature has the stop ID as the identifier
                // We can also set arbitrary JSON properties if we need to
                // print(feature.feature.identifier)
                return true
            }
            .onChange(of: globalFetcher.stops) { stops in
                let map = proxy.map!
                if (map.sourceExists(withId: stopSourceId)) {
                    // Don't create a new source if one already exists
                    map.updateGeoJSONSource(
                        withId: stopSourceId,
                        data: createStopSourceData(stops: stops)
                    )
                } else {
                    // Create a GeoJSON data source for markers
                    var stopSource = GeoJSONSource(id: stopSourceId)
                    stopSource.data = createStopSourceData(stops: stops)
                    try? map.addSource(stopSource)
                    // Add marker image to the map
                    try? map.addImage(UIImage(named: "t-logo")!, id: stopIconId)
                    // Create a symbol layer for markers
                    try? map.addLayer(createStopLayer())
                }
            }.onAppear {
                proxy.location?.override(locationProvider: locationDataManager.$currentLocation.map {
                    if let location = $0 {
                        [Location(clLocation: location)]
                    } else { [] }
                }.eraseToSignal())

                viewport = .followPuck(zoom: viewport.camera?.zoom ?? HomeMapView.defaultZoom)
                
                Task{
                    try await globalFetcher.getGlobalData()
                }

                didAppear?(self)
            }
        }
    }
}
