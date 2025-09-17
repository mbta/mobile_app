//
//  ProductionAppView.swift
//  iosApp
//
//  Created by Brady, Kayla on 4/24/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import Shared
import SwiftPhoenixClient
import SwiftUI

#if DEVORANGE
    let appVariant = AppVariant.devOrange
#elseif STAGING
    let appVariant = AppVariant.staging
#elseif PROD
    let appVariant = AppVariant.prod
#endif

struct ProductionAppView: View {
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
            Self.initKoin(socket: socket)
            self.init(socket: socket)
        }
    }

    init(socket: PhoenixSocket) {
        // ignore updates less than 10m
        _locationDataManager = StateObject(wrappedValue: LocationDataManager(distanceFilter: 10))
        _socketProvider = StateObject(wrappedValue: SocketProvider(socket: socket))
        _viewportProvider = StateObject(wrappedValue: ViewportProvider())
    }

    var body: some View {
        SettingsCacheProvider {
            ContentView(contentVM: contentVM)
                .font(Typography.body)
                .environmentObject(locationDataManager)
                .environmentObject(socketProvider)
                .environmentObject(viewportProvider)
        }
    }

    private static func initSocket() -> PhoenixSocket {
        let socket = Socket(appVariant.socketUrl)
        socket.withRawMessages()
        socket.onOpen {
            Logger().debug("Socket opened")

            Sentry.shared.addBreadcrumb(breadcrumb: .init(level: .info,
                                                          type: nil,
                                                          message: "socket opened",
                                                          category: "socket",
                                                          data: nil))
        }
        socket.onClose {
            Logger().debug("Socket closed")
            Sentry.shared.addBreadcrumb(breadcrumb: .init(level: .info,
                                                          type: nil,
                                                          message: "socket closed",
                                                          category: "socket",
                                                          data: nil))
        }

        socket.onError { error, response in
            Logger().debug("socket error: \(error) \(response)")
            Sentry.shared.addBreadcrumb(breadcrumb: .init(level: .info,
                                                          type: nil,
                                                          message: "socket error",
                                                          category: "socket",
                                                          data: ["error": error, "response": response]))
        }

        return socket
    }

    private static func initKoin(socket: PhoenixSocket) {
        let nativeModule: Koin_coreModule = MakeNativeModuleKt.makeNativeModule(
            accessibilityStatus: AccessibilityStatusRepository(),
            analytics: AnalyticsProvider.shared,
            currentAppVersion: CurrentAppVersionRepository(),
            networkConnectivityMonitor: NetworkConnectivityMonitor(),
            socket: socket,
        )
        HelpersKt.doInitKoin(appVariant: appVariant, nativeModule: nativeModule)
    }

    private static func initSentry() {
        if let sentryDsn = Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN_IOS") as? String {
            let sentryEnv = Bundle.main.object(forInfoDictionaryKey: "SENTRY_ENVIRONMENT") as? String ?? "debug"
            AppSetupKt.initializeSentry(dsn: sentryDsn, environment: sentryEnv)
        } else {
            Logger().warning("skipping sentry initialization - SENTRY_DSN_IOS not configured")
        }
    }
}
