import shared
import SwiftUI

@main
struct IOSApp: App {
    @StateObject var locationDataManager = LocationDataManager()
    @StateObject var backendDispatcher = BackendDispatcher(backend: Backend.companion.platformDefault)

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(locationDataManager)
                .environmentObject(backendDispatcher)
        }
    }
}
