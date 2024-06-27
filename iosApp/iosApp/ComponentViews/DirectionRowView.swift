//
//  DirectionRowView.swift
//  iosApp
//
//  Created by Simon, Emma on 6/27/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct DirectionRowView: View {
    var direction: Direction
    let predictions: RealtimePatterns.Format
    let routeType: RouteType
    let pillDecoration: PredictionRowView.PillDecoration

    init(
        direction: Direction,
        predictions: RealtimePatterns.Format,
        routeType: RouteType,
        pillDecoration: PredictionRowView.PillDecoration = .none
    ) {
        self.direction = direction
        self.predictions = predictions
        self.routeType = routeType
        self.pillDecoration = pillDecoration
    }

    var body: some View {
        PredictionRowView(predictions: predictions, routeType: routeType, pillDecoration: pillDecoration) {
            DirectionLabel(direction: direction)
                .foregroundStyle(Color.text)
        }
        .accessibilityInputLabels([direction.destination])
    }
}

struct DirectionRowView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(alignment: .trailing) {
            let now = Date.now
            let objects = ObjectCollectionBuilder()
            let trip1 = objects.trip { _ in }
            let prediction1 = objects.prediction { prediction in
                prediction.trip = trip1
                prediction.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
            }
            let trip2 = objects.trip { _ in }
            let prediction2 = objects.prediction { prediction in
                prediction.trip = trip2
                prediction.departureTime = now.addingTimeInterval(12 * 60).toKotlinInstant()
            }
            List {
                DirectionRowView(direction: Direction(name: "West", destination: "Some", id: 0),
                                 predictions: RealtimePatterns.FormatSome(trips: [
                                     .init(
                                         trip: .init(trip: trip1, prediction: prediction1),
                                         now: now.toKotlinInstant()
                                     ),
                                     .init(
                                         trip: .init(trip: trip2, prediction: prediction2),
                                         now: now.toKotlinInstant()
                                     ),
                                 ]),
                                 routeType: .heavyRail)
                DirectionRowView(direction: Direction(name: "North", destination: "None", id: 0),
                                 predictions: RealtimePatterns.FormatNone.shared,
                                 routeType: .heavyRail)
                DirectionRowView(direction: Direction(name: "South", destination: "Loading", id: 1),
                                 predictions: RealtimePatterns.FormatLoading.shared,
                                 routeType: .heavyRail)
                DirectionRowView(direction: Direction(name: "East", destination: "No Service", id: 1),
                                 predictions: RealtimePatterns.FormatNoService(
                                     alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                                         alert.effect = .suspension
                                     }
                                 ),
                                 routeType: .heavyRail)
            }
        }.font(Typography.body)
    }
}
