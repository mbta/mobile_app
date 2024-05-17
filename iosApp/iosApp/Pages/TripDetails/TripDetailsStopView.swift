//
//  TripDetailsStopView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-17.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct TripDetailsStopView: View {
    let stop: TripDetailsStopList.Entry
    let now: Instant

    var body: some View {
        HStack {
            Text(stop.stop.name)
            Spacer()
            UpcomingTripView(prediction: .some(stop.format(now: now)))
        }
    }
}

#Preview {
    TripDetailsStopView(
        stop: .init(
            stop: ObjectCollectionBuilder.Single.shared.stop { $0.name = "ABC" },
            stopSequence: 10,
            schedule: nil,
            prediction: nil,
            vehicle: nil
        ),
        now: Date.now.toKotlinInstant()
    )
}
