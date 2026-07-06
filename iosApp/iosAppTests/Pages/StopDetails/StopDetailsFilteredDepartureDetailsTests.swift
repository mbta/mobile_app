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

    func makeLeaf(
        route: Route,
        stop: Stop? = nil,
        patterns: [RoutePattern]? = nil,
        upcomingTrips: [UpcomingTrip]? = nil,
        alerts: [Shared.Alert] = [],
        alertsDownstream: [Shared.Alert] = [],
        objects: ObjectCollectionBuilder = ObjectCollectionBuilder(),
        subwayServiceStartTime: EasternTimeInstant? = nil
    ) -> RouteCardData.Leaf {
        makeLeaf(
            lineOrRoute: .route(route),
            stop: stop,
            patterns: patterns,
            upcomingTrips: upcomingTrips,
            alerts: alerts,
            alertsDownstream: alertsDownstream,
            objects: objects,
            subwayServiceStartTime: subwayServiceStartTime
        )
    }

    func makeLeaf(
        line: Line,
        routes: Set<Route>? = nil,
        stop: Stop? = nil,
        patterns: [RoutePattern]? = nil,
        upcomingTrips: [UpcomingTrip]? = nil,
        alerts: [Shared.Alert] = [],
        alertsDownstream: [Shared.Alert] = [],
        objects: ObjectCollectionBuilder = ObjectCollectionBuilder(),
        subwayServiceStartTime: EasternTimeInstant? = nil
    ) -> RouteCardData.Leaf {
        let routes = routes ?? Set([objects.route { $0.lineId = line.id.idText }])
        return makeLeaf(
            lineOrRoute: .line(line, routes),
            stop: stop,
            patterns: patterns,
            upcomingTrips: upcomingTrips,
            alerts: alerts,
            alertsDownstream: alertsDownstream,
            objects: objects,
            subwayServiceStartTime: subwayServiceStartTime
        )
    }

    func makeLeaf(
        lineOrRoute: LineOrRoute,
        stop: Stop? = nil,
        patterns: [RoutePattern]? = nil,
        upcomingTrips: [UpcomingTrip]? = nil,
        alerts: [Shared.Alert] = [],
        alertsDownstream: [Shared.Alert] = [],
        objects: ObjectCollectionBuilder = ObjectCollectionBuilder(),
        subwayServiceStartTime: EasternTimeInstant? = nil
    ) -> RouteCardData.Leaf {
        let route = lineOrRoute.sortRoute
        let stop = stop ?? objects.stop { _ in }
        let patterns = patterns ?? [objects.routePattern(route: route) { _ in }]
        let upcomingTrips = upcomingTrips ?? []

        return RouteCardData.Leaf(
            lineOrRoute: lineOrRoute,
            stop: stop,
            direction: .init(directionId: 0, route: route),
            routePatterns: patterns,
            stopIds: Set([stop.id]).union(Set(stop.childStopIds)),
            upcomingTrips: upcomingTrips,
            alertsHere: alerts,
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: subwayServiceStartTime,
            alertsDownstream: alertsDownstream,
            context: .stopDetailsFiltered
        )
    }

    func testDisplaysTrips() throws {
        let objects = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        let stop = objects.stop { _ in }
        let route = objects.route()
        let trip1 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "A" }
            prediction.departureTime = now.plus(seconds: 15)
        })
        let trip2 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "B" }
            prediction.departureTime = now.plus(minutes: 3)
        })
        let trip3 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "C" }
            prediction.departureTime = now.plus(minutes: 7)
        })

        let leaf = makeLeaf(route: route, stop: stop, upcomingTrips: [trip1, trip2, trip3], objects: objects)

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        ).environmentObject(ViewportProvider()).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "A"))
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(text: "B"))
        XCTAssertNotNil(try sut.inspect().find(text: "3 min"))
        XCTAssertNotNil(try sut.inspect().find(text: "C"))
        XCTAssertNotNil(try sut.inspect().find(text: "7 min"))
    }

    @MainActor
    func testShowsHeadsignAndPillsWhenBranchingLine() {
        let objects = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        let stop = objects.stop { _ in }
        let line = objects.line { line in line.id = "Green" }
        let routeB = objects.route { route in
            route.id = "Green-B"
            route.lineId = line.id.idText
        }
        let routePatternB = objects.routePattern(route: routeB) { pattern in
            pattern.representativeTrip { $0.headsign = "Headsign B" }
        }
        let trip1 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: routePatternB)
            prediction.departureTime = now.plus(seconds: 15)
        })
        let trip2 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: routePatternB)
            prediction.departureTime = now.plus(minutes: 3)
        })
        let routeC = objects.route { route in
            route.id = "Green-C"
            route.lineId = line.id.idText
        }
        let routePatternC = objects.routePattern(route: routeC) { pattern in
            pattern.representativeTrip { $0.headsign = "Headsign C" }
        }
        let trip3 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: routePatternC)
            prediction.departureTime = now.plus(minutes: 7)
        })

        loadKoinMocks(objects: objects)

        let leaf = makeLeaf(
            line: line,
            routes: [routeB, routeC],
            stop: stop,
            patterns: [routePatternB, routePatternC],
            upcomingTrips: [trip1, trip2, trip3],
            objects: objects
        )

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: line.id, directionId: 0),
            tripFilter: .init(tripId: trip1.id, vehicleId: nil, stopSequence: nil, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        )

        let departuresExp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(DepartureTile.self).find(text: trip1.headsign))
            XCTAssertNotNil(try view.find(DepartureTile.self).find(RoutePill.self))
            XCTAssertNotNil(try view.find(text: "ARR"))
            XCTAssertNotNil(try view.find(text: "3 min"))
            XCTAssertNotNil(try view.find(text: "7 min"))
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))
        wait(for: [departuresExp], timeout: 2)
    }

    func testShowsTripDetails() throws {
        let objects = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        let stop = objects.stop { _ in }
        let route = objects.route { route in route.id = "Green-B" }
        let pattern = objects.routePattern(route: route) { pattern in
            pattern.representativeTrip { $0.headsign = "Matching" }
        }
        let line = objects.line { line in line.id = "Green" }
        let trip1 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: pattern)
            prediction.departureTime = now.plus(minutes: 2)
        })
        let trip2 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: pattern)
            prediction.departureTime = now.plus(minutes: 3)
        })
        let trip3 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: pattern)
            prediction.departureTime = now.plus(minutes: 7)
        })

        let leaf = makeLeaf(
            line: line,
            routes: [route],
            stop: stop,
            upcomingTrips: [trip1, trip2, trip3],
            objects: objects
        )

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: .init(tripId: trip1.trip.id, vehicleId: nil, stopSequence: nil, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        ).environmentObject(ViewportProvider()).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(TripDetailsView.self))
    }

    func testShowsCancelledTripCard() throws {
        let now = EasternTimeInstant.now()
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
            schedule.departureTime = now.plus(seconds: 10)
        }
        let prediction1 = objects.prediction(schedule: schedule1) { prediction in
            prediction.trip = trip1
            prediction.scheduleRelationship = .cancelled
        }
        let upcoming1 = objects.upcomingTrip(schedule: schedule1, prediction: prediction1)
        let trip2 = objects.trip(routePattern: pattern)
        let schedule2 = objects.schedule { schedule in
            schedule.trip = trip2
            schedule.departureTime = now.plus(seconds: 10)
        }
        let prediction2 = objects.prediction(schedule: schedule2) { prediction in
            prediction.trip = trip2
            prediction.scheduleRelationship = .cancelled
        }
        let upcoming2 = objects.upcomingTrip(schedule: schedule2, prediction: prediction2)

        let leaf = makeLeaf(
            route: route,
            stop: stop,
            patterns: [pattern],
            upcomingTrips: [upcoming1, upcoming2],
            objects: objects
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: .init(tripId: trip1.id, vehicleId: nil, stopSequence: nil, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        ).environmentObject(ViewportProvider()).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(text: "Trip cancelled"))
        XCTAssertNotNil(try sut.inspect()
            .find(text: "This trip has been cancelled. We’re sorry for the inconvenience."))
        XCTAssertNotNil(try sut.inspect().find(imageName: "mode-bus-slash"))
    }

    func testShowsCancelledTripCardOnlyOnceWhenAlert() throws {
        let now = EasternTimeInstant.now()
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
            schedule.departureTime = now.plus(seconds: 10)
        }
        let prediction1 = objects.prediction(schedule: schedule1) { prediction in
            prediction.trip = trip1
            prediction.scheduleRelationship = .cancelled
        }
        let upcoming1 = objects.upcomingTrip(schedule: schedule1, prediction: prediction1)
        let trip2 = objects.trip(routePattern: pattern)
        let schedule2 = objects.schedule { schedule in
            schedule.trip = trip2
            schedule.departureTime = now.plus(seconds: 10)
        }
        let prediction2 = objects.prediction(schedule: schedule2) { prediction in
            prediction.trip = trip2
            prediction.scheduleRelationship = .cancelled
        }
        let upcoming2 = objects.upcomingTrip(schedule: schedule2, prediction: prediction2)

        let alert = objects.alert { alert in
            alert.effect = .cancellation
            alert.informedEntity(
                directionId: 0,
                facility: nil,
                route: route.id.idText,
                stop: stop.id,
                trip: trip1.id
            )
        }
        let leaf = makeLeaf(
            route: route,
            stop: stop,
            patterns: [pattern],
            upcomingTrips: [upcoming1, upcoming2],
            alerts: [alert],
            objects: objects
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: .init(tripId: trip1.id, vehicleId: nil, stopSequence: nil, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        ).environmentObject(ViewportProvider()).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "Bus cancelled"))
        XCTAssertThrowsError(try sut.inspect().find(text: "Trip cancelled"))
        XCTAssertThrowsError(try sut.inspect()
            .find(text: "This trip has been cancelled. We’re sorry for the inconvenience."))
    }

    func testShowsNoTripCard() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in route.id = "Green-B" }
        let line = objects.line { line in line.id = "Green" }

        let leaf = makeLeaf(line: line, routes: [route], stop: stop, upcomingTrips: [], objects: objects)

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: .now(),
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        ).environmentObject(ViewportProvider()).withFixedSettings([:])

        XCTAssertThrowsError(try sut.inspect().find(TripDetailsView.self))
        XCTAssertThrowsError(try sut.inspect().find(DepartureTile.self))
        XCTAssertNotNil(try sut.inspect().find(StopDetailsNoTripCard.self))
    }

    @MainActor
    func testShowsSuspensionFallback() {
        let objects = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.header = "Fuchsia Line suspended from Here to There"
            alert.informedEntity(stop: stop.id)
        }

        // in practice any trips should be skipped but for major alerts we want to hide trips if they somehow aren't
        // skipped
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { _ in }
            prediction.departureTime = now.plus(seconds: 15)
        })

        let stopFilter = StopDetailsFilter(routeId: route.id, directionId: 0)

        let leaf = makeLeaf(route: route, stop: stop, upcomingTrips: [trip], alerts: [alert], objects: objects)

        loadKoinMocks(objects: objects)

        let navManager = NavigationManager()

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: navManager,
        )

        let alertCardExp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(AlertCard.self))
            XCTAssertNotNil(try view.find(text: "Suspension"))
            XCTAssertNotNil(try view.find(text: alert.header!))
            try sut.inspect().find(button: "View details").tap()
            XCTAssertEqual(
                navManager.navigationStack.last,
                .alertDetails(alertId: alert.id, line: nil, routes: [route], stop: stop)
            )
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))
        wait(for: [alertCardExp], timeout: 2)
    }

    @MainActor
    func testShowsDownstreamAlertFallback() {
        let objects = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.header = "Fuchsia Line suspended from Here to There"
            alert.informedEntity(stop: stop.id)
        }
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { _ in }
            prediction.departureTime = now.plus(seconds: 15)
        })

        loadKoinMocks(objects: objects)

        let navManager = NavigationManager()

        let leaf = makeLeaf(
            route: route,
            stop: stop,
            upcomingTrips: [trip],
            alertsDownstream: [alert],
            objects: objects
        )

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: navManager,
        )

        let departureTileExp = sut.inspection.inspect { view in
            XCTAssertNotNil(try view.find(DepartureTile.self))
        }

        let alertCardExp = sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(try view.find(AlertCard.self))
            XCTAssertNotNil(try view.find(text: "Service suspended ahead"))
            XCTAssertThrowsError(try view.find(text: alert.header!))
            try view.find(AlertCard.self).find(ViewType.Button.self).tap()
            XCTAssertEqual(
                navManager.navigationStack.last,
                .alertDetails(alertId: alert.id, line: nil, routes: [route], stop: stop)
            )
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))
        wait(for: [departureTileExp, alertCardExp], timeout: 2)
    }

    func testShowsElevatorAlertOnlyOnce() throws {
        let objects = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.header = "Elevator closed at stop"
            alert.informedEntity(
                activities: [.usingWheelchair],
                directionId: nil,
                facility: nil,
                route: nil,
                routeType: nil,
                stop: stop.id,
                trip: nil
            )
        }
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "A" }
            prediction.departureTime = now.plus(seconds: 15)
        })

        let navManager = NavigationManager()

        let leaf = makeLeaf(
            route: route,
            stop: stop,
            patterns: [],
            upcomingTrips: [trip],
            alerts: [alert],
            objects: objects
        )

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: navManager,
        ).environmentObject(ViewportProvider()).withFixedSettings([.stationAccessibility: true])

        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self))
        XCTAssertNotNil(try sut.inspect().find(AlertCard.self))
        XCTAssertNil(try? sut.inspect().find(text: "Elevator Closure"))
        XCTAssertNotNil(try sut.inspect().find(text: XCTUnwrap(alert.header)))
        try sut.inspect().find(AlertCard.self).find(ViewType.Button.self).tap()
        XCTAssertEqual(
            navManager.navigationStack.last,
            .alertDetails(alertId: alert.id, line: nil, routes: nil, stop: stop)
        )
    }

    func testShowsInaccessible() throws {
        let objects = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        let stop = objects.stop { stop in
            stop.wheelchairBoarding = .inaccessible
        }
        let route = objects.route { _ in }
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { _ in }
            prediction.departureTime = now.plus(seconds: 15)
        })

        let leaf = makeLeaf(route: route, stop: stop, upcomingTrips: [trip], objects: objects)

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        ).environmentObject(ViewportProvider()).withFixedSettings([.stationAccessibility: true])

        XCTAssertNotNil(try sut.inspect().find(text: "This stop is not accessible"))
    }

    @MainActor
    func testShowsSubwayDelayAlertFallback() {
        let objects = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
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
                route: route.id.idText, routeType: .lightRail,
                stop: nil, trip: nil
            )
        }
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "A" }
            prediction.departureTime = now.plus(seconds: 15)
        })

        loadKoinMocks(objects: objects)

        let leaf = makeLeaf(route: route, stop: stop, upcomingTrips: [trip], alerts: [alert], objects: objects)

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        )

        let departureTileExp = sut.inspection.inspect { view in
            XCTAssertNotNil(try view.find(DepartureTile.self))
        }

        let alertCardExp = sut.inspection.inspect(after: 1) { _ in
            XCTAssertNotNil(try sut.inspect().find(AlertCard.self))
            XCTAssertNotNil(try sut.inspect().find(text: "Delays due to heavy ridership"))
        }

        ViewHosting
            .host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([.stationAccessibility: true]))
        wait(for: [departureTileExp, alertCardExp], timeout: 2)
    }

    @MainActor
    func testShowsPredictionsAndAlertOnBranchingTrunk() async throws {
        let now = EasternTimeInstant.now()

        let objects = Shared.TestData.clone()
        let stop = objects.getStop(id: "place-kencl")
        let line = objects.getLine(id: "line-Green")
        let routeB = objects.getRoute(id: "Green-B")
        let routeC = objects.getRoute(id: "Green-C")
        let routeD = objects.getRoute(id: "Green-D")

        let alert =
            objects.alert { alert in
                alert.effect = .shuttle
                alert.header = "Green line shuttle on B and C branches"
                alert.informedEntity(
                    activities: [.board, .exit, .ride],
                    directionId: 0, facility: nil,
                    route: routeB.id.idText, routeType: nil,
                    stop: "71151", trip: nil
                )
                alert.informedEntity(
                    activities: [.board, .exit, .ride],
                    directionId: 0, facility: nil,
                    route: routeC.id.idText, routeType: nil,
                    stop: "70151", trip: nil
                )
                alert.summaries = [.init(
                    routeId: nil,
                    stopId: nil,
                    tripId: nil,
                    directionId: nil,
                    summary: "Shuttle buses at \(stop.name)"
                )]
            }
        let alertResponse = AlertsStreamDataResponse(alerts: [alert.id: alert])

        objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.departureTime = now.plus(seconds: 300)
            prediction.routeId = routeD.id.idText
            prediction.stopId = stop.id
            prediction.trip = objects.trip { trip in
                trip.routeId = routeD.id.idText
                trip.routePatternId = "Green-D-855-0"
            }
        })

        loadKoinMocks(objects: objects)

        let global = GlobalResponse(objects: objects)
        let routeCardData = try await RouteCardData.companion.routeCardsForStopList(
            stopIds: [stop.id] + stop.childStopIds,
            globalData: global,
            sortByDistanceFrom: nil,
            schedules: ScheduleResponse(objects: objects),
            predictions: PredictionsStreamDataResponse(objects: objects),
            alerts: alertResponse,
            now: now,
            context: .stopDetailsFiltered
        )!.first!
        let routeStopData = try XCTUnwrap(routeCardData.stopData.first)
        let leaf = try XCTUnwrap(routeStopData.data.first { $0.direction.id == 0 })

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: line.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        )

        let exp = sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(try view.find(ViewType.Text.self) { text in
                try text.string().starts(with: "Shuttle buses at \(stop.name)")
            })
            XCTAssertNotNil(try view.find(text: "5 min"))
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))
        await fulfillment(of: [exp], timeout: 3)
    }

    @MainActor func testLoadsNextTrip() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in route.id = "Red" }

        let now = EasternTimeInstant.now()
        let nextSchedule = objects.schedule { schedule in
            schedule.departureTime = .init(
                year: now.local.year + 1,
                month: .december,
                day: 8,
                hour: 8,
                minute: 0,
                second: 0
            )
        }
        let schedulesRepo = MockScheduleRepository(nextScheduleResponse: .init(nextSchedule: nextSchedule))

        let leaf = makeLeaf(route: route, stop: stop, upcomingTrips: [], objects: objects)

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: .now(),
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            schedulesRepository: schedulesRepo,
            navManager: .init(),
        )

        let exp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(textWhere: { text, _ in
                text.hasPrefix("Next trip on") && text.hasSuffix(", Dec 8")
            }))
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))

        wait(for: [exp], timeout: 2)
    }

    func testHidesEarlyMorningCardWhenPredictionExists() {
        let now = EasternTimeInstant(year: 2025, month: .november, day: 17, hour: 3, minute: 30, second: 0)
        let subwayStartTime = EasternTimeInstant(year: 2025, month: .november, day: 17, hour: 9, minute: 44, second: 0)
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.id = "stop_1" }
        let route = objects.route { route in
            route.id = "route_1"
            route.type = .lightRail
            route.routePatternIds = ["pattern_1"]
        }
        let pattern = objects.routePattern(route: route) { pattern in
            pattern.id = "pattern_1"
            pattern.directionId = 0
            pattern.representativeTripId = "trip_1"
        }
        let trip1 = objects.trip { trip in
            trip.id = "trip_1"
            trip.routeId = "route_1"
            trip.directionId = 0
            trip.routePatternId = "pattern_1"
        }
        let schedule1 = objects.schedule { schedule in
            schedule.tripId = "trip_1"
            schedule.stopId = "stop_1"
            schedule.departureTime = now.plus(minutes: 10)
        }
        let prediction1 = objects.prediction(schedule: schedule1) { prediction in
            prediction.id = "prediction_1"
            prediction.stopId = "stop_1"
            prediction.tripId = "trip_1"
            prediction.routeId = "route_1"
            prediction.directionId = 0
            prediction.departureTime = now.plus(minutes: 10)
        }
        let upcoming1 = UpcomingTrip(trip: trip1, schedule: schedule1, prediction: prediction1)

        let leaf = makeLeaf(
            route: route,
            stop: stop,
            patterns: [pattern],
            upcomingTrips: [upcoming1],
            objects: objects,
            subwayServiceStartTime: subwayStartTime
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: trip1.directionId),
            tripFilter: .init(tripId: trip1.id, vehicleId: nil, stopSequence: nil, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        ).environmentObject(ViewportProvider()).withFixedSettings([:])
        XCTAssertNil(try? sut.inspect().find(text: "Good morning!"))
    }

    func testShowsEarlyMorningCard() throws {
        let now = EasternTimeInstant(year: 2025, month: .november, day: 17, hour: 3, minute: 30, second: 0)
        let subwayStartTime = EasternTimeInstant(year: 2025, month: .november, day: 17, hour: 9, minute: 44, second: 0)
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.id = "stop_1" }
        let route = objects.route { route in
            route.id = "route_1"
            route.type = .lightRail
            route.routePatternIds = ["pattern_1"]
        }
        let pattern = objects.routePattern(route: route) { pattern in
            pattern.id = "pattern_1"
            pattern.directionId = 0
            pattern.representativeTripId = "trip_1"
        }
        let trip1 = objects.trip { trip in
            trip.id = "trip_1"
            trip.routeId = "route_1"
            trip.directionId = 0
            trip.routePatternId = "pattern_1"
        }
        let schedule1 = objects.schedule { schedule in
            schedule.tripId = "trip_1"
            schedule.stopId = "stop_1"
            schedule.departureTime = now.plus(minutes: 10)
        }
        let upcoming1 = UpcomingTrip(trip: trip1, schedule: schedule1, prediction: nil)

        let leaf = makeLeaf(
            route: route,
            stop: stop,
            patterns: [pattern],
            upcomingTrips: [upcoming1],
            objects: objects,
            subwayServiceStartTime: subwayStartTime
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: trip1.directionId),
            tripFilter: .init(tripId: trip1.id, vehicleId: nil, stopSequence: nil, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: now,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        ).environmentObject(ViewportProvider()).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(text: "Good morning!"))
    }

    func testShowsWorldCupBlurb() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = WorldCupService.shared.route
        objects.put(object: route)
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: .init(
                lineOrRoute: .Route(route: route),
                stop: stop,
                direction: .init(directionId: 0, route: route),
                routePatterns: [WorldCupService.shared.routePatternOutbound],
                stopIds: [],
                upcomingTrips: [],
                alertsHere: [],
                allDataLoaded: true,
                hasSchedulesToday: false,
                subwayServiceStartTime: nil,
                alertsDownstream: [],
                context: .stopDetailsFiltered
            ),
            selectedDirection: .init(directionId: 0, route: route),
            favorite: false,
            now: .now(),
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            navManager: .init(),
        ).environmentObject(ViewportProvider()).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(text: "Service from South Station to today’s World Cup match"))
        XCTAssertNotNil(try sut.inspect().find(text: "Boston Stadium Train ticket required"))
        XCTAssertNotNil(try sut.inspect().find(text: "View details"))
    }
}
