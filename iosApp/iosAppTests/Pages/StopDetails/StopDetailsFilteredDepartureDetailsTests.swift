//
//  StopDetailsFilteredDepartureDetailsTests.swift
//  iosAppTests
//
//  Created by esimon on 12/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsFilteredDepartureDetailsTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func makeBundle(
        route: Route,
        stop: Stop? = nil,
        patterns: [RoutePattern]? = nil,
        upcomingTrips: [UpcomingTrip]? = nil,
        alerts: [Shared.Alert] = [],
        objects: ObjectCollectionBuilder = ObjectCollectionBuilder()
    ) -> DepartureDataBundle {
        makeBundle(
            lineOrRoute: RouteCardDataLineOrRouteRoute(route: route),
            stop: stop, patterns: patterns, upcomingTrips: upcomingTrips, alerts: alerts, objects: objects
        )
    }

    func makeBundle(
        line: Line,
        routes: Set<Route>? = nil,
        stop: Stop? = nil,
        patterns: [RoutePattern]? = nil,
        upcomingTrips: [UpcomingTrip]? = nil,
        alerts: [Shared.Alert] = [],
        objects: ObjectCollectionBuilder = ObjectCollectionBuilder()
    ) -> DepartureDataBundle {
        let routes = routes ?? Set([objects.route { $0.lineId = line.id }])
        return makeBundle(
            lineOrRoute: RouteCardDataLineOrRouteLine(line: line, routes: routes),
            stop: stop, patterns: patterns, upcomingTrips: upcomingTrips, alerts: alerts, objects: objects
        )
    }

    func makeBundle(
        lineOrRoute: RouteCardDataLineOrRoute,
        stop: Stop? = nil,
        patterns: [RoutePattern]? = nil,
        upcomingTrips: [UpcomingTrip]? = nil,
        alerts: [Shared.Alert] = [],
        objects: ObjectCollectionBuilder = ObjectCollectionBuilder()
    ) -> DepartureDataBundle {
        let route = lineOrRoute.sortRoute
        let stop = stop ?? objects.stop { _ in }
        let patterns = patterns ?? [objects.routePattern(route: route) { _ in }]
        let upcomingTrips = upcomingTrips ?? []

        let leaf = RouteCardData.Leaf(
            directionId: 0,
            routePatterns: patterns,
            stopIds: Set([stop.id]).union(Set(stop.childStopIds)),
            upcomingTrips: upcomingTrips,
            alertsHere: alerts, allDataLoaded: true, hasSchedulesToday: true, alertsDownstream: []
        )
        let stopData = RouteCardData.RouteStopData(
            stop: stop,
            directions: [
                .init(directionId: 0, route: route),
                .init(directionId: 1, route: route),
            ],
            data: [leaf]
        )
        let routeData = RouteCardData(
            lineOrRoute: lineOrRoute,
            stopData: [stopData],
            context: .stopDetailsFiltered,
            at: Date.now.toKotlinInstant()
        )

        return .init(routeData: routeData, stopData: stopData, leaf: leaf)
    }

    func testDisplaysTrips() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()
        let trip1 = UpcomingTrip(trip: objects.trip { _ in })
        let trip2 = UpcomingTrip(trip: objects.trip { _ in })
        let trip3 = UpcomingTrip(trip: objects.trip { _ in })

        let tile1 = TileData(
            route: route,
            headsign: "A",
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: trip1, routeType: .heavyRail, format: .Arriving())],
                secondaryAlert: nil
            ),
            upcoming: trip1
        )
        let tile2 = TileData(
            route: route,
            headsign: "B",
            formatted: UpcomingFormat.Some(
                trips: [.init(
                    trip: trip2,
                    routeType: .heavyRail,
                    format: .Minutes(minutes: 3)
                )],
                secondaryAlert: nil
            ),
            upcoming: trip2
        )
        let tile3 = TileData(
            route: route,
            headsign: "C",
            formatted: UpcomingFormat.Some(
                trips: [.init(
                    trip: trip3,
                    routeType: .heavyRail,
                    format: .Minutes(minutes: 7)
                )],
                secondaryAlert: nil
            ),
            upcoming: trip3
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile1, tile2, tile3],
            data: makeBundle(route: route, stop: stop, upcomingTrips: [trip1, trip2, trip3], objects: objects),
            noPredictionsStatus: nil,
            alerts: [],
            downstreamAlerts: [],
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertNotNil(try sut.inspect().find(text: "A"))
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(text: "B"))
        XCTAssertNotNil(try sut.inspect().find(text: "3 min"))
        XCTAssertNotNil(try sut.inspect().find(text: "C"))
        XCTAssertNotNil(try sut.inspect().find(text: "7 min"))
    }

    func testAlwaysShowsHeadsignAndPillsWhenLine() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in route.id = "Green-B" }
        let line = objects.line { line in line.id = "Green" }
        let trip1 = UpcomingTrip(trip: objects.trip { _ in })
        let trip2 = UpcomingTrip(trip: objects.trip { _ in })
        let trip3 = UpcomingTrip(trip: objects.trip { _ in })

        let tile1 = TileData(
            route: route,
            headsign: "Matching",
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: trip1, routeType: .lightRail, format: .Arriving())],
                secondaryAlert: nil
            ),
            upcoming: trip1
        )
        let tile2 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: UpcomingFormat.Some(
                trips: [.init(
                    trip: trip2,
                    routeType: .lightRail,
                    format: .Minutes(minutes: 3)
                )],
                secondaryAlert: nil
            ),
            upcoming: trip2
        )
        let tile3 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: UpcomingFormat.Some(
                trips: [.init(
                    trip: trip3,
                    routeType: .lightRail,
                    format: .Minutes(minutes: 7)
                )],
                secondaryAlert: nil
            ),
            upcoming: trip3
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile1, tile2, tile3],
            data: makeBundle(
                line: line,
                routes: [route],
                stop: stop,
                upcomingTrips: [trip1, trip2, trip3],
                objects: objects
            ),
            noPredictionsStatus: nil,
            alerts: [],
            downstreamAlerts: [],
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self).find(text: tile1.headsign!))
        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self).find(RoutePill.self))
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(text: "3 min"))
        XCTAssertNotNil(try sut.inspect().find(text: "7 min"))
    }

    func testShowsTripDetails() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in route.id = "Green-B" }
        let pattern = objects.routePattern(route: route) { _ in }
        let line = objects.line { line in line.id = "Green" }
        let trip = objects.trip { trip in
            trip.routeId = route.id
            trip.routePatternId = pattern.id
        }

        let tile1 = TileData(
            route: route,
            headsign: "Matching",
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: .init(trip: trip), routeType: .lightRail, format: .Arriving())],
                secondaryAlert: nil
            ),
            upcoming: .init(trip: trip)
        )
        let tile2 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: .init(trip: trip), routeType: .lightRail, format: .Minutes(minutes: 3))],
                secondaryAlert: nil
            ),
            upcoming: .init(trip: trip)
        )
        let tile3 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: .init(trip: trip), routeType: .lightRail, format: .Minutes(minutes: 7))],
                secondaryAlert: nil
            ),
            upcoming: .init(trip: trip)
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: .init(tripId: trip.id, vehicleId: nil, stopSequence: nil, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile1, tile2, tile3],
            data: makeBundle(line: line, routes: [route], stop: stop, objects: objects),
            noPredictionsStatus: nil,
            alerts: [],
            downstreamAlerts: [],
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertNotNil(try sut.inspect().find(TripDetailsView.self))
    }

    func testShowsCancelledTripCard() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in
            route.id = "66"
            route.type = .bus
        }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip1 = objects.trip(routePattern: pattern)
        let schedule1 = objects.schedule { schedule in
            schedule.trip = trip1
            schedule.departureTime = now.addingTimeInterval(10).toKotlinInstant()
        }
        let prediction1 = objects.prediction(schedule: schedule1) { prediction in
            prediction.trip = trip1
            prediction.scheduleRelationship = .cancelled
        }
        let upcoming1 = objects.upcomingTrip(schedule: schedule1, prediction: prediction1)
        let trip2 = objects.trip(routePattern: pattern)
        let schedule2 = objects.schedule { schedule in
            schedule.trip = trip2
            schedule.departureTime = now.addingTimeInterval(10).toKotlinInstant()
        }
        let prediction2 = objects.prediction(schedule: schedule2) { prediction in
            prediction.trip = trip2
            prediction.scheduleRelationship = .cancelled
        }
        let upcoming2 = objects.upcomingTrip(schedule: schedule2, prediction: prediction2)

        let tile1 = TileData(
            route: route,
            headsign: "Harvard",
            formatted: UpcomingFormat.Some(
                trips: [.init(
                    trip: upcoming1, routeType: .bus,
                    format: .Cancelled(
                        scheduledTime: schedule1.departureTime!
                    )
                )],
                secondaryAlert: nil
            ),
            upcoming: upcoming1
        )
        let tile2 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: upcoming2, routeType: .bus, format: .Minutes(minutes: 3))],
                secondaryAlert: nil
            ),
            upcoming: upcoming2
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: .init(tripId: trip1.id, vehicleId: nil, stopSequence: nil, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile1, tile2],
            data: makeBundle(
                route: route,
                stop: stop,
                patterns: [pattern],
                upcomingTrips: [upcoming1, upcoming2],
                objects: objects
            ),
            noPredictionsStatus: nil,
            alerts: [],
            downstreamAlerts: [],
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())
        XCTAssertNotNil(try sut.inspect().find(text: "Trip cancelled"))
        XCTAssertNotNil(try sut.inspect()
            .find(text: "This trip has been cancelled. We’re sorry for the inconvenience."))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "mode-bus-slash"
        }))
    }

    func testShowsNoTripCard() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in route.id = "Green-B" }
        let line = objects.line { line in line.id = "Green" }

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [],
            data: makeBundle(line: line, routes: [route], stop: stop, upcomingTrips: [], objects: objects),
            noPredictionsStatus: UpcomingFormat.NoTripsFormatServiceEndedToday(),
            alerts: [],
            downstreamAlerts: [],
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertThrowsError(try sut.inspect().find(TripDetailsView.self))
        XCTAssertThrowsError(try sut.inspect().find(DepartureTile.self))
        XCTAssertNotNil(try sut.inspect().find(StopDetailsNoTripCard.self))
    }

    @MainActor
    func testShowsSuspensionFallback() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.header = "Fuchsia Line suspended from Here to There"
        }
        let trip = UpcomingTrip(trip: objects.trip { _ in })

        // in practice any trips should be skipped but for major alerts we want to hide trips if they somehow aren't
        // skipped
        let tile = TileData(
            route: route,
            headsign: "A",
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: trip, routeType: .heavyRail, format: .Arriving())],
                secondaryAlert: nil
            ),
            upcoming: trip
        )
        let nearbyVM = NearbyViewModel()

        let stopDetailsVM: StopDetailsViewModel = .init()
        stopDetailsVM.global = GlobalResponse(objects: objects)

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile],
            data: makeBundle(route: route, stop: stop, objects: objects),
            noPredictionsStatus: nil,
            alerts: [alert],
            downstreamAlerts: [],
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        )

        let departureTileExp = sut.inspection.inspect { _ in
            XCTAssertThrowsError(try sut.inspect().find(DepartureTile.self))
        }

        let alertCardExp = sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(try view.find(AlertCard.self))
            XCTAssertNotNil(try view.find(text: "Suspension"))
            XCTAssertNotNil(try view.find(text: alert.header!))
            try sut.inspect().find(button: "View details").tap()
            XCTAssertEqual(
                nearbyVM.navigationStack.last,
                .alertDetails(alertId: alert.id, line: nil, routes: [route], stop: stop)
            )
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()))
        wait(for: [departureTileExp, alertCardExp], timeout: 2)
    }

    @MainActor
    func testShowsDownstreamAlertFallback() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.header = "Fuchsia Line suspended from Here to There"
        }
        let trip = UpcomingTrip(trip: objects.trip { _ in })

        let tile = TileData(
            route: route,
            headsign: "A",
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: trip, routeType: .heavyRail, format: .Arriving())],
                secondaryAlert: nil
            ),
            upcoming: trip
        )
        let nearbyVM = NearbyViewModel()

        let stopDetailsVM: StopDetailsViewModel = .init()
        stopDetailsVM.global = GlobalResponse(objects: objects)

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile],
            data: makeBundle(route: route, stop: stop, objects: objects),
            noPredictionsStatus: nil,
            alerts: [],
            downstreamAlerts: [alert],
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        )

        let departureTileExp = sut.inspection.inspect { view in
            XCTAssertNotNil(try view.find(DepartureTile.self))
        }

        let alertCardExp = sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(try view.find(AlertCard.self))
            XCTAssertNotNil(try view.find(text: "Service suspended ahead"))
            XCTAssertThrowsError(try view.find(text: alert.header!))
            try view.find(AlertCard.self).implicitAnyView().button().tap()
            XCTAssertEqual(
                nearbyVM.navigationStack.last,
                .alertDetails(alertId: alert.id, line: nil, routes: [route], stop: stop)
            )
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()))
        wait(for: [departureTileExp, alertCardExp], timeout: 2)
    }

    func testShowsElevatorAlert() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.header = "Elevator closed at stop"
        }
        let trip = UpcomingTrip(trip: objects.trip { _ in })

        let tile = TileData(
            route: route,
            headsign: "A",
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: trip, routeType: .heavyRail, format: .Arriving())],
                secondaryAlert: nil
            ),
            upcoming: trip
        )
        let nearbyVM = NearbyViewModel()
        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.showStationAccessibility = true

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile],
            data: makeBundle(route: route, stop: stop, patterns: [], alerts: [alert], objects: objects),
            noPredictionsStatus: nil,
            alerts: [],
            downstreamAlerts: [],
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self))
        XCTAssertNotNil(try sut.inspect().find(AlertCard.self))
        XCTAssertNotNil(try sut.inspect().find(text: alert.header!))
        try sut.inspect().find(AlertCard.self).implicitAnyView().button().tap()
        XCTAssertEqual(
            nearbyVM.navigationStack.last,
            .alertDetails(alertId: alert.id, line: nil, routes: nil, stop: stop)
        )
    }

    func testShowsInaccessible() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.wheelchairBoarding = .inaccessible
        }
        let route = objects.route { _ in }
        let trip = UpcomingTrip(trip: objects.trip { _ in })
        let tile = TileData(
            route: route,
            headsign: "A",
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: trip, routeType: .heavyRail, format: .Arriving())],
                secondaryAlert: nil
            ),
            upcoming: trip
        )
        let nearbyVM = NearbyViewModel()
        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.showStationAccessibility = true

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile],
            data: makeBundle(route: route, stop: stop, objects: objects),
            noPredictionsStatus: nil,
            alerts: [],
            downstreamAlerts: [],
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertNotNil(try sut.inspect().find(text: "This stop is not accessible"))
    }

    @MainActor
    func testShowsSubwayDelayAlertFallback() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let alert = objects.alert { alert in
            alert.effect = .delay
            alert.header = "Delay header"
            alert.cause = .heavyRidership
            alert.severity = 10
            alert.informedEntity(
                activities: [.board, .exit, .ride],
                directionId: 0, facility: nil,
                route: route.id, routeType: .lightRail,
                stop: nil, trip: nil
            )
        }
        let trip = UpcomingTrip(trip: objects.trip { _ in })

        let tile = TileData(
            route: route,
            headsign: "A",
            formatted: UpcomingFormat.Some(
                trips: [.init(trip: trip, routeType: .heavyRail, format: .Arriving())],
                secondaryAlert: nil
            ),
            upcoming: trip
        )
        let nearbyVM = NearbyViewModel()
        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.showStationAccessibility = true
        stopDetailsVM.global = GlobalResponse(objects: objects)

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile],
            data: makeBundle(route: route, stop: stop, alerts: [alert], objects: objects),
            noPredictionsStatus: nil,
            alerts: [alert],
            downstreamAlerts: [],
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        )

        let departureTileExp = sut.inspection.inspect { view in
            XCTAssertNotNil(try view.find(DepartureTile.self))
        }

        let alertCardExp = sut.inspection.inspect(after: 1) { _ in
            XCTAssertNotNil(try sut.inspect().find(AlertCard.self))
            XCTAssertNotNil(try sut.inspect().find(text: "Delays due to heavy ridership"))
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()))
        wait(for: [departureTileExp, alertCardExp], timeout: 2)
    }
}
