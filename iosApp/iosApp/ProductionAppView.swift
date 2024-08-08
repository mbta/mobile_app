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
    // ignore updates less than 0.1km
    @StateObject var locationDataManager: LocationDataManager

    @StateObject var contentVM: ContentViewModel = .init()
    @StateObject var socketProvider: SocketProvider
    @StateObject var viewportProvider: ViewportProvider

    init() {
        Self.initSentry()
        if CommandLine.arguments.contains("--default-mocks") {
            HelpersKt.startKoinIOSTestApp()
            self.init(socket: MockSocket())
        } else if CommandLine.arguments.contains("--e2e-mocks") {
            HelpersKt.startKoinE2E()
            self.init(socket: MockSocket())
        } else {
            let socket = Self.initSocket()
            Self.initKoin(appCheck: AppCheckRepository(), socket: socket)
            self.init(socket: socket)
        }
    }

    init(socket: PhoenixSocket) {
        _locationDataManager = StateObject(wrappedValue: LocationDataManager(distanceFilter: 100))
        _socketProvider = StateObject(wrappedValue: SocketProvider(socket: socket))
        _viewportProvider = StateObject(wrappedValue: ViewportProvider())
    }

    var body: some View {
        ContentView(contentVM: contentVM)
            .font(Typography.body)
            .environmentObject(locationDataManager)
            .environmentObject(socketProvider)
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
