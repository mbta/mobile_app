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
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    var analytics: Analytics = AnalyticsProvider.shared

    var stop: Stop? { stopDetailsVM.global?.getStop(stopId: stopId) }
    var nowInstant: Instant { now.toKotlinInstant() }

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
        mapVM: MapViewModel,
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

    var pinned: Bool {
        stopDetailsVM.pinnedRoutes.contains(stopFilter.routeId)
    }

    func toggledPinnedRoute() {
        Task {
            if let routeId = stopData?.lineOrRoute.id {
                let pinned = await stopDetailsVM.togglePinnedRoute(routeId)
                analytics.toggledPinnedRoute(pinned: pinned, routeId: routeId)
                stopDetailsVM.loadPinnedRoutes()
            }
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
                    pinned: pinned,
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
    }

    @ViewBuilder
    var header: some View {
        let line: Line? = switch onEnum(of: stopData?.lineOrRoute) {
        case let .line(line): line.line
        default: nil
        }
        VStack(spacing: 8) {
            StopDetailsFilteredHeader(
                route: stopData?.lineOrRoute.sortRoute,
                line: line,
                stop: stop,
                pinned: pinned,
                onPin: toggledPinnedRoute,
                onClose: { nearbyVM.goBack() }
            )
            DebugView {
                Text(verbatim: "stop id: \(stopId)")
            }.padding(.horizontal, 16)
            ErrorBanner(errorBannerVM).padding(.horizontal, 16)
        }
        .fixedSize(horizontal: false, vertical: true)
        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
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
            pinned: pinned,
            now: now,
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            mapVM: mapVM,
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        ).loadingPlaceholder()
    }
}
