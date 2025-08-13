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
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var routeCardData: [RouteCardData]?
    var now: Date

    var stopData: RouteCardData.RouteStopData?

    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: iosApp.MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    @State var inSaveFavoritesFlow = false

    @EnvironmentObject var settingsCache: SettingsCache

    var analytics: Analytics = AnalyticsProvider.shared

    var stop: Stop? { stopDetailsVM.global?.getStop(stopId: stopId) }
    var nowInstant: EasternTimeInstant { now.toEasternInstant() }

    let inspection = Inspection<Self>()

    init(
        stopId: String,
        stopFilter: StopDetailsFilter,
        tripFilter: TripDetailsFilter?,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        routeCardData: [RouteCardData]?,
        now: Date,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: iosApp.MapViewModel,
        stopDetailsVM: StopDetailsViewModel
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.routeCardData = routeCardData
        self.now = now
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM

        let routeData = routeCardData?.first { $0.lineOrRoute.id == stopFilter.routeId }
        stopData = routeData?.stopData.first { $0.stop.id == stopId }
    }

    var routeStopDirection: RouteStopDirection {
        .init(route: stopFilter.routeId, stop: stopId, direction: stopFilter.directionId)
    }

    var favoriteBridge: FavoriteBridge {
        .Favorite(routeStopDirection: routeStopDirection)
    }

    var isFavorite: Bool {
        stopDetailsVM.isFavorite(favoriteBridge, enhancedFavorites: true)
    }

    var toggleFavoriteUpdateBridge: FavoriteUpdateBridge {
        .Favorites(
            updatedValues: [routeStopDirection: .init(bool: !isFavorite)],
            defaultDirection: stopFilter.directionId
        )
    }

    func toggleFavorite() {
        Task {
            let pinned = await stopDetailsVM.updateFavorites(
                toggleFavoriteUpdateBridge,
                enhancedFavorites: true
            )
        }
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
            if let stopData,
               inSaveFavoritesFlow {
                SaveFavoritesFlow(lineOrRoute: stopData.lineOrRoute,
                                  stop: stopData.stop,
                                  directions: stopData.directions
                                      .filter { stopData.availableDirections.contains(KotlinInt(value: $0.id)) },
                                  selectedDirection: routeStopDirection.direction,
                                  context: .stopDetails,
                                  global: stopDetailsVM.global,
                                  isFavorite: { rsd in
                                      stopDetailsVM.isFavorite(
                                          .Favorite(routeStopDirection: rsd),
                                          enhancedFavorites: true
                                      )
                                  },
                                  updateFavorites: { newFavorites in
                                      Task {
                                          await stopDetailsVM.updateFavorites(
                                              .Favorites(updatedValues: newFavorites
                                                  .mapValues { KotlinBoolean(bool: $0) },
                                                  defaultDirection: stopFilter.directionId),
                                              enhancedFavorites: true
                                          )
                                      }
                                  },
                                  onClose: {
                                      inSaveFavoritesFlow = false
                                  })
            }

            VStack(spacing: 8) {
                StopDetailsFilteredHeader(
                    route: stopData?.lineOrRoute.sortRoute,
                    line: line,
                    stop: stop,
                    direction: stopFilter.directionId,
                    pinned: stopDetailsVM.isFavorite(favoriteBridge, enhancedFavorites: true),
                    onPin: {
                        if favoriteBridge is FavoriteBridge.Pinned {
                            toggleFavorite()
                        } else {
                            inSaveFavoritesFlow = true
                        }
                    },
                    onClose: { nearbyVM.goBack() }
                )
                /*  DebugView {
                      Text(verbatim: "stop id: \(stopId)")
                  }.padding(.horizontal, 16)
                 */
                ErrorBanner(errorBannerVM).padding(.horizontal, 16)
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
        let stopData = routeData.stopData.first!
        let leaf = stopData.data.first!

        StopDetailsFilteredPickerView(
            stopId: stopId,
            stopFilter: stopFilter,
            tripFilter: tripFilter,
            setStopFilter: setStopFilter,
            setTripFilter: setTripFilter,
            stopData: stopData,
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
