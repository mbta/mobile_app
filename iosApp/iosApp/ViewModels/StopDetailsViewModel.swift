//
//  StopDetailsViewModel.swift
//  iosApp
//
//  Created by esimon on 11/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftPhoenixClient
import SwiftUI

class StopDetailsViewModel: ObservableObject {
    @Published var global: GlobalResponse?
    @Published var pinnedRoutes: Set<String> = []
    @Published var predictionsByStop: PredictionsByStopJoinResponse?
    @Published var schedulesResponse: ScheduleResponse?

    let globalRepository: IGlobalRepository
    let pinnedRoutesRepository: IPinnedRoutesRepository
    let predictionsRepository: IPredictionsRepository
    let schedulesRepository: ISchedulesRepository
    let tripPredictionsRepository: ITripPredictionsRepository
    let tripRepository: ITripRepository
    let vehicleRepository: IVehicleRepository

    let togglePinnedUsecase = UsecaseDI().toggledPinnedRouteUsecase

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        pinnedRoutesRepository: IPinnedRoutesRepository = RepositoryDI().pinnedRoutes,
        predictionsRepository: IPredictionsRepository = RepositoryDI().predictions,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        tripPredictionsRepository: ITripPredictionsRepository = RepositoryDI().tripPredictions,
        tripRepository: ITripRepository = RepositoryDI().trip,
        vehicleRepository: IVehicleRepository = RepositoryDI().vehicle
    ) {
        self.globalRepository = globalRepository
        self.pinnedRoutesRepository = pinnedRoutesRepository
        self.predictionsRepository = predictionsRepository
        self.schedulesRepository = schedulesRepository
        self.tripPredictionsRepository = tripPredictionsRepository
        self.tripRepository = tripRepository
        self.vehicleRepository = vehicleRepository
    }

    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            Task { @MainActor in global = globalData }
        }
    }

    func getDepartures(
        stopId: String,
        alerts: AlertsStreamDataResponse?,
        useTripHeadsigns: Bool,
        now: Date
    ) -> StopDetailsDepartures? {
        if let global {
            StopDetailsDepartures.companion.fromData(
                stopId: stopId,
                global: global,
                schedules: schedulesResponse,
                predictions: predictionsByStop?.toPredictionsStreamDataResponse(),
                alerts: alerts,
                pinnedRoutes: pinnedRoutes,
                filterAtTime: now.toKotlinInstant(),
                useTripHeadsigns: useTripHeadsigns
            )
        } else {
            nil
        }
    }

    func joinPredictions(_ stopId: String, onSuccess: @escaping () -> Void, onComplete: @escaping () -> Void) {
        // no error handling since persistent errors cause stale predictions
        predictionsRepository.connectV2(stopIds: [stopId], onJoin: { outcome in
            Task { @MainActor in
                if case let .ok(result) = onEnum(of: outcome) {
                    self.predictionsByStop = result.data
                    onSuccess()
                }
                onComplete()
            }
        }, onMessage: { outcome in
            if case let .ok(result) = onEnum(of: outcome) {
                let nextPredictions = if let existingPredictionsByStop = self.predictionsByStop {
                    existingPredictionsByStop.mergePredictions(updatedPredictions: result.data)
                } else {
                    PredictionsByStopJoinResponse(
                        predictionsByStop: [result.data.stopId: result.data.predictions],
                        trips: result.data.trips,
                        vehicles: result.data.vehicles
                    )
                }
                Task { @MainActor in
                    self.predictionsByStop = nextPredictions
                    onSuccess()
                }
            }

            Task { @MainActor in onComplete() }
        })
    }

    func leavePredictions() {
        predictionsRepository.disconnect()
    }

    func loadPinnedRoutes() {
        Task {
            do {
                let nextPinned = try await pinnedRoutesRepository.getPinnedRoutes()
                Task { @MainActor in pinnedRoutes = nextPinned }
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

    func returnFromBackground() {
        if let predictionsByStop,
           predictionsRepository
           .shouldForgetPredictions(predictionCount: predictionsByStop.predictionQuantity()) {
            Task { @MainActor in self.predictionsByStop = nil }
        }
    }
}
