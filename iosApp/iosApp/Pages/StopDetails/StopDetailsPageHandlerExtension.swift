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
    func loadEverything() {
        loadGlobalData()
        fetchStopData(stopId)
        loadPinnedRoutes()
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            globalResponse = globalData
        }
    }

    func loadGlobalData() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "StopDetailsPage.loadGlobalData",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: loadEverything
            )
        }
    }

    func loadPinnedRoutes() {
        Task {
            do {
                pinnedRoutes = try await pinnedRouteRepository.getPinnedRoutes()
            } catch is CancellationError {
                // do nothing on cancellation
            } catch {
                // getPinnedRoutes shouldn't actually fail
                debugPrint(error)
            }
        }
    }

    func togglePinnedRoute(_ routeId: String) {
        Task {
            do {
                _ = try await togglePinnedUsecase.execute(route: routeId)
                loadPinnedRoutes()
            } catch is CancellationError {
                // do nothing on cancellation
            } catch {
                // execute shouldn't actually fail
                debugPrint(error)
            }
        }
    }

    func changeStop(_ stopId: String) {
        leavePredictions()
        fetchStopData(stopId)
    }

    func fetchStopData(_ stopId: String) {
        getSchedule(stopId)
        joinPredictions(stopId)
        updateDepartures(stopId: stopId)
    }

    func getSchedule(_ stopId: String) {
        Task {
            schedulesResponse = nil
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "StopDetailsPage.getSchedule",
                getData: { try await schedulesRepository.getSchedule(stopIds: [stopId]) },
                onSuccess: { schedulesResponse = $0 },
                onRefreshAfterError: loadEverything
            )
        }
    }

    func joinPredictions(_ stopId: String) {
        // no error handling since persistent errors cause stale predictions
        predictionsRepository.connectV2(stopIds: [stopId], onJoin: { outcome in
            DispatchQueue.main.async {
                if case let .ok(result) = onEnum(of: outcome) {
                    predictionsByStop = result.data
                    checkPredictionsStale()
                }
                errorBannerVM.loadingWhenPredictionsStale = false
            }
        }, onMessage: { outcome in
            DispatchQueue.main.async {
                if case let .ok(result) = onEnum(of: outcome) {
                    if let existingPredictionsByStop = predictionsByStop {
                        predictionsByStop = existingPredictionsByStop.mergePredictions(updatedPredictions: result.data)
                    } else {
                        predictionsByStop = PredictionsByStopJoinResponse(
                            predictionsByStop: [result.data.stopId: result.data.predictions],
                            trips: result.data.trips,
                            vehicles: result.data.vehicles
                        )
                    }
                    checkPredictionsStale()
                }
                errorBannerVM.loadingWhenPredictionsStale = false
            }

        })
    }

    func leavePredictions() {
        predictionsRepository.disconnect()
    }

    func checkPredictionsStale() {
        if let lastPredictions = predictionsRepository.lastUpdated {
            errorBannerVM.errorRepository.checkPredictionsStale(
                predictionsLastUpdated: lastPredictions,
                predictionQuantity: Int32(
                    predictionsByStop?.predictionQuantity() ?? 0
                ),
                action: {
                    leavePredictions()
                    joinPredictions(stopId)
                }
            )
        }
    }

    func setTripFilter(departures: StopDetailsDepartures? = nil, stopFilter: StopDetailsFilter? = nil) {
        let tripFilter = (departures ?? internalDepartures)?.autoTripFilter(
            stopFilter: stopFilter ?? self.stopFilter,
            currentTripFilter: tripFilter,
            filterAtTime: now.toKotlinInstant()
        )
        nearbyVM.setLastTripDetailsFilter(stopId, tripFilter)
    }

    func updateDepartures(
        stopId: String? = nil,
        globalResponse: GlobalResponse? = nil,
        pinnedRoutes: Set<String>? = nil,
        predictionsByStop: PredictionsByStopJoinResponse? = nil,
        schedulesResponse: ScheduleResponse? = nil
    ) {
        let stopId = stopId ?? self.stopId
        let globalResponse = globalResponse ?? self.globalResponse

        let newDepartures: StopDetailsDepartures? = if let globalResponse {
            StopDetailsDepartures.companion.fromData(
                stopId: stopId,
                global: globalResponse,
                schedules: schedulesResponse ?? self.schedulesResponse,
                predictions: (predictionsByStop ?? self.predictionsByStop)?.toPredictionsStreamDataResponse(),
                alerts: nearbyVM.alerts,
                pinnedRoutes: pinnedRoutes ?? self.pinnedRoutes,
                filterAtTime: now.toKotlinInstant(),
                useTripHeadsigns: nearbyVM.tripHeadsignsEnabled
            )
        } else {
            nil
        }

        let nextStopFilter = stopFilter ?? newDepartures?.autoStopFilter()
        if stopFilter != nextStopFilter {
            nearbyVM.setLastStopDetailsFilter(stopId, nextStopFilter)
        }

        internalDepartures = newDepartures
        nearbyVM.setDepartures(stopId, newDepartures)
    }
}
