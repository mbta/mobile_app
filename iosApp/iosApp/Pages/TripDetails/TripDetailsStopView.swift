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
                UpcomingTripView(prediction: .some(stop.format(now: now)))
            }
            scrollRoutes(stop.routes)
        }
    }

    func scrollRoutes(_ routes: [Route]) -> some View {
        let routeView = ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                ForEach(routes, id: \.id) { route in
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
    TripDetailsStopView(
        stop: .init(
            stop: ObjectCollectionBuilder.Single.shared.stop { $0.name = "ABC" },
            stopSequence: 10,
            schedule: nil,
            prediction: nil,
            vehicle: nil,
            routes: []
        ),
        now: Date.now.toKotlinInstant()
    )
}
