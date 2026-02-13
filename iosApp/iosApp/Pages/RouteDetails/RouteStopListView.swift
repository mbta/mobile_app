//
//  RouteStopListView.swift
//  iosApp
//
//  Created by Melody Horn on 7/7/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

enum RouteDetailsRowContext: Equatable {
    case details(stop: Stop)
    case favorites(isFavorited: Bool, onTapStar: () -> Void)

    static func == (lhs: RouteDetailsRowContext, rhs: RouteDetailsRowContext) -> Bool {
        switch (lhs, rhs) {
        case let (.details(stop: lhs), .details(stop: rhs)):
            lhs == rhs
        case let (.favorites(isFavorited: lhs, onTapStar: _), .favorites(isFavorited: rhs, onTapStar: _)):
            lhs == rhs
        case (.details, .favorites):
            false
        case (.favorites, .details):
            false
        }
    }
}

struct RouteStopListView<RightSideContent: View>: View {
    let lineOrRoute: LineOrRoute
    let parameters: RouteDetailsStopList.RouteParameters
    let context: RouteDetailsContext
    let globalData: GlobalResponse
    let onClick: (RouteDetailsRowContext) -> Void
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let navCallbacks: NavigationCallbacks
    let errorBannerVM: IErrorBannerViewModel
    let defaultSelectedRouteId: Route.Id?
    let rightSideContent: (RouteDetailsRowContext) -> RightSideContent
    let favoritesVM: IFavoritesViewModel
    let toastVM: IToastViewModel

    let routeStopsRepository: IRouteStopsRepository
    @State var routeStops: RouteStopsResult?
    @State var stopList: RouteDetailsStopList?

    @State var selectedRouteId: Route.Id
    @State var selectedDirection: Int32

    let inspection = Inspection<Self>()

    init(
        lineOrRoute: LineOrRoute,
        context: RouteDetailsContext,
        globalData: GlobalResponse,
        onClick: @escaping (RouteDetailsRowContext) -> Void,
        pushNavEntry: @escaping (SheetNavigationStackEntry) -> Void,
        navCallbacks: NavigationCallbacks,
        errorBannerVM: IErrorBannerViewModel,
        defaultSelectedRouteId: Route.Id? = nil,
        rightSideContent: @escaping (RouteDetailsRowContext) -> RightSideContent,
        routeStopsRepository: IRouteStopsRepository = RepositoryDI().routeStops,
        favoritesVM: IFavoritesViewModel = ViewModelDI().favorites,
        toastVM: IToastViewModel = ViewModelDI().toast,
    ) {
        self.lineOrRoute = lineOrRoute
        self.context = context
        self.globalData = globalData
        self.onClick = onClick
        self.pushNavEntry = pushNavEntry
        self.navCallbacks = navCallbacks
        self.errorBannerVM = errorBannerVM
        self.defaultSelectedRouteId = defaultSelectedRouteId
        self.rightSideContent = rightSideContent
        self.routeStopsRepository = routeStopsRepository
        self.favoritesVM = favoritesVM
        self.toastVM = toastVM

        selectedRouteId = defaultSelectedRouteId ?? lineOrRoute.sortRoute.id
        let parameters = RouteDetailsStopList.RouteParameters(lineOrRoute: lineOrRoute, globalData: globalData)
        self.parameters = parameters
        selectedDirection = Int32(truncating: parameters.availableDirections.first ?? 0)
    }

    private struct RouteStopsParams: Equatable {
        let routeId: Route.Id
        let directionId: Int32
    }

    private struct RouteStopListParams: Equatable {
        let routeId: Route.Id
        let directionId: Int32
        let routeStops: RouteStopsResult?
        let globalData: GlobalResponse
    }

    var body: some View {
        RouteStopListContentView(
            lineOrRoute: lineOrRoute,
            parameters: parameters,
            selectedDirection: selectedDirection,
            setSelectedDirection: { selectedDirection = $0 },
            selectedRouteId: selectedRouteId, setSelectedRouteId: { selectedRouteId = $0 },
            stopList: stopList,
            context: context,
            globalData: globalData,
            onClick: onClick,
            pushNavEntry: pushNavEntry,
            navCallbacks: navCallbacks,
            errorBannerVM: errorBannerVM,
            rightSideContent: rightSideContent,
            favoritesVM: favoritesVM,
            toastVM: toastVM
        )
        .onAppear {
            loadEverything()
            if !parameters.availableDirections.contains(where: { $0.intValue == selectedDirection }) {
                selectedDirection = Int32(truncating: parameters.availableDirections.first ?? 0)
            }
        }
        .onChange(of: RouteStopsParams(routeId: selectedRouteId, directionId: selectedDirection)) {
            loadRouteStops(routeId: $0.routeId, directionId: $0.directionId)
        }
        .onChange(of: RouteStopListParams(
            routeId: selectedRouteId,
            directionId: selectedDirection,
            routeStops: routeStops,
            globalData: globalData
        )) {
            loadStopList(
                routeId: $0.routeId,
                directionId: $0.directionId,
                routeStops: $0.routeStops,
                globalData: $0.globalData
            )
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    private func loadEverything() {
        loadRouteStops(routeId: selectedRouteId, directionId: selectedDirection)
        if let routeStops {
            loadStopList(
                routeId: selectedRouteId,
                directionId: selectedDirection,
                routeStops: routeStops,
                globalData: globalData
            )
        }
    }

    private func loadRouteStops(routeId: Route.Id, directionId: Int32) {
        Task {
            await fetchApi(
                errorKey: "RouteStopListView.loadRouteStops",
                getData: { try await routeStopsRepository.getRouteSegments(
                    routeId: routeId,
                    directionId: directionId
                ) },
                onSuccess: { routeStops = $0 },
                onRefreshAfterError: loadEverything
            )
        }
    }

    private func loadStopList(
        routeId: Route.Id,
        directionId: Int32,
        routeStops: RouteStopsResult?,
        globalData: GlobalResponse
    ) {
        Task {
            stopList = try? await RouteDetailsStopList.companion.fromPieces(
                routeId: routeId,
                directionId: directionId,
                routeStops: routeStops,
                globalData: globalData
            )
        }
    }
}

struct RouteStopListContentView<RightSideContent: View>: View {
    let lineOrRoute: LineOrRoute
    let parameters: RouteDetailsStopList.RouteParameters
    let selectedDirection: Int32
    let setSelectedDirection: (Int32) -> Void
    let selectedRouteId: Route.Id
    let setSelectedRouteId: (Route.Id) -> Void
    let routes: [Route]
    let stopList: RouteDetailsStopList?
    let context: RouteDetailsContext
    let globalData: GlobalResponse
    let onClick: (RouteDetailsRowContext) -> Void
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let navCallbacks: NavigationCallbacks
    let errorBannerVM: IErrorBannerViewModel
    let rightSideContent: (RouteDetailsRowContext) -> RightSideContent
    let favoritesVM: IFavoritesViewModel
    let toastVM: IToastViewModel

    @State var favorites: Favorites = LoadedFavorites.last

    @State var showFavoritesStopConfirmation: Stop?
    @State var showFirstTimeFavoritesToast: Bool?
    @State var displayedToast: ToastViewModel.Toast?
    @State var firstTimeToast: ToastViewModel.Toast?

    @ObservedObject var fcmTokenContainer = FcmTokenContainer.shared
    @EnvironmentObject var settingsCache: SettingsCache

    let inspection = Inspection<Self>()

    init(
        lineOrRoute: LineOrRoute,
        parameters: RouteDetailsStopList.RouteParameters,
        selectedDirection: Int32,
        setSelectedDirection: @escaping (Int32) -> Void,
        selectedRouteId: Route.Id,
        setSelectedRouteId: @escaping (Route.Id) -> Void,
        stopList: RouteDetailsStopList?,
        context: RouteDetailsContext,
        globalData: GlobalResponse,
        onClick: @escaping (RouteDetailsRowContext) -> Void,
        pushNavEntry: @escaping (SheetNavigationStackEntry) -> Void,
        navCallbacks: NavigationCallbacks,
        errorBannerVM: IErrorBannerViewModel,
        rightSideContent: @escaping (RouteDetailsRowContext) -> RightSideContent,
        favoritesVM: IFavoritesViewModel = ViewModelDI().favorites,
        toastVM: IToastViewModel = ViewModelDI().toast
    ) {
        self.lineOrRoute = lineOrRoute
        self.parameters = parameters
        self.selectedDirection = selectedDirection
        self.setSelectedDirection = setSelectedDirection
        self.selectedRouteId = selectedRouteId
        self.setSelectedRouteId = setSelectedRouteId
        routes = lineOrRoute.allRoutes.sorted(by: { $0.sortOrder <= $1.sortOrder })
        self.stopList = stopList
        self.context = context
        self.globalData = globalData
        self.onClick = onClick
        self.pushNavEntry = pushNavEntry
        self.navCallbacks = navCallbacks
        self.errorBannerVM = errorBannerVM
        self.rightSideContent = rightSideContent
        self.favoritesVM = favoritesVM
        self.toastVM = toastVM
    }

    private struct RouteStopsParams: Equatable {
        let routeId: String
        let directionId: Int32
    }

    private struct RouteStopListParams: Equatable {
        let routeId: String
        let directionId: Int32
        let routeStops: RouteStopsResult?
        let globalData: GlobalResponse
    }

    private var routeColor: Color { Color(hex: lineOrRoute.backgroundColor) }
    private var textColor: Color { Color(hex: lineOrRoute.textColor) }
    private var haloColor: Color {
        if lineOrRoute.type == .bus, let routeId = lineOrRoute.id as? Route.Id, !silverRoutes.contains(routeId) {
            Color.haloLight
        } else {
            Color.haloDark
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            SheetHeader(
                title: lineOrRoute.name,
                titleAccessibilityLabel: lineOrRoute.labelWithModeIfBus,
                titleColor: textColor,
                buttonColor: .routeColorContrast,
                buttonTextColor: .routeColorContrastText,
                navCallbacks: navCallbacks,
                closeText: NSLocalizedString("Done", comment: "Button text for closing flow")
            )
            ErrorBanner(errorBannerVM, padding: .init([.horizontal, .top], 16))
            DirectionPicker(
                availableDirections: parameters.availableDirections.map { Int32(truncating: $0) },
                directions: parameters.directions,
                route: lineOrRoute.sortRoute,
                selectedDirectionId: selectedDirection,
                updateDirectionId: { setSelectedDirection($0) }
            )
            .fixedSize(horizontal: false, vertical: true).padding([.horizontal, .top], 14).padding(.bottom, 10)
            if case let .line(lineOrRoute) = onEnum(of: lineOrRoute), routes.count > 1 {
                lineRoutePicker(line: lineOrRoute.line, routes: routes)
            }
            routeStopList(stopList: stopList, onTapStop: onClick)
        }
        .favorites($favorites)
        .background { routeColor.ignoresSafeArea() }
        .task {
            for await model in toastVM.models {
                displayedToast = switch onEnum(of: model) {
                case .hidden: nil
                case let .visible(toast): toast.toast
                }
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .overlay {
            if let showFavoritesStopConfirmation {
                favoriteDialog(stop: showFavoritesStopConfirmation)
            }
        }
        .onChange(of: favorites) { favorites in
            // Only set first time toast on first favorites load, otherwise keep the current value
            showFirstTimeFavoritesToast = if let showFirstTimeFavoritesToast {
                showFirstTimeFavoritesToast
            } else {
                context is RouteDetailsContext.Favorites && favorites.routeStopDirection.isEmpty
            }
        }
        .onChange(of: showFirstTimeFavoritesToast) { _ in
            if showFirstTimeFavoritesToast == true, displayedToast == nil {
                let toast = ToastViewModel.Toast(
                    message: NSLocalizedString("Tap stops to add to Favorites", comment: ""),
                    duration: .indefinite,
                    isTip: true,
                    action: ToastViewModel.ToastActionClose(onClose: { showFirstTimeFavoritesToast = false })
                )
                toastVM.showToast(toast: toast)
                firstTimeToast = toast
            } else if showFirstTimeFavoritesToast == false,
                      displayedToast == firstTimeToast {
                toastVM.hideToast()
            }
        }
        .onDisappear { toastVM.hideToast() }
    }

    @ViewBuilder private func routeStopList(
        stopList: RouteDetailsStopList?,
        onTapStop: @escaping (RouteDetailsRowContext) -> Void
    ) -> some View {
        if let stopList, stopList.directionId == Int32(selectedDirection) {
            let hasTypicalSegment = stopList.segments.contains(where: \.isTypical)
            HaloScrollView(haloColor: haloColor) {
                VStack(spacing: 0) {
                    ForEach(Array(stopList.segments.enumerated()), id: \.offset) { segmentIndex, segment in
                        if segment.isTypical || !hasTypicalSegment {
                            ForEach(Array(segment.stops.enumerated()), id: \.offset) { stopIndex, stop in
                                let stopPlacement = StopPlacement(
                                    isFirst: segmentIndex == stopList.segments.startIndex && stopIndex == segment.stops
                                        .startIndex,
                                    isLast: segmentIndex == stopList.segments
                                        .index(before: stopList.segments.endIndex) && stopIndex == segment.stops
                                        .index(before: segment.stops.endIndex)
                                )
                                let stopRowContext = stopRowContext(stop.stop)
                                StopListRow(
                                    stop: stop.stop,
                                    stopLane: stop.stopLane,
                                    stickConnections: stop.stickConnections,
                                    onClick: { onTapStop(stopRowContext) },
                                    routeAccents: .init(route: lineOrRoute.sortRoute),
                                    stopListContext: .routeDetails,
                                    connectingRoutes: stop.connectingRoutes,
                                    stopPlacement: stopPlacement,
                                    descriptor: { EmptyView() },
                                    rightSideContent: { rightSideContent(stopRowContext) }
                                )
                            }
                        } else {
                            CollapsableStopList(
                                lineOrRoute: lineOrRoute,
                                segment: segment,
                                onClick: { onTapStop(stopRowContext($0.stop)) },
                                isFirstSegment: segmentIndex == stopList.segments.startIndex,
                                isLastSegment: segmentIndex == stopList.segments.endIndex,
                                rightSideContent: { stop in rightSideContent(stopRowContext(stop.stop)) }
                            )
                        }
                    }
                }
                .background { Color.fill2 }
                .withRoundedBorder(color: haloColor, width: 2)
                .padding(2)
                .padding(.horizontal, 14)
                .padding(.top, 4)
                .padding(.bottom, 32)
            }
        } else {
            AnyView(loadingRouteStops())
        }
    }

    @ViewBuilder private func lineRoutePicker(line: Line, routes: [Route]) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(routes, id: \.id) { route in
                let selected = route.id == selectedRouteId
                let rowColor = selected ? Color.fill3 : Color.clear
                let textColor = selected ? Color.text : Color(hex: line.textColor)
                Button {
                    setSelectedRouteId(route.id)
                } label: {
                    HStack(alignment: .center, spacing: 8) {
                        RoutePill(route: route, line: line, type: .fixed)
                        Text(((route.directionDestinations[Int(selectedDirection)] as? String?) ?? "") ?? "")
                            .foregroundStyle(textColor)
                            .font(Typography.title3Bold)
                        Spacer()
                    }
                    .padding(8)
                }
                .accessibilityAddTraits(selected ? [.isSelected, .isHeader] : [])
                .accessibilityHeading(selected ? .h2 : .unspecified)
                .accessibilitySortPriority(selected ? 1 : 0)
                .preventScrollTaps()
                .frame(minHeight: 44)
                .background(rowColor)
                .withRoundedBorder(color: selected ? .halo : Color.clear)
            }
        }
        .accessibilityElement(children: .contain)
        .frame(maxWidth: .infinity)
        .padding(2)
        .background(Color.routeColorContrast)
        .background(Color(hex: line.color))
        .withRoundedBorder(radius: 10, width: 0)
        .padding(.horizontal, 14)
        .padding(.vertical, 4)
    }

    @ViewBuilder private func loadingRouteStops() -> some View {
        let loadingStops = LoadingPlaceholders.shared.routeDetailsStops(
            lineOrRoute: lineOrRoute,
            directionId: Int32(selectedDirection)
        )
        routeStopList(stopList: loadingStops, onTapStop: { _ in }).loadingPlaceholder()
    }

    @ViewBuilder private func favoriteDialog(stop: Stop) -> some View {
        let allPatternsForStop = globalData.getPatternsFor(stopId: stop.id, lineOrRoute: lineOrRoute)
        let stopDirections = lineOrRoute.directions(
            globalData: globalData,
            stop: stop,
            patterns: allPatternsForStop.filter { $0.isTypical() }
        )

        SaveFavoritesFlow(
            lineOrRoute: lineOrRoute,
            stop: stop,
            directions: stopDirections.filter { direction in
                parameters.availableDirections.map { Int32(truncating: $0) }.contains { $0 == direction.id } &&
                    !stop.isLastStopForAllPatterns(
                        directionId: direction.id,
                        patterns: allPatternsForStop,
                        global: globalData
                    )
            },
            selectedDirection: selectedDirection,
            context: .favorites,
            global: globalData,
            isFavorite: { rsd in isFavorite(rsd) },
            updateFavorites: { newFavorites in confirmFavorites(updatedValues: newFavorites) },
            onClose: { showFavoritesStopConfirmation = nil },
            pushNavEntry: pushNavEntry,
            toastVM: toastVM,
        )
    }

    private func confirmFavorites(updatedValues: [RouteStopDirection: FavoriteSettings?]) {
        Task {
            let editContext = switch onEnum(of: context) {
            case .favorites: EditFavoritesContext.favorites
            case .details: EditFavoritesContext.routeDetails
            }

            favoritesVM.updateFavorites(
                updatedFavorites: updatedValues,
                context: editContext,
                defaultDirection: selectedDirection,
                fcmToken: fcmTokenContainer.token,
                includeAccessibility: settingsCache.get(.stationAccessibility),
            )
        }
    }

    private func isFavorite(_ routeStopDirection: RouteStopDirection) -> Bool {
        favorites.routeStopDirection[routeStopDirection] != nil
    }

    private func stopRowContext(_ stop: Stop) -> RouteDetailsRowContext {
        switch onEnum(of: context) {
        case .details: .details(stop: stop)
        case .favorites: .favorites(
                isFavorited: isFavorite(.init(
                    route: lineOrRoute.id,
                    stop: stop.id,
                    direction: selectedDirection
                )),
                onTapStar: {
                    showFavoritesStopConfirmation = stop
                }
            )
        }
    }
}
