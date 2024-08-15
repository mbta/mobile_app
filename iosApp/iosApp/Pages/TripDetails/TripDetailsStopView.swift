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
    let onTapLink: (SheetNavigationStackEntry, TripDetailsStopList.Entry, String?) -> Void

    var body: some View {
        VStack(alignment: .leading) {
            SheetNavigationLink(
                value: .stopDetails(stop.stop, nil),
                action: { entry in onTapLink(entry, stop, nil) },
                label: {
                    HStack {
                        Text(stop.stop.name).foregroundStyle(Color.text)
                        Spacer()
                        UpcomingTripView(prediction: upcomingTripViewState, routeType: nil)
                    }
                }
            )
            scrollRoutes
        }
    }

    var upcomingTripViewState: UpcomingTripView.State {
        if let alert = stop.alert {
            .noService(alert.effect)
        } else {
            .some(stop.format(now: now))
        }
    }

    var scrollRoutes: some View {
        let routeView = ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                ForEach(stop.routes, id: \.id) { route in
                    RoutePill(route: route, line: nil, type: .flex)
                        .onTapGesture {
                            onTapLink(.stopDetails(stop.stop, nil), stop, route.id)
                        }
                }
            }.padding(.horizontal, 20)
        }.padding(.horizontal, -20).onTapGesture {
            onTapLink(.stopDetails(stop.stop, nil), stop, nil)
        }
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
            alert: nil,
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
                    $0.color = "#00843D"
                    $0.textColor = "#ffffff"
                },
            ]
        ),
        now: Date.now.toKotlinInstant(),
        onTapLink: { _, _, _ in }
    ).font(Typography.body)
}
