//
//  TripDetailsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct TripDetailsPage: View {
    let tripId: String
    let vehicleId: String
    let target: TripDetailsTarget?

    var body: some View {
        VStack {
            Text("Trip \(tripId)")
            Text("Vehicle \(vehicleId)")
            if let target {
                Text("Target Stop \(target.stopId)")
                Text("Target Stop Sequence \(target.stopSequence)")
            }
        }
    }
}

#Preview {
    TripDetailsPage(tripId: "1", vehicleId: "a", target: .init(stopId: "place-a", stopSequence: 9))
}
