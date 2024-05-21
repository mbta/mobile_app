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
    @EnvironmentObject var backendProvider: BackendProvider
    @EnvironmentObject var globalFetcher: GlobalFetcher
    @EnvironmentObject var nearbyFetcher: NearbyFetcher
    @EnvironmentObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @EnvironmentObject var searchResultFetcher: SearchResultFetcher
    @EnvironmentObject var socketProvider: SocketProvider
    @EnvironmentObject var tripPredictionsFetcher: TripPredictionsFetcher
    @EnvironmentObject var vehicleFetcher: VehicleFetcher
    @EnvironmentObject var vehiclesFetcher: VehiclesFetcher
    @EnvironmentObject var viewportProvider: ViewportProvider
    @State private var sheetHeight: CGFloat = .zero
    @StateObject var nearbyVM: NearbyViewModel = .init()

    private enum SelectedTab: Hashable {
        case nearby
        case settings
    }

    @State private var selectedTab = SelectedTab.nearby

    var body: some View {
        TabView(selection: $selectedTab) {
            nearbyTab
                .tag(SelectedTab.nearby)
                .tabItem { Label("Nearby", systemImage: "mappin") }
            SettingsPage()
                .tag(SelectedTab.settings)
                .tabItem { Label("Settings", systemImage: "gear") }
        }
    }

    var nearbyTab: some View {
        NavigationStack {
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
                    alertsFetcher: alertsFetcher,
                    globalFetcher: globalFetcher,
                    nearbyFetcher: nearbyFetcher,
                    nearbyVM: nearbyVM,
                    railRouteShapeFetcher: railRouteShapeFetcher,
                    vehiclesFetcher: vehiclesFetcher,
                    viewportProvider: viewportProvider,
                    sheetHeight: $sheetHeight
                )
                .sheet(isPresented: .constant(selectedTab == .nearby)) { sheet }
            }
        }
        .searchable(
            text: $searchObserver.searchText,
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: "Find nearby transit"
        ).onAppear {
            socketProvider.socket.attach()
            Task {
                try await globalFetcher.getGlobalData()
            }
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                socketProvider.socket.attach()
            } else if newPhase == .background {
                socketProvider.socket.detach()
            }
        }.task { alertsFetcher.run() }
    }

    struct AllowsBackgroundInteraction: ViewModifier {
        func body(content: Content) -> some View {
            if #available(iOS 16.4, *) {
                content.presentationBackgroundInteraction(.enabled(upThrough: .medium))
            } else {
                // This is actually a purely cosmetic issue - the interaction still works, things are just greyed out
                // We might need to fix that later if it looks too bad to even ship, but for now, it's probably fine
                content
            }
        }
    }

    var sheet: some View {
        TabView(selection: $selectedTab) {
            nearbySheet
                .tag(SelectedTab.nearby)
                .tabItem { Label("Nearby", systemImage: "mappin") }
            // we want to show nothing in the sheet when the settings tab is open,
            // but an EmptyView here causes the tab to not be listed
            VStack {}
                .tag(SelectedTab.settings)
                .tabItem { Label("Settings", systemImage: "gear") }
        }
        .modifier(AllowsBackgroundInteraction())
    }

    var nearbySheet: some View {
        GeometryReader { proxy in
            NavigationStack(path: $nearbyVM.navigationStack) {
                NearbyTransitPageView(
                    globalFetcher: globalFetcher,
                    nearbyFetcher: nearbyFetcher,
                    nearbyVM: nearbyVM,
                    viewportProvider: viewportProvider,
                    alertsFetcher: alertsFetcher
                )
                .navigationDestination(for: SheetNavigationStackEntry.self) { entry in
                    switch entry {
                    case let .stopDetails(stop, _):
                        StopDetailsPage(
                            globalFetcher: globalFetcher,
                            viewportProvider: viewportProvider,
                            stop: stop, filter: $nearbyVM.navigationStack.lastStopDetailsFilter,
                            nearbyVM: nearbyVM
                        )
                    case let .tripDetails(tripId: tripId, vehicleId: vehicleId, target: target):
                        TripDetailsPage(
                            tripId: tripId,
                            vehicleId: vehicleId,
                            target: target,
                            globalFetcher: globalFetcher,
                            tripPredictionsFetcher: tripPredictionsFetcher,
                            vehicleFetcher: vehicleFetcher
                        )
                    }
                }
            }
            .onChange(of: proxy.size.height) { newValue in
                /*
                 Only update this if we're less than half way up the users screen
                 to mitigate undesired behavior
                 */
                guard newValue < (UIScreen.main.bounds.height / 2) else { return }
                sheetHeight = newValue
            }
            .partialSheetDetents(
                [.small, .medium, .large],
                largestUndimmedDetent: .medium
            )
        }
    }
}
