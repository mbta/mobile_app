//
//  GreenLineHelper.swift
//  iosAppTests
//
//  Created by Simon, Emma on 6/27/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import XCTest
@_spi(Experimental) import MapboxMaps

enum GreenLineHelper {
    static let objects = ObjectCollectionBuilder()

    static let line = objects.line { line in
        line.id = "line-Green"
    }

    static let routeB = objects.route { route in
        route.id = "Green-B"
        route.type = .lightRail
        route.shortName = "B"
        route.lineId = "line-Green"
        route.directionNames = ["West", "East"]
        route.directionDestinations = ["Kenmore & West", "Park St & North"]
    }

    static let routeC = objects.route { route in
        route.id = "Green-C"
        route.type = .lightRail
        route.shortName = "C"
        route.lineId = "line-Green"
        route.directionNames = ["West", "East"]
        route.directionDestinations = ["Kenmore & West", "Park St & North"]
    }

    static let routeE = objects.route { route in
        route.id = "Green-E"
        route.type = .lightRail
        route.shortName = "E"
        route.lineId = "line-Green"
        route.directionNames = ["West", "East"]
        route.directionDestinations = ["Heath Street", "Park St & North"]
    }

    static let stopArlington = objects.stop { stop in
        stop.id = "place-armnl"
        stop.name = "Arlington"
        stop.childStopIds = ["70156", "70157"]
    }

    static let stopEastbound = objects.stop { stop in
        stop.id = "70156"
        stop.name = "Arlington"
        stop.description_ = "Arlington - Green Line - Park Street & North"
        stop.parentStationId = "place-armnl"
    }

    static let stopWestbound = objects.stop { stop in
        stop.id = "70157"
        stop.name = "Arlington"
        stop.description_ = "Arlington - Green Line - Copley & West"
        stop.parentStationId = "place-armnl"
    }

    static let rpB0 = objects.routePattern(route: routeB) { pattern in
        pattern.id = "Green-B-812-0"
        pattern.sortOrder = 100_320_000
        pattern.typicality = .typical
        pattern.directionId = 0
        pattern.representativeTrip { trip in
            trip.headsign = "Boston College"
            trip.directionId = 0
        }
    }

    static let rpB1 = objects.routePattern(route: routeB) { pattern in
        pattern.id = "Green-B-812-1"
        pattern.sortOrder = 100_321_000
        pattern.typicality = .typical
        pattern.directionId = 1
        pattern.representativeTrip { trip in
            trip.headsign = "Government Center"
            trip.directionId = 1
        }
    }

    static let rpC0 = objects.routePattern(route: routeC) { pattern in
        pattern.id = "Green-C-832-0"
        pattern.sortOrder = 100_330_000
        pattern.typicality = .typical
        pattern.directionId = 0
        pattern.representativeTrip { trip in
            trip.headsign = "Cleveland Circle"
            trip.directionId = 0
        }
    }

    static let rpC1 = objects.routePattern(route: routeC) { pattern in
        pattern.id = "Green-C-832-1"
        pattern.sortOrder = 100_331_000
        pattern.typicality = .typical
        pattern.directionId = 1
        pattern.representativeTrip { trip in
            trip.headsign = "Government Center"
            trip.directionId = 1
        }
    }

    static let rpE0 = objects.routePattern(route: routeE) { pattern in
        pattern.id = "Green-E-886-0"
        pattern.sortOrder = 100_350_000
        pattern.typicality = .typical
        pattern.directionId = 0
        pattern.representativeTrip { trip in
            trip.headsign = "Heath Street"
            trip.directionId = 0
        }
    }

    static let rpE1 = objects.routePattern(route: routeE) { pattern in
        pattern.id = "Green-E-886-1"
        pattern.sortOrder = 100_351_000
        pattern.typicality = .typical
        pattern.directionId = 1
        pattern.representativeTrip { trip in
            trip.headsign = "Medford/Tufts"
            trip.directionId = 1
        }
    }

    static let nearbyData = NearbyStaticData.companion.build { builder in
        builder.line(line: line, routes: [routeB, routeC, routeE]) { builder in
            builder.stop(
                stop: stopArlington,
                routes: [routeB, routeC, routeE],
                childStopIds: [stopEastbound.id, stopWestbound.id]
            ) { builder in
                builder.direction(
                    direction: Direction(name: "West", destination: "Kenmore & West", id: 0),
                    routes: [routeB, routeC], patterns: [rpB0, rpC0]
                )
                builder.headsign(route: routeE, headsign: "Heath Street", patterns: [rpE0])
                builder.direction(
                    direction: Direction(name: "East", destination: "Park St & North", id: 1),
                    routes: [routeB, routeC, routeE], patterns: [rpB1, rpC1, rpE1]
                )
            }
        }
    }

    static let state = NearbyViewModel.NearbyTransitState(
        loadedLocation: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
        nearbyByRouteAndStop: nearbyData
    )
}
