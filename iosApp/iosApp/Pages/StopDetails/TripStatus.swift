//
//  TripStatus.swift
//  iosApp
//
//  Created by esimon on 11/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import Shared
import SwiftUI

struct TripStatus: View {
    @ObserveInjection var inject

    let predictions: UpcomingFormat
    @State private var now = EasternTimeInstant.now()
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    func debugCountdown(_ time: EasternTimeInstant) -> String {
        Duration(
            secondsComponent: time.minus(other: now).inWholeSeconds,
            attosecondsComponent: 0
        )
        .formatted(.units(allowed: [.minutes, .seconds], width: .narrow))
    }

    var body: some View {
        switch onEnum(of: predictions) {
        case let .some(trips):
            if let trip = trips.trips.first {
                VStack(spacing: 0) {
                    UpcomingTripView(
                        prediction: .some(trip.format),
                        routeType: trip.routeType,
                        hideDisruptionIcon: true
                    ).enableInjection()
                    if let time = trip.debugTime {
                        DebugView(withSocketInfo: false, content: {
                            Text(debugCountdown(time))
                                .onReceive(timer) { input in now = input.toEasternInstant() }
                        })
                    }
                }
            }
        case let .disruption(alert):
            UpcomingTripView(prediction: .disruption(.init(alert: alert.alert), iconName: alert.iconName))
                .enableInjection()
        case let .noTrips(format):
            UpcomingTripView(prediction: .noTrips(format.noTripsFormat)).enableInjection()
        case .loading:
            UpcomingTripView(prediction: .loading).enableInjection()
        }
    }
}
