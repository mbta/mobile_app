//
//  TripVehicleCard.swift
//  iosApp
//
//  Created by esimon on 12/3/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct TripVehicleCard: View {
    let vehicle: Vehicle
    let route: Route
    let stop: Stop
    let tripId: String

    var routeColor: Color { Color(hex: route.color) }
    var routeTextColor: Color { Color(hex: route.textColor) }

    var body: some View {
        ZStack(alignment: .leading) {
            VStack(spacing: 0) {
                // Use a clear rectangle as a spacer, the Spacer() view doesn't
                // take up enough space, this is always exactly half
                RouteLine(Color.clear)
                RouteLine(routeColor)
            }.padding(.leading, 46)
            HStack(spacing: 8) {
                vehiclePuck
                description
                Spacer()
                liveIndicator
            }
            .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
            .padding(.vertical, 12)
            .padding(.leading, 30)
            .padding(.trailing, 16)
        }
        .background(Color.fill3)
        .foregroundStyle(Color.text)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .padding(1)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 2))
        .padding([.horizontal], 6)
        .fixedSize(horizontal: false, vertical: true)
        .dynamicTypeSize(...DynamicTypeSize.accessibility3)
    }

    @ViewBuilder
    private var description: some View {
        if vehicle.tripId == tripId {
            VStack(alignment: .leading, spacing: 2) {
                vehicleStatusDescription(vehicle.currentStatus)
                    .font(Typography.footnote)
                Text(stop.name)
                    .font(Typography.headlineBold)
            }
            .accessibilityElement()
            .accessibilityAddTraits(.isHeader)
            .accessibilityHeading(.h2)
            .accessibilityLabel(Text(
                "\(route.type.typeText(isOnly: true)) \(vehicleStatusText(vehicle.currentStatus)) \(stop.name)",
                comment: """
                VoiceOver text for the vehicle status on the trip details page,
                ex '[train] [approaching] [Alewife]' or '[bus] [now at] [Harvard]'
                Possible values for the vehicle status are "Approaching", "Next stop", or "Now at"
                """
            ))
        } else {
            Text("This vehicle is completing another trip")
                .font(Typography.headlineBold)
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
        case .incomingAt: NSLocalizedString(
                "Approaching",
                comment: "Label for a vehicle's next stop. For example: Approaching Alewife"
            )
        case .inTransitTo: NSLocalizedString(
                "Next stop",
                comment: "Label for a vehicle's next stop. For example: Next stop Alewife"
            )
        case .stoppedAt: NSLocalizedString(
                "Now at",
                comment: "Label for a where a vehicle is currently stopped. For example: Now at Alewife"
            )
        }
    }

    private var vehiclePuck: some View {
        ZStack {
            Group {
                Image(.vehicleHalo)
                    .resizable()
                    .frame(width: 36, height: 36)
                    .foregroundStyle(Color.fill3)
                Image(.vehiclePuck)
                    .resizable()
                    .frame(width: 32, height: 32)
                    .foregroundStyle(routeColor)
            }
            .rotationEffect(.degrees(225))
            routeIcon(route)
                .resizable()
                .frame(width: 27, height: 27)
                .foregroundColor(routeTextColor)
        }
        .accessibilityHidden(true)
        .padding([.bottom], 6)
    }

    var liveIndicator: some View {
        HStack {
            Image(.liveData)
                .resizable()
                .frame(width: 16, height: 16)
            Text("Live", comment: "Indicates that data is being updated in real-time")
                .font(Typography.footnote)
        }
        .opacity(0.6)
        .accessibilityElement()
        .accessibilityAddTraits(.isHeader)
        .accessibilityLabel(Text(
            "Real-time arrivals updating live",
            comment: "VoiceOver label for real-time indicator icon"
        ))
    }
}

struct TripVehicleCard_Previews: PreviewProvider {
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
            TripVehicleCard(vehicle: vehicle, route: red, stop: stop, tripId: trip.id)
        }
        .previewDisplayName("VehicleCard")
    }
}
