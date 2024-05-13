//
//  VehicleCardView.swift
//  iosApp
//
//  Created by Brady, Kayla on 5/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct VehicleCardView: View {
    let vehicle: Vehicle
    let route: Route
    let stop: Stop
    let trip: Trip
    var now: Date = .now

    var body: some View {
        if vehicle.tripId == trip.id {
            vehicleOnTripView
        } else {
            Text("This vehicle is completing another trip.")
        }
    }

    @ViewBuilder
    func vehicleStatusDescription(_ vehicleStatus: __Bridge__Vehicle_CurrentStatus) -> some View {
        switch vehicleStatus {
        case __Bridge__Vehicle_CurrentStatus.incomingAt: Text("Approaching")
        case __Bridge__Vehicle_CurrentStatus.inTransitTo: Text("Next stop")
        case __Bridge__Vehicle_CurrentStatus.stoppedAt: Text("Now at")
        }
    }

    @ViewBuilder
    var vehicleOnTripView: some View {
        VStack {
            HStack {
                VStack {
                    RoutePill(route: route)
                    routeIconView(route)
                }.padding([.trailing], 8)
                VStack {
                    HStack {
                        Text(trip.headsign).padding([.bottom], 8)
                        Spacer()
                        Text("Live")
                        Image(.liveData)
                    }

                    VStack(alignment: .leading) {
                        vehicleStatusDescription(vehicle.currentStatus)
                            .font(.caption)
                        Text(stop.name).bold()
                    }.frame(maxWidth: .infinity, alignment: .leading)
                }
            }.frame(maxWidth: .infinity, alignment: .leading)

            HStack {
                Text("last updated \(lastUpdatedSeconds(), specifier: "%.0f")s ago")
                    .font(.caption2)

            }.frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func lastUpdatedSeconds() -> TimeInterval {
        now.timeIntervalSince(vehicle.updatedAt.toNSDate())
    }
}

struct VehicleCardView_Previews: PreviewProvider {
    static var previews: some View {
        let objects = ObjectCollectionBuilder()
        let red = objects.route { route in
            route.id = "Red"
            route.longName = "Red Line"
            route.color = "DA291C"
            route.type = RouteType.heavyRail
            route.textColor = "FFFFFF"
        }
        let trip = objects.trip { trip in
            trip.id = "1234"
            trip.headsign = "Alewife"
        }
        let vehicle = Vehicle(id: "y1234", bearing: nil,
                              currentStatus: __Bridge__Vehicle_CurrentStatus.inTransitTo,
                              directionId: 1,
                              latitude: 0.0,
                              longitude: 0.0,
                              updatedAt: Date.now.addingTimeInterval(-10).toKotlinInstant(),
                              routeId: "66",
                              stopId: "place-davis",
                              tripId: trip.id)

        let stop = objects.stop { stop in
            stop.name = "Davis"
        }

        List {
            VehicleCardView(vehicle: vehicle, route: red, stop: stop, trip: trip)
        }.previewDisplayName("VehicleCard")
    }
}
