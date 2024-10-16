//
//  TripDetailsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import shared
import SwiftPhoenixClient
import SwiftUI

struct TripDetailsPage: View {
    let tripId: String
    let vehicleId: String
    let routeId: String
    let target: TripDetailsTarget?

    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    var globalRepository: IGlobalRepository
    @State var globalResponse: GlobalResponse?
    @State var tripPredictionsRepository: ITripPredictionsRepository
    @State var tripPredictions: PredictionsStreamDataResponse?
    @State var tripRepository: ITripRepository
    @State var trip: Trip?
    @State var tripSchedulesResponse: TripSchedulesResponse?
    @State var vehicleRepository: IVehicleRepository
    @State var vehicleResponse: VehicleStreamDataResponse?

    @State var isReturningFromBackground = false

    var errorBannerRepository: IErrorBannerStateRepository
    let analytics: TripDetailsAnalytics

    @State var now = Date.now.toKotlinInstant()

    let inspection = Inspection<Self>()

    private var routeType: RouteType? {
        globalResponse?.routes[routeId]?.type
    }

    init(
        tripId: String,
        vehicleId: String,
        routeId: String,
        target: TripDetailsTarget?,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
        globalRepository: IGlobalRepository = RepositoryDI().global,
        tripPredictionsRepository: ITripPredictionsRepository = RepositoryDI().tripPredictions,
        tripRepository: ITripRepository = RepositoryDI().trip,
        vehicleRepository: IVehicleRepository = RepositoryDI().vehicle,
        errorBannerRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        analytics: TripDetailsAnalytics = AnalyticsProvider.shared
    ) {
        self.tripId = tripId
        self.vehicleId = vehicleId
        self.routeId = routeId
        self.target = target
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.globalRepository = globalRepository
        self.tripPredictionsRepository = tripPredictionsRepository
        self.tripRepository = tripRepository
        self.vehicleRepository = vehicleRepository
        self.errorBannerRepository = errorBannerRepository
        self.analytics = analytics
    }

    var body: some View {
        VStack {
            header
            if let trip, let globalResponse {
                let vehicle = vehicleResponse?.vehicle
                if let stops = TripDetailsStopList.companion.fromPieces(
                    trip: trip,
                    tripSchedules: tripSchedulesResponse,
                    tripPredictions: tripPredictions,
                    vehicle: vehicle, alertsData: nearbyVM.alerts, globalData: globalResponse
                ) {
                    vehicleCardView
                    ErrorBanner(loadingWhenPredictionsStale: isReturningFromBackground)
                    if let target, let stopSequence = target.stopSequence, let splitStops = stops.splitForTarget(
                        targetStopId: target.stopId,
                        targetStopSequence: Int32(stopSequence),
                        globalData: globalResponse
                    ) {
                        TripDetailsStopListSplitView(
                            splitStops: splitStops,
                            now: now,
                            onTapLink: onTapStop,
                            routeType: routeType
                        )
                    } else {
                        TripDetailsStopListView(stops: stops, now: now, onTapLink: onTapStop, routeType: routeType)
                    }
                } else {
                    Text("Couldn't load stop list")
                }
            } else {
                ProgressView()
            }
        }
        .task {
            loadEverything()
        }
        .task {
            now = Date.now.toKotlinInstant()
            while !Task.isCancelled {
                checkPredictionsStale()
                do {
                    try await Task.sleep(for: .seconds(1))
                } catch {
                    debugPrint("Can't sleep", error)
                }
                now = Date.now.toKotlinInstant()
            }
        }
        .onAppear { joinRealtime() }
        .onDisappear { leaveRealtime() }
        .onChange(of: tripId) {
            leavePredictions()
            joinPredictions(tripId: $0)
        }
        .onChange(of: vehicleId) { vehicleId in
            leaveVehicle()
            joinVehicle(vehicleId: vehicleId)
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .withScenePhaseHandlers(
            onActive: {
                if let tripPredictions,
                   tripPredictionsRepository
                   .shouldForgetPredictions(predictionCount: tripPredictions.predictionQuantity()) {
                    self.tripPredictions = nil
                }
                joinRealtime()
            },
            onInactive: leaveRealtime,
            onBackground: {
                leaveRealtime()
                isReturningFromBackground = true
            }
        )
    }

    private func loadEverything() {
        loadGlobalData()
        loadTripSchedules()
        loadTrip()
    }

    private func loadGlobalData() {
        Task {
            await fetchApi(
                errorBannerRepository,
                errorKey: "TripDetailsPage.loadGlobalData",
                getData: globalRepository.getGlobalData,
                onSuccess: { globalResponse = $0 },
                onRefreshAfterError: loadEverything
            )
        }
    }

    private func loadTripSchedules() {
        Task {
            await fetchApi(
                errorBannerRepository,
                errorKey: "TripDetailsPage.loadTripSchedules",
                getData: { try await tripRepository.getTripSchedules(tripId: tripId) },
                onSuccess: { tripSchedulesResponse = $0 },
                onRefreshAfterError: loadEverything
            )
        }
    }

    private func loadTrip() {
        Task {
            let response: ApiResult<TripResponse> = try await tripRepository.getTrip(tripId: tripId)
            let errorKey = "TripDetailsPage.loadTrip"
            switch onEnum(of: response) {
            case let .ok(okResponse):
                errorBannerRepository.clearDataError(key: errorKey)
                trip = okResponse.data.trip
            case .error:
                errorBannerRepository.setDataError(key: errorKey, action: loadEverything)
                trip = nil
            }
        }
    }

    private func joinRealtime() {
        joinPredictions(tripId: tripId)
        joinVehicle(vehicleId: vehicleId)
    }

    private func leaveRealtime() {
        leavePredictions()
        leaveVehicle()
    }

    private func joinPredictions(tripId: String) {
        tripPredictionsRepository.connect(tripId: tripId) { outcome in
            DispatchQueue.main.async {
                isReturningFromBackground = false
                // no error handling since persistent errors cause stale predictions
                switch onEnum(of: outcome) {
                case let .ok(result): tripPredictions = result.data
                case .error: break
                }
                checkPredictionsStale()
            }
        }
    }

    private func leavePredictions() {
        tripPredictionsRepository.disconnect()
    }

    private func joinVehicle(vehicleId: String) {
        vehicleRepository.connect(vehicleId: vehicleId) { outcome in
            DispatchQueue.main.async {
                let errorKey = "TripDetailsPage.joinVehicle"
                switch onEnum(of: outcome) {
                case let .ok(result):
                    errorBannerRepository.clearDataError(key: errorKey)
                    vehicleResponse = result.data
                    mapVM.selectedVehicle = result.data.vehicle
                case .error:
                    errorBannerRepository.setDataError(key: errorKey, action: loadEverything)
                    vehicleResponse = nil
                }
            }
        }
    }

    private func leaveVehicle() {
        vehicleRepository.disconnect()
        if mapVM.selectedVehicle?.id == vehicleId {
            mapVM.selectedVehicle = nil
        }
    }

    private func checkPredictionsStale() {
        if let lastPredictions = tripPredictionsRepository.lastUpdated {
            errorBannerRepository.checkPredictionsStale(
                predictionsLastUpdated: lastPredictions,
                predictionQuantity: Int32(tripPredictions?.predictionQuantity() ?? 0),
                action: {
                    leavePredictions()
                    joinPredictions(tripId: tripId)
                }
            )
        }
    }

    @ViewBuilder
    var vehicleCardView: some View {
        let trip: Trip? = tripPredictions?.trips[tripId]
        let vehicle: Vehicle? = vehicleResponse?.vehicle
        let vehicleStop: Stop? = if let stopId = vehicle?.stopId, let allStops = globalResponse?.stops {
            allStops[stopId]?.resolveParent(stops: allStops)
        } else {
            nil
        }
        let route: Route? = if let routeId = trip?.routeId {
            globalResponse?.routes[routeId]
        } else {
            nil
        }
        VehicleCardView(
            vehicle: vehicle,
            route: route,
            line: globalResponse?.getLine(lineId: route?.lineId),
            stop: vehicleStop,
            trip: trip
        )
    }

    @ViewBuilder
    var header: some View {
        let trip: Trip? = tripPredictions?.trips[tripId]
        let route: Route? = if let routeId = trip?.routeId {
            globalResponse?.routes[routeId]
        } else {
            nil
        }
        TripDetailsHeader(
            route: route,
            line: globalResponse?.getLine(lineId: route?.lineId),
            trip: trip,
            onBack: nearbyVM.goBack,
            onClose: { nearbyVM.navigationStack.removeAll() }
        )
    }

    func onTapStop(
        entry: SheetNavigationStackEntry,
        stop: TripDetailsStopList.Entry,
        connectingRouteId: String?
    ) {
        // resolve parent stop before following link
        let realEntry = switch entry {
        case let .stopDetails(stop, filter): SheetNavigationStackEntry.stopDetails(
                stop.resolveParent(stops: globalResponse?.stops ?? [:]),
                filter
            )
        default: entry
        }
        nearbyVM.pushNavEntry(realEntry)
        analytics.tappedDownstreamStop(
            routeId: trip?.routeId ?? "",
            stopId: stop.stop.id,
            tripId: tripId,
            connectingRouteId: connectingRouteId
        )
    }
}
