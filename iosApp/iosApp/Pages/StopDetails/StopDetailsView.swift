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
    @ObserveInjection var inject

    var filters: StopDetailsPageFilters

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

    let inspection = Inspection<Self>()

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
                navCallbacks: navCallbacks,
                errorBannerVM: errorBannerVM,
                nearbyVM: nearbyVM,
                mapVM: mapVM,
                stopDetailsVM: stopDetailsVM
            )
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .enableInjection()
        } else {
            StopDetailsUnfilteredView(
                stopId: filters.stopId,
                routeData: routeData,
                favorites: favorites,
                global: global,
                now: now.toEasternInstant(),
                setStopFilter: setStopFilter,
                navCallbacks: navCallbacks,
                errorBannerVM: errorBannerVM,
                nearbyVM: nearbyVM,
            )
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .enableInjection()
        }
    }
}
