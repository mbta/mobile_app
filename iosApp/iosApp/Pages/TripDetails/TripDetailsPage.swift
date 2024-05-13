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
                    List(stops.stops, id: \.stopSequence) { stop in
                        HStack {
                            Text(stop.stop.name)
                            Spacer()
                            UpcomingTripView(prediction: .some(stop.format(now: now)))
                        }
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
                    try await Task.sleep(for: .seconds(5))
                } catch {
                    debugPrint("Can't sleep", error)
                }
                now = Date.now.toKotlinInstant()
            }
        }
        .onAppear {
            tripPredictionsFetcher.run(tripId: tripId)
        }
        .onChange(of: tripId) { tripId in
            tripPredictionsFetcher.leave()
            tripPredictionsFetcher.run(tripId: tripId)
        }
        .onDisappear {
            tripPredictionsFetcher.leave()
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }
}
