import os
import shared
import SwiftPhoenixClient
import SwiftUI

@main
struct IOSApp: App {
    let backend: BackendProtocol = CommandLine.arguments.contains("-testing") ? IdleBackend() : Backend()

    // ignore updates less than 0.1km
    @StateObject var locationDataManager: LocationDataManager

    @StateObject var alertsFetcher: AlertsFetcher
    @StateObject var backendProvider: BackendProvider
    @StateObject var globalFetcher: GlobalFetcher
    @StateObject var nearbyFetcher: NearbyFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher
    @StateObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @StateObject var scheduleFetcher: ScheduleFetcher
    @StateObject var searchResultFetcher: SearchResultFetcher
    @StateObject var socketProvider: SocketProvider
    @StateObject var viewportProvider: ViewportProvider
    @StateObject var pinnedRouteRepositoryProvider: PinnedRouteRepositoryProvider
    @StateObject var togglePinnedRouteUsecaseProvider: TogglePinnedRouteUsecaseProvider

    init() {
        if let sentryDsn = Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN") as? String {
            let sentryEnv = Bundle.main.object(forInfoDictionaryKey: "SENTRY_ENVIRONMENT") as? String ?? "debug"
            AppSetupKt.initializeSentry(dsn: sentryDsn, environment: sentryEnv)
        } else {
            Logger().warning("skipping sentry initialization - SENTRY_DSN not configured")
        }
        KoinHelpersKt.doInitKoin()

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
        _locationDataManager = StateObject(wrappedValue: LocationDataManager(distanceFilter: 100))

        _alertsFetcher = StateObject(wrappedValue: AlertsFetcher(socket: socket))
        _backendProvider = StateObject(wrappedValue: BackendProvider(backend: backend))
        _globalFetcher = StateObject(wrappedValue: GlobalFetcher(backend: backend))
        _nearbyFetcher = StateObject(wrappedValue: NearbyFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(socket: socket))
        _railRouteShapeFetcher = StateObject(wrappedValue: RailRouteShapeFetcher(backend: backend))
        _scheduleFetcher = StateObject(wrappedValue: ScheduleFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
        _socketProvider = StateObject(wrappedValue: SocketProvider(socket: socket))
        _viewportProvider = StateObject(wrappedValue: ViewportProvider())
        let repository = PinnedRoutesRepositoryImpl(dataStore: createDataStore())
        let usecase = TogglePinnedRouteUsecase(repository: repository)
        _pinnedRouteRepositoryProvider = StateObject(wrappedValue: PinnedRouteRepositoryProvider(repository))
        _togglePinnedRouteUsecaseProvider = StateObject(wrappedValue: TogglePinnedRouteUsecaseProvider(usecase))
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(locationDataManager)
                .environmentObject(alertsFetcher)
                .environmentObject(backendProvider)
                .environmentObject(globalFetcher)
                .environmentObject(nearbyFetcher)
                .environmentObject(predictionsFetcher)
                .environmentObject(railRouteShapeFetcher)
                // TODO: Fully replace scheduleFetcher
                .environmentObject(scheduleFetcher)
                .environmentObject(searchResultFetcher)
                .environmentObject(socketProvider)
                .environmentObject(viewportProvider)
                .environmentObject(togglePinnedRouteUsecaseProvider)
                .environmentObject(pinnedRouteRepositoryProvider)
        }
    }
}
