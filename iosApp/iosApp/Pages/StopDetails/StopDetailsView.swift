//
//  StopDetailsView.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation

import OrderedCollections
import shared
import SwiftPhoenixClient
import SwiftUI

struct StopDetailsView: View {
    var stopId: String
    var stopFilter: StopDetailsFilter?
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var departures: StopDetailsDepartures?
    var now: Date
    var servedRoutes: [StopDetailsFilterPills.FilterBy] = []

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    var analytics: StopDetailsAnalytics = AnalyticsProvider.shared
    let inspection = Inspection<Self>()

    init(
        stopId: String,
        stopFilter: StopDetailsFilter?,
        tripFilter: TripDetailsFilter?,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        departures: StopDetailsDepartures?,
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
        self.departures = departures
        self.now = now
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM

        if let departures {
            servedRoutes = departures.routes.map { patterns in
                if let line = patterns.line {
                    return .line(line)
                }
                return .route(
                    patterns.representativeRoute
                )
            }
        }
    }

    var stop: Stop? {
        stopDetailsVM.global?.stops[stopId]
    }

    var body: some View {
        if let stopFilter {
            StopDetailsFilteredView(
                stopId: stopId,
                stopFilter: stopFilter,
                tripFilter: tripFilter,
                setStopFilter: setStopFilter,
                setTripFilter: setTripFilter,
                departures: departures,
                now: now,
                errorBannerVM: errorBannerVM,
                nearbyVM: nearbyVM,
                mapVM: mapVM,
                stopDetailsVM: stopDetailsVM
            )
        } else {
            StopDetailsUnfilteredView(
                stopId: stopId,
                setStopFilter: setStopFilter,
                departures: departures,
                now: now,
                errorBannerVM: errorBannerVM,
                nearbyVM: nearbyVM,
                stopDetailsVM: stopDetailsVM
            )
        }
    }
}
