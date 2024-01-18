import SwiftUI

@main
struct IOSApp: App {
    @StateObject var locationDataManager = LocationDataManager()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(locationDataManager)
        }
    }
}
