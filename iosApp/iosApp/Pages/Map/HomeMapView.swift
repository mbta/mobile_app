//
//  HomeMapView.swift
//  iosApp
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Algorithms
import os
import Polyline
import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

struct HomeMapView: View {
    static let stopZoomThreshold: CGFloat = ViewportProvider.defaultZoom - 0.25

    private let routeLayerId = "route-layer"
    private let routeSourceId = "route-source"
    private let stopLayerId = "stop-layer"
    private let stopSourceId = "stop-source"
    private let stopIconId = "t-logo"

    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @ObservedObject var viewportProvider: ViewportProvider

    @StateObject private var locationDataManager: LocationDataManager
    @State var recenterButton: ViewAnnotation?

    init(
        globalFetcher: GlobalFetcher,
        railRouteShapeFetcher: RailRouteShapeFetcher,
        locationDataManager: LocationDataManager = .init(distanceFilter: 1),
        viewportProvider: ViewportProvider
    ) {
        self.railRouteShapeFetcher = railRouteShapeFetcher
        self.globalFetcher = globalFetcher
        self.viewportProvider = viewportProvider
        _locationDataManager = StateObject(wrappedValue: locationDataManager)
    }

    var body: some View {
        MapReader { proxy in
            Map(viewport: $viewportProvider.viewport) {
                Puck2D().pulsing(.none)
            }
            .gestureOptions(.init(rotateEnabled: false, pitchEnabled: false))
            .mapStyle(.light)
            .onCameraChanged { change in
                updateStopOpacity(
                    map: proxy.map,
                    opacity: change.cameraState.zoom > HomeMapView.stopZoomThreshold ? 1 : 0
                )
            }
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
            .onChange(of: globalFetcher.stops) { _ in handleRouteResponse(proxy.map, railRouteShapeFetcher.response) }
            .onChange(of: railRouteShapeFetcher.response) { response in handleRouteResponse(proxy.map, response) }
            .overlay(alignment: .topTrailing) {
                if !viewportProvider.viewport.isFollowing, locationDataManager.currentLocation != nil {
                    RecenterButton { viewportProvider.follow() }
                }
            }
        }
    }

    var didAppear: ((Self) -> Void)?

    func createRouteLayer(route: Route) -> Layer {
        var routeLayer = LineLayer(id: getRouteLayerId(route.id), source: getRouteSourceId(route.id))
        routeLayer.lineWidth = .constant(5.0)
        routeLayer.lineColor = .expression(Exp(.match) {
            Exp(.get) { "LineType" }
            "Normal"

            UIColor(hex: route.color)
            "Alert"
            UIColor(.cyan)
            UIColor(hex: route.color)
        })
        routeLayer.lineBorderWidth = .constant(1.0)
        routeLayer.lineBorderColor = .constant(StyleColor(.white))
        /*   routeLayer.lineDasharray = .expression(Exp(.match) {
             Exp(.get) { "LineType" }
             "Normal"

             []
             "Alert"
             [5, 5]
             []
         })*/
        routeLayer.lineJoin = .constant(.round)
        routeLayer.lineCap = .constant(.round)
        return routeLayer
    }

    func affectedStops(alert: shared.Alert) -> [String: Stop] {
        let stops: [Stop] = alert.informedEntity.compactMap { informedEntity in
            informedEntity.stop != nil ? globalFetcher.stopsById[informedEntity.stop!] : nil
        }

        return Dictionary(uniqueKeysWithValues: stops.map { ($0.id, $0) })
    }

    func splitAlertIntoStopBoundaries(alert: shared.Alert, routePattern: RoutePattern) -> [(Bool, ArraySlice<String>)] {
        let stopsAffectedByAlert: [String: Stop] = affectedStops(alert: alert)

        // TODO: should be grouping by informedEntity more, or safe to assume it will always be the same?
        let routeIdForAlert = alert.informedEntity[0].route!
        let trip: Trip? = railRouteShapeFetcher.response?.trips[routePattern.representativeTripId]
        // print("TRIP FOUND \(trip?.id) \(trip?.stopIds)")
        // hardcoding canonical-Orange-C1-0 stops for now
        // stop ids not included by default https://api-v3.mbta.com/trips/canonical-Orange-C1-0?include=stops&fields[stop]=id
        let tripStopIds = ["70036", "70034", "70032", "70278", "70030", "70028", "70026", "70024", "70022", "70020", "70018", "70016", "70014", "70012", "70010", "70008", "70006", "70004", "70002", "70001"]

        let stopIdsChunkedByAlert = tripStopIds.chunked(on: { stopsAffectedByAlert[$0] != nil })
        return stopIdsChunkedByAlert ?? []
    }

    func splitRouteIntoSegments(routePattern: RoutePattern, trip _: Trip, shape: shared.Shape) -> [Feature] {
        let polyline = Polyline(encodedPolyline: shape.polyline!)
        if routePattern.representativeTripId == "canonical-Orange-C1-0" {
            print("TRYING OL alert split")

            let alert = olShuttleAlert
            let splitBoundaries = splitAlertIntoStopBoundaries(alert: alert, routePattern: routePattern)
            print(splitBoundaries)
        }

        let half1: [LocationCoordinate2D] = Array(polyline.coordinates!.prefix(polyline.coordinates!.count / 2))
        let half2: [LocationCoordinate2D] = Array(polyline.coordinates!.suffix(polyline.coordinates!.count / 2))

        var feature1 = Feature(geometry: LineString(half1))
        feature1.properties = ["LineType": "Normal"]

        var feature2 = Feature(geometry: LineString(half2))
        feature2.properties = ["LineType": "Alert"]

        return [feature1, feature2]
    }

    func createRouteSourceData(route: Route, routesResponse: RouteResponse) -> GeoJSONSourceData {
        let routeFeatures: [Feature] = route.routePatternIds!
            .map { patternId -> RoutePattern? in
                routesResponse.routePatterns[patternId]
            }
            .filter { pattern in
                pattern?.typicality == .typical && pattern?.directionId == 0
            }
            .flatMap { pattern in
                guard let pattern,
                      let representativeTrip = routesResponse.trips[pattern.representativeTripId],
                      let shapeId = representativeTrip.shapeId,
                      let shape = routesResponse.shapes[shapeId] else { return [] as [Feature] }
                return splitRouteIntoSegments(routePattern: pattern, trip: representativeTrip, shape: shape)
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

        viewportProvider.follow(animation: .default(maxDuration: 0))

        Task {
            try await globalFetcher.getGlobalData()
        }
        Task {
            try await railRouteShapeFetcher.getRailRouteShapes()
        }

        didAppear?(self)
    }

    func handleGlobalStops(_ possibleMap: MapboxMap?, _ stops: [Stop]) {
        guard let map = possibleMap else {
            return
        }
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
        guard let map = possibleMap else {
            return
        }
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
