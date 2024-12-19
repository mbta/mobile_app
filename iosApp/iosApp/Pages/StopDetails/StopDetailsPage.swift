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
            .onChange(of: stopDetailsVM.global) { _ in updateDepartures() }
            .onChange(of: stopDetailsVM.pinnedRoutes) { _ in updateDepartures() }
            .onChange(of: stopDetailsVM.stopData) { stopData in
                errorBannerVM.loadingWhenPredictionsStale = !(stopData?.predictionsLoaded ?? true)
                updateDepartures()
            }
            .onChange(of: stopFilter) { nextStopFilter in setTripFilter(stopFilter: nextStopFilter) }
            .onChange(of: internalDepartures) { _ in
                let nextStopFilter = setStopFilter()
                setTripFilter(stopFilter: nextStopFilter)
            }
            .task(id: stopId) {
                while !Task.isCancelled {
                    now = Date.now
                    updateDepartures()
                    stopDetailsVM.checkStopPredictionsStale()
                    try? await Task.sleep(for: .seconds(5))
                }
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .withScenePhaseHandlers(
                onActive: {
                    stopDetailsVM.returnFromBackground()
                    stopDetailsVM.joinStopPredictions(stopId)
                },
                onInactive: stopDetailsVM.leaveStopPredictions,
                onBackground: {
                    stopDetailsVM.leaveStopPredictions()
                    errorBannerVM.loadingWhenPredictionsStale = true
                }
            )
    }

    func setStopFilter() -> StopDetailsFilter? {
        let nextStopFilter = stopFilter ?? internalDepartures?.autoStopFilter()
        if stopFilter != nextStopFilter {
            nearbyVM.setLastStopDetailsFilter(stopId, nextStopFilter)
        }
        return nextStopFilter
    }

    func setTripFilter(stopFilter: StopDetailsFilter?) {
        let tripFilter = internalDepartures?.autoTripFilter(
            stopFilter: stopFilter,
            currentTripFilter: tripFilter,
            filterAtTime: now.toKotlinInstant()
        )
        nearbyVM.setLastTripDetailsFilter(stopId, tripFilter)
    }

    func updateDepartures() {
        Task {
            if stopId != stopDetailsVM.stopData?.stopId { return }
            let nextDepartures = stopDetailsVM.getDepartures(
                stopId: stopId,
                alerts: nearbyVM.alerts,
                useTripHeadsigns: nearbyVM.tripHeadsignsEnabled,
                now: now
            )
            Task { @MainActor in
                nearbyVM.setDepartures(stopId, nextDepartures)
                internalDepartures = nextDepartures
            }
        }
    }
}
