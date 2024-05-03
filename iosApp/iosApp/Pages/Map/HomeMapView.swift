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
    @ObservedObject var alertsFetcher: AlertsFetcher
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @ObservedObject var vehiclesFetcher: VehiclesFetcher
    @ObservedObject var viewportProvider: ViewportProvider

    var stopRepository: IStopRepository
    @State var stopMapData: StopMapResponse?

    @StateObject private var locationDataManager: LocationDataManager
    @Binding var navigationStack: [SheetNavigationStackEntry]
    @Binding var sheetHeight: CGFloat

    @State private var layerManager: IMapLayerManager?
    @State private var recenterButton: ViewAnnotation?
    @State private var now = Date.now
    @State private var currentStopAlerts: [String: AlertAssociatedStop] = [:]
    @State var selectedStop: Stop?

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 300, on: .main, in: .common).autoconnect()
    let log = Logger()

    private var isNearbyNotFollowing: Bool {
        !viewportProvider.viewport.isFollowing && locationDataManager.currentLocation != nil && navigationStack.isEmpty
    }

    init(
        alertsFetcher: AlertsFetcher,
        globalFetcher: GlobalFetcher,
        nearbyFetcher: NearbyFetcher,
        railRouteShapeFetcher: RailRouteShapeFetcher,
        vehiclesFetcher: VehiclesFetcher,
        viewportProvider: ViewportProvider,
        stopRepository: IStopRepository = RepositoryDI().stop,
        locationDataManager: LocationDataManager = .init(distanceFilter: 1),
        navigationStack: Binding<[SheetNavigationStackEntry]>,
        sheetHeight: Binding<CGFloat>,
        layerManager: IMapLayerManager? = nil
    ) {
        self.alertsFetcher = alertsFetcher
        self.globalFetcher = globalFetcher
        self.nearbyFetcher = nearbyFetcher
        self.railRouteShapeFetcher = railRouteShapeFetcher
        self.vehiclesFetcher = vehiclesFetcher
        self.viewportProvider = viewportProvider
        self.stopRepository = stopRepository
        _locationDataManager = StateObject(wrappedValue: locationDataManager)
        _navigationStack = navigationStack
        _sheetHeight = sheetHeight
        _layerManager = State(wrappedValue: layerManager)
    }

    var body: some View {
        modifiedMap.overlay(alignment: .topTrailing) {
            if !viewportProvider.viewport.isFollowing, locationDataManager.currentLocation != nil {
                RecenterButton { Task { viewportProvider.follow() } }
            }
        }
    }

    @ViewBuilder
    var modifiedMap: some View {
        ProxyModifiedMap(mapContent: AnyView(annotatedMap), handleAppear: handleAppear,
                         handleTryLayerInit: handleTryLayerInit, globalFetcher: globalFetcher,
                         railRouteShapeFetcher: railRouteShapeFetcher)
            .onChange(of: alertsFetcher.alerts) { nextAlerts in
                currentStopAlerts = globalFetcher.getRealtimeAlertsByStop(
                    alerts: nextAlerts,
                    filterAtTime: now.toKotlinInstant()
                )
            }
            .onChange(of: currentStopAlerts) { nextStopAlerts in
                handleStopAlertChange(alertsByStop: nextStopAlerts)
            }
            .onChange(of: globalFetcher.response) { _ in
                currentStopAlerts = globalFetcher.getRealtimeAlertsByStop(
                    alerts: alertsFetcher.alerts,
                    filterAtTime: now.toKotlinInstant()
                )
            }
            .onChange(of: locationDataManager.authorizationStatus) { status in
                guard status == .authorizedAlways || status == .authorizedWhenInUse else { return }
                viewportProvider.follow(animation: .easeInOut(duration: 0))
            }
            .onChange(of: navigationStack) { nextNavStack in
                handleNavStackChange(navigationStack: nextNavStack)
            }
            .onChange(of: selectedStop) { nextSelectedStop in
                handleSelectedStopChange(selectedStop: nextSelectedStop)
            }
            .onDisappear {
                vehiclesFetcher.leave()
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .onReceive(timer) { input in
                now = input
                currentStopAlerts = globalFetcher.getRealtimeAlertsByStop(
                    alerts: alertsFetcher.alerts,
                    filterAtTime: now.toKotlinInstant()
                )
            }
    }

    @ViewBuilder
    var annotatedMap: some View {
        AnnotatedMap(
            stopMapData: stopMapData,
            filter: navigationStack.lastStopDetailsFilter,
            nearbyLocation: isNearbyNotFollowing ? nearbyFetcher.loadedLocation : nil,
            sheetHeight: sheetHeight,
            vehicles: vehiclesFetcher.vehicles,
            viewportProvider: viewportProvider,
            handleCameraChange: handleCameraChange,
            handleStopLayerTap: handleStopLayerTap
        )
    }

    var didAppear: ((Self) -> Void)?

    func handleAppear(location: LocationManager?, map _: MapboxMap?) {
        switch navigationStack.last {
        case let .stopDetails(stop, _):
            selectedStop = stop
        case _:
            selectedStop = nil
        }
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
        if locationDataManager.currentLocation != nil {
            viewportProvider.follow(animation: .easeInOut(duration: .zero))
        }

        didAppear?(self)
    }

    func handleCameraChange(_ change: CameraChanged) {
        guard let layerManager else { return }
        layerManager.updateStopLayerZoom(change.cameraState.zoom)
        viewportProvider.updateCameraState(change.cameraState)
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

        let routeSourceGenerator = RouteSourceGenerator(
            routeData: routeResponse.routesWithSegmentedShapes,
            routesById: routes,
            stopsById: stops,
            alertsByStop: currentStopAlerts
        )
        let stopSourceGenerator = StopSourceGenerator(
            stops: stops,
            selectedStop: selectedStop,
            routeLines: routeSourceGenerator.routeLines,
            alertsByStop: currentStopAlerts
        )

        layerManager.addSources(
            routeSourceGenerator: routeSourceGenerator,
            stopSourceGenerator: stopSourceGenerator
        )

        layerManager.addLayers(
            routeLayerGenerator: RouteLayerGenerator(),
            stopLayerGenerator: StopLayerGenerator(stopLayerTypes: MapLayerManager.stopLayerTypes)
        )

        layerManager.updateStopLayerZoom(map.cameraState.zoom)

        self.layerManager = layerManager
    }

    func handleNavStackChange(navigationStack: [SheetNavigationStackEntry]) {
        if let filter = navigationStack.lastStopDetailsFilter {
            vehiclesFetcher.run(routeId: filter.routeId, directionId: Int(filter.directionId))
        } else {
            vehiclesFetcher.leave()
        }

        switch navigationStack.last {
        case let .stopDetails(stop, _):
            selectedStop = stop
        case _:
            selectedStop = nil
        }
    }

    func handleSelectedStopChange(selectedStop: Stop?) {
        let updatedStopSources = StopSourceGenerator(
            stops: globalFetcher.stops,
            selectedStop: selectedStop,
            routeLines: layerManager?.routeSourceGenerator?.routeLines,
            alertsByStop: currentStopAlerts
        )
        layerManager?.updateSourceData(stopSourceGenerator: updatedStopSources)
        stopMapData = nil
        if let selectedStop {
            viewportProvider.animateTo(coordinates: selectedStop.coordinate, zoom: 17.0)
            Task {
                stopMapData = try await stopRepository.getStopMapData(stopId: selectedStop.id)
            }
        }
    }

    func handleStopAlertChange(alertsByStop: [String: AlertAssociatedStop]) {
        let updatedStopSources = StopSourceGenerator(
            stops: globalFetcher.stops,
            selectedStop: selectedStop,
            routeLines: layerManager?.routeSourceGenerator?.routeLines,
            alertsByStop: alertsByStop
        )
        layerManager?.updateSourceData(stopSourceGenerator: updatedStopSources)
        if let railResponse = railRouteShapeFetcher.response {
            let updatedRouteSources = RouteSourceGenerator(
                routeData: railResponse.routesWithSegmentedShapes,
                routesById: globalFetcher.routes,
                stopsById: globalFetcher.stops,
                alertsByStop: alertsByStop
            )
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
}

struct ProxyModifiedMap: View {
    var mapContent: AnyView
    var handleAppear: (_ location: LocationManager?, _ map: MapboxMap?) -> Void
    var handleTryLayerInit: (_ map: MapboxMap?) -> Void
    var globalFetcher: GlobalFetcher
    var railRouteShapeFetcher: RailRouteShapeFetcher

    var body: some View {
        MapReader { proxy in
            mapContent
                .onAppear { handleAppear(proxy.location, proxy.map) }
                .onChange(of: globalFetcher.response) { _ in
                    handleTryLayerInit(proxy.map)
                }
                .onChange(of: railRouteShapeFetcher.response) { _ in
                    handleTryLayerInit(proxy.map)
                }
        }
    }
}
