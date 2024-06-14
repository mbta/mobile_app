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
        VStack(alignment: .leading) {
            HStack {
                Text(stop.stop.name)
                Spacer()
                UpcomingTripView(prediction: .some(stop.format(now: now)), routeType: nil)
            }
            scrollRoutes
        }
    }

    var scrollRoutes: some View {
        let routeView = ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                ForEach(stop.routes, id: \.id) { route in
                    RoutePill(route: route)
                }
            }.padding(.horizontal, 20)
        }.padding(.horizontal, -20)
        if #available(iOS 16.4, *) {
            return routeView.scrollBounceBehavior(.basedOnSize, axes: [.horizontal])
        }
        return routeView
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    return TripDetailsStopView(
        stop: .init(
            stop: objects.stop { $0.name = "ABC" },
            stopSequence: 10,
            schedule: nil,
            prediction: nil,
            vehicle: nil,
            routes: [
                objects.route {
                    $0.longName = "Red Line"
                    $0.color = "#DA291C"
                    $0.textColor = "#ffffff"
                },
                objects.route {
                    $0.longName = "Green Line"
                    $0.color = "#008400"
                    $0.textColor = "#ffffff"
                },
            ]
        ),
        now: Date.now.toKotlinInstant()
    )
}
