//
//  TripDetailsStopListSplitView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-17.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct TripDetailsStopListSplitView: View {
    let splitStops: TripDetailsStopList.TargetSplit
    let now: Instant

    var body: some View {
        List {
            if !splitStops.collapsedStops.isEmpty {
                DisclosureGroup(LocalizedStringKey("\(splitStops.collapsedStops.count, specifier: "%ld") stops")) {
                    ForEach(splitStops.collapsedStops, id: \.stopSequence) { stop in
                        TripDetailsStopView(stop: stop, now: now)
                    }
                }
            }
            TripDetailsStopView(stop: splitStops.targetStop, now: now)
                .listRowBackground(Color.fill1)
            ForEach(splitStops.followingStops, id: \.stopSequence) { stop in
                TripDetailsStopView(stop: stop, now: now)
            }
        }
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    let stop1 = objects.stop { $0.name = "A" }
    let stop2 = objects.stop { $0.name = "B" }
    let stop3 = objects.stop { $0.name = "C" }

    let pred1 = objects.prediction { $0.departureTime = Date.now.toKotlinInstant() }
    let pred2 = objects.prediction { $0.departureTime = Date.now.addingTimeInterval(60).toKotlinInstant() }
    let pred3 = objects.prediction { $0.departureTime = Date.now.addingTimeInterval(2 * 60).toKotlinInstant() }

    func entry(_ stop: Stop, _ stopSequence: Int, _ prediction: Prediction) -> TripDetailsStopList.Entry {
        TripDetailsStopList.Entry(
            stop: stop,
            stopSequence: Int32(stopSequence),
            schedule: nil,
            prediction: prediction,
            vehicle: nil
        )
    }

    return TripDetailsStopListSplitView(
        splitStops: .init(
            collapsedStops: [entry(stop1, 10, pred1)],
            targetStop: entry(stop2, 20, pred2),
            followingStops: [entry(stop3, 30, pred3)]
        ),
        now: Date.now.toKotlinInstant()
    )
}