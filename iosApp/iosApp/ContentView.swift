import CoreLocation
@_spi(Experimental) import MapboxMaps
import shared
import SwiftPhoenixClient
import SwiftUI

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase

    let platform = Platform_iosKt.getPlatform().name
    @StateObject var searchObserver = TextFieldObserver()
    @EnvironmentObject var locationDataManager: LocationDataManager
    @EnvironmentObject var socketProvider: SocketProvider
    @EnvironmentObject var viewportProvider: ViewportProvider

    @ObservedObject var contentVM: ContentViewModel

    @State private var sheetHeight: CGFloat = UIScreen.main.bounds.height / 2
    @StateObject var errorBannerVM = ErrorBannerViewModel()
    @StateObject var nearbyVM = NearbyViewModel()
    @StateObject var mapVM = MapViewModel()
    @StateObject var searchVM = SearchViewModel()
    @StateObject var settingsVM = SettingsViewModel()

    let transition: AnyTransition = .asymmetric(insertion: .push(from: .bottom), removal: .opacity)
    var screenTracker: ScreenTracker = AnalyticsProvider.shared

    let inspection = Inspection<Self>()

    private enum SelectedTab: Hashable {
        case nearby
        case more
    }

    private func tabText(_ tab: SelectedTab) -> String {
        switch tab {
        case .nearby: NSLocalizedString(
                "Nearby",
                comment: "The label for the Nearby Transit page in the navigation bar"
            )
        case .more: NSLocalizedString("More", comment: "The label for the More page in the navigation bar")
        }
    }

    @State private var selectedTab = SelectedTab.nearby

    var body: some View {
        VStack {
            contents
        }
        .task { await contentVM.loadConfig() }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .onAppear {
            Task { await contentVM.loadOnboardingScreens() }
        }
        .task {
            // We can't set stale caches in ResponseCache on init because of our Koin setup,
            // so this is here to get the cached data into the global flow and kick off an async request asap.
            do {
                _ = try await RepositoryDI().global.getGlobalData()
            } catch {}
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                socketProvider.socket.attach()
                nearbyVM.joinAlertsChannel()
            } else if newPhase == .background {
                nearbyVM.leaveAlertsChannel()
                socketProvider.socket.detach()
            }
        }
        .onChange(of: contentVM.configResponse) { response in
            switch onEnum(of: response) {
            case let .ok(response): contentVM.configureMapboxToken(token: response.data.mapboxPublicToken)
            default: debugPrint("Skipping mapbox token configuration")
            }
        }
        .onReceive(mapVM.lastMapboxErrorSubject
            .debounce(for: .seconds(1), scheduler: DispatchQueue.main)) { _ in
                Task { await contentVM.loadConfig() }
        }
    }

    @ViewBuilder
    var contents: some View {
        if let onboardingScreensPending = contentVM.onboardingScreensPending, !onboardingScreensPending.isEmpty {
            OnboardingPage(screens: onboardingScreensPending, onFinish: {
                contentVM.onboardingScreensPending = []
            })
        } else if selectedTab == .more {
            TabView(selection: $selectedTab) {
                nearbyTab
                    .tag(SelectedTab.nearby)
                    .tabItem { TabLabel(tabText(.nearby), image: .tabIconNearby) }
                MorePage(viewModel: settingsVM)
                    .tag(SelectedTab.more)
                    .tabItem { TabLabel(tabText(.more), image: .tabIconMore) }
                    .onAppear { screenTracker.track(screen: .settings) }
            }
        } else {
            nearbyTab
        }
    }

    @State var selectedDetent: PresentationDetent = .halfScreen
    @State var visibleNearbySheet: SheetNavigationStackEntry = .nearby
    @State private var showingLocationPermissionAlert = false

    @ViewBuilder var nearbySheetContents: some View {
        // Putting the TabView in a VStack prevents the tabs from covering the nearby transit contents
        // when re-opening nearby transit
        VStack {
            TabView(selection: $selectedTab) {
                NearbyTransitPageView(
                    errorBannerVM: errorBannerVM,
                    nearbyVM: nearbyVM,
                    viewportProvider: viewportProvider
                )
                .tag(SelectedTab.nearby)
                .tabItem { TabLabel(tabText(.nearby), image: .tabIconNearby) }
                // we want to show nothing in the sheet when the settings tab is open,
                // but an EmptyView here causes the tab to not be listed
                VStack {}
                    .tag(SelectedTab.more)
                    .tabItem { TabLabel(tabText(.more), image: .tabIconMore) }
            }
        }
    }

    @ViewBuilder
    var nearbyTab: some View {
        VStack {
            if contentVM.hideMaps {
                if nearbyVM.navigationStack.lastSafe() == .nearby {
                    SearchOverlay(searchObserver: searchObserver, nearbyVM: nearbyVM, searchVM: searchVM)
                    if !searchObserver.isSearching {
                        LocationAuthButton(showingAlert: $showingLocationPermissionAlert)
                            .padding(.bottom, 8)
                    }
                }
                if !(nearbyVM.navigationStack.lastSafe() == .nearby && searchObserver.isSearching) {
                    mapWithSheets
                }
            } else {
                ZStack(alignment: .top) {
                    mapWithSheets
                    VStack(alignment: .center, spacing: 0) {
                        if nearbyVM.navigationStack.lastSafe() == .nearby {
                            SearchOverlay(searchObserver: searchObserver, nearbyVM: nearbyVM, searchVM: searchVM)

                            if !searchObserver.isSearching {
                                LocationAuthButton(showingAlert: $showingLocationPermissionAlert)
                            }
                        }
                        if !searchObserver.isSearching, !viewportProvider.viewport.isFollowing,
                           locationDataManager.currentLocation != nil {
                            VStack(alignment: .trailing) {
                                RecenterButton { Task { viewportProvider.follow() } }
                            }.frame(maxWidth: .infinity, alignment: .topTrailing)
                        }
                    }.frame(maxWidth: .infinity, alignment: .trailing)
                }
            }
        }
        .background(Color.fill1)
        .onAppear {
            Task { await errorBannerVM.activate() }
            Task { await contentVM.loadHideMaps() }
            Task { await settingsVM.getSections() }
        }
    }

    @ViewBuilder var mapSection: some View {
        // If we don't have an access token, don't attempt to show the map
        switch onEnum(of: contentVM.configResponse) {
        case .ok:
            HomeMapView(
                contentVM: contentVM,
                mapVM: mapVM,
                nearbyVM: nearbyVM,
                viewportProvider: viewportProvider,
                sheetHeight: $sheetHeight
            )
        default:
            GeometryReader { proxy in
                Image(.emptyMapGrid)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(maxWidth: proxy.size.width, maxHeight: proxy.size.height)
                    .clipped()
            }
            .tag("empty-map-grid")
        }
    }

    @ViewBuilder var mapWithSheets: some View {
        let nav = nearbyVM.navigationStack.lastSafe()
        let sheetItemId: String? = nav.sheetItemIdentifiable()?.id
        if contentVM.hideMaps {
            navSheetContents
                .fullScreenCover(item: .constant(nav.coverItemIdentifiable()), onDismiss: {
                    if case .alertDetails = nearbyVM.navigationStack.last {
                        nearbyVM.goBack()
                    }
                }, content: coverContents)
                .onAppear {
                    // The NearbyTransitPageView uses the viewport provider to determine what location to load,
                    // since we have no map when it's hidden, we need to manually update the camera position.
                    viewportProvider.updateCameraState(locationDataManager.currentLocation)
                }
                .onChange(of: locationDataManager.currentLocation) { location in
                    viewportProvider.updateCameraState(location)
                }
        } else {
            mapSection
                .sheet(
                    isPresented: .constant(
                        !(searchObserver.isSearching && nav == .nearby)
                            && selectedTab == .nearby
                            && !showingLocationPermissionAlert
                            && contentVM.onboardingScreensPending != nil
                    ),
                    content: {
                        GeometryReader { proxy in
                            VStack {
                                navSheetContents
                                    .presentationDetents([.small, .halfScreen, .almostFull], selection: $selectedDetent)
                                    .interactiveDismissDisabled()
                                    .modifier(AllowsBackgroundInteraction())
                            }
                            // within the sheet to prevent issues on iOS 16 with two modal views open at once
                            .fullScreenCover(
                                item: .constant(nav.coverItemIdentifiable()),
                                onDismiss: {
                                    if case .alertDetails = nearbyVM.navigationStack.last {
                                        nearbyVM.goBack()
                                    }
                                },
                                content: coverContents
                            )
                            .onChange(of: sheetItemId) { _ in
                                selectedDetent = .halfScreen
                            }
                            .onAppear {
                                recordSheetHeight(proxy.size.height)
                            }
                            .onChange(of: proxy.size.height) { newValue in
                                recordSheetHeight(newValue)
                            }
                        }
                    }
                )
        }
    }

    @ViewBuilder
    var navSheetContents: some View {
        NavigationStack {
            VStack {
                switch nearbyVM.navigationStack.lastSafe() {
                case .alertDetails:
                    EmptyView()

                case let .stopDetails(stop, filter):
                    // Wrapping in a TabView helps the page to animate in as a single unit
                    // Otherwise only the header animates
                    TabView {
                        StopDetailsPage(
                            viewportProvider: viewportProvider,
                            stop: stop, filter: filter,
                            errorBannerVM: errorBannerVM,
                            nearbyVM: nearbyVM
                        )

                        .toolbar(.hidden, for: .tabBar)
                        .onAppear {
                            let filtered = filter != nil
                            screenTracker.track(
                                screen: filtered ? .stopDetailsFiltered : .stopDetailsUnfiltered
                            )
                        }
                    }
                    // Set id per stop so that transitioning from one stop to another is handled by removing
                    // the existing stop view & creating a new one
                    .id(stop.id)
                    .transition(transition)

                case let .tripDetails(
                    tripId: tripId,
                    vehicleId: vehicleId,
                    target: target,
                    routeId: routeId,
                    directionId: _
                ):
                    TabView {
                        TripDetailsPage(
                            tripId: tripId,
                            vehicleId: vehicleId,
                            routeId: routeId,
                            target: target,
                            errorBannerVM: errorBannerVM,
                            nearbyVM: nearbyVM,
                            mapVM: mapVM
                        ).toolbar(.hidden, for: .tabBar)
                            .onAppear {
                                screenTracker.track(screen: .tripDetails)
                            }
                    }
                    .transition(transition)

                case .nearby:
                    nearbySheetContents
                        .transition(transition)
                        .onAppear {
                            screenTracker.track(screen: .nearbyTransit)
                        }
                }
            }
            .animation(.easeInOut, value: nearbyVM.navigationStack.lastSafe().sheetItemIdentifiable()?.id)
        }
    }

    private func coverContents(coverIdentityEntry: NearbyCoverItem) -> some View {
        let entry = coverIdentityEntry.stackEntry
        return NavigationStack {
            switch entry {
            case let .alertDetails(alertId, line, routes):
                AlertDetailsPage(alertId: alertId, line: line, routes: routes, nearbyVM: nearbyVM)
            default:
                EmptyView()
            }
        }
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
            content.presentationBackgroundInteraction(.enabled(upThrough: .halfScreen))
        }
    }
}
