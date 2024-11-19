//
//  TripDetailsView.swift
//  iosApp
//
//  Created by esimon on 11/14/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import shared
import SwiftPhoenixClient
import SwiftUI

struct TripDetailsView: View {
    let tripId: String
    let vehicleId: String?
    let routeId: String
    let stopId: String
    let stopSequence: Int?

    var global: GlobalResponse?

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @State var tripPredictionsRepository: ITripPredictionsRepository
    @State var tripPredictions: PredictionsStreamDataResponse?
    @State var tripPredictionsLoaded: Bool = false
    @State var tripRepository: ITripRepository
    @State var trip: Trip?
    @State var tripSchedulesResponse: TripSchedulesResponse?
    @State var vehicleRepository: IVehicleRepository
    @State var vehicleResponse: VehicleStreamDataResponse?

    let analytics: TripDetailsAnalytics

    @State var now = Date.now.toKotlinInstant()

    let inspection = Inspection<Self>()

    private var routeType: RouteType? {
        global?.routes[routeId]?.type
    }

    init(
        tripId: String,
        vehicleId: String?,
        routeId: String,
        stopId: String,
        stopSequence: Int?,
        global: GlobalResponse?,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
        tripPredictionsRepository: ITripPredictionsRepository = RepositoryDI().tripPredictions,
        tripRepository: ITripRepository = RepositoryDI().trip,
        vehicleRepository: IVehicleRepository = RepositoryDI().vehicle,
        analytics: TripDetailsAnalytics = AnalyticsProvider.shared
    ) {
        self.tripId = tripId
        self.vehicleId = vehicleId
        self.routeId = routeId
        self.stopId = stopId
        self.stopSequence = stopSequence
        self.global = global
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.tripPredictionsRepository = tripPredictionsRepository
        self.tripRepository = tripRepository
        self.vehicleRepository = vehicleRepository
        self.analytics = analytics
    }

    var body: some View {
        VStack(spacing: 16) {
            if nearbyVM.showDebugMessages {
                DebugView {
                    VStack {
                        Text(verbatim: "trip id \(tripId)")
                        Text(verbatim: "vehicle id: \(vehicleId)")
                    }
                }
            }
            if tripPredictionsLoaded, let global, let vehicle = vehicleResponse?.vehicle,
               let stops = TripDetailsStopList.companion.fromPieces(
                   tripId: tripId,
                   directionId: trip?.directionId ?? vehicle.directionId,
                   tripSchedules: tripSchedulesResponse,
                   tripPredictions: tripPredictions,
                   vehicle: vehicle,
                   alertsData: nearbyVM.alerts,
                   globalData: global
               ) {
                vehicleCardView
                if let stopSequence, let splitStops = stops.splitForTarget(
                    targetStopId: stopId,
                    targetStopSequence: Int32(stopSequence),
                    globalData: global
                ) {
                    TripDetailsStopListSplitView(
                        splitStops: splitStops,
                        now: now,
                        onTapLink: onTapStop,
                        routeType: routeType
                    )
                    .onAppear { didLoadData?(self) }
                } else {
                    TripDetailsStopListView(stops: stops, now: now, onTapLink: onTapStop, routeType: routeType)
                        .onAppear { didLoadData?(self) }
                }
            } else {
                loadingBody()
            }
        }
        .task { loadEverything(tripId: tripId) }
        .task { now = Date.now.toKotlinInstant() }
        .onAppear { joinRealtime() }
        .onDisappear { leaveRealtime() }
        .onChange(of: tripId) { nextTripId in
            mapVM.selectedVehicle = nil
            trip = nil
            tripSchedulesResponse = nil
            leavePredictions()
            tripPredictions = nil
            errorBannerVM.errorRepository.clearDataError(key: "TripDetailsView.loadTripSchedules")
            errorBannerVM.errorRepository.clearDataError(key: "TripDetailsView.loadTrip")
            loadTripSchedules(tripId: nextTripId)
            loadTrip(tripId: nextTripId)
            joinPredictions(tripId: nextTripId)
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
            onBackground: { leaveRealtime() }
        )
    }

    var didLoadData: ((Self) -> Void)?

    @ViewBuilder private func loadingBody() -> some View {
        TripDetailsStopListView(
            stops: LoadingPlaceholders.shared.tripDetailsStops(),
            now: now,
            onTapLink: { _, _, _ in },
            routeType: nil
        )
        .loadingPlaceholder()
    }

    private func loadEverything(tripId: String) {
        loadTripSchedules(tripId: tripId)
        loadTrip(tripId: tripId)
    }

    private func loadTripSchedules(tripId: String) {
        Task {
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "TripDetailsPage.loadTripSchedules",
                getData: { try await tripRepository.getTripSchedules(tripId: tripId) },
                onSuccess: { tripSchedulesResponse = $0 },
                onRefreshAfterError: { loadEverything(tripId: tripId) }
            )
        }
    }

    private func loadTrip(tripId: String) {
        Task {
            let response: ApiResult<TripResponse> = try await tripRepository.getTrip(tripId: tripId)
            let errorKey = "TripDetailsPage.loadTrip"
            switch onEnum(of: response) {
            case let .ok(okResponse):
                errorBannerVM.errorRepository.clearDataError(key: errorKey)
                trip = okResponse.data.trip
            case .error:
                errorBannerVM.errorRepository.setDataError(key: errorKey, action: { loadEverything(tripId: tripId) })
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
                // no error handling since persistent errors cause stale predictions
                switch onEnum(of: outcome) {
                case let .ok(result): tripPredictions = result.data
                case .error: break
                }
                tripPredictionsLoaded = true
            }
        }
    }

    private func leavePredictions() {
        tripPredictionsLoaded = false
        tripPredictionsRepository.disconnect()
    }

    private func joinVehicle(vehicleId: String?) {
        guard let vehicleId else { return }
        vehicleRepository.connect(vehicleId: vehicleId) { outcome in
            DispatchQueue.main.async {
                let errorKey = "TripDetailsPage.joinVehicle"
                switch onEnum(of: outcome) {
                case let .ok(result):
                    errorBannerVM.errorRepository.clearDataError(key: errorKey)
                    vehicleResponse = result.data
                    mapVM.selectedVehicle = result.data.vehicle
                case .error:
                    errorBannerVM.errorRepository.setDataError(
                        key: errorKey,
                        action: { loadEverything(tripId: tripId) }
                    )
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

    @ViewBuilder
    var vehicleCardView: some View {
        let trip: Trip? = tripPredictions?.trips[tripId]
        let vehicle: Vehicle? = vehicleResponse?.vehicle
        let vehicleStop: Stop? = if let stopId = vehicle?.stopId, let allStops = global?.stops {
            allStops[stopId]?.resolveParent(stops: allStops)
        } else {
            nil
        }
        let route: Route? = if let routeId = trip?.routeId ?? vehicle?.routeId {
            global?.routes[routeId]
        } else {
            nil
        }
        VehicleCardView(
            vehicle: vehicle,
            route: route,
            stop: vehicleStop,
            tripId: tripId
        )
    }

    @ViewBuilder
    var header: some View {
        let trip: Trip? = tripPredictions?.trips[tripId]
        let vehicle: Vehicle? = vehicleResponse?.vehicle
        let route: Route? = if let routeId = trip?.routeId ?? vehicle?.routeId {
            global?.routes[routeId]
        } else {
            nil
        }
        TripDetailsHeader(
            route: route,
            line: global?.getLine(lineId: route?.lineId),
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
        case let .legacyStopDetails(stop, filter): SheetNavigationStackEntry.legacyStopDetails(
                stop.resolveParent(stops: global?.stops ?? [:]),
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
