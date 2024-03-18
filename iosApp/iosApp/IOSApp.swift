import os
import shared
import SwiftPhoenixClient
import SwiftUI

@main
struct IOSApp: App {
    let backend: BackendProtocol = CommandLine.arguments.contains("-testing") ? IdleBackend() : Backend()

    // ignore updates less than 0.1km
    @StateObject var locationDataManager = LocationDataManager(distanceFilter: 100)

    @StateObject var alertsFetcher: AlertsFetcher
    @StateObject var globalFetcher: GlobalFetcher
    @StateObject var nearbyFetcher: NearbyFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher
    @StateObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @StateObject var scheduleFetcher: ScheduleFetcher
    @StateObject var searchResultFetcher: SearchResultFetcher
    @StateObject var socketProvider: SocketProvider
    @StateObject var viewportProvider: ViewportProvider

    init() {
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

        _alertsFetcher = StateObject(wrappedValue: AlertsFetcher(socket: socket))
        _globalFetcher = StateObject(wrappedValue: GlobalFetcher(backend: backend))
        _nearbyFetcher = StateObject(wrappedValue: NearbyFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(socket: socket))
        _railRouteShapeFetcher = StateObject(wrappedValue: RailRouteShapeFetcher(backend: backend))
        _scheduleFetcher = StateObject(wrappedValue: ScheduleFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
        _socketProvider = StateObject(wrappedValue: SocketProvider(socket: socket))
        _viewportProvider = StateObject(wrappedValue: ViewportProvider())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(locationDataManager)
                .environmentObject(alertsFetcher)
                .environmentObject(globalFetcher)
                .environmentObject(nearbyFetcher)
                .environmentObject(predictionsFetcher)
                .environmentObject(railRouteShapeFetcher)
                .environmentObject(scheduleFetcher)
                .environmentObject(searchResultFetcher)
                .environmentObject(socketProvider)
                .environmentObject(viewportProvider)
        }
    }
}
