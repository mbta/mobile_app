//
//  StopDetailsPageHandlerExtension.swift
//  iosApp
//
//  Created by esimon on 11/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftPhoenixClient
import SwiftUI

extension StopDetailsPage {
    func changeStop(_ stopId: String) {
        stopDetailsVM.leavePredictions()
        fetchStopData(stopId)
    }

    func checkPredictionsStale() {
        Task {
            if let lastPredictions = stopDetailsVM.predictionsRepository.lastUpdated {
                errorBannerVM.errorRepository.checkPredictionsStale(
                    predictionsLastUpdated: lastPredictions,
                    predictionQuantity: Int32(
                        stopDetailsVM.predictionsByStop?.predictionQuantity() ?? 0
                    ),
                    action: {
                        stopDetailsVM.leavePredictions()
                        joinPredictions()
                    }
                )
            }
        }
    }

    func fetchStopData(_ stopId: String) {
        getSchedule(stopId)
        joinPredictions()
        updateDepartures()
    }

    func getSchedule(_ stopId: String) {
        Task {
            stopDetailsVM.schedulesResponse = nil
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "StopDetailsPage.getSchedule",
                getData: { try await stopDetailsVM.schedulesRepository.getSchedule(stopIds: [stopId]) },
                onSuccess: { @MainActor in stopDetailsVM.schedulesResponse = $0 },
                onRefreshAfterError: loadEverything
            )
        }
    }

    func joinPredictions() {
        stopDetailsVM.joinPredictions(
            stopId,
            onSuccess: { checkPredictionsStale() },
            onComplete: { @MainActor in errorBannerVM.loadingWhenPredictionsStale = false }
        )
    }

    func loadEverything() {
        loadGlobalData()
        fetchStopData(stopId)
        stopDetailsVM.loadPinnedRoutes()
    }

    func loadGlobalData() {
        Task(priority: .high) {
            await stopDetailsVM.activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "StopDetailsPage.loadGlobalData",
                getData: { try await stopDetailsVM.globalRepository.getGlobalData() },
                onRefreshAfterError: loadEverything
            )
        }
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
        let nextDepartures = stopDetailsVM.getDepartures(
            stopId: stopId,
            alerts: nearbyVM.alerts,
            useTripHeadsigns: nearbyVM.tripHeadsignsEnabled,
            now: now
        )
        nearbyVM.setDepartures(stopId, nextDepartures)
        internalDepartures = nextDepartures
    }
}
