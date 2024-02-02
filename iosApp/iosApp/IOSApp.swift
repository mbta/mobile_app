import shared
import SwiftUI

@main
struct IOSApp: App {
    @StateObject var locationDataManager = LocationDataManager()
    @StateObject var nearbyFetcher: NearbyFetcher
    @StateObject var searchResultFetcher: SearchResultFetcher

    init() {
        let backend = Backend()
        _nearbyFetcher = StateObject(wrappedValue: NearbyFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(locationDataManager)
                .environmentObject(nearbyFetcher)
                .environmentObject(searchResultFetcher)
        }
    }
}
