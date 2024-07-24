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
    var onBack: () -> Void
    let route: Route?
    let line: Line?
    let trip: Trip?

    var body: some View {
        HStack {
            ActionButton(kind: .back, action: onBack)
            Spacer().frame(width: 16)
            if let trip, let route {
                RoutePill(route: route, line: line, type: .fixed)
                Spacer().frame(width: 8)
                toHeadsign(trip.headsign)
                Spacer()
                liveIndicator
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
    }

    var liveIndicator: some View {
        HStack {
            Image(.liveData)
                .resizable()
                .frame(width: 16, height: 16)
            Text("Live")
                .font(Typography.footnote)
        }
    }

    func toHeadsign(_ headsign: String) -> some View {
        Text("to \(headsign)")
            .font(Typography.headlineBold)
            .accessibilityHeading(.h1)
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
            route.longName = "Red Line"
        }
        let longTripName = objects.trip { trip in
            trip.id = "target"
            trip.headsign = "This Is A Really Long Trip Name"
        }
        VStack {
            TripDetailsHeader(onBack: {}, route: route, line: nil, trip: trip)
            TripDetailsHeader(onBack: {}, route: route, line: nil, trip: longTripName)
        }
    }
}
