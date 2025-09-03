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

    var errorBannerVM: IErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: iosApp.MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    @State var global: GlobalResponse?
    @State var inSaveFavoritesFlow = false

    @EnvironmentObject var settingsCache: SettingsCache

    var analytics: Analytics = AnalyticsProvider.shared

    var stop: Stop? { global?.getStop(stopId: stopId) }
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
        errorBannerVM: IErrorBannerViewModel,
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

    var isFavorite: Bool {
        stopDetailsVM.isFavorite(routeStopDirection)
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
        .global($global, errorKey: "StopDetailsFilteredView")
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
                                  global: global,
                                  isFavorite: { rsd in stopDetailsVM.isFavorite(rsd) },
                                  updateFavorites: { newFavorites in
                                      Task {
                                          await stopDetailsVM.updateFavorites(newFavorites,
                                                                              stopFilter.directionId)
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
                    isFavorite: stopDetailsVM.isFavorite(routeStopDirection),
                    onFavorite: { inSaveFavoritesFlow = true },
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
