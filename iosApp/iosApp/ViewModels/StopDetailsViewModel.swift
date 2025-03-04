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

struct StopData: Equatable {
    let stopId: String
    let schedules: ScheduleResponse
    var predictionsByStop: PredictionsByStopJoinResponse?
    var predictionsLoaded: Bool = false
}

struct TripData {
    let tripFilter: TripDetailsFilter
    let trip: Trip
    var tripSchedules: TripSchedulesResponse
    var tripPredictions: PredictionsStreamDataResponse?
    var tripPredictionsLoaded: Bool = false
    var vehicle: Vehicle?
}

// A subset of route attributes only for displaying as UI accents,
// this is split out to allow defaults for when a route may not exist
struct TripRouteAccents: Hashable {
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

// swiftlint:disable:next type_body_length
class StopDetailsViewModel: ObservableObject {
    @Published var global: GlobalResponse?
    @Published var hideMaps: Bool = false
    @Published var showElevatorAccessibility: Bool = false
    @Published var pinnedRoutes: Set<String> = []

    @Published var stopData: StopData?
    @Published var tripData: TripData?
    @Published var explainer: Explainer?

    private let errorBannerRepository: IErrorBannerStateRepository
    private let globalRepository: IGlobalRepository
    private let pinnedRoutesRepository: IPinnedRoutesRepository
    private let predictionsRepository: IPredictionsRepository
    private let schedulesRepository: ISchedulesRepository
    private let settingsRepository: ISettingsRepository
    private let tripPredictionsRepository: ITripPredictionsRepository
    private let tripRepository: ITripRepository
    private let vehicleRepository: IVehicleRepository

    private let togglePinnedUsecase = UsecaseDI().toggledPinnedRouteUsecase

    init(
        errorBannerRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        globalRepository: IGlobalRepository = RepositoryDI().global,
        pinnedRoutesRepository: IPinnedRoutesRepository = RepositoryDI().pinnedRoutes,
        predictionsRepository: IPredictionsRepository = RepositoryDI().predictions,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        settingsRepository: ISettingsRepository = RepositoryDI().settings,
        tripPredictionsRepository: ITripPredictionsRepository = RepositoryDI().tripPredictions,
        tripRepository: ITripRepository = RepositoryDI().trip,
        vehicleRepository: IVehicleRepository = RepositoryDI().vehicle
    ) {
        self.errorBannerRepository = errorBannerRepository
        self.globalRepository = globalRepository
        self.pinnedRoutesRepository = pinnedRoutesRepository
        self.predictionsRepository = predictionsRepository
        self.schedulesRepository = schedulesRepository
        self.settingsRepository = settingsRepository
        self.tripPredictionsRepository = tripPredictionsRepository
        self.tripRepository = tripRepository
        self.vehicleRepository = vehicleRepository
    }

    private func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            Task { @MainActor in self.global = globalData }
        }
    }

    func checkStopPredictionsStale() {
        Task {
            if let lastPredictions = predictionsRepository.lastUpdated {
                errorBannerRepository.checkPredictionsStale(
                    predictionsLastUpdated: lastPredictions,
                    predictionQuantity: Int32(
                        stopData?.predictionsByStop?.predictionQuantity() ?? 0
                    ),
                    action: {
                        self.leaveStopPredictions()
                        if let stopId = self.stopData?.stopId {
                            self.joinStopPredictions(stopId)
                        }
                    }
                )
            }
        }
    }

    @MainActor func clearStopDetails() {
        clearTripDetails()
        leaveStopPredictions()
        stopData = nil
        errorBannerRepository.clearDataError(key: "StopDetailsPage.getSchedule")
    }

    @MainActor func clearTripDetails() {
        leaveTripChannels()
        tripData = nil
        errorBannerRepository.clearDataError(key: "TripDetailsView.joinVehicle")
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
        now: Date
    ) -> StopDetailsDepartures? {
        if let global, let schedules = stopData?.schedules {
            StopDetailsDepartures.companion.fromData(
                stopId: stopId,
                global: global,
                schedules: schedules,
                predictions: stopData?.predictionsByStop?.toPredictionsStreamDataResponse(),
                alerts: alerts,
                pinnedRoutes: pinnedRoutes,
                filterAtTime: now.toKotlinInstant()
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

    func handleStopAppear(_ stopId: String) {
        Task {
            loadGlobalData()
            loadPinnedRoutes()
            await handleStopChange(stopId)
            loadSettings()
        }
    }

    @MainActor
    func handleStopChange(_ stopId: String) {
        clearStopDetails()
        Task {
            await self.loadStopDetails(stopId: stopId)
            self.joinStopPredictions(stopId)
        }
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
                    tripPredictionsLoaded: true,
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
                    tripPredictionsLoaded: true,
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

    func joinStopPredictions(_ stopId: String) {
        // no error handling since persistent errors cause stale predictions
        predictionsRepository.connectV2(stopIds: [stopId], onJoin: { outcome in
            Task { @MainActor in
                if case let .ok(result) = onEnum(of: outcome) {
                    self.stopData?.predictionsByStop = result.data
                    self.checkStopPredictionsStale()
                }
                self.stopData?.predictionsLoaded = true
            }
        }, onMessage: { outcome in
            if case let .ok(result) = onEnum(of: outcome) {
                let nextPredictions = if let existingPredictionsByStop = self.stopData?.predictionsByStop {
                    existingPredictionsByStop.mergePredictions(updatedPredictions: result.data)
                } else {
                    PredictionsByStopJoinResponse(
                        predictionsByStop: [result.data.stopId: result.data.predictions],
                        trips: result.data.trips,
                        vehicles: result.data.vehicles
                    )
                }
                Task { @MainActor in
                    self.stopData?.predictionsByStop = nextPredictions
                    self.checkStopPredictionsStale()
                }
            }

            Task { @MainActor in self.stopData?.predictionsLoaded = true }
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
                self.tripData?.tripPredictionsLoaded = true
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
        let errorKey = "TripDetailsView.joinVehicle"
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

    func leaveStopPredictions() {
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

    func loadGlobalData() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorBannerRepository,
                errorKey: "StopDetailsPage.loadGlobalData",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: loadGlobalData
            )
        }
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

    func loadSettings() {
        Task {
            let loaded = await settingsRepository.load([.hideMaps, .elevatorAccessibility])
            Task { @MainActor in
                hideMaps = loaded.getSafe(.hideMaps)
                showElevatorAccessibility = loaded.getSafe(.elevatorAccessibility)
            }
        }
    }

    func loadStopDetails(stopId: String) async {
        let schedules = await loadStopSchedules(stopId: stopId)
        let task = Task { @MainActor in
            if let schedules {
                self.stopData = StopData(stopId: stopId, schedules: schedules)
            } else {
                self.stopData = nil
            }
        }
        await task.value
    }

    private func loadStopSchedules(stopId: String) async -> ScheduleResponse? {
        let task = Task<ScheduleResponse?, Error> {
            var result: ScheduleResponse?
            await fetchApi(
                self.errorBannerRepository,
                errorKey: "StopDetailsPage.getSchedule",
                getData: { try await self.schedulesRepository.getSchedule(stopIds: [stopId]) },
                onSuccess: { @MainActor in result = $0 },
                onRefreshAfterError: { Task { await self.handleStopChange(stopId) } }
            )
            return result
        }

        do {
            return try await task.value
        } catch {
            return nil
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
        let errorKey = "TripDetailsView.loadTrip"
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
                errorKey: "TripDetailsView.loadTripSchedules",
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
        if let stopPredictions = stopData?.predictionsByStop,
           predictionsRepository
           .shouldForgetPredictions(predictionCount: stopPredictions.predictionQuantity()) {
            stopData?.predictionsByStop = nil
        }

        if let tripPredictions = tripData?.tripPredictions,
           tripPredictionsRepository
           .shouldForgetPredictions(predictionCount: tripPredictions.predictionQuantity()) {
            tripData?.tripPredictions = nil
        }
    }

    func togglePinnedRoute(_ routeId: String) async -> Bool {
        let task = Task<Bool, Error> {
            do {
                let newValue = try await self.togglePinnedUsecase.execute(route: routeId).boolValue
                self.loadPinnedRoutes()
                return newValue
            } catch is CancellationError {
                // do nothing on cancellation
            } catch {
                // execute shouldn't actually fail
                debugPrint(error)
            }
            return false
        }

        do {
            return try await task.value
        } catch {
            return false
        }
    }
}
