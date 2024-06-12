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

    @ObservedObject var tripPredictionsFetcher: TripPredictionsFetcher
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var vehicleFetcher: VehicleFetcher
    var tripSchedulesRepository: ITripSchedulesRepository
    @State var tripSchedulesResponse: TripSchedulesResponse?

    @State var now = Date.now.toKotlinInstant()

    let inspection = Inspection<Self>()

    init(
        tripId: String,
        vehicleId: String,
        target: TripDetailsTarget?,
        globalFetcher: GlobalFetcher,
        nearbyVM: NearbyViewModel,
        tripPredictionsFetcher: TripPredictionsFetcher,
        tripSchedulesRepository: ITripSchedulesRepository = RepositoryDI().tripSchedules,
        vehicleFetcher: VehicleFetcher
    ) {
        self.tripId = tripId
        self.vehicleId = vehicleId
        self.target = target
        self.globalFetcher = globalFetcher
        self.nearbyVM = nearbyVM
        self.tripPredictionsFetcher = tripPredictionsFetcher
        self.tripSchedulesRepository = tripSchedulesRepository
        self.vehicleFetcher = vehicleFetcher
    }

    var body: some View {
        VStack {
            SheetHeader(onClose: { nearbyVM.goBack() })

            if let globalData = globalFetcher.response {
                let vehicle = vehicleFetcher.response?.vehicle
                if let stops = TripDetailsStopList.companion.fromPieces(
                    tripSchedules: tripSchedulesResponse,
                    tripPredictions: tripPredictionsFetcher.predictions,
                    vehicle: vehicle, globalData: globalData
                ) {
                    vehicleCardView
                    if let target, let splitStops = stops.splitForTarget(
                        targetStopId: target.stopId,
                        targetStopSequence: Int32(target.stopSequence),
                        globalData: globalData
                    ) {
                        TripDetailsStopListSplitView(splitStops: splitStops, now: now)
                    } else {
                        TripDetailsStopListView(stops: stops, now: now)
                    }
                } else {
                    Text("Couldn't load stop list")
                }
            } else {
                ProgressView()
            }

            tripPredictionsFetcher.errorText
        }
        .task {
            do {
                tripSchedulesResponse = try await tripSchedulesRepository.getTripSchedules(tripId: tripId)
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
        .onAppear { joinRealtime()
        }
        .onDisappear { leaveRealtime()
        }
        .onChange(of: tripId) { joinPredictions(tripId: $0) }
        .onChange(of: vehicleId) { vehicleId in
            vehicleFetcher.run(vehicleId: vehicleId)
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .withScenePhaseHandlers(onActive: joinRealtime,
                                onInactive: leaveRealtime,
                                onBackground: leaveRealtime)
    }

    private func joinRealtime() {
        joinPredictions(tripId: tripId)
        vehicleFetcher.run(vehicleId: vehicleId)
    }

    private func leaveRealtime() {
        leavePredictions()
        vehicleFetcher.leave()
    }

    private func joinPredictions(tripId: String) {
        tripPredictionsFetcher.run(tripId: tripId)
    }

    private func leavePredictions() {
        tripPredictionsFetcher.leave()
    }

    @ViewBuilder
    var vehicleCardView: some View {
        let trip: Trip? = tripPredictionsFetcher.predictions?.trips[tripId]
        let vehicle: Vehicle? = vehicleFetcher.response?.vehicle
        let vehicleStop: Stop? = if let stopId = vehicle?.stopId {
            globalFetcher.stops[stopId]?.resolveParent(stops: globalFetcher.stops)
        } else {
            nil
        }
        let route: Route? = if let routeId = trip?.routeId {
            globalFetcher.routes[routeId]
        } else {
            nil
        }
        VehicleCardView(vehicle: vehicle,
                        route: route,
                        stop: vehicleStop,
                        trip: trip,
                        now: now.toNSDate())
    }
}
