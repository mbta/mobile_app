//
//  HeadsignRowView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct HeadsignRowView: View {
    var headsign: String
    let predictions: RealtimePatterns.Format
    let pillDecoration: PredictionRowView.PillDecoration

    init(
        headsign: String,
        predictions: RealtimePatterns.Format,
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
        .accessibilityInputLabels([headsign])
    }
}

struct HeadsignRowView_Previews: PreviewProvider {
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
                HeadsignRowView(headsign: "Some",
                                predictions: RealtimePatterns.FormatSome(trips: [
                                    .init(
                                        trip: .init(trip: trip1, prediction: prediction1),
                                        routeType: RouteType.lightRail,
                                        now: now.toKotlinInstant(), context: .nearbyTransit
                                    ),
                                    .init(
                                        trip: .init(trip: trip2, prediction: prediction2),
                                        routeType: .heavyRail,
                                        now: now.toKotlinInstant(), context: .nearbyTransit
                                    ),
                                ]))
                HeadsignRowView(headsign: "None",
                                predictions: RealtimePatterns.FormatNone.shared)
                HeadsignRowView(headsign: "Loading",
                                predictions: RealtimePatterns.FormatLoading.shared)
                HeadsignRowView(headsign: "No Service",
                                predictions: RealtimePatterns.FormatNoService(
                                    alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                                        alert.effect = .suspension
                                    }
                                ))
            }
        }.font(Typography.body)
    }
}
