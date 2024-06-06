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
        if selectedTab == .settings {
            TabView(selection: $selectedTab) {
                nearbyTab
                    .tag(SelectedTab.nearby)
                    .tabItem { Label("Nearby", systemImage: "mappin") }
                SettingsPage()
                    .tag(SelectedTab.settings)
                    .tabItem { Label("Settings", systemImage: "gear") }
            }
        } else {
            nearbyTab
        }
    }

    @State var selectedDetent: PresentationDetent = .medium
    @State var visibleNearbySheet: SheetNavigationStackEntry = .nearby

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
                // TODO: move sheet contents into own view
                .sheet(item: .constant($nearbyVM.navigationStack.wrappedValue.lastSafe()), onDismiss: {
                    if visibleNearbySheet == nearbyVM.navigationStack.last {
                        // When the visible sheet matches the last nav entry, then a dismissal indicates
                        // an intentional action remove the sheet and replace it with the previous one.

                        // When the visible sheet *doesn't* match the latest item in the nav stack, it is
                        // being dismissed so that it can be automatically replaced with the new one.
                        nearbyVM.goBack()
                    } else {}
                }) { entry in

                    GeometryReader { proxy in
                        NavigationStack {
                            switch entry {
                            case let .stopDetails(stop, _):

                                // If I change this to nearby transit, it pops at the small detent
                                // as expected, but only the first time

                                StopDetailsPage(
                                    globalFetcher: globalFetcher,
                                    viewportProvider: viewportProvider,
                                    stop: stop, filter: $nearbyVM.navigationStack.lastStopDetailsFilter,
                                    nearbyVM: nearbyVM
                                ).onAppear {
                                    visibleNearbySheet = entry
                                }

                            case let .tripDetails(tripId: tripId, vehicleId: vehicleId, target: target):

                                TripDetailsPage(
                                    tripId: tripId,
                                    vehicleId: vehicleId,
                                    target: target,
                                    globalFetcher: globalFetcher,
                                    nearbyVM: nearbyVM,
                                    tripPredictionsFetcher: tripPredictionsFetcher,
                                    vehicleFetcher: vehicleFetcher
                                ).onAppear {
                                    visibleNearbySheet = entry
                                }

                            case .nearby:
                                TabView(selection: $selectedTab) {
                                    NearbyTransitPageView(
                                        globalFetcher: globalFetcher,
                                        nearbyFetcher: nearbyFetcher,
                                        nearbyVM: nearbyVM,
                                        viewportProvider: viewportProvider,
                                        alertsFetcher: alertsFetcher
                                    ).tag(SelectedTab.nearby)
                                        .tabItem { Label("Nearby", systemImage: "mappin") }
                                    // we want to show nothing in the sheet when the settings tab is open,
                                    // but an EmptyView here causes the tab to not be listed
                                    VStack {}
                                        .tag(SelectedTab.settings)
                                        .tabItem { Label("Settings", systemImage: "gear") }
                                }.onAppear {
                                    visibleNearbySheet = entry
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
                        // Adding id here prevents the next sheet from opening at the large detent.
                        // https://stackoverflow.com/a/77429540
                        .id(entry)
                        .presentationDetents([.medium, .large], selection: $selectedDetent)
                        .interactiveDismissDisabled(visibleNearbySheet == .nearby)
                        .modifier(AllowsBackgroundInteraction())
                    }
                }
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
}
