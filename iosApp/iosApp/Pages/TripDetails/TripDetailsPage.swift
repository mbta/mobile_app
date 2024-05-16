//
//  TripDetailsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct TripDetailsPage: View {
    let tripId: String
    let vehicleId: String
    let target: TripDetailsTarget?

    @ObservedObject var globalFetcher: GlobalFetcher
    var tripSchedulesRepository: ITripSchedulesRepository
    @State var tripSchedulesResponse: TripSchedulesResponse?

    let inspection = Inspection<Self>()

    init(
        tripId: String,
        vehicleId: String,
        target: TripDetailsTarget?,
        globalFetcher: GlobalFetcher,
        tripSchedulesRepository: ITripSchedulesRepository = RepositoryDI().tripSchedules
    ) {
        self.tripId = tripId
        self.vehicleId = vehicleId
        self.target = target
        self.globalFetcher = globalFetcher
        self.tripSchedulesRepository = tripSchedulesRepository
    }

    var body: some View {
        VStack {
            Text("Trip \(tripId)")
            Text("Vehicle \(vehicleId)")
            if let target {
                Text("Target Stop \(target.stopId)")
                Text("Target Stop Sequence \(target.stopSequence)")
            }

            if let globalData = globalFetcher.response, let tripSchedulesResponse {
                if let stops = tripSchedulesResponse.stops(globalData: globalData) {
                    List(stops, id: \.id) {
                        Text($0.name)
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
                tripSchedulesResponse = try await tripSchedulesRepository.getTripSchedules(tripId: tripId)
            } catch {
                debugPrint(error)
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }
}

#Preview {
    TripDetailsPage(
        tripId: "1",
        vehicleId: "a",
        target: .init(stopId: "place-a", stopSequence: 9),
        globalFetcher: GlobalFetcher(backend: IdleBackend()),
        tripSchedulesRepository: IdleTripSchedulesRepository()
    )
}
