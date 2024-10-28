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
    let vehicle: Vehicle?
    let route: Route?
    let line: Line?
    let stop: Stop?
    let trip: Trip?

    var body: some View {
        if let vehicle, let route, let stop, let trip {
            VehicleOnTripView(
                vehicle: vehicle,
                route: route,
                line: line,
                stop: stop,
                trip: trip
            )
        } else {
            Text("Loading...")
        }
    }
}

struct VehicleOnTripView: View {
    let vehicle: Vehicle
    let route: Route
    let line: Line?
    let stop: Stop
    let trip: Trip

    var backgroundColor: Color {
        if route.id.starts(with: "Shuttle"), let line {
            Color(hex: line.color)
        } else {
            Color(hex: route.color)
        }
    }

    var textColor: Color {
        if route.id.starts(with: "Shuttle"), let line {
            Color(hex: line.textColor)
        } else {
            Color(hex: route.textColor)
        }
    }

    var body: some View {
        HStack {
            routeVehicle
                .padding([.leading], 8)
            description
        }
        .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
        .background(backgroundColor)
        .withRoundedBorder(width: 2)
        .padding([.horizontal], 8)
    }

    @ViewBuilder
    private var description: some View {
        if vehicle.tripId == trip.id {
            VStack(alignment: .leading, spacing: 2) {
                vehicleStatusDescription(vehicle.currentStatus)
                    .font(Typography.caption)
                    .foregroundColor(textColor)
                Text(stop.name)
                    .font(Typography.headlineBold)
                    .foregroundColor(textColor)
            }
            .accessibilityElement()
            .accessibilityAddTraits(.isHeader)
            .accessibilityHeading(.h2)
            .accessibilityLabel(
                "\(route.type.typeText(isOnly: true)) \(vehicleStatusText(vehicle.currentStatus)) \(stop.name)"
            )
        } else {
            Text("This vehicle is completing another trip.")
                .font(Typography.headlineBold)
                .foregroundColor(textColor)
                .accessibilityAddTraits(.isHeader)
                .accessibilityHeading(.h2)
        }
    }

    @ViewBuilder
    private func vehicleStatusDescription(
        _ vehicleStatus: __Bridge__Vehicle_CurrentStatus
    ) -> some View {
        Text(vehicleStatusText(vehicleStatus))
    }

    private func vehicleStatusText(
        _ vehicleStatus: __Bridge__Vehicle_CurrentStatus
    ) -> String {
        switch vehicleStatus {
        case .incomingAt: NSLocalizedString("Approaching",
                                            comment: "Label for a vehicle's next stop. For example: Approaching Alewife")
        case .inTransitTo: NSLocalizedString("Next stop",
                                             comment: "Label for a vehicle's next stop. For example: Next stop Alewife")
        case .stoppedAt: NSLocalizedString("Now at",
                                           comment: "Label for a where a vehicle is currently stopped. For example: Now at Alewife")
        }
    }

    private var routeVehicle: some View {
        ZStack {
            Group {
                Image(.vehicleHalo)
                Image(.vehiclePuck).foregroundStyle(Color(hex: route.color))
            }
            .frame(width: 28, height: 28)
            .rotationEffect(.degrees(225))
            routeIcon(route)
                .resizable()
                .frame(width: 24, height: 24)
                .foregroundColor(textColor)
        }
        .accessibilityHidden(true)
        .padding([.bottom], 4)
        .frame(width: 56, height: 56)
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
                              currentStopSequence: 30,
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
            VehicleCardView(vehicle: vehicle, route: red, line: nil, stop: stop, trip: trip)
        }
        .previewDisplayName("VehicleCard")
    }
}
