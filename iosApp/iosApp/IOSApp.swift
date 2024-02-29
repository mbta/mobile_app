import os
import Sentry
import shared
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

    init() {
        let backend = backend

        if let sentryDsn = Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN") as? String {
            let sentryEnv = Bundle.main.object(forInfoDictionaryKey: "SENTRY_ENVIRONMENT") as? String ?? "debug"
            AppSetupKt.initializeSentry(dsn: sentryDsn, environment: sentryEnv)
        } else {
            Logger().warning("skipping sentry initialization - SENTRY_DSN not configured")
        }

        do {
            throw TestFakeError.thisIsATest
        } catch {
            SentrySDK.capture(error: error)
        }

        _globalFetcher = StateObject(wrappedValue: GlobalFetcher(backend: backend))
        _nearbyFetcher = StateObject(wrappedValue: NearbyFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(backend: backend))
        _railRouteShapeFetcher = StateObject(wrappedValue: RailRouteShapeFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
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
                .task {
                    do {
                        try await backend.runSocket()
                    } catch {
                        predictionsFetcher.socketError = error
                    }
                }
        }
    }
}
