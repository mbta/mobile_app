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

    @StateObject var backendProvider: BackendProvider
    @StateObject var contentVM: ContentViewModel = .init()
    @StateObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @StateObject var searchResultFetcher: SearchResultFetcher
    @StateObject var socketProvider: SocketProvider
    @StateObject var vehiclesFetcher: VehiclesFetcher
    @StateObject var viewportProvider: ViewportProvider

    init() {
        Self.initSentry()
        let socket = Self.initSocket()
        Self.initKoin(appCheck: AppCheckRepository(), socket: socket)
        self.init(socket: socket)
    }

    init(socket: PhoenixSocket) {
        let backend = backend
        _locationDataManager = StateObject(wrappedValue: LocationDataManager(distanceFilter: 100))
        _backendProvider = StateObject(wrappedValue: BackendProvider(backend: backend))
        _railRouteShapeFetcher = StateObject(wrappedValue: RailRouteShapeFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
        _socketProvider = StateObject(wrappedValue: SocketProvider(socket: socket))
        _vehiclesFetcher = StateObject(wrappedValue: VehiclesFetcher(socket: socket))
        _viewportProvider = StateObject(wrappedValue: ViewportProvider())
    }

    var body: some View {
        ContentView(contentVM: contentVM)
            .font(Typography.body)
            .environmentObject(locationDataManager)
            .environmentObject(backendProvider)
            .environmentObject(railRouteShapeFetcher)
            .environmentObject(searchResultFetcher)
            .environmentObject(socketProvider)
            .environmentObject(vehiclesFetcher)
            .environmentObject(viewportProvider)
    }

    private static func initSocket() -> PhoenixSocket {
        let socket = Socket(appVariant.socketUrl)
        socket.withRawMessages()
        socket.onOpen {
            Logger().debug("Socket opened")
        }
        socket.onClose {
            Logger().debug("Socket closed")
        }
        return socket
    }

    private static func initKoin(appCheck: IAppCheckRepository, socket: PhoenixSocket) {
        let nativeModule: Koin_coreModule = MakeNativeModuleKt.makeNativeModule(appCheck: appCheck, socket: socket)
        HelpersKt.doInitKoin(appVariant: appVariant, nativeModule: nativeModule)
    }

    private static func initSentry() {
        if let sentryDsn = Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN") as? String {
            let sentryEnv = Bundle.main.object(forInfoDictionaryKey: "SENTRY_ENVIRONMENT") as? String ?? "debug"
            AppSetupKt.initializeSentry(dsn: sentryDsn, environment: sentryEnv)
        } else {
            Logger().warning("skipping sentry initialization - SENTRY_DSN not configured")
        }
    }
}
