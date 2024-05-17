//
//  TripDetailsStopListView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-15.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct TripDetailsStopListView: View {
    let stops: TripDetailsStopList
    let now: Instant

    var body: some View {
        List(stops.stops, id: \.stopSequence) { stop in
            HStack {
                Text(stop.stop.name)
                Spacer()
                UpcomingTripView(prediction: .some(stop.format(now: now)))
            }
        }
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()

    let stop1 = objects.stop { stop in
        stop.name = "Stop 1"
    }
    let stop2 = objects.stop { stop in
        stop.name = "Stop 2"
    }
    let sched2 = objects.schedule { schedule in
        schedule.stopId = stop2.id
        schedule.stopSequence = 2
        schedule.departureTime = Date.now.toKotlinInstant()
    }
    let pred2 = objects.prediction(schedule: sched2) { prediction in
        prediction.departureTime = Date.now.addingTimeInterval(5 * 60).toKotlinInstant()
    }

    return TripDetailsStopListView(
        stops: .init(stops: [
            .init(stop: stop1, stopSequence: 1, schedule: nil, prediction: nil, vehicle: nil),
            .init(stop: stop2, stopSequence: 2, schedule: sched2, prediction: pred2, vehicle: nil),
        ]),
        now: Date.now.toKotlinInstant()
    )
}
