//
//  StopDetailsFilteredView.swift
//  iosApp
//
//  Created by esimon on 11/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import Shared
import SwiftUI

struct StopDetailsFilteredView: View {
    @ObserveInjection var inject
    var stopId: String
    var stopFilter: StopDetailsFilter
    var tripFilter: TripDetailsFilter?

    var routeData: StopDetailsViewModel.RouteData?
    var alerts: AlertsStreamDataResponse?
    var favorites: Favorites
    var global: GlobalResponse?
    var now: Date

    var onUpdateFavorites: () -> Void
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void
    var navCallbacks: NavigationCallbacks

    var errorBannerVM: IErrorBannerViewModel
    var mapVM: IMapViewModel
    var stopDetailsVM: IStopDetailsViewModel
    var tripDetailsVM: ITripDetailsViewModel
    var favoritesVM: IFavoritesViewModel

    @State var inSaveFavoritesFlow = false
    @State var alertSummaries: [String: AlertSummary?] = [:]

    @ObservedObject var fcmTokenContainer = FcmTokenContainer.shared
    @EnvironmentObject var navManager: NavigationManager
    @EnvironmentObject var settingsCache: SettingsCache

    var analytics: Analytics = AnalyticsProvider.shared

    let inspection = Inspection<Self>()

    init(
        stopId: String,
        stopFilter: StopDetailsFilter,
        tripFilter: TripDetailsFilter?,
        routeData: StopDetailsViewModel.RouteData?,
        alerts: AlertsStreamDataResponse?,
        favorites: Favorites,
        global: GlobalResponse?,
        now: Date,
        onUpdateFavorites: @escaping () -> Void,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        navCallbacks: NavigationCallbacks,
        errorBannerVM: IErrorBannerViewModel,
        mapVM: IMapViewModel,
        stopDetailsVM: IStopDetailsViewModel,
        favoritesVM: IFavoritesViewModel = ViewModelDI().favorites,
        tripDetailsVM: ITripDetailsViewModel = ViewModelDI().tripDetails,
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter
        self.routeData = routeData
        self.alerts = alerts
        self.favorites = favorites
        self.global = global
        self.now = now
        self.onUpdateFavorites = onUpdateFavorites
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.navCallbacks = navCallbacks
        self.errorBannerVM = errorBannerVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM
        self.favoritesVM = favoritesVM
        self.tripDetailsVM = tripDetailsVM
    }

    var isFavorite: Bool { if let routeStopDirection { favorites.isFavorite(routeStopDirection) } else { false }}
    var nowInstant: EasternTimeInstant { now.toEasternInstant() }
    var routeStopDirection: RouteStopDirection? { .init(
        route: stopFilter.routeId,
        stop: stopId,
        direction: stopFilter.directionId
    ) }
    var stop: Stop? { global?.getStop(stopId: stopId) }

    var lineOrRoute: LineOrRoute? { global?.getLineOrRoute(lineOrRouteId: stopFilter.routeId) }

    var realtimeStopData: RouteCardData.RouteStopData? {
        if case let .filtered(data) = onEnum(of: routeData), routeData?.filters.stopId == stopId,
           routeData?.filters.stopFilter?.routeId == stopFilter.routeId {
            data.stopData
        } else {
            nil
        }
    }

    var tripPageFilter: TripDetailsPageFilter? {
        if let tripFilter {
            .init(stopId: stopId, stopFilter: stopFilter, tripFilter: tripFilter)
        } else {
            nil
        }
    }

    var leaf: RouteCardData.Leaf? {
        realtimeStopData?.data
            .first {
                $0.stop.id == stopId && $0.lineOrRoute.id == stopFilter.routeId && $0.directionId == stopFilter
                    .directionId
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
            for await model in stopDetailsVM.models {
                alertSummaries = model.alertSummaries as? [String: AlertSummary?] ?? [:]
            }
        }
        .manageVM(
            tripDetailsVM,
            alerts: alerts,
            context: .stopDetails,
            filters: tripPageFilter,
        )
        .accessibilityHidden(inSaveFavoritesFlow)
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
                pattern.isTypical()
            }
        )

        let availableDirections = directions.filter {
            !stop.isLastStopForAllPatterns(directionId: $0.id, patterns: allPatternsForStop, global: global)
        }

        VStack(spacing: 0) {
            ZStack {
                Color.fill2.ignoresSafeArea(.all)
                header(
                    lineOrRoute: lineOrRoute,
                    directions: directions,
                    availableDirections: availableDirections
                )
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
                            selectedDirectionId: stopFilter.directionId,
                            updateDirectionId: { setStopFilter(StopDetailsFilter(
                                routeId: stopFilter.routeId,
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
            StopDetailsFilteredDepartureDetails(
                stopId: stopId,
                stopFilter: stopFilter,
                tripFilter: tripFilter,
                setStopFilter: setStopFilter,
                setTripFilter: setTripFilter,
                leaf: leaf,
                alertSummaries: alertSummaries,
                selectedDirection: directions[Int(stopFilter.directionId)],
                favorite: isFavorite,
                now: now.toEasternInstant(),
                errorBannerVM: errorBannerVM,
                mapVM: mapVM,
                stopDetailsVM: stopDetailsVM,
            )
        } else {
            loadingDepartures()
        }
    }

    @ViewBuilder
    func loading() -> some View {
        let routeData = LoadingPlaceholders.shared.routeCardData(
            routeId: stopFilter.routeId,
            trips: 10,
            context: .stopDetailsFiltered,
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
            routeId: stopFilter.routeId,
            trips: 10,
            context: .stopDetailsFiltered,
            now: nowInstant
        )

        return StopDetailsFilteredDepartureDetails(
            stopId: stopId,
            stopFilter: stopFilter,
            tripFilter: tripFilter,
            setStopFilter: setStopFilter,
            setTripFilter: setTripFilter,
            leaf: routeData.stopData.first!.data.first!,
            alertSummaries: alertSummaries,
            selectedDirection: routeData.stopData.first!.directions[Int(stopFilter.directionId)],
            favorite: isFavorite,
            now: now.toEasternInstant(),
            errorBannerVM: errorBannerVM,
            mapVM: mapVM,
            stopDetailsVM: stopDetailsVM,
        ).loadingPlaceholder()
    }

    @ViewBuilder
    func header(lineOrRoute: LineOrRoute, directions _: [Direction], availableDirections: [Direction]) -> some View {
        let line: Line? = switch onEnum(of: lineOrRoute) {
        case let .line(line): line.line
        default: nil
        }
        VStack(spacing: 0) {
            if let stop, let routeStopDirection, inSaveFavoritesFlow {
                SaveFavoritesFlow(
                    lineOrRoute: lineOrRoute,
                    stop: stop,
                    directions: availableDirections,
                    selectedDirection: routeStopDirection.direction,
                    context: .stopDetails,
                    global: global,
                    isFavorite: { favorites.isFavorite($0) },
                    updateFavorites: { updatedValues in
                        favoritesVM.updateFavorites(
                            updatedFavorites: updatedValues,
                            context: .stopDetails,
                            defaultDirection: routeStopDirection.direction,
                            fcmToken: fcmTokenContainer.token,
                        )
                    },
                    onClose: { inSaveFavoritesFlow = false },
                    pushNavEntry: { navManager.pushNavEntry($0) },
                )
            }

            VStack(spacing: 0) {
                StopDetailsFilteredHeader(
                    route: lineOrRoute.sortRoute,
                    line: line,
                    stop: stop,
                    direction: stopFilter.directionId,
                    isFavorite: isFavorite,
                    onFavorite: { inSaveFavoritesFlow = true },
                    navCallbacks: navCallbacks,
                )
                ErrorBanner(errorBannerVM, padding: .init([.horizontal, .bottom], 16))
                DebugView {
                    VStack(alignment: .leading) {
                        Text(verbatim: "stop id: \(stopId)")
                        Text(verbatim: "trip id: \(tripFilter?.tripId ?? "nil")")
                        Text(verbatim: "vehicle id: \(tripFilter?.vehicleId ?? "nil")")
                    }
                }
                .padding(.horizontal, 6)
            }
            .fixedSize(horizontal: false, vertical: true)
            .dynamicTypeSize(...DynamicTypeSize.accessibility1)
        }
    }
}
