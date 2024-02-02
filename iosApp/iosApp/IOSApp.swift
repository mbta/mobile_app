import shared
import SwiftUI

@main
struct IOSApp: App {
    @StateObject var locationDataManager = LocationDataManager()
    @StateObject var nearbyFetcher = NearbyFetcher(backend: Backend.companion.platformDefault)
    @StateObject var searchResultFetcher = SearchResultFetcher(backend: Backend.companion.platformDefault)

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(locationDataManager)
                .environmentObject(nearbyFetcher)
                .environmentObject(searchResultFetcher)
        }
    }
}
