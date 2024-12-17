//
//  StopDetailsViewModel.swift
//  iosApp
//
//  Created by esimon on 11/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

// swiftlint:disable type_body_length

import shared
import SwiftPhoenixClient
import SwiftUI

struct TripData {
    let tripFilter: TripDetailsFilter
    let trip: Trip
    var tripSchedules: TripSchedulesResponse
    var tripPredictions: PredictionsStreamDataResponse?
    var vehicle: Vehicle?
}

// A subset of route attributes only for displaying as UI accents,
// this is split out to allow defaults for when a route may not exist
struct TripRouteAccents {
    let color: Color
    let textColor: Color
    let type: RouteType

    init(color: Color = .halo, textColor: Color = .text, type: RouteType = .bus) {
        self.color = color
        self.textColor = textColor
        self.type = type
    }

    init(route: Route) {
        color = route.uiColor
        textColor = route.uiTextColor
        type = route.type
    }
}

class StopDetailsViewModel: ObservableObject {
    @Published var global: GlobalResponse?
    @Published var pinnedRoutes: Set<String> = []
    @Published var predictionsByStop: PredictionsByStopJoinResponse?
    @Published var schedulesResponse: ScheduleResponse?

    @Published var tripData: TripData?

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
            Task { @MainActor in self.global = globalData }
        }
    }

    @MainActor func clearTripDetails() {
        leaveTripChannels()
        tripData = nil
        errorBannerRepository.clearDataError(key: "TripDetailsView.loadTripSchedules")
        errorBannerRepository.clearDataError(key: "TripDetailsView.loadTrip")
    }

    private func clearAndLoadTripDetails(_ filter: TripDetailsFilter) {
        Task {
            await self.clearTripDetails()
            await self.loadTripDetails(tripFilter: filter)
            self.joinTripChannels(tripFilter: filter)
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

    func getTripRouteAccents() -> TripRouteAccents {
        guard let routeId = tripData?.trip.routeId,
              let route = global?.routes[routeId]
        else {
            return TripRouteAccents()
        }
        return TripRouteAccents(route: route)
    }

    @MainActor
    func handleTripFilterChange(_ tripFilter: TripDetailsFilter?) {
        guard let tripFilter else {
            // If the next trip filter is nil, clear everything and leave realtime
            clearTripDetails()
            return
        }
        if let tripData {
            let currentFilter = tripData.tripFilter
            if currentFilter.tripId == tripFilter.tripId, currentFilter.vehicleId == tripFilter.vehicleId {
                // If the filter changed but the trip and vehicle are the same, replace the filter but keep all the data
                self.tripData = TripData(
                    tripFilter: tripFilter,
                    trip: tripData.trip,
                    tripSchedules: tripData.tripSchedules,
                    tripPredictions: tripData.tripPredictions,
                    vehicle: tripData.vehicle
                )
                return
            } else if currentFilter.tripId == tripFilter.tripId {
                // If only the vehicle changed but the trip is the same, clear and reload only the vehicle,
                // keep the prediction channel open and copy static trip data into new trip data
                leaveVehicle()
                self.tripData = TripData(
                    tripFilter: tripFilter,
                    trip: tripData.trip,
                    tripSchedules: tripData.tripSchedules,
                    tripPredictions: tripData.tripPredictions,
                    vehicle: nil
                )
                joinVehicle(tripFilter: tripFilter)
                return
            } else if currentFilter.vehicleId == tripFilter.vehicleId {
                // If only the trip changed but the vehicle is the same, clear and reload only the trip details,
                // keep the vehicle channel open and copy the last vehicle into the new trip data
                let currentVehicle = tripData.vehicle
                leaveTripPredictions()
                self.tripData = nil
                Task {
                    await self.loadTripDetails(tripFilter: tripFilter)
                    Task { @MainActor in self.tripData?.vehicle = currentVehicle }
                    self.joinTripPredictions(tripFilter: tripFilter)
                }
                return
            }
            // If current trip data exists but neither the trip ID or vehicle ID match,
            // fall through and reload everything
        }

        clearAndLoadTripDetails(tripFilter)
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

    func joinTripChannels(tripFilter: TripDetailsFilter) {
        joinVehicle(tripFilter: tripFilter)
        joinTripPredictions(tripFilter: tripFilter)
    }

    private func joinTripPredictions(tripFilter: TripDetailsFilter) {
        leaveTripPredictions()
        tripPredictionsRepository.connect(tripId: tripFilter.tripId) { outcome in
            Task { @MainActor in
                // no error handling since persistent errors cause stale predictions
                switch onEnum(of: outcome) {
                case let .ok(result): self.tripData?.tripPredictions = result.data
                case .error: break
                }
            }
        }
    }

    private func joinVehicle(tripFilter: TripDetailsFilter) {
        leaveVehicle()
        guard let vehicleId = tripFilter.vehicleId else {
            // If the filter has a null vehicle ID, we can't join anything, clear the vehicle and return
            Task { @MainActor in tripData?.vehicle = nil }
            return
        }
        let errorKey = "TripDetailsPage.joinVehicle"
        vehicleRepository.connect(vehicleId: vehicleId) { outcome in
            Task { @MainActor in
                switch onEnum(of: outcome) {
                case let .ok(result):
                    self.tripData?.vehicle = result.data.vehicle
                    self.errorBannerRepository.clearDataError(key: errorKey)
                case .error:
                    self.tripData?.vehicle = nil
                    self.errorBannerRepository.setDataError(
                        key: errorKey,
                        action: { self.clearAndLoadTripDetails(tripFilter) }
                    )
                }
            }
        }
    }

    func leavePredictions() {
        predictionsRepository.disconnect()
    }

    func leaveTripChannels() {
        leaveTripPredictions()
        leaveVehicle()
    }

    private func leaveTripPredictions() {
        tripPredictionsRepository.disconnect()
    }

    private func leaveVehicle() {
        vehicleRepository.disconnect()
    }

    func loadPinnedRoutes() {
        Task {
            do {
                let nextPinned = try await pinnedRoutesRepository.getPinnedRoutes()
                Task { @MainActor in self.pinnedRoutes = nextPinned }
            } catch is CancellationError {
                // do nothing on cancellation
            } catch {
                // getPinnedRoutes shouldn't actually fail
                debugPrint(error)
            }
        }
    }

    private func loadTripDetails(tripFilter: TripDetailsFilter) async {
        async let tripResult = loadTrip(tripFilter: tripFilter)
        async let scheduleResult = loadTripSchedules(tripFilter: tripFilter)
        let results = await (tripResult, scheduleResult)
        let task = Task { @MainActor in
            if let trip = results.0, let schedules = results.1 {
                self.tripData = TripData(tripFilter: tripFilter, trip: trip, tripSchedules: schedules)
            } else {
                self.tripData = nil
            }
        }

        await task.value
    }

    private func loadTrip(tripFilter: TripDetailsFilter) async -> Trip? {
        let errorKey = "TripDetailsPage.loadTrip"
        do {
            let response: ApiResult<TripResponse> = try await tripRepository.getTrip(tripId: tripFilter.tripId)
            let task = Task<Trip?, Error> { @MainActor in
                switch onEnum(of: response) {
                case let .ok(okResponse):
                    self.errorBannerRepository.clearDataError(key: errorKey)
                    return okResponse.data.trip
                case .error:
                    self.errorBannerRepository.setDataError(
                        key: errorKey,
                        action: { self.clearAndLoadTripDetails(tripFilter) }
                    )
                    return nil
                }
            }

            return try await task.value
        } catch {
            return nil
        }
    }

    private func loadTripSchedules(tripFilter: TripDetailsFilter) async -> TripSchedulesResponse? {
        let task = Task<TripSchedulesResponse?, Error> {
            var result: TripSchedulesResponse?
            await fetchApi(
                self.errorBannerRepository,
                errorKey: "TripDetailsPage.loadTripSchedules",
                getData: { try await self.tripRepository.getTripSchedules(tripId: tripFilter.tripId) },
                onSuccess: { @MainActor in result = $0 },
                onRefreshAfterError: { self.clearAndLoadTripDetails(tripFilter) }
            )
            return result
        }

        do {
            return try await task.value
        } catch {
            return nil
        }
    }

    @MainActor
    func returnFromBackground() {
        if let predictionsByStop,
           predictionsRepository
           .shouldForgetPredictions(predictionCount: predictionsByStop.predictionQuantity()) {
            self.predictionsByStop = nil
        }

        if let predictions = tripData?.tripPredictions,
           tripPredictionsRepository
           .shouldForgetPredictions(predictionCount: predictions.predictionQuantity()) {
            tripData?.tripPredictions = nil
        }
    }

    func togglePinnedRoute(_ routeId: String) {
        Task {
            do {
                _ = try await self.togglePinnedUsecase.execute(route: routeId)
                self.loadPinnedRoutes()
            } catch is CancellationError {
                // do nothing on cancellation
            } catch {
                // execute shouldn't actually fail
                debugPrint(error)
            }
        }
    }
}
