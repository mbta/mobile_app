//
//  TripDetailsHeader.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct TripDetailsHeader: View {
    let route: Route?
    let line: Line?
    let trip: Trip?
    var onBack: () -> Void = {}
    var onClose: () -> Void = {}

    var body: some View {
        HStack(alignment: .center, spacing: 16) {
            ActionButton(kind: .back, action: onBack)
            if let trip, let route {
                HStack(alignment: .center, spacing: 8) {
                    RoutePill(route: route, line: line, type: .fixed)
                    toHeadsign(trip.headsign)
                }
                .accessibilityElement()
                .accessibilityAddTraits(.isHeader)
                .accessibilityHeading(.h1)
                .accessibilityLabel(Text(
                    "\(route.label) \(route.type.typeText(isOnly: true)) to \(trip.headsign)",
                    comment: """
                    VoiceOver text for the trip details page header,
                    describes the route and destination of the vehicle,
                    ex 'Red Line train to Alewife'
                    """
                ))
            }
            Spacer()
            ActionButton(kind: .close, action: onClose)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }

    var liveIndicator: some View {
        HStack {
            Image(.liveData)
                .resizable()
                .frame(width: 16, height: 16)
            Text("Live", comment: "Indicates that data is being updated in real-time")
                .font(Typography.footnote)
        }
        .accessibilityElement()
        .accessibilityAddTraits(.isHeader)
        .accessibilityLabel(Text(
            "Real-time arrivals updating live",
            comment: "VoiceOver label for real-time indicator icon"
        ))
    }

    func toHeadsign(_ headsign: String) -> some View {
        Text("to \(headsign)", comment: """
        Label for the destination of a vehicle.
        For example "[Red Line] to Alewife"
        """)
        .font(Typography.headlineBold)
    }
}

struct TripDetailsHeader_Previews: PreviewProvider {
    static var previews: some View {
        let objects = ObjectCollectionBuilder()
        let trip = objects.trip { trip in
            trip.id = "target"
            trip.headsign = "Alewife"
        }
        let route = objects.route { route in
            route.type = .heavyRail
            route.color = "DA291C"
            route.longName = "Red Line"
            route.textColor = "FFFFFF"
        }
        let longestTripNameRoute = objects.route { route in
            route.type = .bus
            route.color = "FFC72C"
            route.shortName = "441"
            route.textColor = "000000"
        }
        let longestTripName = objects.trip { trip in
            trip.id = "target"
            trip.headsign = "Marblehead via Central Square & Paradise Rd (omits Point of Pines)"
        }
        let crRoute = objects.route { route in
            route.type = .commuterRail
            route.color = "80276C"
            route.longName = "Providence/Stoughton Line"
            route.textColor = "FFFFFF"
        }
        let crTrip = objects.trip { trip in
            trip.headsign = "Providence"
        }
        VStack {
            TripDetailsHeader(route: route, line: nil, trip: trip)
            TripDetailsHeader(route: longestTripNameRoute, line: nil, trip: longestTripName)
            TripDetailsHeader(route: crRoute, line: nil, trip: crTrip)
        }
    }
}
