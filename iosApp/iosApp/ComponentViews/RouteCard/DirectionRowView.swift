//
//  DirectionRowView.swift
//  iosApp
//
//  Created by Simon, Emma on 6/27/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct DirectionRowView: View {
    var direction: Direction
    let predictions: UpcomingFormat
    let pillDecoration: PredictionRowView.PillDecoration

    init(
        direction: Direction,
        predictions: UpcomingFormat,
        pillDecoration: PredictionRowView.PillDecoration = .none
    ) {
        self.direction = direction
        self.predictions = predictions
        self.pillDecoration = pillDecoration
    }

    var body: some View {
        PredictionRowView(predictions: predictions, pillDecoration: pillDecoration) {
            DirectionLabel(direction: direction, pillDecoration: pillDecoration)
                .foregroundStyle(Color.text)
        }
    }
}

struct DirectionRowView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(alignment: .trailing) {
            let now = EasternTimeInstant.now()
            let objects = ObjectCollectionBuilder()
            let trip1 = objects.trip { _ in }
            let prediction1 = objects.prediction { prediction in
                prediction.trip = trip1
                prediction.departureTime = now.plus(minutes: 5)
            }
            let trip2 = objects.trip { _ in }
            let prediction2 = objects.prediction { prediction in
                prediction.trip = trip2
                prediction.departureTime = now.plus(minutes: 12)
            }
            List {
                DirectionRowView(
                    direction: Direction(name: "West", destination: "Some", id: 0),
                    predictions: UpcomingFormat.Some(trips: [
                        .init(
                            trip: .init(trip: trip1, prediction: prediction1),
                            routeType: RouteType.heavyRail,
                            now: now, context: .nearbyTransit,
                            lastTrip: false,
                        ),
                        .init(
                            trip: .init(trip: trip2, prediction: prediction2),
                            routeType: RouteType.heavyRail,
                            now: now, context: .nearbyTransit,
                            lastTrip: false,
                        ),
                    ], secondaryAlert: nil)
                )
                DirectionRowView(
                    direction: Direction(name: "North", destination: "None", id: 0),
                    predictions: UpcomingFormat.NoTrips(
                        noTripsFormat: UpcomingFormat.NoTripsFormatPredictionsUnavailable()
                    )
                )
                DirectionRowView(
                    direction: Direction(name: "South", destination: "Loading", id: 1),
                    predictions: UpcomingFormat.Loading.shared
                )
                DirectionRowView(
                    direction: Direction(name: "East", destination: "No Service", id: 1),
                    predictions: UpcomingFormat.Disruption(
                        alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                            alert.effect = .suspension
                        }, mapStopRoute: .green
                    )
                )
            }
        }.font(Typography.body)
    }
}
