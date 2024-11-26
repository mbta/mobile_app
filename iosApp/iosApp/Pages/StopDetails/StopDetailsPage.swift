//
//  StopDetailsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftPhoenixClient
import SwiftUI

struct StopDetailsPage: View {
    var stopId: String
    var stopFilter: StopDetailsFilter?
    var tripFilter: TripDetailsFilter?

    // StopDetailsPage maintains its own internal state of the departures presented.
    // This way, when transitioning between one StopDetailsPage and another, each separate page shows
    // their respective  departures rather than both showing the departures for the newly presented stop.
    @State var internalDepartures: StopDetailsDepartures?
    @State var now = Date.now

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    var analytics: StopDetailsAnalytics = AnalyticsProvider.shared
    let inspection = Inspection<Self>()

    init(
        stopId: String,
        stopFilter: StopDetailsFilter?,
        tripFilter: TripDetailsFilter?,
        internalDepartures _: StopDetailsDepartures? = nil,

        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
        stopDetailsVM: StopDetailsViewModel,
        viewportProvider: ViewportProvider
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter

        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM
        self.viewportProvider = viewportProvider
    }

    @ViewBuilder
    var stopDetails: some View {
        StopDetailsView(
            stopId: stopId,
            stopFilter: stopFilter,
            tripFilter: tripFilter,
            setStopFilter: { filter in nearbyVM.setLastStopDetailsFilter(stopId, filter) },
            setTripFilter: { filter in nearbyVM.setLastTripDetailsFilter(stopId, filter) },
            departures: internalDepartures,
            now: now,
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            mapVM: mapVM,
            stopDetailsVM: stopDetailsVM
        )
    }

    var body: some View {
        stopDetails
            .onChange(of: stopId) { nextStopId in changeStop(nextStopId) }
            .onChange(of: stopDetailsVM.global) { _ in updateDepartures() }
            .onChange(of: stopDetailsVM.pinnedRoutes) { _ in updateDepartures() }
            .onChange(of: stopDetailsVM.predictionsByStop) { _ in updateDepartures() }
            .onChange(of: stopDetailsVM.schedulesResponse) { _ in updateDepartures() }
            .onChange(of: stopFilter) { nextStopFilter in setTripFilter(stopFilter: nextStopFilter) }
            .onChange(of: internalDepartures) { _ in
                let nextStopFilter = setStopFilter()
                setTripFilter(stopFilter: nextStopFilter)
            }
            .onAppear { loadEverything() }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .task(id: stopId) {
                while !Task.isCancelled {
                    now = Date.now
                    updateDepartures()
                    checkPredictionsStale()
                    try? await Task.sleep(for: .seconds(5))
                }
            }
            .onDisappear { stopDetailsVM.leavePredictions() }
            .withScenePhaseHandlers(
                onActive: {
                    stopDetailsVM.returnFromBackground()
                    joinPredictions()
                },
                onInactive: stopDetailsVM.leavePredictions,
                onBackground: {
                    stopDetailsVM.leavePredictions()
                    errorBannerVM.loadingWhenPredictionsStale = true
                }
            )
    }
}
