import AppcuesKit
import CoreLocation
import shared
import SwiftPhoenixClient
import SwiftUI
@_spi(Experimental) import MapboxMaps

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase

    let platform = Platform_iosKt.getPlatform().name
    @StateObject var searchObserver = TextFieldObserver()
    @EnvironmentObject var locationDataManager: LocationDataManager
    @EnvironmentObject var backendProvider: BackendProvider
    @EnvironmentObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @EnvironmentObject var searchResultFetcher: SearchResultFetcher
    @EnvironmentObject var socketProvider: SocketProvider
    @EnvironmentObject var vehiclesFetcher: VehiclesFetcher
    @EnvironmentObject var viewportProvider: ViewportProvider

    @ObservedObject var contentVM: ContentViewModel

    @State private var sheetHeight: CGFloat = UIScreen.main.bounds.height / 2
    @StateObject var nearbyVM: NearbyViewModel = .init()
    @StateObject var mapVM = MapViewModel()
    var screenTracker: ScreenTracker = AnalyticsProvider.shared

    let inspection = Inspection<Self>()

    private enum SelectedTab: Hashable {
        case nearby
        case settings
    }

    @State private var selectedTab = SelectedTab.nearby

    var body: some View {
        contents

            .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @ViewBuilder
    var contents: some View {
        if selectedTab == .settings {
            TabView(selection: $selectedTab) {
                nearbyTab
                    .tag(SelectedTab.nearby)
                    .tabItem { Label("Nearby", systemImage: "mappin") }
                SettingsPage()
                    .tag(SelectedTab.settings)
                    .tabItem { Label("Settings", systemImage: "gear") }
                    .onAppear {
                        screenTracker.track(screen: .settings)
                    }
            }

        } else {
            nearbyTab
        }
    }

    @State var selectedDetent: PresentationDetent = .halfScreen
    @State var visibleNearbySheet: SheetNavigationStackEntry = .nearby

    @ViewBuilder var nearbySheetContents: some View {
        // Putting the TabView in a VStack prevents the tabs from covering the nearby transit contents
        // when re-opening nearby transit
        VStack {
            TabView(selection: $selectedTab) {
                NearbyTransitPageView(
                    nearbyVM: nearbyVM,
                    viewportProvider: viewportProvider
                )
                .tag(SelectedTab.nearby)
                .tabItem { Label("Nearby", systemImage: "mappin") }
                // we want to show nothing in the sheet when the settings tab is open,
                // but an EmptyView here causes the tab to not be listed
                VStack {}
                    .tag(SelectedTab.settings)
                    .tabItem { Label("Settings", systemImage: "gear") }
            }
        }
    }

    @ViewBuilder
    var nearbyTab: some View {
        VStack {
            if contentVM.searchEnabled {
                TextField("Find nearby transit", text: $searchObserver.searchText)
                SearchView(
                    query: searchObserver.debouncedText,
                    fetcher: searchResultFetcher
                )
            }
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

            mapSection
                .sheet(
                    item: .constant($nearbyVM.navigationStack.wrappedValue.lastSafe().sheetItemIdentifiable()),
                    onDismiss: {
                        selectedDetent = .halfScreen

                        if visibleNearbySheet == nearbyVM.navigationStack.last {
                            // When the visible sheet matches the last nav entry, then a dismissal indicates
                            // an intentional action remove the sheet and replace it with the previous one.

                            // When the visible sheet *doesn't* match the latest item in the nav stack, it is
                            // being dismissed so that it can be automatically replaced with the new one.
                            nearbyVM.goBack()
                        } else {}
                    }
                ) { sheetIdentityEntry in
                    let entry = sheetIdentityEntry.stackEntry
                    GeometryReader { proxy in
                        NavigationStack {
                            switch entry {
                            case let .stopDetails(stop, _):
                                StopDetailsPage(
                                    viewportProvider: viewportProvider,
                                    stop: stop, filter: $nearbyVM.navigationStack.lastStopDetailsFilter,
                                    nearbyVM: nearbyVM
                                ).onAppear {
                                    visibleNearbySheet = entry
                                    screenTracker.track(screen: .stopDetails)
                                }

                            case let .tripDetails(
                                tripId: tripId,
                                vehicleId: vehicleId,
                                target: target,
                                routeId: _,
                                directionId: _
                            ):
                                TripDetailsPage(
                                    tripId: tripId,
                                    vehicleId: vehicleId,
                                    target: target,
                                    nearbyVM: nearbyVM,
                                    mapVM: mapVM
                                ).onAppear {
                                    screenTracker.track(screen: .tripDetails)
                                    visibleNearbySheet = entry
                                }

                            case .nearby:
                                nearbySheetContents
                                    .onAppear {
                                        visibleNearbySheet = entry
                                        screenTracker.track(screen: .nearbyTransit)
                                    }
                            }
                        }
                        .onAppear {
                            recordSheetHeight(proxy.size.height)
                        }
                        .onChange(of: proxy.size.height) { newValue in
                            recordSheetHeight(newValue)
                        }
                        // Adding id here prevents the next sheet from opening at the large detent.
                        // https://stackoverflow.com/a/77429540
                        .id(sheetIdentityEntry.id)
                        .presentationDetents([.small, .halfScreen, .almostFull], selection: $selectedDetent)
                        .interactiveDismissDisabled(visibleNearbySheet == .nearby)
                        .modifier(AllowsBackgroundInteraction())
                    }
                }
        }
        .onAppear {
            socketProvider.socket.attach()
            Task {
                await contentVM.loadSettings()
            }
            Task {
                if contentVM.dynamicMapKeyEnabled {
                    await contentVM.loadConfig()
                }
            }
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                socketProvider.socket.attach()
            } else if newPhase == .background {
                socketProvider.socket.detach()
            }
        }
        .onChange(of: contentVM.dynamicMapKeyEnabled) { isEnabled in
            if isEnabled {
                Task {
                    await contentVM.loadConfig()
                }
            }
        }
        .onChange(of: contentVM.configResponse) { response in
            switch onEnum(of: response) {
            case let .ok(response): contentVM.configureMapboxToken(token: response.data.mapboxPublicToken)
            default: debugPrint("Skipping mapbox token configuration")
            }
        }
    }

    @ViewBuilder var mapSection: some View {
        HomeMapView(
            mapVM: mapVM,
            nearbyVM: nearbyVM,
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: vehiclesFetcher,
            viewportProvider: viewportProvider,
            sheetHeight: $sheetHeight
        )
    }

    private func recordSheetHeight(_ newSheetHeight: CGFloat) {
        /*
         Only update this if we're less than half way up the users screen. Otherwise,
         the entire map is blocked by the sheet anyway, so it doesn't need to respond to height changes
         */
        guard newSheetHeight < (UIScreen.main.bounds.height / 2) else { return }
        sheetHeight = newSheetHeight
    }

    struct AllowsBackgroundInteraction: ViewModifier {
        func body(content: Content) -> some View {
            if #available(iOS 16.4, *) {
                content.presentationBackgroundInteraction(.enabled(upThrough: .halfScreen))
            } else {
                // This is actually a purely cosmetic issue - the interaction still works, things are just greyed out
                // We might need to fix that later if it looks too bad to even ship, but for now, it's probably fine
                content
            }
        }
    }
}
