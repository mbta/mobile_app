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

    let analytics: TripDetailsAnalytics

    @State var now = Date.now.toKotlinInstant()

    let inspection = Inspection<Self>()

    init(
        tripId: String,
        vehicleId: String,
        target: TripDetailsTarget?,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
        globalRepository: IGlobalRepository = RepositoryDI().global,
        tripPredictionsRepository: ITripPredictionsRepository = RepositoryDI().tripPredictions,
        tripRepository: ITripRepository = RepositoryDI().trip,
        vehicleRepository: IVehicleRepository = RepositoryDI().vehicle,
        analytics: TripDetailsAnalytics = AnalyticsProvider.shared
    ) {
        self.tripId = tripId
        self.vehicleId = vehicleId
        self.target = target
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.globalRepository = globalRepository
        self.tripPredictionsRepository = tripPredictionsRepository
        self.tripRepository = tripRepository
        self.vehicleRepository = vehicleRepository
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
                    if let target, let stopSequence = target.stopSequence, let splitStops = stops.splitForTarget(
                        targetStopId: target.stopId,
                        targetStopSequence: Int32(stopSequence),
                        globalData: globalResponse
                    ) {
                        TripDetailsStopListSplitView(splitStops: splitStops, now: now, onTapLink: onTapStop)
                    } else {
                        TripDetailsStopListView(stops: stops, now: now, onTapLink: onTapStop)
                    }
                } else {
                    Text("Couldn't load stop list")
                }
            } else {
                ProgressView()
            }
        }
        .task {
            do {
                globalResponse = try await globalRepository.getGlobalData()
            } catch {
                debugPrint(error)
            }
        }
        .task {
            do {
                tripSchedulesResponse = try await tripRepository.getTripSchedules(tripId: tripId)
            } catch {
                debugPrint(error)
            }
        }
        .task {
            do {
                let response: ApiResult<TripResponse> = try await tripRepository.getTrip(tripId: tripId)
                trip = switch onEnum(of: response) {
                case let .ok(okResponse): okResponse.data.trip
                case .error: nil
                }
            } catch {
                debugPrint(error)
            }
        }
        .task {
            now = Date.now.toKotlinInstant()
            while !Task.isCancelled {
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
        .onChange(of: tripId) { joinPredictions(tripId: $0) }
        .onChange(of: vehicleId) { vehicleId in
            joinVehicle(vehicleId: vehicleId)
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .withScenePhaseHandlers(onActive: joinRealtime,
                                onInactive: leaveRealtime,
                                onBackground: leaveRealtime)
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
                if let data = outcome.data {
                    tripPredictions = data
                } else {
                    tripPredictions = nil
                }
            }
        }
    }

    private func leavePredictions() {
        tripPredictionsRepository.disconnect()
    }

    private func joinVehicle(vehicleId: String) {
        vehicleRepository.connect(vehicleId: vehicleId) { outcome in
            DispatchQueue.main.async {
                if let data = outcome.data {
                    vehicleResponse = data
                    mapVM.selectedVehicle = data.vehicle
                } else {
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
            onBack: nearbyVM.goBack,
            route: route,
            line: globalResponse?.getLine(lineId: route?.lineId),
            trip: trip
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
