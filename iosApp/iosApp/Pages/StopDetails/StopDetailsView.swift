//
//  StopDetailsView.swift
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

struct StopDetailsView: View {
    var stopId: String
    var stopFilter: StopDetailsFilter?
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var routeCardData: [RouteCardData]?
    var favorites: Favorites
    var now: Date
    var onUpdateFavorites: () -> Void

    var errorBannerVM: IErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    var mapVM: IMapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    let inspection = Inspection<Self>()

    init(
        stopId: String,
        stopFilter: StopDetailsFilter?,
        tripFilter: TripDetailsFilter?,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        routeCardData: [RouteCardData]?,
        favorites: Favorites,
        now: Date,
        onUpdateFavorites: @escaping () -> Void,
        errorBannerVM: IErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: IMapViewModel,
        stopDetailsVM: StopDetailsViewModel
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.routeCardData = routeCardData
        self.favorites = favorites
        self.now = now
        self.onUpdateFavorites = onUpdateFavorites
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM
    }

    var body: some View {
        if let stopFilter {
            StopDetailsFilteredView(
                stopId: stopId,
                stopFilter: stopFilter,
                tripFilter: tripFilter,
                setStopFilter: setStopFilter,
                setTripFilter: setTripFilter,
                routeCardData: routeCardData,
                favorites: favorites,
                now: now,
                onUpdateFavorites: onUpdateFavorites,
                errorBannerVM: errorBannerVM,
                nearbyVM: nearbyVM,
                mapVM: mapVM,
                stopDetailsVM: stopDetailsVM
            )
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
        } else {
            StopDetailsUnfilteredView(
                stopId: stopId,
                setStopFilter: setStopFilter,
                routeCardData: routeCardData,
                favorites: favorites,
                now: now.toEasternInstant(),
                errorBannerVM: errorBannerVM,
                nearbyVM: nearbyVM,
                stopDetailsVM: stopDetailsVM
            )
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
        }
    }
}
