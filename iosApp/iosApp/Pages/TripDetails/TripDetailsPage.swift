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
    let route: Route
    let stop: Stop
    let vehicle: Vehicle
    let trip: Trip

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
        let objects = ObjectCollectionBuilder()
        route = objects.route { route in
            route.id = "Red"
            route.longName = "Red Line"
            route.color = "DA291C"
            route.type = RouteType.heavyRail
            route.textColor = "FFFFFF"
        }
        vehicle = Vehicle(id: "y1234", bearing: nil,
                          currentStatus: __Bridge__Vehicle_CurrentStatus.inTransitTo,
                          directionId: 1,
                          latitude: 0.0,
                          longitude: 0.0,
                          updatedAt: ISO8601DateFormatter().date(from: "2024-05-15T07:04:15Z")!.toKotlinInstant(),
                          routeId: "66",
                          stopId: "place-davis",
                          tripId: "1234")
        trip = objects.trip { trip in
            trip.headsign = "Alewife"
        }

        stop = objects.stop { stop in
            stop.name = "Davis"
        }
    }

    var body: some View {
        VStack {
            VehicleCardView(vehicle: vehicle, route: route, stop: stop, trip: trip)
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
