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
    @Published var stopSourceData: StopSourceData = .init()
    @Published var stopMapData: StopMapResponse?
    @Published var allRailSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = []
    @Published var globalData: GlobalResponse?
    var stopUpdateTask: Task<Void, Error>?
    var routeUpdateTask: Task<Void, Error>?
    var snappedStopRouteLines: [RouteLineData] = []

    var lastMapboxErrorSubject: PassthroughSubject<Date?, Never>

    var layerManager: IMapLayerManager?
    var mapboxHttpInterceptor: MapHttpInterceptor?

    private var subscriptions = Set<AnyCancellable>()

    init(
        allRailSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = [],
        layerManager: IMapLayerManager? = nil,
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

    private func getLineFeatures(globalData: GlobalResponse?, globalMapData: GlobalMapData?) async throws -> MapboxMaps
        .FeatureCollection {
        try await RouteFeaturesBuilder.shared.buildCollection(
            routeLines: RouteFeaturesBuilder.shared.generateRouteLines(
                routeData: routeSourceData,
                globalData: globalData,
                globalMapData: globalMapData
            )
        ).toMapbox()
    }

    func updateRouteSource(globalData: GlobalResponse?, globalMapData: GlobalMapData?) {
        guard let layerManager else { return }
        if routeUpdateTask != nil, routeUpdateTask?.isCancelled != true {
            routeUpdateTask?.cancel()
        }
        routeUpdateTask = Task(priority: .high) {
            try Task.checkCancellation()
            let routeFeatures = try await self.getLineFeatures(globalData: globalData, globalMapData: globalMapData)
            try Task.checkCancellation()
            layerManager.updateSourceData(routeData: routeFeatures)
        }
    }

    private func getStopFeatures(globalMapData: GlobalMapData) async throws -> MapboxMaps.FeatureCollection {
        try await StopFeaturesBuilder.shared.buildCollection(
            stopData: stopSourceData,
            globalMapData: globalMapData,
            linesToSnap: snappedStopRouteLines
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
