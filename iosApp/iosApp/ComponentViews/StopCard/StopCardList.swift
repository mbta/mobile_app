//
//  StopCardList.swift
//  iosApp
//
//  Created by Melody Horn on 6/25/26.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopCardList<EmptyView: View>: View {
    @ObserveInjection var inject

    let stopCardData: [StopCardData]?
    @ViewBuilder let emptyView: () -> EmptyView
    let global: GlobalResponse?
    let now: Date
    let isFavorite: (RouteStopDirection) -> Bool
    let pushNavEntry: (SheetNavigationStackEntry) -> Void

    var body: some View {
        if let stopCardData, !stopCardData.isEmpty {
            HaloScrollView {
                LazyVStack(alignment: .center, spacing: 18) {
                    ForEach(stopCardData, id: \.stop.id) { stopCardData in
                        StopCard(
                            cardData: stopCardData,
                            global: global,
                            now: now.toEasternInstant(),
                            isFavorite: isFavorite,
                            pushNavEntry: pushNavEntry
                        )
                    }
                }
                .padding(.vertical, 4)
                .padding(.horizontal, 16)
            }
            .enableInjection()
        } else if let stopCardData, stopCardData.isEmpty {
            HaloScrollView {
                VStack {
                    emptyView()
                    Spacer()
                }
                .padding(.horizontal, 16)
            }
            .enableInjection()
        } else {
            ScrollView([]) {
                LazyVStack(alignment: .center, spacing: 14) {
                    ForEach(0 ..< 5) { _ in
                        LoadingStopCard()
                    }
                }
                .padding(.vertical, 4)
                .padding(.horizontal, 16)
                .loadingPlaceholder(withShimmer: false)
            }
            .enableInjection()
        }
    }
}

#Preview {
    let now = EasternTimeInstant.now()
    let objects = TestData.clone(namespace: "StopCardListPreview")

    let ruggles = objects.getStop(id: "place-rugg")
    let tremontAtMelneaCass = objects.stop { stop in
        stop.id = "1227"
        stop.locationType = .stop
        stop.name = "Tremont St @ Melnea Cass Blvd"
        stop.wheelchairBoarding = .accessible
    }
    let boylston = objects.getStop(id: "place-boyls")
    let ol = LineOrRoute.route(objects.getRoute(id: "Orange"))
    let olSouthboundPattern = objects.getRoutePattern(id: "Orange-3-0")
    let olNorthboundPattern = objects.getRoutePattern(id: "Orange-3-1")
    let gl = LineOrRoute.line(
        objects.getLine(id: "line-Green"),
        [
            objects.getRoute(id: "Green-B"),
            objects.getRoute(id: "Green-C"),
            objects.getRoute(id: "Green-D"),
            objects.getRoute(id: "Green-E")
        ]
    )
    let bus43 = LineOrRoute.route(objects.route { route in
        route.id = "43"
        route.color = "FFC72C"
        route.directionNames = ["Outbound", "Inbound"]
        route.directionDestinations = ["Ruggles Station", "Park Street Station"]
        route.shortName = "43"
        route.textColor = "000000"
        route.type = .bus
    })
    let bus43Inbound = objects.routePattern(route: bus43.route) { routePattern in
        routePattern.directionId = 1
        routePattern.sortOrder = 504_301_000
        routePattern.typicality = .typical
        routePattern.representativeTrip { trip in
            trip.headsign = "Park St & Tremont St"
            trip.stopIds = ["17869", "1227", "10000"]
        }
    }

    let olSouthboundLeaf = RouteCardData.Leaf(
        lineOrRoute: ol,
        stop: ruggles,
        direction: .init(directionId: 0, route: ol.route),
        routePatterns: [olSouthboundPattern],
        stopIds: Set(ruggles.childStopIds),
        upcomingTrips: [
            objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: olSouthboundPattern)
                prediction.departureTime = now.plus(minutes: 3)
            }), objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: olSouthboundPattern)
                prediction.departureTime = now.plus(minutes: 11)
            })
        ],
        alertsHere: [],
        allDataLoaded: true,
        hasSchedulesToday: true,
        subwayServiceStartTime: nil,
        alertsDownstream: [objects.alert { $0.effect = .suspension }],
        context: .favorites
    )

    let olNorthboundLeaf = RouteCardData.Leaf(
        lineOrRoute: ol,
        stop: ruggles,
        direction: .init(directionId: 1, route: ol.route),
        routePatterns: [olNorthboundPattern],
        stopIds: Set(ruggles.childStopIds),
        upcomingTrips: [
            objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: olNorthboundPattern)
                prediction.departureTime = now.plus(minutes: 6)
            }), objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: olNorthboundPattern)
                prediction.departureTime = now.plus(minutes: 14)
            })
        ],
        alertsHere: [],
        allDataLoaded: true,
        hasSchedulesToday: true,
        subwayServiceStartTime: nil,
        alertsDownstream: [],
        context: .favorites
    )

    let bus43RugglesLeaf = RouteCardData.Leaf(
        lineOrRoute: bus43,
        stop: ruggles,
        direction: .init(directionId: 1, route: bus43.route),
        routePatterns: [bus43Inbound],
        stopIds: Set(ruggles.childStopIds),
        upcomingTrips: [
            objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: bus43Inbound)
                prediction.departureTime = now.plus(minutes: 22)
            }), objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: bus43Inbound)
                prediction.departureTime = now.plus(minutes: 56)
            }), objects.upcomingTrip(schedule: objects.schedule { schedule in
                schedule.trip = objects.trip(routePattern: bus43Inbound)
                schedule.departureTime = now.plus(hours: 12)
            })
        ],
        alertsHere: [],
        allDataLoaded: true,
        hasSchedulesToday: true,
        subwayServiceStartTime: nil,
        alertsDownstream: [],
        context: .favorites
    )

    let bus43TremontAtMelneaCassLeaf = RouteCardData.Leaf(
        lineOrRoute: bus43,
        stop: tremontAtMelneaCass,
        direction: .init(directionId: 1, route: bus43.route),
        routePatterns: [bus43Inbound],
        stopIds: [tremontAtMelneaCass.id],
        upcomingTrips: [
            objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: bus43Inbound)
                prediction.departureTime = now.plus(minutes: 25)
            }), objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: bus43Inbound)
                prediction.departureTime = now.plus(minutes: 59)
            }), objects.upcomingTrip(schedule: objects.schedule { schedule in
                schedule.trip = objects.trip(routePattern: bus43Inbound)
                schedule.departureTime = now.plus(hours: 12)
            })
        ],
        alertsHere: [],
        allDataLoaded: true,
        hasSchedulesToday: true,
        subwayServiceStartTime: nil,
        alertsDownstream: [],
        context: .favorites
    )

    let glWestboundLeaf = RouteCardData.Leaf(
        lineOrRoute: gl,
        stop: boylston,
        direction: .init(name: "West", destination: nil, id: 0),
        routePatterns: [
            objects.getRoutePattern(id: "Green-B-812-0"),
            objects.getRoutePattern(id: "Green-C-832-0"),
            objects.getRoutePattern(id: "Green-D-855-0"),
            objects.getRoutePattern(id: "Green-E-886-0")
        ],
        stopIds: Set(boylston.childStopIds),
        upcomingTrips: [
            objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: objects.getRoutePattern(id: "Green-C-832-0"))
                prediction.departureTime = now.plus(minutes: 3)
            }), objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: objects.getRoutePattern(id: "Green-B-812-0"))
                prediction.departureTime = now.plus(minutes: 5)
            }), objects.upcomingTrip(prediction: objects.prediction { prediction in
                prediction.trip = objects.trip(routePattern: objects.getRoutePattern(id: "Green-D-855-0"))
                prediction.departureTime = now.plus(minutes: 10)
            })
        ],
        alertsHere: [],
        allDataLoaded: true,
        hasSchedulesToday: true,
        subwayServiceStartTime: nil,
        alertsDownstream: [],
        context: .favorites
    )

    VStack {
        StopCardList(
            stopCardData: [
                .init(stop: ruggles, data: [olSouthboundLeaf, olNorthboundLeaf, bus43RugglesLeaf]),
                .init(stop: tremontAtMelneaCass, data: [bus43TremontAtMelneaCassLeaf]),
                .init(stop: boylston, data: [glWestboundLeaf])
            ],
            emptyView: {},
            global: .init(objects: objects),
            now: now.toNSDateLosingTimeZone(),
            isFavorite: { _ in true },
            pushNavEntry: { _ in }
        )
    }
    .withFixedSettings([.favoritesByStop: true, .stationAccessibility: true])
}
