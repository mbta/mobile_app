import shared
import SwiftUI

@main
struct IOSApp: App {
    let backend = Backend()
    // ignore updates less than 0.1km
    @StateObject var locationDataManager = LocationDataManager(distanceFilter: 100)
    @StateObject var nearbyFetcher: NearbyFetcher
    @StateObject var searchResultFetcher: SearchResultFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher

    init() {
        let backend = backend
        _nearbyFetcher = StateObject(wrappedValue: NearbyFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(backend: backend))
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(locationDataManager)
                .environmentObject(nearbyFetcher)
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
