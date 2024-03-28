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
    private let cameraDebouncer = Debouncer(delay: 0.25)
    private let layerInitDispatchGroup = DispatchGroup()

    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @ObservedObject var viewportProvider: ViewportProvider

    @State private var layerManager: MapLayerManager?
    @StateObject private var locationDataManager: LocationDataManager
    @State private var recenterButton: ViewAnnotation?

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
            .onCameraChanged { change in handleCameraChange(change) }
            .ornamentOptions(.init(scaleBar: .init(visibility: .hidden)))
            .onLayerTapGesture(StopLayerGenerator.getStopLayerId(.stop)) { _, _ in
                // Each stop feature has the stop ID as the identifier
                // We can also set arbitrary JSON properties if we need to
                // print(feature.feature.identifier)
                true
            }
            .accessibilityIdentifier("transitMap")
            .onAppear { handleAppear(location: proxy.location, map: proxy.map) }
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

    func handleAppear(location: LocationManager?, map: MapboxMap?) {
        // Wait for routes and stops to both load before initializing layers
        layerInitDispatchGroup.enter()
        Task {
            try await globalFetcher.getGlobalData()
            layerInitDispatchGroup.leave()
        }
        layerInitDispatchGroup.enter()
        Task {
            try await railRouteShapeFetcher.getRailRouteShapes()
            layerInitDispatchGroup.leave()
        }
        layerInitDispatchGroup.notify(queue: .main) {
            guard let map,
                  let globalResponse = globalFetcher.response,
                  let routeResponse = railRouteShapeFetcher.response
            else { return }
            handleLayerInit(map, globalResponse.stops, routeResponse)
        }

        // Set MapBox to use the current location to display puck
        location?.override(locationProvider: locationDataManager.$currentLocation.map {
            if let location = $0 {
                [Location(clLocation: location)]
            } else { [] }
        }.eraseToSignal())

        // If location data is provided, follow the user's location
        Task {
            if locationDataManager.currentLocation != nil {
                viewportProvider.follow(animation: .easeInOut(duration: .zero))
            }
        }

        didAppear?(self)
    }

    func handleCameraChange(_ change: CameraChanged) {
        guard let layerManager else { return }
        layerManager.updateStopLayers(change.cameraState.zoom)
        cameraDebouncer.debounce {
            viewportProvider.cameraState = change.cameraState
        }
    }

    func handleLayerInit(_ map: MapboxMap, _ stops: [Stop], _ routeResponse: RouteResponse) {
        let layerManager = MapLayerManager(map: map)

        let routeSourceGenerator = RouteSourceGenerator(routeData: routeResponse)
        let stopSourceGenerator = StopSourceGenerator(
            stops: stops,
            routeSourceDetails: routeSourceGenerator.routeSourceDetails
        )
        layerManager.addSources(sources: routeSourceGenerator.routeSources + stopSourceGenerator.stopSources)

        let routeLayerGenerator = RouteLayerGenerator(routeData: routeResponse)
        let stopLayerGenerator = StopLayerGenerator(stopLayerTypes: MapLayerManager.stopLayerTypes)
        layerManager.addLayers(layers: routeLayerGenerator.routeLayers + stopLayerGenerator.stopLayers)

        self.layerManager = layerManager
    }

    func isNearbyNotFollowing() -> Bool {
        nearbyFetcher.loadedLocation != nil
            && nearbyFetcher.loadedLocation != locationDataManager.currentLocation?.coordinate
    }
}
