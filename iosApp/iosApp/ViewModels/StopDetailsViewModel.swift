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

    @Published var trip: Trip?
    @Published var tripPredictions: PredictionsStreamDataResponse?
    @Published var tripPredictionsLoaded: Bool = false
    @Published var tripSchedules: TripSchedulesResponse?
    @Published var vehicle: Vehicle?

    let errorBannerRepository: IErrorBannerStateRepository
    let globalRepository: IGlobalRepository
    let pinnedRoutesRepository: IPinnedRoutesRepository
    let predictionsRepository: IPredictionsRepository
    let schedulesRepository: ISchedulesRepository
    let tripPredictionsRepository: ITripPredictionsRepository
    let tripRepository: ITripRepository
    let vehicleRepository: IVehicleRepository

    let togglePinnedUsecase = UsecaseDI().toggledPinnedRouteUsecase

    init(
        errorBannerRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        globalRepository: IGlobalRepository = RepositoryDI().global,
        pinnedRoutesRepository: IPinnedRoutesRepository = RepositoryDI().pinnedRoutes,
        predictionsRepository: IPredictionsRepository = RepositoryDI().predictions,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        tripPredictionsRepository: ITripPredictionsRepository = RepositoryDI().tripPredictions,
        tripRepository: ITripRepository = RepositoryDI().trip,
        vehicleRepository: IVehicleRepository = RepositoryDI().vehicle
    ) {
        self.errorBannerRepository = errorBannerRepository
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

    func clearTripDetails() {
        trip = nil
        tripSchedules = nil
        leaveTripPredictions()
        tripPredictions = nil
        leaveVehicle()
        vehicle = nil
        errorBannerRepository.clearDataError(key: "TripDetailsView.loadTripSchedules")
        errorBannerRepository.clearDataError(key: "TripDetailsView.loadTrip")
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

    func joinTripPredictions(tripId: String) {
        tripPredictionsRepository.connect(tripId: tripId) { outcome in
            DispatchQueue.main.async {
                // no error handling since persistent errors cause stale predictions
                switch onEnum(of: outcome) {
                case let .ok(result): self.tripPredictions = result.data
                case .error: break
                }
                self.tripPredictionsLoaded = true
            }
        }
    }

    func joinVehicle(
        tripId: String,
        vehicleId: String?,
        onSuccess: @escaping (Vehicle?) -> Void
    ) {
        guard let vehicleId else { return }
        let errorKey = "TripDetailsPage.joinVehicle"
        vehicleRepository.connect(vehicleId: vehicleId) { outcome in
            Task { @MainActor in
                switch onEnum(of: outcome) {
                case let .ok(result):
                    self.vehicle = result.data.vehicle
                    onSuccess(self.vehicle)
                    self.errorBannerRepository.clearDataError(key: errorKey)
                case .error:
                    self.vehicle = nil
                    self.errorBannerRepository.setDataError(
                        key: errorKey,
                        action: {
                            self.loadTripDetails(tripId: tripId)
                            self.joinVehicle(tripId: tripId, vehicleId: vehicleId, onSuccess: onSuccess)
                        }
                    )
                }
            }
        }
    }

    func leavePredictions() {
        predictionsRepository.disconnect()
    }

    func leaveTripPredictions() {
        tripPredictionsLoaded = false
        tripPredictionsRepository.disconnect()
    }

    func leaveVehicle() {
        vehicleRepository.disconnect()
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

    func loadTripDetails(tripId: String) {
        loadTrip(tripId: tripId)
        loadTripSchedules(tripId: tripId)
    }

    func loadTrip(tripId: String) {
        Task {
            let errorKey = "TripDetailsPage.loadTrip"
            let response: ApiResult<TripResponse> = try await tripRepository.getTrip(tripId: tripId)
            Task { @MainActor in
                switch onEnum(of: response) {
                case let .ok(okResponse):
                    trip = okResponse.data.trip
                    errorBannerRepository.clearDataError(key: errorKey)
                case .error:
                    trip = nil
                    errorBannerRepository.setDataError(
                        key: errorKey,
                        action: { self.loadTripDetails(tripId: tripId) }
                    )
                }
            }
        }
    }

    func loadTripSchedules(tripId: String) {
        Task {
            await fetchApi(
                errorBannerRepository,
                errorKey: "TripDetailsPage.loadTripSchedules",
                getData: { try await tripRepository.getTripSchedules(tripId: tripId) },
                onSuccess: { @MainActor in tripSchedules = $0 },
                onRefreshAfterError: { self.loadTripDetails(tripId: tripId) }
            )
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
            self.predictionsByStop = nil
        }

        if let tripPredictions,
           tripPredictionsRepository
           .shouldForgetPredictions(predictionCount: tripPredictions.predictionQuantity()) {
            self.tripPredictions = nil
        }
    }
}
