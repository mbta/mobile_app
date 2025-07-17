import CoreLocation
@_spi(Experimental) import MapboxMaps
import Shared
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

    @State private var contentHeight: CGFloat = UIScreen.current?.bounds.height ?? 0
    @State private var sheetHeight: CGFloat =
        (UIScreen.current?.bounds.height ?? 0) * PresentationDetent.mediumDetentFraction
    @StateObject var errorBannerVM = ErrorBannerViewModel()
    @State var favoritesVM = ViewModelDI().favorites
    @StateObject var nearbyVM = NearbyViewModel()
    @StateObject var mapVM = iosApp.MapViewModel()
    @StateObject var settingsVM = SettingsViewModel()
    @StateObject var stopDetailsVM = StopDetailsViewModel()

    @EnvironmentObject var settingsCache: SettingsCache
    var hideMaps: Bool { settingsCache.get(.hideMaps) }
    var enhancedFavorites: Bool { settingsCache.get(.enhancedFavorites) }

    let transition: AnyTransition = .asymmetric(insertion: .push(from: .bottom), removal: .opacity)
    let analytics: Analytics = AnalyticsProvider.shared

    let inspection = Inspection<Self>()

    @State var selectedDetent: PresentationDetent = .medium
    @State private var selectedTab = SelectedTab.nearby
    @State private var showingLocationPermissionAlert = false
    @State private var tabBarVisibility = Visibility.hidden

    struct AnalyticsParams: Equatable {
        let stopId: String?
        let analyticsScreen: AnalyticsScreen?
    }

    var body: some View {
        GeometryReader { proxy in
            VStack {
                contents
            }
            .onAppear { contentHeight = proxy.size.height }
            .onChange(of: proxy.size.height) { contentHeight = $0 }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .onAppear {
            Task { await contentVM.loadFeaturePromos() }
            Task { await contentVM.loadOnboardingScreens() }
            analytics.recordSession(colorScheme: colorScheme)
            analytics.recordSession(voiceOver: voiceOver)
            analytics.recordSession(hideMaps: hideMaps)
            updateTabBarVisibility(selectedTab)

            if let screen = nearbyVM.navigationStack.lastSafe().analyticsScreen {
                analytics.track(screen: screen)
            }
        }
        .task {
            // We can't set stale caches in ResponseCache on init because of our Koin setup,
            // so this is here to get the cached data into the global flow and kick off an async request asap.
            do {
                _ = try await RepositoryDI().global.getGlobalData()
            } catch {}
        }
        .onChange(of: selectedTab) { nextTab in
            nearbyVM.pushNavEntry(nextTab.associatedSheetNavEntry)
            updateTabBarVisibility(nextTab)
        }
        .onChange(of: AnalyticsParams(
            stopId: nearbyVM.navigationStack.lastSafe().stopId(),
            analyticsScreen: nearbyVM.navigationStack.lastSafe().analyticsScreen
        )) { params in
            guard let screen = params.analyticsScreen else { return }
            analytics.track(screen: screen)
        }
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
        .onChange(of: hideMaps) { _ in
            analytics.recordSession(hideMaps: hideMaps)
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
            mainContent
        }
    }

    @ViewBuilder
    var mainContent: some View {
        VStack {
            if hideMaps {
                ZStack(alignment: .top) {
                    searchHeaderBackground
                    VStack {
                        if nearbyVM.navigationStack.lastSafe().isEntrypoint {
                            SearchOverlay(searchObserver: searchObserver, nearbyVM: nearbyVM)
                                .padding(.top, 12)
                            if !searchObserver.isSearching {
                                LocationAuthButton(showingAlert: $showingLocationPermissionAlert)
                                    .padding(.bottom, 8)
                            }
                        }

                        if !(nearbyVM.navigationStack.lastSafe().isEntrypoint && searchObserver.isSearching) {
                            mapWithSheets
                        }
                    }
                }
            } else {
                ZStack(alignment: .top) {
                    mapWithSheets.accessibilityHidden(searchObserver.isSearching)
                    searchHeaderBackground
                    VStack(alignment: .center, spacing: 20) {
                        if nearbyVM.navigationStack.lastSafe().isEntrypoint {
                            SearchOverlay(searchObserver: searchObserver, nearbyVM: nearbyVM)

                            if !searchObserver.isSearching {
                                LocationAuthButton(showingAlert: $showingLocationPermissionAlert)
                            }
                        }
                        if !searchObserver.isSearching {
                            VStack(alignment: .trailing, spacing: 20) {
                                if !viewportProvider.viewport.isFollowing,
                                   locationDataManager.currentLocation != nil {
                                    RecenterButton(icon: .faLocationArrowSolid, size: 17.33) {
                                        viewportProvider.follow()
                                    }
                                }
                                if !viewportProvider.viewport.isOverview,
                                   let (routeType, selectedVehicle, stop) = recenterOnVehicleButtonInfo() {
                                    RecenterButton(icon: routeIconResource(routeType), size: 32) {
                                        viewportProvider.vehicleOverview(vehicle: selectedVehicle, stop: stop)
                                    }
                                }
                            }.frame(maxWidth: .infinity, alignment: .topTrailing)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.top, 12)
                }
            }
        }
        .background(Color.sheetBackground)
        .onAppear {
            Task { await errorBannerVM.activate() }
        }
    }

    @ViewBuilder
    var navSheetContents: some View {
        let navEntry = nearbyVM.navigationStack.lastSafe()
        NavigationStack {
            VStack {
                switch navEntry {
                case .editFavorites, .favorites, .nearby:
                    tabbedSheetContents.transition(transition)

                case let .routeDetails(navEntry):
                    // Wrapping in a TabView helps the page to animate in as a single unit
                    // Otherwise only the header animates
                    TabView {
                        RouteDetailsView(
                            selectionId: navEntry.routeId, context: navEntry.context,
                            onOpenStopDetails: { nearbyVM.pushNavEntry(.stopDetails(
                                stopId: $0,
                                stopFilter: nil,
                                tripFilter: nil
                            )) }, onBack: { nearbyVM.goBack() }, onClose: { nearbyVM.popToEntrypoint() },
                            errorBannerVM: errorBannerVM
                        )
                        .toolbar(.hidden, for: .tabBar)
                    }
                    .transition(transition)

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
                        .toolbar(.hidden, for: .tabBar)
                    }
                    // Set id per stop so that transitioning from one stop to another is handled by removing
                    // the existing stop view & creating a new one
                    .id(stopId)
                    .transition(transition)
                    .animation(.easeOut, value: stopId)
                    .onChange(of: stopId) { nextStopId in stopDetailsVM.handleStopChange(nextStopId) }
                    .onAppear { stopDetailsVM.handleStopAppear(stopId) }
                    .onDisappear { stopDetailsVM.leaveStopPredictions() }

                default: EmptyView()
                }
            }
            .animation(.easeOut, value: nearbyVM.navigationStack.lastSafe().sheetItemIdentifiable()?.id)
            .background { Color.fill2.ignoresSafeArea(edges: .all).animation(nil, value: "") }
        }
    }

    @ViewBuilder
    var tabbedSheetContents: some View {
        // Putting the TabView in a VStack prevents the tabs from covering the nearby transit contents
        // when re-opening nearby transit
        VStack {
            TabView(selection: $selectedTab) {
                if enhancedFavorites {
                    favoritesPage
                        .toolbar(tabBarVisibility, for: .tabBar)
                        .tag(SelectedTab.favorites)
                        .tabItem { TabLabel(tab: SelectedTab.favorites) }
                }

                nearbyPage
                    .toolbar(tabBarVisibility, for: .tabBar)
                    .tag(SelectedTab.nearby)
                    .tabItem { TabLabel(tab: SelectedTab.nearby) }

                VStack {}
                    .toolbar(.hidden, for: .tabBar)
                    .onAppear { selectedTab = .more }
                    .tag(SelectedTab.more)
                    .tabItem { TabLabel(tab: SelectedTab.more) }
            }
        }
    }

    @ViewBuilder
    var favoritesPage: some View {
        let navEntry = nearbyVM.navigationStack.lastSafe()
        VStack {
            if case .editFavorites = navEntry {
                // Wrapping in a TabView helps the page to animate in as a single unit
                // Otherwise only the header animates
                TabView {
                    EditFavoritesPage(
                        viewModel: favoritesVM,
                        onClose: { nearbyVM.popToEntrypoint() },
                        errorBannerVM: errorBannerVM
                    )
                    .toolbar(.hidden, for: .tabBar)
                }
                .transition(transition)
            } else {
                FavoritesPage(
                    errorBannerVM: errorBannerVM,
                    favoritesVM: favoritesVM,
                    nearbyVM: nearbyVM,
                    viewportProvider: viewportProvider
                )
                .transition(transition)
            }
        }.animation(.easeOut, value: navEntry.sheetItemIdentifiable()?.id)
    }

    @ViewBuilder
    var nearbyPage: some View {
        NearbyTransitPage(
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            viewportProvider: viewportProvider,
            noNearbyStops: { NoNearbyStopsView(
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
    var map: some View {
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
        if hideMaps {
            navSheetContents
                .fullScreenCover(item: .constant(nav.coverItemIdentifiable()), onDismiss: {
                    // Don't navigate back if hideMaps has been changed and the cover is being switched over
                    if hideMaps == false { return }
                    switch nearbyVM.navigationStack.last {
                    case .alertDetails, .more: nearbyVM.goBack()
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
            map.sheet(
                isPresented: .constant(
                    !(searchObserver.isSearching && nav.isEntrypoint)
                        && !showingLocationPermissionAlert
                        && contentVM.onboardingScreensPending != nil
                ),
                content: {
                    GeometryReader { proxy in
                        VStack {
                            navSheetContents
                                .presentationDetents([.small, .medium, .almostFull], selection: $selectedDetent)
                                .interactiveDismissDisabled()
                                .modifier(AllowsBackgroundInteraction())
                        }
                        // within the sheet to prevent issues on iOS 16 with two modal views open at once
                        .fullScreenCover(
                            item: .constant(nav.coverItemIdentifiable()),
                            onDismiss: {
                                // Don't navigate back if hideMaps has been changed and the cover is being switched over
                                if hideMaps { return }
                                switch nearbyVM.navigationStack.last {
                                case .alertDetails, .more: nearbyVM.goBack()
                                default: break
                                }
                            },
                            content: coverContents
                        )
                        .onChange(of: sheetItemId) { _ in selectedDetent = .medium }
                        .onAppear { recordSheetHeight(proxy.size.height) }
                        .onChange(of: proxy.size.height) { newValue in recordSheetHeight(newValue) }
                    }
                }
            )
        }
    }

    @ViewBuilder
    private func coverContents(coverIdentityEntry: NearbyCoverItem) -> some View {
        let entry = coverIdentityEntry.stackEntry
        NavigationStack {
            switch entry {
            case let .alertDetails(alertId, line, routes, stop):
                AlertDetailsPage(alertId: alertId, line: line, routes: routes, stop: stop, nearbyVM: nearbyVM)

            case .more:
                TabView(selection: $selectedTab) {
                    if enhancedFavorites {
                        VStack {}
                            .onAppear { selectedTab = .favorites }
                            .toolbar(.hidden, for: .tabBar)
                            .tag(SelectedTab.favorites)
                            .tabItem { TabLabel(tab: SelectedTab.favorites) }
                    }

                    VStack {}
                        .onAppear { selectedTab = .nearby }
                        .toolbar(.hidden, for: .tabBar)
                        .tag(SelectedTab.nearby)
                        .tabItem { TabLabel(tab: SelectedTab.nearby) }

                    MorePage(viewModel: settingsVM)
                        .toolbar(tabBarVisibility, for: .tabBar)
                        .toolbarBackground(.visible, for: .tabBar)
                        .tag(SelectedTab.more)
                        .tabItem { TabLabel(tab: SelectedTab.more) }
                }

            default:
                EmptyView()
            }
        }
    }

    @ViewBuilder
    var searchHeaderBackground: some View {
        (
            searchObserver.isSearching && nearbyVM.navigationStack.lastSafe().isEntrypoint
                ? Color.fill2 : Color.clear
        ).ignoresSafeArea(.all)
    }

    private func recenterOnVehicleButtonInfo() -> (RouteType, Vehicle, Stop)? {
        guard case let .stopDetails(stopId: _, stopFilter: stopFilter, tripFilter: tripFilter) = nearbyVM
            .navigationStack.lastSafe(),
            let selectedVehicle = mapVM.selectedVehicle, tripFilter?.vehicleId == selectedVehicle.id,
            let globalData = mapVM.globalData,
            let stop = nearbyVM.getTargetStop(global: globalData),
            let routeId = selectedVehicle.routeId ?? stopFilter?.routeId,
            let route = globalData.getRoute(routeId: routeId) else { return nil }
        return (route.type, selectedVehicle, stop)
    }

    private func recordSheetHeight(_ newSheetHeight: CGFloat) {
        /*
         Only update this if we're less than half way up the users screen. Otherwise,
         the entire map is blocked by the sheet anyway, so it doesn't need to respond to height changes
         */
        guard newSheetHeight < ((contentHeight - 8) * PresentationDetent.mediumDetentFraction) else { return }
        sheetHeight = newSheetHeight
    }

    private func updateTabBarVisibility(_: SelectedTab) {
        let shouldShowTabBar =
            nearbyVM.navigationStack.lastSafe().isEntrypoint
                && !searchObserver.isSearching

        tabBarVisibility = shouldShowTabBar ? .visible : .hidden
    }

    struct AllowsBackgroundInteraction: ViewModifier {
        func body(content: Content) -> some View {
            content.presentationBackgroundInteraction(.enabled(upThrough: .medium))
        }
    }
}
