import CoreLocation
@_spi(Experimental) import MapboxMaps
import Shared
import SwiftPhoenixClient
import SwiftUI

// swiftlint:disable:next type_body_length
struct ContentView: View {
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.accessibilityVoiceOverEnabled) var voiceOver

    @StateObject var searchObserver = TextFieldObserver()
    @EnvironmentObject var locationDataManager: LocationDataManager
    @EnvironmentObject var socketProvider: SocketProvider
    @EnvironmentObject var viewportProvider: ViewportProvider

    @ObservedObject var contentVM: ContentViewModel

    @State private var contentHeight: CGFloat = UIScreen.current?.bounds.height ?? 0
    @State private var sheetHeight: CGFloat =
        (UIScreen.current?.bounds.height ?? 0) * PresentationDetent.mediumDetentFraction
    @State var errorBannerVM = ViewModelDI().errorBanner
    @State var favoritesVM = ViewModelDI().favorites
    @State var routeCardDataVM = ViewModelDI().routeCardData
    @State var stopDetailsVM = ViewModelDI().stopDetails
    @State var toastVM = ViewModelDI().toast

    @StateObject var nearbyVM = NearbyViewModel()
    @State var mapVM = ViewModelDI().map

    @EnvironmentObject var settingsCache: SettingsCache
    var hideMaps: Bool { settingsCache.get(.hideMaps) }

    let transition: AnyTransition = .asymmetric(insertion: .push(from: .bottom), removal: .opacity)
    let analytics: Analytics = AnalyticsProvider.shared

    let inspection = Inspection<Self>()

    @State var selectedDetent: PresentationDetent = .medium
    @State private var selectedTab: SelectedTab? = nil
    @State private var showingLocationPermissionAlert = false
    @State private var tabBarVisibility = Visibility.hidden
    @State private var selectedVehicle: Vehicle?
    @State private var globalData: GlobalResponse?

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
            mapVM.setViewportManager(viewportManager: viewportProvider)
            Task { await contentVM.loadPendingFeaturePromosAndTabPreferences() }
            Task { await contentVM.loadOnboardingScreens() }
            analytics.recordSession(colorScheme: colorScheme)
            analytics.recordSession(voiceOver: voiceOver)
            analytics.recordSession(hideMaps: hideMaps)
            updateTabBarVisibility()

            if let screen = nearbyVM.navigationStack.lastSafe().analyticsScreen {
                analytics.track(screen: screen)
            }
        }
        .global($globalData, errorKey: "ContentView")
        .onChange(of: contentVM.defaultTab) { newTab in
            selectedTab = switch newTab {
            case .favorites: .favorites
            case .nearby: .nearby
            case nil: nil
            }
        }
        .onChange(of: selectedTab) { nextTab in
            if let nextTab {
                nearbyVM.pushNavEntry(nextTab.associatedSheetNavEntry)
                updateTabBarVisibility()
            }
        }

        .onChange(of: nearbyVM.navigationStack.lastSafe()) { _ in
            updateTabBarVisibility()
        }
        .onChange(of: AnalyticsParams(
            stopId: nearbyVM.navigationStack.lastSafe().stopId(),
            analyticsScreen: nearbyVM.navigationStack.lastSafe().analyticsScreen
        )) { params in
            guard let screen = params.analyticsScreen else { return }
            analytics.track(screen: screen)
        }
        .onChange(of: searchObserver.isSearching) { _ in updateTabBarVisibility() }
        .onChange(of: colorScheme) { _ in
            analytics.recordSession(colorScheme: colorScheme)
        }
        .onChange(of: voiceOver) { _ in
            analytics.recordSession(voiceOver: voiceOver)
        }
        .onChange(of: hideMaps) { _ in
            analytics.recordSession(hideMaps: hideMaps)
        }
        .onChange(of: contentVM.featurePromosPending) { promos in
            if let promos, promos.contains(where: { $0 == .enhancedFavorites }) {
                favoritesVM.setIsFirstExposureToNewFavorites(isFirst: true)
            }
        }
        .withScenePhaseHandlers(
            onActive: {
                socketProvider.socket.attach()
                nearbyVM.joinAlertsChannel()
            },
            onBackground: {
                nearbyVM.leaveAlertsChannel()
                socketProvider.socket.detach()
            }
        )
        .withBackgroundTimer {
            nearbyVM.popToEntrypoint()
            mapVM.recenter(type: .currentLocation)
        }
        .onReceive(
            contentVM.mapboxConfigManager.lastMapboxErrorSubject
                .debounce(for: .seconds(1), scheduler: DispatchQueue.main)
        ) { _ in
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
                            HStack(alignment: .top) {
                                VStack(alignment: .leading) {
                                    if navCallbacks.backButtonPresentation == .floating {
                                        RecenterButton(
                                            icon: .faChevronLeft,
                                            label: Text("Back", comment: "VoiceOver label for a generic back button"),
                                            size: 18.67
                                        ) {
                                            nearbyVM.goBack()
                                        }
                                    }
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 20) {
                                    if !viewportProvider.viewport.isFollowing,
                                       locationDataManager.currentLocation != nil,
                                       nearbyVM.navigationStack.lastSafe().showCurrentLocation {
                                        RecenterButton(icon: .faLocationArrowSolid, label: Text(
                                            "Recenter map on my location",
                                            comment: "Screen reader text describing the behavior of the map recenter on current location button"
                                        ), size: 17.33) {
                                            mapVM.recenter(type: .currentLocation)
                                        }
                                    }
                                    if !viewportProvider.viewport.isOverview,
                                       let routeType = vehicleRouteType() {
                                        RecenterButton(icon: routeIconResource(routeType), label: Text(
                                            "Recenter map on \(routeType.typeText(isOnly: true))",
                                            comment: "Screen reader text describing the behavior of the map recenter on trip button (e.g. “Recenter map on train”)"
                                        ), size: 32) {
                                            mapVM.recenter(type: .trip)
                                        }
                                    }
                                }
                            }.frame(maxWidth: .infinity, alignment: .top)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.top, 12)
                }
            }
        }
        .background(Color.sheetBackground)
    }

    @ViewBuilder
    var sheetContents: some View {
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
                            )) },
                            pushNavEntry: { entry in nearbyVM.pushNavEntry(entry) },
                            navCallbacks: navCallbacks,
                            errorBannerVM: errorBannerVM,
                        )
                        .toolbar(.hidden, for: .tabBar)
                    }
                    .transition(transition)

                case let .routePicker(navEntry):
                    // Wrapping in a TabView helps the page to animate in as a single unit
                    // Otherwise only the header animates
                    TabView {
                        RoutePickerView(
                            context: navEntry.context,
                            path: navEntry.path,
                            errorBannerVM: errorBannerVM,
                            onOpenRouteDetails: { routeId, context in
                                nearbyVM.pushNavEntry(
                                    SheetNavigationStackEntry.routeDetails(
                                        SheetRoutes.RouteDetails(routeId: routeId, context: context)
                                    )
                                )
                            },
                            onOpenPickerPath: { path, context in
                                if navEntry.path != path {
                                    nearbyVM.pushNavEntry(
                                        SheetNavigationStackEntry.routePicker(
                                            SheetRoutes.RoutePicker(path: path, context: context)
                                        )
                                    )
                                }
                            },
                            navCallbacks: navCallbacks
                        )
                        .toolbar(.hidden, for: .tabBar)
                    }
                    .id(navEntry)
                    .transition(transition)
                    .animation(.easeOut, value: navEntry)

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
                            navCallbacks: navCallbacks,
                            errorBannerVM: errorBannerVM,
                            nearbyVM: nearbyVM,
                            mapVM: mapVM,
                            routeCardDataVM: routeCardDataVM,
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

                case let .tripDetails(filter: filter):
                    // Wrapping in a TabView helps the page to animate in as a single unit
                    // Otherwise only the header animates
                    TabView {
                        TripDetailsPage(
                            filter: filter,
                            navCallbacks: navCallbacks,
                            nearbyVM: nearbyVM,
                        )
                        .toolbar(.hidden, for: .tabBar)
                    }
                    // Set id per trip so that transitioning from one trip to another is handled by removing
                    // the existing trip view & creating a new one
                    .id(filter.tripId)
                    .transition(transition)
                    .animation(.easeOut, value: filter.tripId)

                default: EmptyView()
                }
            }
            .animation(.easeOut, value: nearbyVM.navigationStack.lastSafe().sheetItemIdentifiable()?.id)
            .background { Color.fill2.ignoresSafeArea(edges: .all).animation(nil, value: "") }
        }
        .onOpenURL { url in
            let deepLink = DeepLinkState.companion.from(url: url.absoluteString)
            nearbyVM.popToEntrypoint()

            switch onEnum(of: deepLink) {
            case let .stop(stop):
                let nav = stop.sheetRoute
                nearbyVM.pushNavEntry(.stopDetails(
                    stopId: nav.stopId,
                    stopFilter: nav.stopFilter,
                    tripFilter: nav.tripFilter
                ))

            case let .alert(alert):
                var stop: Stop?
                if let stopId = alert.stopId {
                    nearbyVM.pushNavEntry(
                        .stopDetails(stopId: stopId, stopFilter: nil, tripFilter: nil)
                    )
                    stop = globalData?.getStop(stopId: alert.stopId)
                }
                var line: Line?
                var routes: [Route]?
                if let routeId = alert.routeId {
                    let lineOrRouteId = LineOrRoute.Id.companion.fromString(id: routeId)
                    let lineOrRoute = globalData?.getLineOrRoute(lineOrRouteId: lineOrRouteId)
                    switch onEnum(of: lineOrRoute) {
                    case let .line(resolved):
                        line = resolved.line
                        routes = Array(resolved.routes)
                    case let .route(resolved):
                        routes = [resolved.route]
                    default: break
                    }
                }
                nearbyVM.pushNavEntry(
                    .alertDetails(alertId: alert.alertId, line: line, routes: routes, stop: stop)
                )

            default: break
            }
        }
    }

    @ViewBuilder
    var sheetContentsPlaceholder: some View {
        ZStack {
            Color.sheetBackground.ignoresSafeArea(.all)
            VStack {
                VStack(spacing: 16) {
                    SheetHeader(
                        title: NSLocalizedString("Nearby Transit", comment: ""),
                        navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating)
                    )
                    .loadingPlaceholder(withShimmer: false)
                    ScrollView {
                        LazyVStack(alignment: .center, spacing: 14) {
                            ForEach(0 ..< 5) { _ in
                                LoadingRouteCard()
                            }
                        }
                        .padding(.vertical, 4)
                        .padding(.horizontal, 16)
                        .loadingPlaceholder(withShimmer: false)
                    }
                    Spacer()
                }
            }
        }
    }

    @ViewBuilder
    var navSheetContents: some View {
        if let defaultTab = contentVM.defaultTab,
           let featurePromosPending = contentVM.featurePromosPending,
           let onboardingScreensPending = contentVM.onboardingScreensPending {
            sheetContents
                .toast(vm: toastVM, tabBarVisible: tabBarVisibility == .visible)
        } else {
            sheetContentsPlaceholder
        }
    }

    @ViewBuilder
    var tabbedSheetContents: some View {
        // Putting the TabView in a VStack prevents the tabs from covering the nearby transit contents
        // when re-opening nearby transit
        VStack {
            TabView(selection: $selectedTab) {
                favoritesPage
                    .toolbar(tabBarVisibility, for: .tabBar)
                    .tag(SelectedTab.favorites)
                    .tabItem { TabLabel(tab: SelectedTab.favorites) }
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
                        navCallbacks: navCallbacks.doCopy(
                            onBack: navCallbacks.onBack,
                            onClose: navCallbacks.onClose,
                            backButtonPresentation: .header
                        ),
                        errorBannerVM: errorBannerVM,
                        toastVM: toastVM,
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
        }
        .animation(.easeOut, value: navEntry.sheetItemIdentifiable()?.id)
        .task { await contentVM.setTabPreference(.favorites) }
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
                    viewportProvider.panToDefaultCenter()
                }
            ) }
        )
        .task { await contentVM.setTabPreference(.nearby) }
    }

    @ViewBuilder
    var map: some View {
        HomeMapView(
            contentVM: contentVM,
            mapVM: mapVM,
            nearbyVM: nearbyVM,
            routeCardDataVM: routeCardDataVM,
            viewportProvider: viewportProvider,
            locationDataManager: locationDataManager,
            sheetHeight: $sheetHeight,
            selectedVehicle: $selectedVehicle
        ).accessibilityHidden(searchObserver.isSearching)
    }

    @ViewBuilder var mapWithSheets: some View {
        let nav = nearbyVM.navigationStack.lastSafe()
        let sheetRoute = nav.toSheetRoute()
        if hideMaps {
            navSheetContents
                .fullScreenCover(item: .constant(nav.coverItemIdentifiable()), onDismiss: {
                    // Don't navigate back if hideMaps has been changed and the cover is being switched over
                    if hideMaps == false { return }
                    switch nearbyVM.navigationStack.last {
                    case .alertDetails, .more, .saveFavorite: nearbyVM.goBack()
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
                                case .alertDetails, .more, .saveFavorite: nearbyVM.goBack()
                                default: break
                                }
                            },
                            content: coverContents
                        )
                        .onChange(of: sheetRoute) { [oldSheetRoute = sheetRoute] newSheetRoute in
                            if let oldSheetRoute,
                               let newSheetRoute,
                               SheetRoutes.companion.shouldResetSheetHeight(first: oldSheetRoute,
                                                                            second: newSheetRoute) {
                                selectedDetent = .medium
                            }
                            errorBannerVM.setSheetRoute(sheetRoute: newSheetRoute)
                        }
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
                    VStack {}
                        .onAppear { selectedTab = .favorites }
                        .toolbar(.hidden, for: .tabBar)
                        .tag(SelectedTab.favorites)
                        .tabItem { TabLabel(tab: SelectedTab.favorites) }
                    VStack {}
                        .onAppear { selectedTab = .nearby }
                        .toolbar(.hidden, for: .tabBar)
                        .tag(SelectedTab.nearby)
                        .tabItem { TabLabel(tab: SelectedTab.nearby) }

                    MorePage()
                        .toolbar(tabBarVisibility, for: .tabBar)
                        .toolbarBackground(.visible, for: .tabBar)
                        .tag(SelectedTab.more)
                        .tabItem { TabLabel(tab: SelectedTab.more) }
                }

            case let .saveFavorite(routeId, stopId, selectedDirection, context):
                SaveFavoritePage(
                    routeId: routeId,
                    stopId: stopId,
                    selectedDirection: selectedDirection,
                    context: context,
                    updateFavorites: { favorites in
                        print("~~~ update in ContentView")
                        favoritesVM.updateFavorites(
                            updatedFavorites: favorites,
                            context: context,
                            defaultDirection: selectedDirection
                        )
                    },
                    navCallbacks: navCallbacks,
                    nearbyVM: nearbyVM
                )

            default:
                EmptyView()
            }
        }
    }

    var navCallbacks: NavigationCallbacks {
        .init(
            onBack: { nearbyVM.goBack() },
            onClose: { nearbyVM.popToEntrypoint() },
            backButtonPresentation: selectedDetent == .almostFull || hideMaps ||
                !nearbyVM.navigationStack.hasFloatingBackButton() ? .header : .floating
        )
    }

    @ViewBuilder
    var searchHeaderBackground: some View {
        (
            searchObserver.isSearching && nearbyVM.navigationStack.lastSafe().isEntrypoint
                ? Color.fill2 : Color.clear
        ).ignoresSafeArea(.all)
    }

    private func vehicleRouteType() -> RouteType? {
        guard let routeId = selectedVehicle?.routeId,
              let route = globalData?.getRoute(routeId: routeId)
        else { return nil }
        return route.type
    }

    private func recordSheetHeight(_ newSheetHeight: CGFloat) {
        /*
         Only update this if we're less than half way up the users screen. Otherwise,
         the entire map is blocked by the sheet anyway, so it doesn't need to respond to height changes
         */
        guard newSheetHeight < ((contentHeight - 8) * PresentationDetent.mediumDetentFraction) else { return }
        sheetHeight = newSheetHeight
    }

    private func updateTabBarVisibility() {
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
