import shared
import SwiftPhoenixClient
import SwiftUI

@main
struct IOSApp: App {
    let backend = Backend()
    let socket = Socket("wss://mobile-app-backend-staging.mbtace.com/socket/websocket")
    // ignore updates less than 0.1km
    @StateObject var locationDataManager = LocationDataManager(distanceFilter: 100)
    @StateObject var nearbyFetcher: NearbyFetcher
    @StateObject var globalFetcher: GlobalFetcher
    @StateObject var searchResultFetcher: SearchResultFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher

    init() {
        let backend = backend
        let socket = socket
        socket.onOpen {
            print("Socket opened")
        }
        socket.onClose {
            print("Socket closed")
        }

        socket.connect()
        _nearbyFetcher = StateObject(wrappedValue: NearbyFetcher(backend: backend))
        _globalFetcher = StateObject(wrappedValue: GlobalFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(socket: socket))
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
                        predictionsFetcher.socketError = error
                    }
                }
        }
    }
}
