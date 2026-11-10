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

    var filters: StopCentricPageFilters

    var routeData: StopCentricViewModel.RouteData?
    var alerts: AlertsStreamDataResponse?
    var favorites: Favorites?
    var global: GlobalResponse?
    var now: Date

    var onUpdateFavorites: () -> Void
    var setRouteStopFilter: (RouteStopFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void
    var navCallbacks: NavigationCallbacks

    var errorBannerVM: IErrorBannerViewModel
    var mapVM: IMapViewModel
    var stopCentricVM: IStopCentricViewModel

    @ObservedObject var navManager: NavigationManager

    let inspection = Inspection<Self>()

    var body: some View {
        switch onEnum(of: filters) {
        case let .routeStopPageFilters(filters):
            StopDetailsFilteredView(
                stopId: filters.stopId,
                routeStopFilter: filters.routeStopFilter,
                tripFilter: filters.tripFilter,
                routeData: routeData,
                alerts: alerts,
                favorites: favorites,
                global: global,
                now: now,
                onUpdateFavorites: onUpdateFavorites,
                setRouteStopFilter: setRouteStopFilter,
                setTripFilter: setTripFilter,
                navCallbacks: navCallbacks,
                errorBannerVM: errorBannerVM,
                mapVM: mapVM,
                stopCentricVM: stopCentricVM,
                navManager: navManager,
            )
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .enableInjection()
        case let .stopOverviewPageFilters(filters):
            StopDetailsUnfilteredView(
                stopId: filters.stopId,
                routeData: routeData,
                favorites: favorites,
                global: global,
                now: now.toEasternInstant(),
                setRouteStopFilter: setRouteStopFilter,
                navCallbacks: navCallbacks,
                errorBannerVM: errorBannerVM,
                navManager: navManager,
            )
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .enableInjection()
        }
    }
}
