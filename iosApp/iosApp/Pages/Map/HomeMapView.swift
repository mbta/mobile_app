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
    @State var lastNavEntry: SheetNavigationStackEntry?

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
            .onChange(of: lastNavEntry) { [oldNavEntry = lastNavEntry] nextNavEntry in
                handleLastNavChange(oldNavEntry: oldNavEntry, nextNavEntry: nextNavEntry)
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
        lastNavEntry = navigationStack.last
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
            selectedStop: lastNavEntry?.stop(),
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

        lastNavEntry = navigationStack.last
    }

    func handleLastNavChange(oldNavEntry: SheetNavigationStackEntry?, nextNavEntry: SheetNavigationStackEntry?) {
        if let nextNavEntry {
            switch nextNavEntry {
            case let .stopDetails(stop, filter):
                if oldNavEntry?.stop()?.id == stop.id {
                    handleRouteFilterChange(filter)
                } else {
                    handleStopDetailsChange(stop, filter)
                }
            }
        } else {
            clearSelectedStop()
        }
    }

    func handleStopDetailsChange(_ stop: Stop, _ filter: StopDetailsFilter?) {
        let updatedStopSources = StopSourceGenerator(
            stops: globalFetcher.stops,
            selectedStop: stop,
            routeLines: layerManager?.routeSourceGenerator?.routeLines,
            alertsByStop: currentStopAlerts
        )
        layerManager?.updateSourceData(stopSourceGenerator: updatedStopSources)
        viewportProvider.animateTo(coordinates: stop.coordinate, zoom: 17.0)

        Task {
            stopMapData = try await stopRepository.getStopMapData(stopId: stop.id)

            if let stopMapData {
                if let filter {
                    layerManager?.updateSourceData(routeSourceGenerator: filterRouteSourceData(stopMapData, filter))

                } else {
                    layerManager?.updateSourceData(routeSourceGenerator: filterRouteSourceToRail(stopMapData))
                }
            }
        }
    }

    func handleRouteFilterChange(_ filter: StopDetailsFilter?) {
        if let stopMapData {
            if let filter {
                layerManager?.updateSourceData(routeSourceGenerator: filterRouteSourceData(stopMapData, filter))

            } else {
                layerManager?.updateSourceData(routeSourceGenerator: filterRouteSourceToRail(stopMapData))
            }
        }
    }

    func clearSelectedStop() {
        stopMapData = nil
        // reset to showing all rail routes
        let updatedRouteSources = RouteSourceGenerator(
            routeData: railRouteShapeFetcher.response?.routesWithSegmentedShapes ?? [],
            routesById: globalFetcher.routes,
            stopsById: globalFetcher.stops,
            alertsByStop: currentStopAlerts
        )
        let updatedStopSources = StopSourceGenerator(
            stops: globalFetcher.stops,
            selectedStop: nil,
            routeLines: layerManager?.routeSourceGenerator?.routeLines,
            alertsByStop: currentStopAlerts
        )
        layerManager?.updateSourceData(routeSourceGenerator: updatedRouteSources, stopSourceGenerator: updatedStopSources)
    }

    private func filterRouteSourceToRail(_ stopMapResponse: StopMapResponse) -> RouteSourceGenerator {
        let stopRailRouteIds: Set<String> = Set(stopMapResponse.routeShapes.filter { routeWithShape in
            let maybeRouteType = globalFetcher.routes[routeWithShape.routeId]?.type
            if let routeType = maybeRouteType {
                return routeType == RouteType.heavyRail ||
                    routeType == RouteType.lightRail ||
                    routeType == RouteType.commuterRail
            } else {
                return false
            }
        }.map(\.routeId))
        return RouteSourceGenerator(
            routeData: (railRouteShapeFetcher.response?.routesWithSegmentedShapes ?? [])
                .filter { stopRailRouteIds.contains($0.routeId) },
            routesById: globalFetcher.routes,
            stopsById: globalFetcher.stops,
            alertsByStop: currentStopAlerts
        )
    }

    private func filterRouteSourceData(_ stopMapResponse: StopMapResponse, _ filter: StopDetailsFilter) -> RouteSourceGenerator {
        let targetRouteData = stopMapResponse.routeShapes.first { $0.routeId == filter.routeId }
        if let targetRouteData {
            let targetDirectionShapes = targetRouteData.segmentedShapes.filter { $0.directionId == filter.directionId }
            return RouteSourceGenerator(
                routeData: [.init(routeId: targetRouteData.routeId, segmentedShapes: targetDirectionShapes)],
                routesById: globalFetcher.routes,
                stopsById: globalFetcher.stops,
                alertsByStop: currentStopAlerts
            )

        } else {
            return RouteSourceGenerator(
                routeData: [],
                routesById: globalFetcher.routes,
                stopsById: globalFetcher.stops,
                alertsByStop: currentStopAlerts
            )
        }
    }

    func handleStopAlertChange(alertsByStop: [String: AlertAssociatedStop]) {
        let updatedStopSources = StopSourceGenerator(
            stops: globalFetcher.stops,
            selectedStop: lastNavEntry?.stop(),
            routeLines: layerManager?.routeSourceGenerator?.routeLines,
            alertsByStop: alertsByStop
        )
        if let existingRouteSourceGenerator = layerManager?.routeSourceGenerator {
            let updatedRouteSources = RouteSourceGenerator(routeData: existingRouteSourceGenerator.routeData,
                                                           routesById: globalFetcher.routes,
                                                           stopsById: globalFetcher.stops,
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
