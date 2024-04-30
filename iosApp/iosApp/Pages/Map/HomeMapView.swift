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

    @ObservedObject var alertsFetcher: AlertsFetcher
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @ObservedObject var vehiclesFetcher: VehiclesFetcher
    @ObservedObject var viewportProvider: ViewportProvider

    @StateObject private var locationDataManager: LocationDataManager
    @Binding var navigationStack: [SheetNavigationStackEntry]
    @Binding var sheetHeight: CGFloat

    @State private var layerManager: MapLayerManager?
    @State private var recenterButton: ViewAnnotation?
    @State private var now = Date.now
    @State private var currentStopAlerts: [String: AlertAssociatedStop] = [:]

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 300, on: .main, in: .common).autoconnect()
    let log = Logger()

    init(
        alertsFetcher: AlertsFetcher,
        globalFetcher: GlobalFetcher,
        nearbyFetcher: NearbyFetcher,
        railRouteShapeFetcher: RailRouteShapeFetcher,
        vehiclesFetcher: VehiclesFetcher,
        viewportProvider: ViewportProvider,
        locationDataManager: LocationDataManager = .init(distanceFilter: 1),
        navigationStack: Binding<[SheetNavigationStackEntry]>,
        sheetHeight: Binding<CGFloat>
    ) {
        self.alertsFetcher = alertsFetcher
        self.globalFetcher = globalFetcher
        self.nearbyFetcher = nearbyFetcher
        self.railRouteShapeFetcher = railRouteShapeFetcher
        self.vehiclesFetcher = vehiclesFetcher
        self.viewportProvider = viewportProvider
        _locationDataManager = StateObject(wrappedValue: locationDataManager)
        _navigationStack = navigationStack
        _sheetHeight = sheetHeight
    }

    var body: some View {
        MapReader { proxy in
            Map(viewport: $viewportProvider.viewport) {
                Puck2D().pulsing(.none)
                if isNearbyNotFollowing(), navigationStack.isEmpty {
                    MapViewAnnotation(coordinate: nearbyFetcher.loadedLocation!) {
                        Circle()
                            .strokeBorder(.white, lineWidth: 2.5)
                            .background(Circle().fill(.orange))
                            .frame(width: 22, height: 22)
                    }
                }
                if let filter = navigationStack.lastStopDetailsFilter, let vehicles = vehiclesFetcher.vehicles {
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
            .gestureOptions(.init(rotateEnabled: false, pitchEnabled: false))
            .mapStyle(.light)
            .onCameraChanged { change in handleCameraChange(change) }
            .ornamentOptions(.init(scaleBar: .init(visibility: .hidden)))
            .onLayerTapGesture(StopLayerGenerator.getStopLayerId(.stop), perform: handleStopLayerTap)
            .onLayerTapGesture(StopLayerGenerator.getStopLayerId(.station), perform: handleStopLayerTap)
            .additionalSafeAreaInsets(.bottom, sheetHeight)
            .accessibilityIdentifier("transitMap")
            .onAppear { handleAppear(location: proxy.location, map: proxy.map) }
            .onChange(of: globalFetcher.response) { _ in
                handleTryLayerInit(map: proxy.map)
                currentStopAlerts = globalFetcher.getRealtimeAlertsByStop(
                    alerts: alertsFetcher.alerts,
                    filterAtTime: now.toKotlinInstant()
                )
            }
            .onChange(of: railRouteShapeFetcher.response) { _ in
                handleTryLayerInit(map: proxy.map)
            }
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
            .onChange(of: navigationStack.lastStopDetailsFilter) { filter in
                if let filter {
                    vehiclesFetcher.run(routeId: filter.routeId, directionId: Int(filter.directionId))
                } else {
                    vehiclesFetcher.leave()
                }
            }
            .onDisappear {
                vehiclesFetcher.leave()
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
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
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

    func handleLayerInit(
        _ map: MapboxMap,
        _ stops: [String: Stop],
        _ routes: [String: Route],
        _ routeResponse: MapFriendlyRouteResponse
    ) {
        let layerManager = MapLayerManager(map: map)

        let routeSourceGenerator = RouteSourceGenerator(routeData: routeResponse, stopsById: stops,
                                                        alertsByStop: currentStopAlerts)
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
        let updatedStopSources = StopSourceGenerator(
            stops: globalFetcher.stops,
            routeSourceDetails: layerManager?.routeSourceGenerator?.routeSourceDetails,
            alertsByStop: alertsByStop
        )
        layerManager?.updateSourceData(stopSourceGenerator: updatedStopSources)
        if let railResponse = railRouteShapeFetcher.response {
            let updatedRouteSources = RouteSourceGenerator(routeData: railResponse, stopsById: globalFetcher.stops,
                                                           alertsByStop: alertsByStop)
            layerManager?.updateSourceData(routeSourceGenerator: updatedRouteSources)
        }
    }

    func handleStopLayerTap(feature: QueriedFeature, _: MapContentGestureContext) -> Bool {
        guard case let .string(stopId) = feature.feature.properties?[StopSourceGenerator.propIdKey] else {
            let featureId = feature.feature.identifier.debugDescription
            log.error("""
                Stop icon featureId=`\(featureId)` was tapped, but had invalid stop id prop. sourceId=\(feature.source)
            """)
            return true
        }
        guard let stop = globalFetcher.stops[stopId] else {
            let featureId = feature.feature.identifier.debugDescription
            log.error("""
                Stop icon featureId=`\(featureId)` was tapped but stopId=\(stopId) didn't exist in global stops.
            """)
            return true
        }

        navigationStack.removeAll()
        navigationStack.append(.stopDetails(stop, nil))
        return true
    }

    func isNearbyNotFollowing() -> Bool {
        nearbyFetcher.loadedLocation != nil
            && nearbyFetcher.loadedLocation != locationDataManager.currentLocation?.coordinate
    }
}
