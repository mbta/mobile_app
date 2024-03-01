import os
import shared
import SwiftPhoenixClient
import SwiftUI

@main
struct IOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    let backend = Backend()
    let socket: PhoenixSocket

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
        self.socket = socket
    }

    var body: some Scene {
        WindowGroup {
            MainView(socket: socket, backend: backend)
        }
    }
}

struct MainView: View {
    @Environment(\.scenePhase) private var scenePhase

    let backend = Backend()
    let socket: PhoenixSocket

    // ignore updates less than 0.1km
    @StateObject var locationDataManager = LocationDataManager(distanceFilter: 100)

    @StateObject var globalFetcher: GlobalFetcher
    @StateObject var nearbyFetcher: NearbyFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher
    @StateObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @StateObject var searchResultFetcher: SearchResultFetcher

    init(socket: PhoenixSocket, backend: BackendProtocol) {
        let backend = backend
        self.socket = socket

        socket.connect()

        _globalFetcher = StateObject(wrappedValue: GlobalFetcher(backend: backend))
        _nearbyFetcher = StateObject(wrappedValue: NearbyFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(socket: socket))
        _railRouteShapeFetcher = StateObject(wrappedValue: RailRouteShapeFetcher(backend: backend))
        _searchResultFetcher = StateObject(wrappedValue: SearchResultFetcher(backend: backend))
    }

    var body: some View {
        ContentView()
            .environmentObject(locationDataManager)
            .environmentObject(globalFetcher)
            .environmentObject(nearbyFetcher)
            .environmentObject(predictionsFetcher)
            .environmentObject(railRouteShapeFetcher)
            .environmentObject(searchResultFetcher)
            .onChange(of: scenePhase) { newPhase in
                if newPhase == .active {
                    socket.connect()
                } else if newPhase == .background {
                    socket.disconnect(code: .normal, reason: "backgrounded", callback: nil)
                }
            }
    }
}
