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

    var departureData: DepartureDataBundle?

    var alerts: [Shared.Alert]
    var downstreamAlerts: [Shared.Alert]

    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    var analytics: Analytics = AnalyticsProvider.shared

    var tiles: [TileData] = []
    var noPredictionsStatus: UpcomingFormat.NoTripsFormat?

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
        let stopData = routeData?.stopData.first { $0.stop.id == stopId }
        let leafData = stopData?.data.first { $0.directionId == stopFilter.directionId }

        alerts = leafData?.alertsHere ?? []
        downstreamAlerts = leafData?.alertsDownstream ?? []

        if let routeData, let stopData, let leafData {
            let leafFormat = leafData.format(
                now: nowInstant,
                representativeRoute: routeData.lineOrRoute.sortRoute,
                globalData: stopDetailsVM.global,
                context: .stopDetailsFiltered
            )

            noPredictionsStatus = leafFormat.noPredictionsStatus()
            tiles = leafFormat.tileData()
            departureData = .init(routeData: routeData, stopData: stopData, leaf: leafData)
        } else {
            noPredictionsStatus = nil
            tiles = []
            departureData = nil
        }
    }

    var pinned: Bool {
        stopDetailsVM.pinnedRoutes.contains(stopFilter.routeId)
    }

    func toggledPinnedRoute() {
        Task {
            if let routeId = departureData?.routeData.lineOrRoute.id {
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

            if !nearbyVM.groupByDirection {
                DebugView { Text(String(
                    stringLiteral: "Turn on the Group by Direction feature toggle, we’re dropping support for headsign grouping"
                )).padding(8) }.padding(16).frame(maxHeight: .infinity)
            } else if let departureData {
                StopDetailsFilteredDepartureDetails(
                    stopId: stopId,
                    stopFilter: stopFilter,
                    tripFilter: tripFilter,
                    setStopFilter: setStopFilter,
                    setTripFilter: setTripFilter,
                    tiles: tiles,
                    data: departureData,
                    noPredictionsStatus: noPredictionsStatus,
                    alerts: alerts,
                    downstreamAlerts: downstreamAlerts,
                    pinned: pinned,
                    now: now,
                    errorBannerVM: errorBannerVM,
                    nearbyVM: nearbyVM,
                    mapVM: mapVM,
                    stopDetailsVM: stopDetailsVM
                )
            } else {
                loadingBody()
            }
        }
    }

    @ViewBuilder
    var header: some View {
        let line: Line? = switch onEnum(of: departureData?.routeData.lineOrRoute) {
        case let .line(line): line.line
        default: nil
        }
        VStack(spacing: 8) {
            StopDetailsFilteredHeader(
                route: departureData?.routeData.lineOrRoute.sortRoute,
                line: line,
                stop: stop,
                pinned: pinned,
                onPin: toggledPinnedRoute,
                onClose: { nearbyVM.goBack() }
            )
            if nearbyVM.showDebugMessages {
                DebugView {
                    Text(verbatim: "stop id: \(stopId)")
                }.padding(.horizontal, 16)
            }
            ErrorBanner(errorBannerVM).padding(.horizontal, 16)
        }
        .fixedSize(horizontal: false, vertical: true)
        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
    }

    @ViewBuilder private func loadingBody() -> some View {
        let loadingData = LoadingPlaceholders.shared.departureDataBundle(
            routeId: stopFilter.routeId,
            trips: 10,
            context: .stopDetailsFiltered,
            now: nowInstant
        )
        let leafFormat = loadingData.leaf.format(
            now: nowInstant,
            representativeRoute: loadingData.routeData.lineOrRoute.sortRoute,
            globalData: stopDetailsVM.global,
            context: .stopDetailsFiltered
        )
        let tiles = leafFormat.tileData()

        StopDetailsFilteredDepartureDetails(
            stopId: stopId,
            stopFilter: stopFilter,
            tripFilter: tripFilter,
            setStopFilter: setStopFilter,
            setTripFilter: setTripFilter,
            tiles: tiles,
            data: loadingData,
            noPredictionsStatus: nil,
            alerts: alerts,
            downstreamAlerts: downstreamAlerts,
            pinned: pinned,
            now: now,
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            mapVM: mapVM,
            stopDetailsVM: stopDetailsVM
        ).loadingPlaceholder()
    }
}
