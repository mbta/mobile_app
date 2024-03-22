//
//  HomeMapView.swift
//  iosApp
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import Polyline
import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

struct HomeMapView: View {
    static let stopZoomThreshold: CGFloat = ViewportProvider.defaultZoom - 0.25

    private let cameraDebouncer = Debouncer(delay: 0.25)

    private let routeLayerId = "route-layer"
    private let routeSourceId = "route-source"
    private let stopLayerId = "stop-layer"
    private let stopSourceId = "stop-source"
    private let stopIconId = "t-logo"

    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @ObservedObject var viewportProvider: ViewportProvider

    @StateObject private var locationDataManager: LocationDataManager
    @State var recenterButton: ViewAnnotation?

    init(
        globalFetcher: GlobalFetcher,
        nearbyFetcher: NearbyFetcher,
        railRouteShapeFetcher: RailRouteShapeFetcher,
        locationDataManager: LocationDataManager = .init(distanceFilter: 1),
        viewportProvider: ViewportProvider
    ) {
        self.globalFetcher = globalFetcher
        self.nearbyFetcher = nearbyFetcher
        self.railRouteShapeFetcher = railRouteShapeFetcher
        self.viewportProvider = viewportProvider
        _locationDataManager = StateObject(wrappedValue: locationDataManager)
    }

    var body: some View {
        MapReader { proxy in
            Map(viewport: $viewportProvider.viewport) {
                Puck2D().pulsing(.none)
                if isNearbyNotFollowing() {
                    MapViewAnnotation(coordinate: nearbyFetcher.loadedLocation!) {
                        Circle()
                            .strokeBorder(.white, lineWidth: 2.5)
                            .background(Circle().fill(.orange))
                            .frame(width: 22, height: 22)
                    }
                }
            }
            .gestureOptions(.init(rotateEnabled: false, pitchEnabled: false))
            .mapStyle(.light)
            .onCameraChanged { change in handleCameraChange(proxy.map, change) }
            .ornamentOptions(.init(scaleBar: .init(visibility: .hidden)))
            .onLayerTapGesture(stopLayerId) { _, _ in
                // Each stop feature has the stop ID as the identifier
                // We can also set arbitrary JSON properties if we need to
                // print(feature.feature.identifier)
                true
            }
            .accessibilityIdentifier("transitMap")
            .onAppear { handleAppear(location: proxy.location) }
            .onChange(of: globalFetcher.stops) { stops in handleGlobalStops(proxy.map, stops) }
            .onChange(of: railRouteShapeFetcher.response) { response in handleRouteResponse(proxy.map, response) }
            .onChange(of: locationDataManager.authorizationStatus) { status in
                if status == .authorizedAlways || status == .authorizedWhenInUse {
                    Task { viewportProvider.follow(animation: .easeInOut(duration: 0)) }
                }
            }
            .overlay(alignment: .topTrailing) {
                if !viewportProvider.viewport.isFollowing, locationDataManager.currentLocation != nil {
                    RecenterButton { Task { viewportProvider.follow() } }
                }
            }
        }
    }

    var didAppear: ((Self) -> Void)?

    func createRouteLayer(route: Route) -> Layer {
        var routeLayer = LineLayer(id: getRouteLayerId(route.id), source: getRouteSourceId(route.id))
        routeLayer.lineWidth = .constant(4.0)
        routeLayer.lineColor = .constant(StyleColor(UIColor(hex: route.color)))
        routeLayer.lineBorderWidth = .constant(1.0)
        routeLayer.lineBorderColor = .constant(StyleColor(.white))
        routeLayer.lineJoin = .constant(.round)
        routeLayer.lineCap = .constant(.round)
        return routeLayer
    }

    func createRouteSourceData(route: Route, routesResponse: RouteResponse) -> GeoJSONSourceData {
        let routeFeatures: [Feature] = route.routePatternIds!
            .map { patternId -> RoutePattern? in
                routesResponse.routePatterns[patternId]
            }
            .filter { pattern in
                pattern?.typicality == .typical
            }
            .compactMap { pattern in
                guard let pattern,
                      let representativeTrip = routesResponse.trips[pattern.representativeTripId],
                      let shapeId = representativeTrip.shapeId,
                      let shape = routesResponse.shapes[shapeId] else { return nil }
                let polyline = Polyline(encodedPolyline: shape.polyline!)
                return Feature(geometry: LineString(polyline.coordinates!))
            }
        return .featureCollection(FeatureCollection(features: routeFeatures))
    }

    func createStopLayer() -> Layer {
        var stopLayer = SymbolLayer(id: stopLayerId, source: stopSourceId)
        stopLayer.iconImage = .constant(.name(stopIconId))
        stopLayer.iconAllowOverlap = .constant(true)
        stopLayer.minZoom = HomeMapView.stopZoomThreshold - 0.25
        stopLayer.iconOpacity = .constant(0)
        stopLayer.iconOpacityTransition = StyleTransition(duration: 1, delay: 0)

        return stopLayer
    }

    func createStopSourceData(stops: [Stop]) -> GeoJSONSourceData {
        let stopFeatures = stops
            .filter { stop in
                stop.parentStationId == nil
            }
            .map { stop in
                var stopFeature = Feature(
                    geometry: Point(CLLocationCoordinate2D(latitude: stop.latitude, longitude: stop.longitude))
                )
                stopFeature.identifier = FeatureIdentifier(stop.id)
                return stopFeature
            }

        return .featureCollection(FeatureCollection(features: stopFeatures))
    }

    func getRouteSourceId(_ routeId: String) -> String { "\(routeSourceId)-\(routeId)" }
    func getRouteLayerId(_ routeId: String) -> String { "\(routeLayerId)-\(routeId)" }

    func handleAppear(location: LocationManager?) {
        location?.override(locationProvider: locationDataManager.$currentLocation.map {
            if let location = $0 {
                [Location(clLocation: location)]
            } else { [] }
        }.eraseToSignal())

        Task {
            if locationDataManager.currentLocation != nil {
                viewportProvider.follow(animation: .default(maxDuration: 0))
            }
        }
        Task {
            try await globalFetcher.getGlobalData()
        }
        Task {
            try await railRouteShapeFetcher.getRailRouteShapes()
        }

        didAppear?(self)
    }

    func handleCameraChange(_ possibleMap: MapboxMap?, _ change: CameraChanged) {
        cameraDebouncer.debounce {
            guard let map = possibleMap else { return }
            viewportProvider.cameraState = change.cameraState
            updateStopOpacity(
                map: map,
                opacity: change.cameraState.zoom > HomeMapView.stopZoomThreshold ? 1 : 0
            )
        }
    }

    func handleGlobalStops(_ possibleMap: MapboxMap?, _ stops: [Stop]) {
        guard let map = possibleMap else { return }
        if map.sourceExists(withId: stopSourceId) {
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
    }

    func handleRouteResponse(_ possibleMap: MapboxMap?, _ response: RouteResponse?) {
        guard let map = possibleMap else { return }
        guard let routesResponse = response else { return }
        // Reverse sort routes so lowest sorted ones are placed lowest on the map
        let sortedRoutes = routesResponse.routes.sorted { aRoute, bRoute in
            aRoute.sortOrder >= bRoute.sortOrder
        }
        for route in sortedRoutes {
            if map.sourceExists(withId: getRouteSourceId(route.id)) {
                // Don't create new sources if they already exist
                map.updateGeoJSONSource(
                    withId: getRouteSourceId(route.id),
                    data: createRouteSourceData(route: route, routesResponse: routesResponse)
                )
            } else {
                // Create a GeoJSON data source for each typical route pattern shape in this route
                var routeSource = GeoJSONSource(id: getRouteSourceId(route.id))
                routeSource.data = createRouteSourceData(route: route, routesResponse: routesResponse)
                do {
                    try map.addSource(routeSource)
                } catch {
                    let id = getRouteSourceId(route.id)
                    Logger().error("Failed to add route source \(id)\n\(error)")
                }

                do {
                    // Create a line layer for each route
                    if map.layerExists(withId: "puck") {
                        try map.addLayer(createRouteLayer(route: route), layerPosition: .below("puck"))
                    } else {
                        try map.addLayer(createRouteLayer(route: route))
                    }
                } catch {
                    let id = getRouteLayerId(route.id)
                    Logger().error("Failed to add route layer \(id)\n\(error)")
                }
            }
        }
    }

    func isNearbyNotFollowing() -> Bool {
        nearbyFetcher.loadedLocation != nil
            && nearbyFetcher.loadedLocation != locationDataManager.currentLocation?.coordinate
    }

    func updateStopOpacity(map: MapboxMap?, opacity: Double) {
        try? map?.updateLayer(withId: stopLayerId, type: SymbolLayer.self) { layer in
            if layer.iconOpacity != .constant(opacity) {
                layer.iconOpacity = .constant(opacity)
            }
        }
    }
}

struct RecenterButton: View {
    var perform: () -> Void
    var body: some View {
        Image(systemName: "location")
            .frame(width: 50, height: 50)
            .foregroundColor(.white)
            .background(.gray.opacity(0.8))
            .clipShape(Circle())
            .padding(20)
            .onTapGesture(perform: perform)
            .transition(AnyTransition.opacity.animation(.linear(duration: 0.25)))
            .accessibilityIdentifier("mapRecenterButton")
    }
}
