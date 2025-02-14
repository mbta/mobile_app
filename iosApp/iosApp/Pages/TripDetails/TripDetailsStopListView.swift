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
    let onTapLink: (SheetNavigationStackEntry, TripDetailsStopList.Entry, String?) -> Void
    let routeType: RouteType?

    var body: some View {
        List(stops.stops, id: \.stopSequence) { stop in
            TripDetailsStopView(stop: stop, now: now, onTapLink: onTapLink, routeType: routeType)
        }.listStyle(.plain)
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
        stops: .init(tripId: "", stops: [
            .init(
                stop: stop1,
                stopSequence: 1,
                disruption: nil,
                schedule: nil,
                prediction: nil,
                predictionStop: nil,
                vehicle: nil,
                routes: []
            ),
            .init(
                stop: stop2,
                stopSequence: 2,
                disruption: nil,
                schedule: sched2,
                prediction: pred2,
                predictionStop: stop2,
                vehicle: nil,
                routes: []
            ),
        ]),
        now: Date.now.toKotlinInstant(),
        onTapLink: { _, _, _ in },
        routeType: nil
    ).font(Typography.body)
}
