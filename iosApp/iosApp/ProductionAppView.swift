//
//  ProductionAppView.swift
//  iosApp
//
//  Created by Brady, Kayla on 4/24/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import shared
import SwiftPhoenixClient
import SwiftUI

#if STAGING
    let appVariant = AppVariant.staging
#elseif PROD
    let appVariant = AppVariant.prod
#endif

struct ProductionAppView: View {
    let backend: BackendProtocol =
        if CommandLine.arguments.contains("-testing") {
            IdleBackend()
        } else {
            Backend(appVariant: appVariant)
        }

    // ignore updates less than 0.1km
    @StateObject var locationDataManager: LocationDataManager

    @StateObject var alertsFetcher: AlertsFetcher
    @StateObject var backendProvider: BackendProvider
    @StateObject var globalFetcher: GlobalFetcher
    @StateObject var nearbyFetcher: NearbyFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher
    @StateObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @StateObject var searchResultFetcher: SearchResultFetcher
    @StateObject var socketProvider: SocketProvider
    @StateObject var viewportProvider: ViewportProvider

    init() {
        if let sentryDsn = Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN") as? String {
            let sentryEnv = Bundle.main.object(forInfoDictionaryKey: "SENTRY_ENVIRONMENT") as? String ?? "debug"
            AppSetupKt.initializeSentry(dsn: sentryDsn, environment: sentryEnv)
        } else {
            Logger().warning("skipping sentry initialization - SENTRY_DSN not configured")
        }

        HelpersKt.doInitKoin(appVariant: appVariant)

        let socket = Socket(appVariant.socketUrl)
        socket.withRawMessages()
        socket.onOpen {
            Logger().debug("Socket opened")
        }
        socket.onClose {
            Logger().debug("Socket closed")
        }
        self.init(socket: socket)
    }

    init(socket: PhoenixSocket) {
        let backend = backend
        _locationDataManager = StateObject(wrappedValue: LocationDataManager(distanceFilter: 100))

        _alertsFetcher = StateObject(wrappedValue: AlertsFetcher(socket: socket))
        _backendProvider = StateObject(wrappedValue: BackendProvider(backend: backend))
        _globalFetcher = StateObject(wrappedValue: GlobalFetcher(backend: backend))
        _nearbyFetcher = StateObject(wrappedValue: NearbyFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(socket: socket))
        _railRouteShapeFetcher = StateObject(wrappedValue: RailRouteShapeFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
        _socketProvider = StateObject(wrappedValue: SocketProvider(socket: socket))
        _viewportProvider = StateObject(wrappedValue: ViewportProvider())
    }

    var body: some View {
        ContentView()
            .environmentObject(locationDataManager)
            .environmentObject(alertsFetcher)
            .environmentObject(backendProvider)
            .environmentObject(globalFetcher)
            .environmentObject(nearbyFetcher)
            .environmentObject(predictionsFetcher)
            .environmentObject(railRouteShapeFetcher)
            .environmentObject(searchResultFetcher)
            .environmentObject(socketProvider)
            .environmentObject(viewportProvider)
    }
}