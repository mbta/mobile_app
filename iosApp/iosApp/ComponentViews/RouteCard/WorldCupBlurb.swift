//
//  WorldCupBlurb.swift
//  iosApp
//
//  Created by Melody Horn on 4/3/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct WorldCupBlurb: View {
    @ObserveInjection var inject
    let leaf: RouteCardData.Leaf
    let routeAccents: TripRouteAccents
    let offerDetails: Bool

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                Image(.soccerBallIcon)
                VStack(alignment: .leading, spacing: 6) {
                    if leaf.direction.id == 0 {
                        Text(AttributedString.tryMarkdown(NSLocalizedString(
                            "Service from **South Station** to today’s **World Cup match**",
                            comment: ""
                        )))
                        .font(Typography.body)
                    } else {
                        Text(AttributedString.tryMarkdown(NSLocalizedString(
                            "Service from today’s **World Cup match** to **South Station**",
                            comment: ""
                        )))
                        .font(Typography.body)
                    }
                    Text("Boston Stadium Train ticket required", comment: "")
                        .font(Typography.footnote)
                        .opacity(0.6)
                }
                .multilineTextAlignment(.leading)
                .foregroundStyle(Color.text)
                Spacer()
            }
            if offerDetails {
                Link(destination: .init(string: WorldCupService.shared.scheduleUrl)!) {
                    Text("View details", comment: "Button that shows more informaton about an alert")
                        .frame(maxWidth: .infinity)
                }
                .foregroundStyle(routeAccents.textColor)
                .font(Typography.bodySemibold)
                .padding(10)
                .frame(minHeight: 44)
                .background(routeAccents.color)
                .clipShape(.rect(cornerRadius: 8.0))
                .preventScrollTaps()
            }
        }
        .enableInjection()
    }
}

#Preview("Nearby") {
    let objects = TestData.clone(namespace: "WorldCupBlurbPreview")
    let route = WorldCupService.shared.route
    objects.put(object: route)
    let stop = objects.getStop(id: "place-sstat")
    let global = GlobalResponse(objects: objects)
    let data = RouteCardData(lineOrRoute: .route(route), stopData: [.init(route: route, stop: stop, data: [
        .init(
            lineOrRoute: .route(route),
            stop: stop,
            direction: .init(directionId: 0, route: route),
            routePatterns: [],
            stopIds: [],
            upcomingTrips: [],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: nil,
            alertsDownstream: [],
            context: .nearbyTransit
        ),
    ])], at: .now())
    return RouteCard(
        cardData: data,
        global: global,
        now: .now(),
        isFavorite: { _ in false },
        pushNavEntry: { _ in },
        showStopHeader: false
    )
    .padding(16)
}

#Preview("Stop Details") {
    let objects = TestData.clone(namespace: "WorldCupBlurbPreview")
    let route = WorldCupService.shared.route
    objects.put(object: route)
    let stop = objects.getStop(id: "place-sstat")
    let global = GlobalResponse(objects: objects)
    let data = RouteCardData(lineOrRoute: .route(route), stopData: [.init(route: route, stop: stop, data: [
        .init(
            lineOrRoute: .route(route),
            stop: stop,
            direction: .init(directionId: 0, route: route),
            routePatterns: [],
            stopIds: [],
            upcomingTrips: [],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: nil,
            alertsDownstream: [],
            context: .nearbyTransit
        ),
    ])], at: .now())
    startKoinIOSTestApp()
    let leaf = data.stopData.first!.data.first!
    return StopDetailsFilteredDepartureDetails(
        stopId: stop.id,
        stopFilter: .init(routeId: route.id, directionId: 0),
        tripFilter: nil,
        setStopFilter: { _ in },
        setTripFilter: { _ in },
        leaf: leaf,
        selectedDirection: leaf.direction,
        favorite: false,
        now: .now(),
        errorBannerVM: MockErrorBannerViewModel(),
        mapVM: MockMapViewModel(),
        stopDetailsVM: MockStopDetailsViewModel(),
        schedulesRepository: MockScheduleRepository(),
        navManager: NavigationManager()
    )
    .padding(16)
    .withFixedSettings([:])
}
