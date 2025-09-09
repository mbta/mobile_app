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
    var filters: StopDetailsPageFilters

    var routeData: StopDetailsViewModel.RouteData?
    var favorites: Favorites
    var global: GlobalResponse?
    var now: Date

    var onUpdateFavorites: () -> Void
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var errorBannerVM: IErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: iosApp.MapViewModel
    var stopDetailsVM: IStopDetailsViewModel

    let inspection = Inspection<Self>()

    init(
        filters: StopDetailsPageFilters,
        routeData: StopDetailsViewModel.RouteData?,
        favorites: Favorites,
        global: GlobalResponse?,
        now: Date,
        onUpdateFavorites: @escaping () -> Void,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        errorBannerVM: IErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: iosApp.MapViewModel,
        stopDetailsVM: IStopDetailsViewModel
    ) {
        self.filters = filters
        self.routeData = routeData
        self.favorites = favorites
        self.global = global
        self.now = now
        self.onUpdateFavorites = onUpdateFavorites
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM
    }

    var body: some View {
        if let stopFilter = filters.stopFilter {
            StopDetailsFilteredView(
                stopId: filters.stopId,
                stopFilter: stopFilter,
                tripFilter: filters.tripFilter,
                routeData: routeData,
                favorites: favorites,
                global: global,
                now: now,
                onUpdateFavorites: onUpdateFavorites,
                setStopFilter: setStopFilter,
                setTripFilter: setTripFilter,
                errorBannerVM: errorBannerVM,
                nearbyVM: nearbyVM,
                mapVM: mapVM,
                stopDetailsVM: stopDetailsVM
            )
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
        } else {
            StopDetailsUnfilteredView(
                stopId: filters.stopId,
                routeData: routeData,
                favorites: favorites,
                global: global,
                now: now.toEasternInstant(),
                setStopFilter: setStopFilter,
                errorBannerVM: errorBannerVM,
                nearbyVM: nearbyVM,
            )
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
        }
    }
}
