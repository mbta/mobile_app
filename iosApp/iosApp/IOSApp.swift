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
    @StateObject var nearbyFetcher: NearbyFetcher
    @StateObject var globalFetcher: GlobalFetcher
    @StateObject var searchResultFetcher: SearchResultFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher

    init() {
        let backend = backend

        if let sentryDsn = Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN") as? String {
            AppSetupKt.initializeSentry(dsn: sentryDsn)
        } else {
            print("skipping sentry initialization - SENTRY_DSN not configured")
        }

        do {
            throw TestFakeError.thisIsATest
        } catch {
            SentrySDK.capture(error: error)
        }

        _nearbyFetcher = StateObject(wrappedValue: NearbyFetcher(backend: backend))
        _globalFetcher = StateObject(wrappedValue: GlobalFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(backend: backend))
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(locationDataManager)
                .environmentObject(nearbyFetcher)
                .environmentObject(globalFetcher)
                .environmentObject(searchResultFetcher)
                .environmentObject(predictionsFetcher)
                .task {
                    do {
                        try await backend.runSocket()
                    } catch {
                        debugPrint("failed to run socket", error)
                    }
                }
        }
    }
}
