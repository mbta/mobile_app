//
//  MapboxConfigManager.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-06-26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import Foundation
@_spi(Experimental) import MapboxMaps
import os
import Shared

protocol IMapboxConfigManager {
    var lastMapboxErrorSubject: PassthroughSubject<Date?, Never> { get }

    func loadConfig() async
}

class MapboxConfigManager: ObservableObject, IMapboxConfigManager {
    let configUsecase: ConfigUseCase
    let lastMapboxErrorSubject: PassthroughSubject<Date?, Never>
    let configureMapboxToken: (String) -> Void
    var mapboxHttpInterceptor: MapHttpInterceptor?

    init(
        configUsecase: ConfigUseCase = UsecaseDI().configUsecase,
        setHttpInterceptor: @escaping (_ interceptor: MapHttpInterceptor?) -> Void = { interceptor in
            HttpServiceFactory.setHttpServiceInterceptorForInterceptor(interceptor)
        },
        configureMapboxToken: @escaping (String) -> Void = { token in MapboxOptions.accessToken = token }
    ) {
        self.configUsecase = configUsecase
        lastMapboxErrorSubject = .init()
        self.configureMapboxToken = configureMapboxToken
        mapboxHttpInterceptor = MapHttpInterceptor(updateLastErrorTimestamp: {
            self.updateLastErrorTimestamp()
        })
        setHttpInterceptor(mapboxHttpInterceptor)
    }

    @MainActor func loadConfig() async {
        do {
            let config = try await configUsecase.getConfig()
            switch onEnum(of: config) {
            case let .ok(result):
                configureMapboxToken(result.data.mapboxPublicToken)
            default: break
            }
        } catch {
            Logger().error("Failed to load Mapbox config")
        }
    }

    private func updateLastErrorTimestamp() {
        lastMapboxErrorSubject.send(Date.now)
    }
}
