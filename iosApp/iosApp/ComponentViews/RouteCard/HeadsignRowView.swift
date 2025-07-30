//
//  HeadsignRowView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct HeadsignRowView: View {
    var headsign: String
    let predictions: UpcomingFormat
    let pillDecoration: PredictionRowView.PillDecoration

    init(
        headsign: String,
        predictions: UpcomingFormat,
        pillDecoration: PredictionRowView.PillDecoration = .none
    ) {
        self.headsign = headsign
        self.predictions = predictions
        self.pillDecoration = pillDecoration
    }

    var body: some View {
        PredictionRowView(predictions: predictions, pillDecoration: pillDecoration) {
            Text(headsign)
                .foregroundStyle(Color.text)
                .font(Typography.bodySemibold)
                .multilineTextAlignment(.leading)
        }
    }
}

struct HeadsignRowView_Previews: PreviewProvider {
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
            let secondaryAlert = UpcomingFormat.SecondaryAlert(
                iconName: "alert-large-bus-issue"
            )
            let greenLineEAlert = UpcomingFormat.SecondaryAlert(
                iconName: "alert-large-green-issue"
            )
            let greenLineERoute = objects.route { route in
                route.id = "Green-E"
                route.shortName = "E"
                route.color = "00843D"
                route.type = .lightRail
                route.textColor = "FFFFFF"
                route.longName = "Green Line E"
            }

            List {
                HeadsignRowView(
                    headsign: "Some",
                    predictions: UpcomingFormat.Some(trips: [
                        .init(
                            trip: .init(trip: trip1, prediction: prediction1),
                            routeType: RouteType.lightRail,
                            now: now, context: .nearbyTransit
                        ),
                        .init(
                            trip: .init(trip: trip2, prediction: prediction2),
                            routeType: .heavyRail,
                            now: now, context: .nearbyTransit
                        ),
                    ], secondaryAlert: nil)
                )
                HeadsignRowView(
                    headsign: "Some with Alert",
                    predictions: UpcomingFormat.Some(trips: [
                        .init(
                            trip: .init(trip: trip1, prediction: prediction1),
                            routeType: RouteType.lightRail,
                            now: now, context: .nearbyTransit
                        ),
                        .init(
                            trip: .init(trip: trip2, prediction: prediction2),
                            routeType: .heavyRail,
                            now: now, context: .nearbyTransit
                        ),
                    ], secondaryAlert: secondaryAlert)
                )
                HeadsignRowView(
                    headsign: "None",
                    predictions: UpcomingFormat.NoTrips(
                        noTripsFormat: UpcomingFormat.NoTripsFormatPredictionsUnavailable()
                    )
                )
                HeadsignRowView(
                    headsign: "None with Alert",
                    predictions: UpcomingFormat.NoTrips(
                        noTripsFormat: UpcomingFormat.NoTripsFormatPredictionsUnavailable(),
                        secondaryAlert: secondaryAlert
                    )
                )
                HeadsignRowView(
                    headsign: "Decorated None with Alert",
                    predictions: UpcomingFormat.NoTrips(
                        noTripsFormat: UpcomingFormat.NoTripsFormatPredictionsUnavailable(),
                        secondaryAlert: greenLineEAlert
                    ),
                    pillDecoration: .onRow(route: greenLineERoute)
                )
                HeadsignRowView(
                    headsign: "Loading",
                    predictions: UpcomingFormat.Loading.shared
                )
                HeadsignRowView(
                    headsign: "No Service",
                    predictions: UpcomingFormat.Disruption(
                        alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                            alert.effect = .suspension
                        }, mapStopRoute: .orange
                    )
                )
            }
        }.font(Typography.body)
    }
}
