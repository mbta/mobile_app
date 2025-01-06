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

struct StopDetailsPageFilters: Equatable {
    var stopId: String
    var stopFilter: StopDetailsFilter?
    var tripFilter: TripDetailsFilter?
}

struct StopDetailsPage: View {
    var filters: StopDetailsPageFilters

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

    var stopId: String { filters.stopId }
    var stopFilter: StopDetailsFilter? { filters.stopFilter }
    var tripFilter: TripDetailsFilter? { filters.tripFilter }

    init(
        filters: StopDetailsPageFilters,
        internalDepartures _: StopDetailsDepartures? = nil,

        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
        stopDetailsVM: StopDetailsViewModel,
        viewportProvider: ViewportProvider
    ) {
        self.filters = filters

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
            .fullScreenCover(
                isPresented: .init(
                    get: { stopDetailsVM.explainer != nil },
                    set: { value in if !value { stopDetailsVM.explainer = nil } }
                )
            ) {
                if let explainer = stopDetailsVM.explainer {
                    ExplainerPage(
                        explainer: explainer,
                        onClose: { stopDetailsVM.explainer = nil }
                    )
                }
            }
            .onChange(of: stopDetailsVM.global) { _ in updateDepartures() }
            .onChange(of: stopDetailsVM.pinnedRoutes) { _ in updateDepartures() }
            .onChange(of: stopDetailsVM.stopData) { stopData in
                errorBannerVM.loadingWhenPredictionsStale = !(stopData?.predictionsLoaded ?? true)
                updateDepartures()
            }
            .onChange(of: filters) { nextFilters in setTripFilter(filters: nextFilters) }
            .onChange(of: internalDepartures) { _ in
                let nextStopFilter = setStopFilter()
                setTripFilter(filters: .init(stopId: stopId, stopFilter: nextStopFilter, tripFilter: tripFilter))
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

    func setTripFilter(filters: StopDetailsPageFilters) {
        let tripFilter = internalDepartures?.autoTripFilter(
            stopFilter: filters.stopFilter,
            currentTripFilter: filters.tripFilter,
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
