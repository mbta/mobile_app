//
//  StopDetailsFilteredPickerView.swift
//  iosApp
//
//  Created by esimon on 12/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopDetailsFilteredPickerView: View {
    @ObserveInjection var inject
    var stopId: String
    var stopFilter: StopDetailsFilter
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var route: Route
    var leaf: RouteCardData.Leaf?
    var availableDirections: [Int32]
    var directions: [Direction]
    var alertSummaries: [String: AlertSummary?]

    var favorite: Bool

    var now: Date

    var errorBannerVM: IErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    var mapVM: IMapViewModel
    var stopDetailsVM: IStopDetailsViewModel

    @EnvironmentObject var viewportProvider: ViewportProvider

    let inspection = Inspection<Self>()

    init(
        stopId: String,
        stopFilter: StopDetailsFilter,
        tripFilter: TripDetailsFilter?,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        route: Route,
        leaf: RouteCardData.Leaf?,
        availableDirections: [Int32],
        directions: [Direction],
        alertSummaries: [String: AlertSummary?],
        favorite: Bool,
        now: Date,
        errorBannerVM: IErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: IMapViewModel,
        stopDetailsVM: IStopDetailsViewModel,
        viewportProvider _: ViewportProvider
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.route = route
        self.leaf = leaf
        self.availableDirections = availableDirections
        self.directions = directions
        self.alertSummaries = alertSummaries
        self.favorite = favorite
        self.now = now
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 16) {
                DirectionPicker(
                    availableDirections: availableDirections,
                    directions: directions,
                    route: route,
                    filter: stopFilter,
                    setFilter: { setStopFilter($0) }
                )
                .fixedSize(horizontal: false, vertical: true)
                .padding([.horizontal, .top], 16)
                .padding(.bottom, 6)
                .dynamicTypeSize(...DynamicTypeSize.accessibility1)

                if let leaf {
                    StopDetailsFilteredDepartureDetails(
                        stopId: stopId,
                        stopFilter: stopFilter,
                        tripFilter: tripFilter,
                        setStopFilter: setStopFilter,
                        setTripFilter: setTripFilter,
                        leaf: leaf,
                        alertSummaries: alertSummaries,
                        selectedDirection: directions[Int(stopFilter.directionId)],
                        favorite: favorite,
                        now: now.toEasternInstant(),
                        errorBannerVM: errorBannerVM,
                        nearbyVM: nearbyVM,
                        mapVM: mapVM,
                        stopDetailsVM: stopDetailsVM,
                        viewportProvider: .init()
                    )
                } else {
                    let routeData = LoadingPlaceholders.shared.routeCardData(
                        routeId: stopFilter.routeId,
                        trips: 10,
                        context: .stopDetailsFiltered,
                        now: now.toEasternInstant()
                    )
                    let stopData = routeData.stopData.first!
                    let leaf = stopData.data.first!
                    StopDetailsFilteredDepartureDetails(
                        stopId: stopId,
                        stopFilter: stopFilter,
                        tripFilter: tripFilter,
                        setStopFilter: { _ in },
                        setTripFilter: { _ in },
                        leaf: leaf,
                        alertSummaries: alertSummaries,
                        selectedDirection: stopData.directions[0],
                        favorite: false,
                        now: now.toEasternInstant(),
                        errorBannerVM: errorBannerVM,
                        nearbyVM: nearbyVM,
                        mapVM: mapVM,
                        stopDetailsVM: stopDetailsVM,
                        viewportProvider: .init()
                    ).loadingPlaceholder()
                }
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .ignoresSafeArea(.all)
        .enableInjection()
    }
}
