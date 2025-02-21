import CoreLocation
@_spi(Experimental) import MapboxMaps
import shared
import SwiftPhoenixClient
import SwiftUI

// swiftlint:disable:next type_body_length
struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.accessibilityVoiceOverEnabled) var voiceOver

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
    @StateObject var stopDetailsVM = StopDetailsViewModel()

    let transition: AnyTransition = .asymmetric(insertion: .push(from: .bottom), removal: .opacity)
    let analytics: Analytics = AnalyticsProvider.shared

    let inspection = Inspection<Self>()

    @State private var selectedTab = SelectedTab.nearby
    @State private var sheetTabBarVisibility = Visibility.hidden
    @State private var baseTabBarVisibility = Visibility.hidden

    func updateTabBarVisibility(_ tab: SelectedTab) {
        let shouldShowSheetTabBar = !contentVM.hideMaps
            && tab == SelectedTab.nearby
            && nearbyVM.navigationStack.lastSafe() == .nearby
            && !searchObserver.isSearching

        sheetTabBarVisibility = shouldShowSheetTabBar ? .visible : .hidden

        let shouldShowBaseTabBar = !searchObserver.isSearching && (
            !contentVM.hideMaps || nearbyVM.navigationStack.lastSafe() == .nearby
        )
        baseTabBarVisibility = shouldShowBaseTabBar ? .visible : .hidden
    }

    var body: some View {
        VStack {
            contents
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .onAppear {
            Task { await contentVM.loadFeaturePromos() }
            Task { await contentVM.loadOnboardingScreens() }
            Task { await nearbyVM.loadSettings() }
            analytics.recordSession(colorScheme: colorScheme)
            analytics.recordSession(voiceOver: voiceOver)
            analytics.recordSession(hideMaps: contentVM.hideMaps)
            updateTabBarVisibility(selectedTab)
        }
        .task {
            // We can't set stale caches in ResponseCache on init because of our Koin setup,
            // so this is here to get the cached data into the global flow and kick off an async request asap.
            do {
                _ = try await RepositoryDI().global.getGlobalData()
            } catch {}
        }
        .onChange(of: selectedTab) { nextTab in
            Task { await nearbyVM.loadSettings() }
            updateTabBarVisibility(nextTab)
        }
        .onChange(of: nearbyVM.navigationStack.lastSafe()) { _ in updateTabBarVisibility(selectedTab) }
        .onChange(of: contentVM.hideMaps) { _ in updateTabBarVisibility(selectedTab) }
        .onChange(of: searchObserver.isSearching) { _ in updateTabBarVisibility(selectedTab) }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                socketProvider.socket.attach()
                nearbyVM.joinAlertsChannel()
            } else if newPhase == .background {
                nearbyVM.leaveAlertsChannel()
                socketProvider.socket.detach()
            }
        }
        .onChange(of: colorScheme) { _ in
            analytics.recordSession(colorScheme: colorScheme)
        }
        .onChange(of: voiceOver) { _ in
            analytics.recordSession(voiceOver: voiceOver)
        }
        .onChange(of: contentVM.hideMaps) { _ in
            analytics.recordSession(hideMaps: contentVM.hideMaps)
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
        } else if let featurePromosPending = contentVM.featurePromosPending, !featurePromosPending.isEmpty {
            PromoPage(screens: featurePromosPending, onFinish: {
                contentVM.featurePromosPending = []
            })
        } else {
            TabView(selection: $selectedTab) {
                nearbyTab
                    .toolbar(baseTabBarVisibility, for: .tabBar, .bottomBar)
                    .tag(SelectedTab.nearby)
                    .tabItem { TabLabel(tab: SelectedTab.nearby) }
                MorePage(viewModel: settingsVM)
                    .tag(SelectedTab.more)
                    .tabItem { TabLabel(tab: SelectedTab.more) }
                    .onAppear { analytics.track(screen: .settings) }
            }
        }
    }

    @State var selectedDetent: PresentationDetent = .halfScreen
    @State var visibleNearbySheet: SheetNavigationStackEntry = .nearby
    @State private var showingLocationPermissionAlert = false

    @ViewBuilder
    var nearbySheetContents: some View {
        if contentVM.hideMaps {
            nearbyPage
        } else {
            // Putting the TabView in a VStack prevents the tabs from covering the nearby transit contents
            // when re-opening nearby transit
            VStack {
                TabView(selection: $selectedTab) {
                    nearbyPage
                        .toolbar(sheetTabBarVisibility, for: .tabBar)
                        .tag(SelectedTab.nearby)
                        .tabItem { TabLabel(tab: SelectedTab.nearby) }
                    // we want to show nothing in the sheet when the settings tab is open,
                    // but an EmptyView here causes the tab to not be listed
                    VStack {}
                        .tag(SelectedTab.more)
                        .tabItem { TabLabel(tab: SelectedTab.more) }
                }
            }
        }
    }

    @ViewBuilder
    var nearbyPage: some View {
        NearbyTransitPageView(
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            viewportProvider: viewportProvider,
            noNearbyStops: { NoNearbyStopsView(
                hideMaps: contentVM.hideMaps,
                onOpenSearch: { searchObserver.isFocused = true },
                onPanToDefaultCenter: {
                    viewportProvider.setIsManuallyCentering(true)
                    viewportProvider.animateTo(
                        coordinates: ViewportProvider.Defaults.center,
                        zoom: 13.75
                    )
                }
            ) }
        )
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
                        .accessibilityHidden(searchObserver.isSearching)
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
                                RecenterButton(icon: .faLocationArrowSolid, size: 17.33) {
                                    viewportProvider.follow()
                                }
                            }.frame(maxWidth: .infinity, alignment: .topTrailing)
                        }
                        if !searchObserver.isSearching, !viewportProvider.viewport.isOverview,
                           let (routeType, selectedVehicle, stop) = recenterOnVehicleButtonInfo() {
                            VStack(alignment: .trailing) {
                                RecenterButton(icon: routeIconResource(routeType), size: 32) {
                                    viewportProvider.vehicleOverview(vehicle: selectedVehicle, stop: stop)
                                }
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

    @ViewBuilder
    var mapSection: some View {
        HomeMapView(
            contentVM: contentVM,
            mapVM: mapVM,
            nearbyVM: nearbyVM,
            viewportProvider: viewportProvider,
            locationDataManager: locationDataManager,
            sheetHeight: $sheetHeight
        ).accessibilityHidden(searchObserver.isSearching)
    }

    @ViewBuilder var mapWithSheets: some View {
        let nav = nearbyVM.navigationStack.lastSafe()
        let sheetItemId: String? = nav.sheetItemIdentifiable()?.id
        if contentVM.hideMaps {
            navSheetContents
                .fullScreenCover(item: .constant(nav.coverItemIdentifiable()), onDismiss: {
                    switch nearbyVM.navigationStack.last {
                    case .alertDetails: nearbyVM.goBack()
                    default: break
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
                                    switch nearbyVM.navigationStack.last {
                                    case .alertDetails: nearbyVM.goBack()
                                    default: break
                                    }
                                },
                                content: coverContents
                            )
                            .onChange(of: sheetItemId) { _ in selectedDetent = .halfScreen }
                            .onAppear { recordSheetHeight(proxy.size.height) }
                            .onChange(of: proxy.size.height) { newValue in recordSheetHeight(newValue) }
                        }
                    }
                )
        }
    }

    @ViewBuilder
    var navSheetContents: some View {
        let navEntry = nearbyVM.navigationStack.lastSafe()
        NavigationStack {
            VStack {
                switch navEntry {
                case let .stopDetails(stopId, stopFilter, tripFilter):
                    // Wrapping in a TabView helps the page to animate in as a single unit
                    // Otherwise only the header animates
                    TabView {
                        StopDetailsPage(
                            filters: .init(
                                stopId: stopId,
                                stopFilter: stopFilter,
                                tripFilter: tripFilter
                            ),
                            errorBannerVM: errorBannerVM,
                            nearbyVM: nearbyVM,
                            mapVM: mapVM,
                            stopDetailsVM: stopDetailsVM,
                            viewportProvider: viewportProvider
                        )
                        .onAppear {
                            analytics.track(
                                screen: stopFilter != nil ? .stopDetailsFiltered : .stopDetailsUnfiltered
                            )
                        }
                        .toolbar(.hidden, for: .tabBar)
                    }
                    // Set id per stop so that transitioning from one stop to another is handled by removing
                    // the existing stop view & creating a new one
                    .id(stopId)
                    .onChange(of: stopId) { nextStopId in stopDetailsVM.handleStopChange(nextStopId) }
                    .onAppear { stopDetailsVM.handleStopAppear(stopId) }
                    .onDisappear { stopDetailsVM.leaveStopPredictions() }
                    .transition(transition)

                case let .legacyStopDetails(stop, filter):
                    // Wrapping in a TabView helps the page to animate in as a single unit
                    // Otherwise only the header animates
                    TabView {
                        LegacyStopDetailsPage(
                            viewportProvider: viewportProvider,
                            stop: stop, filter: filter,
                            errorBannerVM: errorBannerVM,
                            nearbyVM: nearbyVM
                        )
                        .toolbar(.hidden, for: .tabBar)
                        .onAppear {
                            let filtered = filter != nil
                            analytics.track(
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
                            .onAppear { analytics.track(screen: .tripDetails) }
                    }
                    .transition(transition)

                case .nearby:
                    nearbySheetContents
                        .transition(transition)
                        .onAppear { analytics.track(screen: .nearbyTransit) }

                default: EmptyView()
                }
            }
            .animation(.easeInOut, value: nearbyVM.navigationStack.lastSafe().sheetItemIdentifiable()?.id)
        }
    }

    private func recenterOnVehicleButtonInfo() -> (RouteType, Vehicle, Stop)? {
        guard case let .stopDetails(stopId: _, stopFilter: stopFilter, tripFilter: tripFilter) = nearbyVM
            .navigationStack.lastSafe(),
            let selectedVehicle = mapVM.selectedVehicle, tripFilter?.vehicleId == selectedVehicle.id,
            let globalData = mapVM.globalData,
            let stop = nearbyVM.getTargetStop(global: globalData),
            let routeId = selectedVehicle.routeId ?? stopFilter?.routeId,
            let route = globalData.routes[routeId] else { return nil }
        return (route.type, selectedVehicle, stop)
    }

    private func coverContents(coverIdentityEntry: NearbyCoverItem) -> some View {
        let entry = coverIdentityEntry.stackEntry
        return NavigationStack {
            switch entry {
            case let .alertDetails(alertId, line, routes, stop):
                AlertDetailsPage(alertId: alertId, line: line, routes: routes, stop: stop, nearbyVM: nearbyVM)
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
        sheetHeight = newSheetHeight - 55
    }

    struct AllowsBackgroundInteraction: ViewModifier {
        func body(content: Content) -> some View {
            content.presentationBackgroundInteraction(.enabled(upThrough: .halfScreen))
        }
    }
}
