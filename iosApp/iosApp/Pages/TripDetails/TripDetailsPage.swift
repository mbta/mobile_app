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
    var tripSchedulesRepository: ITripSchedulesRepository
    @State var tripSchedulesResponse: TripSchedulesResponse?

    @State var now = Date.now.toKotlinInstant()

    let inspection = Inspection<Self>()

    init(
        tripId: String,
        vehicleId: String,
        target: TripDetailsTarget?,
        globalFetcher: GlobalFetcher,
        tripPredictionsFetcher: TripPredictionsFetcher,
        tripSchedulesRepository: ITripSchedulesRepository = RepositoryDI().tripSchedules
    ) {
        self.tripId = tripId
        self.vehicleId = vehicleId
        self.target = target
        self.globalFetcher = globalFetcher
        self.tripPredictionsFetcher = tripPredictionsFetcher
        self.tripSchedulesRepository = tripSchedulesRepository
    }

    var body: some View {
        VStack {
            if let globalData = globalFetcher.response {
                if let stops = TripDetailsStopList.companion.fromPieces(
                    tripSchedules: tripSchedulesResponse,
                    tripPredictions: tripPredictionsFetcher.predictions,
                    globalData: globalData
                ) {
                    vehicleCardView
                    TripDetailsStopListView(stops: stops, now: now)
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
        .onAppear { joinPredictions(tripId: tripId) }
        .onChange(of: tripId) { joinPredictions(tripId: $0) }
        .onDisappear { leavePredictions() }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .withScenePhaseHandlers(onActive: { joinPredictions(tripId: tripId) },
                                onInactive: leavePredictions,
                                onBackground: leavePredictions)
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
        let vehicle: Vehicle? = tripPredictionsFetcher.predictions?.vehicles[vehicleId]
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
