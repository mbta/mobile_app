//
//  HomeMapView.swift
//  iosApp
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import OSLog
import Polyline
import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

struct HomeMapView: View {
    private let cameraDebouncer = Debouncer(delay: 0.25)

    @ObservedObject var alertsFetcher: AlertsFetcher
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @ObservedObject var viewportProvider: ViewportProvider

    @State private var layerManager: MapLayerManager?
    @StateObject private var locationDataManager: LocationDataManager
    @State private var recenterButton: ViewAnnotation?
    @State private var now = Date.now
    @State private var currentStopAlerts: [String: AlertAssociatedStop] = [:]
    @Binding var sheetHeight: CGFloat

    let timer = Timer.publish(every: 300, on: .main, in: .common).autoconnect()

    init(
        alertsFetcher: AlertsFetcher,
        globalFetcher: GlobalFetcher,
        nearbyFetcher: NearbyFetcher,
        railRouteShapeFetcher: RailRouteShapeFetcher,
        locationDataManager: LocationDataManager = .init(distanceFilter: 1),
        viewportProvider: ViewportProvider,
        sheetHeight: Binding<CGFloat>
    ) {
        self.alertsFetcher = alertsFetcher
        self.globalFetcher = globalFetcher
        self.nearbyFetcher = nearbyFetcher
        self.railRouteShapeFetcher = railRouteShapeFetcher
        self.viewportProvider = viewportProvider
        _sheetHeight = sheetHeight
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
            .onLayerTapGesture(StopLayerGenerator.getStopLayerId(.stop), perform: handleStopLayerTap)
            .onLayerTapGesture(StopLayerGenerator.getStopLayerId(.station), perform: handleStopLayerTap)
            .additionalSafeAreaInsets(.bottom, sheetHeight)
            .accessibilityIdentifier("transitMap")
            .onAppear { handleAppear(location: proxy.location, map: proxy.map) }
            .onChange(of: globalFetcher.response) { _ in handleTryLayerInit(map: proxy.map) }
            .onChange(of: railRouteShapeFetcher.response) { _ in handleTryLayerInit(map: proxy.map) }
            .onChange(of: locationDataManager.authorizationStatus) { status in
                if status == .authorizedAlways || status == .authorizedWhenInUse {
                    Task { viewportProvider.follow(animation: .easeInOut(duration: 0)) }
                }
            }
            .onChange(of: alertsFetcher.alerts) { nextAlerts in
                currentStopAlerts = globalFetcher.getRealtimeAlertsByStop(
                    alerts: nextAlerts,
                    filterAtTime: now.toKotlinInstant()
                )
            }
            .onReceive(timer) { input in
                now = input
                currentStopAlerts = globalFetcher.getRealtimeAlertsByStop(
                    alerts: alertsFetcher.alerts,
                    filterAtTime: now.toKotlinInstant()
                )
            }
            .onChange(of: currentStopAlerts) { nextStopAlerts in
                handleStopAlertChange(alertsByStop: nextStopAlerts)
            }
            .overlay(alignment: .topTrailing) {
                if !viewportProvider.viewport.isFollowing, locationDataManager.currentLocation != nil {
                    RecenterButton { Task { viewportProvider.follow() } }
                }
            }
        }
    }

    var didAppear: ((Self) -> Void)?

    func handleAppear(location: LocationManager?, map _: MapboxMap?) {
        Task {
            try await railRouteShapeFetcher.getRailRouteShapes()
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
        layerManager.updateStopLayerZoom(change.cameraState.zoom)
        cameraDebouncer.debounce {
            viewportProvider.cameraState = change.cameraState
        }
    }

    func handleTryLayerInit(map: MapboxMap?) {
        guard let map,
              let globalResponse = globalFetcher.response,
              let routeResponse = railRouteShapeFetcher.response
        else {
            return
        }

        handleLayerInit(map, globalResponse.stops, globalResponse.routes, routeResponse)
    }

    func handleLayerInit(_ map: MapboxMap, _ stops: [String: Stop], _ routes: [String: Route], _ routeResponse: MapFriendlyRouteResponse) {
        let layerManager = MapLayerManager(map: map)

        let routeSourceGenerator = RouteSourceGenerator(routeData: routeResponse, stopsById: stops)
        layerManager.addSources(
            routeSourceGenerator: routeSourceGenerator,
            stopSourceGenerator: StopSourceGenerator(
                stops: stops,
                routeSourceDetails: routeSourceGenerator.routeSourceDetails,
                alertsByStop: currentStopAlerts
            )
        )

        layerManager.addLayers(
            routeLayerGenerator: RouteLayerGenerator(mapFriendlyRoutesResponse: routeResponse, routesById: routes),
            stopLayerGenerator: StopLayerGenerator(stopLayerTypes: MapLayerManager.stopLayerTypes)
        )

        self.layerManager = layerManager
    }

    func handleStopAlertChange(alertsByStop: [String: AlertAssociatedStop]) {
        let updatedSources = StopSourceGenerator(
            stops: globalFetcher.stops,
            routeSourceDetails: layerManager?.routeSourceGenerator?.routeSourceDetails,
            alertsByStop: alertsByStop
        )
        layerManager?.updateSourceData(stopSourceGenerator: updatedSources)
    }

    func handleStopLayerTap(feature _: QueriedFeature, _: MapContentGestureContext) -> Bool {
        // Each stop feature has the stop ID as the identifier
        // We can also set arbitrary JSON properties if we need to
        true
    }

    func isNearbyNotFollowing() -> Bool {
        nearbyFetcher.loadedLocation != nil
            && nearbyFetcher.loadedLocation != locationDataManager.currentLocation?.coordinate
    }
}
