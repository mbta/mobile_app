//
//  RouteStopView.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import OrderedCollections
import Shared
import SwiftPhoenixClient
import SwiftUI

struct RouteStopView: View {
    @ObserveInjection var inject

    var filters: RouteStopPageFilters

    var routeData: StopCentricViewModel.RouteData?
    var alerts: AlertsStreamDataResponse?
    var favorites: Favorites?
    var global: GlobalResponse?
    var now: Date

    var onUpdateFavorites: () -> Void
    var setRouteStopFilter: (RouteStopFilter) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void
    var navCallbacks: NavigationCallbacks

    var errorBannerVM: IErrorBannerViewModel
    var mapVM: IMapViewModel
    var stopCentricVM: IStopCentricViewModel
    var favoritesVM: IFavoritesViewModel
    var tripDetailsVM: ITripDetailsViewModel

    @ObservedObject var navManager: NavigationManager

    var stopId: String { filters.stopId }
    var routeStopFilter: RouteStopFilter { filters.routeStopFilter }
    var tripFilter: TripDetailsFilter? { filters.tripFilter }

    let inspection = Inspection<Self>()

    init(
        filters: RouteStopPageFilters,
        routeData: StopCentricViewModel.RouteData?,
        alerts: AlertsStreamDataResponse?,
        favorites: Favorites?,
        global: GlobalResponse?,
        now: Date,
        onUpdateFavorites: @escaping () -> Void,
        setRouteStopFilter: @escaping (RouteStopFilter) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        navCallbacks: NavigationCallbacks,
        errorBannerVM: IErrorBannerViewModel,
        mapVM: IMapViewModel,
        stopCentricVM: IStopCentricViewModel,
        favoritesVM: IFavoritesViewModel = ViewModelDI().favorites,
        tripDetailsVM: ITripDetailsViewModel = ViewModelDI().tripDetails,
        navManager: NavigationManager,
    ) {
        self.filters = filters
        self.routeData = routeData
        self.alerts = alerts
        self.favorites = favorites
        self.global = global
        self.now = now
        self.onUpdateFavorites = onUpdateFavorites
        self.setRouteStopFilter = setRouteStopFilter
        self.setTripFilter = setTripFilter
        self.navCallbacks = navCallbacks
        self.errorBannerVM = errorBannerVM
        self.mapVM = mapVM
        self.stopCentricVM = stopCentricVM
        self.favoritesVM = favoritesVM
        self.tripDetailsVM = tripDetailsVM
        self.navManager = navManager
    }

    var isFavorite: Bool {
        favorites?.isFavorite(routeStopDirection) ?? false
    }

    var nowInstant: EasternTimeInstant { now.toEasternInstant() }
    var routeStopDirection: RouteStopDirection { .init(
        route: routeStopFilter.routeId,
        stop: stopId,
        direction: routeStopFilter.directionId
    ) }
    var stop: Stop? { global?.getStop(stopId: stopId) }

    var lineOrRoute: LineOrRoute? { global?.getLineOrRoute(lineOrRouteId: routeStopFilter.routeId) }

    var realtimeStopData: RouteCardData.RouteStopData? {
        if case let .routeStop(data) = onEnum(of: routeData), data.filteredWith.stopId == stopId,
           data.filteredWith.routeStopFilter.routeId == routeStopFilter.routeId {
            data.stopData
        } else {
            nil
        }
    }

    var tripPageFilter: TripDetailsPageFilter? {
        if let tripFilter {
            .init(stopId: stopId, routeStopFilter: routeStopFilter, tripFilter: tripFilter)
        } else {
            nil
        }
    }

    var leaf: RouteCardData.Leaf? {
        realtimeStopData?.data
            .first {
                $0.stop.id == stopId && $0.lineOrRoute.id == routeStopFilter.routeId && $0.direction
                    .id == routeStopFilter.directionId
            }
    }

    var body: some View {
        VStack(spacing: 0) {
            if let lineOrRoute, let global, let stop {
                loaded(lineOrRoute: lineOrRoute, stop: stop, global: global)
            } else {
                loading()
            }
        }
        .task {
            // There's no good way to know if favorites have been updated through the edit favorites flow,
            // so reload them any time this page is opened
            onUpdateFavorites()
        }
        .manageVM(
            tripDetailsVM,
            alerts: alerts,
            context: .routeStop,
            filters: tripPageFilter,
        )
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .enableInjection()
    }

    @ViewBuilder
    func loaded(lineOrRoute: LineOrRoute, stop: Stop, global: GlobalResponse) -> some View {
        let allPatternsForStop: [RoutePattern] = global.getPatternsFor(stopId: stopId, lineOrRoute: lineOrRoute)

        let directions: [Direction] = lineOrRoute.directions(
            globalData: global,
            stop: stop,
            patterns: allPatternsForStop.filter { pattern in
                pattern.isTypical() || pattern.isCanonicalOnly()
            }
        )

        let availableDirections = directions.filter {
            !stop.isLastStopForAllPatterns(directionId: $0.id, patterns: allPatternsForStop, global: global)
        }

        VStack(spacing: 0) {
            ZStack {
                Color.fill2.ignoresSafeArea(.all)
                header(lineOrRoute: lineOrRoute, stop: stop)
            }
            .fixedSize(horizontal: false, vertical: true)

            ZStack(alignment: .top) {
                Color(hex: lineOrRoute.backgroundColor).ignoresSafeArea(.all)
                Rectangle()
                    .fill(Color.halo)
                    .frame(height: 2)
                    .frame(maxWidth: .infinity)

                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 16) {
                        DirectionPicker(
                            availableDirections: availableDirections.map(\.id),
                            directions: directions,
                            route: lineOrRoute.sortRoute,
                            selectedDirectionId: routeStopFilter.directionId,
                            updateDirectionId: { setRouteStopFilter(RouteStopFilter(
                                routeId: routeStopFilter.routeId,
                                directionId: $0
                            )) }
                        )
                        .fixedSize(horizontal: false, vertical: true)
                        .padding([.horizontal, .top], 16)
                        .padding(.bottom, 6)
                        .dynamicTypeSize(...DynamicTypeSize.accessibility1)

                        departures(directions: directions)
                    }
                }
            }
        }
    }

    @ViewBuilder
    func departures(directions: [Direction]) -> some View {
        if let leaf {
            RouteStopDeparturesView(
                stopId: stopId,
                routeStopFilter: routeStopFilter,
                tripFilter: tripFilter,
                setRouteStopFilter: setRouteStopFilter,
                setTripFilter: setTripFilter,
                leaf: leaf,
                selectedDirection: directions[Int(routeStopFilter.directionId)],
                favorite: isFavorite,
                now: now.toEasternInstant(),
                errorBannerVM: errorBannerVM,
                mapVM: mapVM,
                stopCentricVM: stopCentricVM,
                navManager: navManager,
            )
        } else {
            loadingDepartures()
        }
    }

    @ViewBuilder
    func loading() -> some View {
        let routeData = LoadingPlaceholders.shared.routeCardData(
            routeId: routeStopFilter.routeId,
            trips: 10,
            context: .routeStop,
            now: nowInstant
        )

        return loaded(
            lineOrRoute: routeData.lineOrRoute,
            stop: routeData.stopData.first!.stop,
            global: GlobalResponse(objects: ObjectCollectionBuilder())
        ).loadingPlaceholder()
    }

    func loadingDepartures() -> some View {
        let routeData = LoadingPlaceholders.shared.routeCardData(
            routeId: routeStopFilter.routeId,
            trips: 10,
            context: .routeStop,
            now: nowInstant
        )

        let leaf = routeData.stopData.first!.data.first!
        return RouteStopDeparturesView(
            stopId: stopId,
            routeStopFilter: routeStopFilter,
            tripFilter: tripFilter,
            setRouteStopFilter: setRouteStopFilter,
            setTripFilter: setTripFilter,
            leaf: leaf,
            selectedDirection: leaf.direction,
            favorite: isFavorite,
            now: now.toEasternInstant(),
            errorBannerVM: errorBannerVM,
            mapVM: mapVM,
            stopCentricVM: stopCentricVM,
            navManager: navManager,
        ).loadingPlaceholder()
    }

    @ViewBuilder
    func header(lineOrRoute: LineOrRoute, stop: Stop) -> some View {
        let line: Line? = switch onEnum(of: lineOrRoute) {
        case let .line(line): line.line
        default: nil
        }
        VStack(spacing: 0) {
            VStack(spacing: 0) {
                RouteStopHeader(
                    route: lineOrRoute.sortRoute,
                    line: line,
                    stop: stop,
                    direction: routeStopFilter.directionId,
                    isFavorite: isFavorite,
                    onFavorite: {
                        navManager.pushNavEntry(
                            .saveFavorite(
                                routeId: lineOrRoute.id,
                                stopId: stopId,
                                selectedDirection: routeStopFilter.directionId,
                                context: .routeStop
                            )
                        )
                    },
                    navCallbacks: navCallbacks,
                )
                ErrorBanner(errorBannerVM, padding: .init([.horizontal, .bottom], 16))
                ChannelDebugView {
                    Text(verbatim: "stop id: \(stopId)")
                }
                .padding(.horizontal, 6)
            }
            .fixedSize(horizontal: false, vertical: true)
            .dynamicTypeSize(...DynamicTypeSize.accessibility1)
        }
    }
}
