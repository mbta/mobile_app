import CoreLocation
import shared
import SwiftPhoenixClient
import SwiftUI

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase

    let platform = Platform_iosKt.getPlatform().name
    @StateObject var searchObserver = TextFieldObserver()
    @EnvironmentObject var locationDataManager: LocationDataManager
    @EnvironmentObject var alertsFetcher: AlertsFetcher
    @EnvironmentObject var globalFetcher: GlobalFetcher
    @EnvironmentObject var nearbyFetcher: NearbyFetcher
    @EnvironmentObject var predictionsFetcher: PredictionsFetcher
    @EnvironmentObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @EnvironmentObject var scheduleFetcher: ScheduleFetcher
    @EnvironmentObject var searchResultFetcher: SearchResultFetcher
    @EnvironmentObject var socketProvider: SocketProvider
    @EnvironmentObject var viewportProvider: ViewportProvider
    @State private var sheetHeight: CGFloat = .zero
    @State private var navigationStack: [SheetNavigationStackEntry] = []

    private var sheetDetents: Set<PartialSheetDetent> {
        if #available(iOS 16, *) {
            [.small, .medium, .large]
        } else {
            [.medium, .large]
        }
    }

    var body: some View {
        NavigationView {
            VStack {
                SearchView(
                    query: searchObserver.debouncedText,
                    fetcher: searchResultFetcher
                )
                switch locationDataManager.authorizationStatus {
                case .notDetermined:
                    Button("Allow Location", action: {
                        locationDataManager.locationFetcher.requestWhenInUseAuthorization()
                    })
                case .authorizedAlways, .authorizedWhenInUse:
                    EmptyView()
                case .denied, .restricted:
                    Text("Location access denied or restricted")
                @unknown default:
                    Text("Location access state unknown")
                }
                HomeMapView(
                    globalFetcher: globalFetcher,
                    nearbyFetcher: nearbyFetcher,
                    railRouteShapeFetcher: railRouteShapeFetcher,
                    viewportProvider: viewportProvider,
                    sheetHeight: $sheetHeight
                )
                .ignoresSafeArea(edges: .bottom)
                .sheet(isPresented: .constant(true)) {
                    NavigationStack(path: $navigationStack) {
                        nearbyTransit
                            .navigationBarHidden(true)
                            .navigationDestination(for: SheetNavigationStackEntry.self) { entry in
                                switch entry {
                                case let .stopDetails(stop, route):
                                    StopDetailsPage(stop: stop, route: route)
                                }
                            }
                    }
                    .partialSheetDetents(
                        sheetDetents,
                        largestUndimmedDetent: .medium
                    )
                }
            }
        }
        .searchable(
            text: $searchObserver.searchText,
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: "Find nearby transit"
        ).onAppear {
            socketProvider.socket.connect()
            Task {
                try await globalFetcher.getGlobalData()
            }
        }.onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                socketProvider.socket.connect()
            } else if newPhase == .background {
                socketProvider.socket.disconnect(code: .normal, reason: "backgrounded", callback: nil)
            }
        }.task { alertsFetcher.run() }
    }

    private var nearbyTransit: some View {
        GeometryReader { proxy in
            NearbyTransitPageView(
                currentLocation: locationDataManager.currentLocation?.coordinate,
                globalFetcher: globalFetcher,
                nearbyFetcher: nearbyFetcher,
                scheduleFetcher: scheduleFetcher,
                predictionsFetcher: predictionsFetcher,
                viewportProvider: viewportProvider,
                alertsFetcher: alertsFetcher
            )
            .onChange(of: proxy.size.height) { newValue in
                // Not actually restricted to iOS 16, this just behaves terribly on iOS 15
                if #available(iOS 16, *) {
                    sheetHeight = newValue
                }
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        let mockSocket = Socket(endPoint: "/socket", transport: { _ in PhoenixTransportMock() })

        ContentView()
            .environmentObject(LocationDataManager())
            .environmentObject(NearbyFetcher(backend: IdleBackend()))
            .environmentObject(GlobalFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(PredictionsFetcher(socket: mockSocket))
            .environmentObject(SocketProvider(socket: mockSocket))
            .environmentObject(AlertsFetcher(socket: mockSocket))
            .environmentObject(ViewportProvider())
    }
}
