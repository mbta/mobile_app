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
    var stopId: String
    var stopFilter: StopDetailsFilter
    var tripFilter: TripDetailsFilter?

    var routeData: StopDetailsViewModel.RouteData?
    var favorites: Favorites
    var global: GlobalResponse?
    var now: Date

    var onUpdateFavorites: () -> Void
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void
    var navCallbacks: NavigationCallbacks

    var errorBannerVM: IErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    var mapVM: IMapViewModel
    var stopDetailsVM: IStopDetailsViewModel
    var favoritesVM: IFavoritesViewModel

    @State var inSaveFavoritesFlow = false
    @State var alertSummaries: [String: AlertSummary?] = [:]

    @EnvironmentObject var settingsCache: SettingsCache

    var analytics: Analytics = AnalyticsProvider.shared

    let inspection = Inspection<Self>()

    init(
        stopId: String,
        stopFilter: StopDetailsFilter,
        tripFilter: TripDetailsFilter?,
        routeData: StopDetailsViewModel.RouteData?,
        favorites: Favorites,
        global: GlobalResponse?,
        now: Date,
        onUpdateFavorites: @escaping () -> Void,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        navCallbacks: NavigationCallbacks,
        errorBannerVM: IErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: IMapViewModel,
        stopDetailsVM: IStopDetailsViewModel,
        favoritesVM: IFavoritesViewModel = ViewModelDI().favorites
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter
        self.routeData = routeData
        self.favorites = favorites
        self.global = global
        self.now = now
        self.onUpdateFavorites = onUpdateFavorites
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.navCallbacks = navCallbacks
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM
        self.favoritesVM = favoritesVM
    }

    var isFavorite: Bool { if let routeStopDirection { favorites.isFavorite(routeStopDirection) } else { false }}
    var nowInstant: EasternTimeInstant { now.toEasternInstant() }
    var routeStopDirection: RouteStopDirection? { .init(
        route: stopFilter.routeId,
        stop: stopId,
        direction: stopFilter.directionId
    ) }
    var stop: Stop? { global?.getStop(stopId: stopId) }
    var stopData: RouteCardData.RouteStopData? {
        if case let .filtered(data) = onEnum(of: routeData) {
            data.stopData
        } else { nil }
    }

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Color.fill2.ignoresSafeArea(.all)
                header
            }
            .fixedSize(horizontal: false, vertical: true)

            if let stopData {
                StopDetailsFilteredPickerView(
                    stopId: stopId,
                    stopFilter: stopFilter,
                    tripFilter: tripFilter,
                    setStopFilter: setStopFilter,
                    setTripFilter: setTripFilter,
                    stopData: stopData,
                    alertSummaries: alertSummaries,
                    favorite: isFavorite,
                    now: now,
                    errorBannerVM: errorBannerVM,
                    nearbyVM: nearbyVM,
                    mapVM: mapVM,
                    stopDetailsVM: stopDetailsVM,
                    viewportProvider: .init()
                )
            } else {
                loadingBody()
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
        .accessibilityHidden(inSaveFavoritesFlow)
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @ViewBuilder
    var header: some View {
        let line: Line? = switch onEnum(of: stopData?.lineOrRoute) {
        case let .line(line): line.line
        default: nil
        }
        VStack(spacing: 0) {
            if let stopData, let routeStopDirection, inSaveFavoritesFlow {
                SaveFavoritesFlow(
                    lineOrRoute: stopData.lineOrRoute,
                    stop: stopData.stop,
                    directions: stopData.directions
                        .filter { stopData.availableDirections.contains(KotlinInt(value: $0.id)) },
                    selectedDirection: routeStopDirection.direction,
                    context: .stopDetails,
                    global: global,
                    isFavorite: { favorites.isFavorite($0) },
                    updateFavorites: { updatedValues in
                        favoritesVM.updateFavorites(
                            updatedFavorites: updatedValues,
                            context: .stopDetails,
                            defaultDirection: routeStopDirection.direction
                        )
                    },
                    onClose: { inSaveFavoritesFlow = false },
                    pushNavEntry: { nearbyVM.pushNavEntry($0) },
                )
            }

            VStack(spacing: 0) {
                StopDetailsFilteredHeader(
                    route: stopData?.lineOrRoute.sortRoute,
                    line: line,
                    stop: stop,
                    direction: stopFilter.directionId,
                    isFavorite: isFavorite,
                    onFavorite: { inSaveFavoritesFlow = true },
                    navCallbacks: navCallbacks,
                )
                ErrorBanner(errorBannerVM, padding: .init([.horizontal, .bottom], 16))
            }
            .fixedSize(horizontal: false, vertical: true)
            .dynamicTypeSize(...DynamicTypeSize.accessibility1)
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        let routeData = LoadingPlaceholders.shared.routeCardData(
            routeId: stopFilter.routeId,
            trips: 10,
            context: .stopDetailsFiltered,
            now: nowInstant
        )

        StopDetailsFilteredPickerView(
            stopId: stopId,
            stopFilter: stopFilter,
            tripFilter: tripFilter,
            setStopFilter: setStopFilter,
            setTripFilter: setTripFilter,
            stopData: routeData.stopData.first!,
            alertSummaries: [:],
            favorite: false,
            now: now,
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            mapVM: mapVM,
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        ).loadingPlaceholder()
    }
}
