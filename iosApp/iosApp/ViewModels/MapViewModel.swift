//
//  MapViewModel.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-06-26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import Foundation
@_spi(Experimental) import MapboxMaps
import Shared

class MapViewModel: ObservableObject {
    @Published var selectedVehicle: Vehicle?
    @Published var routeSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = []
    @Published var stopLayerState: StopLayerGenerator.State = .init()
    @Published var stopMapData: StopMapResponse?
    @Published var allRailSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = []
    @Published var globalData: GlobalResponse?
    var stopUpdateTask: Task<Void, Error>?
    var routeUpdateTask: Task<Void, Error>?
    var snappedStopRouteSources: [RouteSourceData] = []

    var lastMapboxErrorSubject: PassthroughSubject<Date?, Never>

    var layerManager: iosApp.IMapLayerManager?
    var mapboxHttpInterceptor: MapHttpInterceptor?

    private var subscriptions = Set<AnyCancellable>()

    init(
        allRailSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = [],
        layerManager: iosApp.IMapLayerManager? = nil,
        setHttpInterceptor: @escaping (_ interceptor: MapHttpInterceptor?) -> Void = { interceptor in
            HttpServiceFactory.setHttpServiceInterceptorForInterceptor(interceptor)
        },
        globalData: GlobalResponse? = nil
    ) {
        self.allRailSourceData = allRailSourceData
        self.layerManager = layerManager
        self.globalData = globalData
        lastMapboxErrorSubject = .init()

        mapboxHttpInterceptor = MapHttpInterceptor(updateLastErrorTimestamp: {
            self.updateLastErrorTimestamp()
        })
        setHttpInterceptor(mapboxHttpInterceptor)
    }

    func updateLastErrorTimestamp() {
        lastMapboxErrorSubject.send(Date.now)
    }

    func updateSources(globalData: GlobalResponse?, globalMapData: GlobalMapData?) {
        updateStopSource(globalMapData: globalMapData)
        updateRouteSource(globalData: globalData, globalMapData: globalMapData)
    }

    private func getRouteSources(globalData: GlobalResponse?,
                                 globalMapData: GlobalMapData?) async throws -> [RouteSourceData] {
        guard let globalData else { return [] }
        return try await RouteFeaturesBuilder.shared.generateRouteSources(
            routeData: routeSourceData,
            globalData: globalData,
            globalMapData: globalMapData
        )
    }

    func updateRouteSource(globalData: GlobalResponse?, globalMapData: GlobalMapData?) {
        guard let layerManager else { return }
        if routeUpdateTask != nil, routeUpdateTask?.isCancelled != true {
            routeUpdateTask?.cancel()
        }
        routeUpdateTask = Task(priority: .high) {
            try Task.checkCancellation()
            let routeSources = try await self.getRouteSources(globalData: globalData, globalMapData: globalMapData)
            try Task.checkCancellation()
            layerManager.updateSourceData(routeData: routeSources)
        }
    }

    private func getStopFeatures(globalMapData: GlobalMapData) async throws -> MapboxMaps.FeatureCollection {
        try await StopFeaturesBuilder.shared.buildCollection(
            globalMapData: globalMapData,
            routeSourceDetails: snappedStopRouteSources
        ).toMapbox()
    }

    func updateStopSource(globalMapData: GlobalMapData?) {
        guard let globalMapData, let layerManager else { return }
        if stopUpdateTask != nil, stopUpdateTask?.isCancelled != true {
            stopUpdateTask?.cancel()
        }
        stopUpdateTask = Task(priority: .high) {
            try Task.checkCancellation()
            let stopFeatures = try await self.getStopFeatures(globalMapData: globalMapData)
            try Task.checkCancellation()
            layerManager.updateSourceData(stopData: stopFeatures)
        }
    }
}
