import os
import shared
import SwiftPhoenixClient
import SwiftUI

enum TestFakeError: Error {
    case thisIsATest
}

@main
struct IOSApp: App {
    let backend = Backend()

    // ignore updates less than 0.1km
    @StateObject var locationDataManager = LocationDataManager(distanceFilter: 100)

    @StateObject var globalFetcher: GlobalFetcher
    @StateObject var nearbyFetcher: NearbyFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher
    @StateObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @StateObject var searchResultFetcher: SearchResultFetcher
    @StateObject var socketProvider: SocketProvider

    init() {
        if let sentryDsn = Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN") as? String {
            let sentryEnv = Bundle.main.object(forInfoDictionaryKey: "SENTRY_ENVIRONMENT") as? String ?? "debug"
            AppSetupKt.initializeSentry(dsn: sentryDsn, environment: sentryEnv)
        } else {
            Logger().warning("skipping sentry initialization - SENTRY_DSN not configured")
        }

        do {
            throw TestFakeError.thisIsATest
        } catch {
            Sentry.shared.captureError(error: error)
        }

        let socket = Socket(SocketUtils.companion.url)
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

        _globalFetcher = StateObject(wrappedValue: GlobalFetcher(backend: backend))
        _nearbyFetcher = StateObject(wrappedValue: NearbyFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(socket: socket))
        _railRouteShapeFetcher = StateObject(wrappedValue: RailRouteShapeFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
        _socketProvider = StateObject(wrappedValue: SocketProvider(socket: socket))
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(locationDataManager)
                .environmentObject(globalFetcher)
                .environmentObject(nearbyFetcher)
                .environmentObject(predictionsFetcher)
                .environmentObject(railRouteShapeFetcher)
                .environmentObject(searchResultFetcher)
                .environmentObject(socketProvider)
        }
    }
}
