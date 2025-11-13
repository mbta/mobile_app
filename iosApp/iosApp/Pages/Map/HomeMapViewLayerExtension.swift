//
//  HomeMapViewLayerExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import Shared
import SwiftUI

/*
 Functions for manipulating the layers displayed on the map.
 */
extension HomeMapView {
    @MainActor
    func handleTryLayerInit(map: MapboxMap?) async throws {
        guard let map else { return }
        try await handleLayerInit(map)
    }

    func handleAccessTokenLoaded(_ map: MapboxMap?) {
        map?.mapStyle = .init(uri: appVariant.styleUri(colorScheme: colorScheme))
    }

    @MainActor
    func handleLayerInit(_ map: MapboxMap) async throws {
        let layerManager = MapLayerManager(map: map)
        await layerManager.addIcons()
        try await layerManager.setUpAnchorLayers()
        mapVM.layerManagerInitialized(layerManager: layerManager)
    }
}
