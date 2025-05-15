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
    var stopId: String
    var stopFilter: StopDetailsFilter
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var stopData: RouteCardData.RouteStopData
    var leaf: RouteCardData.Leaf?

    var pinned: Bool

    var now: Date

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    @EnvironmentObject var viewportProvider: ViewportProvider

    let inspection = Inspection<Self>()

    var routeColor: Color { Color(hex: stopData.lineOrRoute.backgroundColor) }

    init(
        stopId: String,
        stopFilter: StopDetailsFilter,
        tripFilter: TripDetailsFilter? = nil,
        setStopFilter: @escaping (StopDetailsFilter?) -> Void,
        setTripFilter: @escaping (TripDetailsFilter?) -> Void,
        stopData: RouteCardData.RouteStopData, pinned: Bool, now: Date,
        errorBannerVM: ErrorBannerViewModel, nearbyVM: NearbyViewModel, mapVM: MapViewModel,
        stopDetailsVM: StopDetailsViewModel, viewportProvider _: ViewportProvider
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter
        self.setStopFilter = setStopFilter
        self.setTripFilter = setTripFilter
        self.stopData = stopData
        self.pinned = pinned
        self.now = now
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM

        leaf = stopData.data.first { $0.directionId == stopFilter.directionId }
    }

    var body: some View {
        ZStack(alignment: .top) {
            routeColor.ignoresSafeArea(.all)
            Rectangle()
                .fill(Color.halo)
                .frame(height: 2)
                .frame(maxWidth: .infinity)
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 16) {
                    DirectionPicker(
                        stopData: stopData,
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
                            selectedDirection: stopData.directions[Int(stopFilter.directionId)],
                            pinned: pinned,
                            now: now,
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
                            now: now.toKotlinInstant()
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
                            selectedDirection: stopData.directions[0],
                            pinned: false,
                            now: now,
                            errorBannerVM: errorBannerVM,
                            nearbyVM: nearbyVM,
                            mapVM: mapVM,
                            stopDetailsVM: stopDetailsVM,
                            viewportProvider: .init()
                        ).loadingPlaceholder()
                    }
                }
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .ignoresSafeArea(.all)
    }
}
