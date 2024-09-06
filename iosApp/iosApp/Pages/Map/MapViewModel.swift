//
//  MapViewModel.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-06-26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import Foundation
import shared
@_spi(Experimental) import MapboxMaps

class MapViewModel: ObservableObject {
    @Published var childStops: [String: Stop]?
    @Published var selectedVehicle: Vehicle?
    @Published var routeSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = []
    @Published var stopSourceData: StopSourceData = .init()
    @Published var stopMapData: StopMapResponse?
    @Published var allRailSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = []
    var snappedStopRouteLines: [RouteLineData] = []

    var lastMapboxErrorSubject: PassthroughSubject<Date?, Never>

    var layerManager: IMapLayerManager?
    var mapboxHttpInterceptor: MapHttpInterceptor?

    private var subscriptions = Set<AnyCancellable>()

    init(allRailSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = [],
         layerManager: IMapLayerManager? = nil,
         setHttpInterceptor: @escaping (_ interceptor: MapHttpInterceptor?) -> Void = { interceptor in
             HttpServiceFactory.setHttpServiceInterceptorForInterceptor(interceptor)
         }) {
        self.allRailSourceData = allRailSourceData
        self.layerManager = layerManager
        lastMapboxErrorSubject = .init()

        mapboxHttpInterceptor = MapHttpInterceptor(updateLastErrorTimestamp: {
            self.updateLastErrorTimestamp()
        })
        setHttpInterceptor(mapboxHttpInterceptor)
    }

    func updateLastErrorTimestamp() {
        lastMapboxErrorSubject.send(Date.now)
    }

    func updateRouteSource(routeLines: [RouteLineData]) {
        layerManager?
            .updateSourceData(routeData: RouteFeaturesBuilder.shared.buildCollection(routeLines: routeLines).toMapbox())
    }

    func updateStopSource(_ stopData: MapboxMaps.FeatureCollection) {
        layerManager?.updateSourceData(stopData: stopData)
    }

    func updateChildStopSource(_ childStopData: MapboxMaps.FeatureCollection) {
        layerManager?.updateSourceData(childStopData: childStopData)
    }
}
