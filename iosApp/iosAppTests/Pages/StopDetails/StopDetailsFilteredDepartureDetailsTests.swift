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
        objects: ObjectCollectionBuilder = ObjectCollectionBuilder()
    ) -> RouteCardData.Leaf {
        makeLeaf(
            lineOrRoute: .route(route),
            stop: stop, patterns: patterns, upcomingTrips: upcomingTrips,
            alerts: alerts, alertsDownstream: alertsDownstream, objects: objects
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
        objects: ObjectCollectionBuilder = ObjectCollectionBuilder()
    ) -> RouteCardData.Leaf {
        let routes = routes ?? Set([objects.route { $0.lineId = line.id }])
        return makeLeaf(
            lineOrRoute: .line(line, routes),
            stop: stop, patterns: patterns, upcomingTrips: upcomingTrips,
            alerts: alerts, alertsDownstream: alertsDownstream, objects: objects
        )
    }

    func makeLeaf(
        lineOrRoute: RouteCardData.LineOrRoute,
        stop: Stop? = nil,
        patterns: [RoutePattern]? = nil,
        upcomingTrips: [UpcomingTrip]? = nil,
        alerts: [Shared.Alert] = [],
        alertsDownstream: [Shared.Alert] = [],
        objects: ObjectCollectionBuilder = ObjectCollectionBuilder()
    ) -> RouteCardData.Leaf {
        let route = lineOrRoute.sortRoute
        let stop = stop ?? objects.stop { _ in }
        let patterns = patterns ?? [objects.routePattern(route: route) { _ in }]
        let upcomingTrips = upcomingTrips ?? []

        let leaf = RouteCardData.Leaf(
            lineOrRoute: lineOrRoute,
            stop: stop,
            directionId: 0,
            routePatterns: patterns,
            stopIds: Set([stop.id]).union(Set(stop.childStopIds)),
            upcomingTrips: upcomingTrips,
            alertsHere: alerts, allDataLoaded: true, hasSchedulesToday: true,
            alertsDownstream: alertsDownstream,
            context: .stopDetailsFiltered
        )

        return leaf
    }

    func testDisplaysTrips() throws {
        let objects = ObjectCollectionBuilder()
        let now = Date.now
        let stop = objects.stop { _ in }
        let route = objects.route()
        let trip1 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "A" }
            prediction.departureTime = (now + 15).toKotlinInstant()
        })
        let trip2 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "B" }
            prediction.departureTime = (now + 3 * 60).toKotlinInstant()
        })
        let trip3 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "C" }
            prediction.departureTime = (now + 7 * 60).toKotlinInstant()
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
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider()).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "A"))
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(text: "B"))
        XCTAssertNotNil(try sut.inspect().find(text: "3 min"))
        XCTAssertNotNil(try sut.inspect().find(text: "C"))
        XCTAssertNotNil(try sut.inspect().find(text: "7 min"))
    }

    @MainActor
    func testUpdatesTilesWhenNowChanges() throws {
        let objects = ObjectCollectionBuilder()
        let now = Date.now
        let stop = objects.stop { _ in }
        let route = objects.route()
        let trip1 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "A" }
            prediction.departureTime = (now + 15).toKotlinInstant()
        })

        let leaf = makeLeaf(route: route, stop: stop, upcomingTrips: [trip1], objects: objects)

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
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        )

        let updateNowExp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "A"))
            XCTAssertNotNil(try view.find(text: "ARR"))
            try view.find(ViewType.VStack.self).callOnChange(newValue: now + 120)
            XCTAssertThrowsError(try view.find(text: "ARR"))
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))
        wait(for: [updateNowExp], timeout: 2)
    }

    func testShowsHeadsignAndPillsWhenBranchingLine() throws {
        let objects = ObjectCollectionBuilder()
        let now = Date.now
        let stop = objects.stop { _ in }
        let line = objects.line { line in line.id = "Green" }
        let routeB = objects.route { route in
            route.id = "Green-B"
            route.lineId = line.id
        }
        let routePatternB = objects.routePattern(route: routeB) { pattern in
            pattern.representativeTrip { $0.headsign = "Headsign B" }
        }
        let trip1 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: routePatternB)
            prediction.departureTime = (now + 15).toKotlinInstant()
        })
        let trip2 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: routePatternB)
            prediction.departureTime = (now + 3 * 60).toKotlinInstant()
        })
        let routeC = objects.route { route in
            route.id = "Green-C"
            route.lineId = line.id
        }
        let routePatternC = objects.routePattern(route: routeC) { pattern in
            pattern.representativeTrip { $0.headsign = "Headsign C" }
        }
        let trip3 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: routePatternC)
            prediction.departureTime = (now + 7 * 60).toKotlinInstant()
        })

        let leaf = makeLeaf(
            line: line,
            routes: [routeB, routeC],
            stop: stop,
            patterns: [routePatternB, routePatternC],
            upcomingTrips: [trip1, trip2, trip3],
            objects: objects
        )

        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.global = .init(objects: objects)

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
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        ).environmentObject(ViewportProvider()).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self).find(text: trip1.headsign))
        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self).find(RoutePill.self))
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(text: "3 min"))
        XCTAssertNotNil(try sut.inspect().find(text: "7 min"))
    }

    func testShowsTripDetails() throws {
        let objects = ObjectCollectionBuilder()
        let now = Date.now
        let stop = objects.stop { _ in }
        let route = objects.route { route in route.id = "Green-B" }
        let pattern = objects.routePattern(route: route) { pattern in
            pattern.representativeTrip { $0.headsign = "Matching" }
        }
        let line = objects.line { line in line.id = "Green" }
        let trip1 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: pattern)
            prediction.departureTime = now.addingTimeInterval(120).toKotlinInstant()
        })
        let trip2 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: pattern)
            prediction.departureTime = (now + 3 * 60).toKotlinInstant()
        })
        let trip3 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: pattern)
            prediction.departureTime = (now + 7 * 60).toKotlinInstant()
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
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider()).withFixedSettings([:])

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
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider()).withFixedSettings([:])
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
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider()).withFixedSettings([:])

        XCTAssertThrowsError(try sut.inspect().find(TripDetailsView.self))
        XCTAssertThrowsError(try sut.inspect().find(DepartureTile.self))
        XCTAssertNotNil(try sut.inspect().find(StopDetailsNoTripCard.self))
    }

    @MainActor
    func testShowsSuspensionFallback() throws {
        let objects = ObjectCollectionBuilder()
        let now = Date.now
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.header = "Fuchsia Line suspended from Here to There"
        }

        // in practice any trips should be skipped but for major alerts we want to hide trips if they somehow aren't
        // skipped
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { _ in }
            prediction.departureTime = (now + 15).toKotlinInstant()
        })

        let nearbyVM = NearbyViewModel()

        let stopDetailsVM: StopDetailsViewModel = .init()
        stopDetailsVM.global = GlobalResponse(objects: objects)

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

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))
        wait(for: [departureTileExp, alertCardExp], timeout: 2)
    }

    @MainActor
    func testShowsDownstreamAlertFallback() throws {
        let objects = ObjectCollectionBuilder()
        let now = Date.now
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.header = "Fuchsia Line suspended from Here to There"
        }
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { _ in }
            prediction.departureTime = (now + 15).toKotlinInstant()
        })

        let nearbyVM = NearbyViewModel()

        let stopDetailsVM: StopDetailsViewModel = .init()
        stopDetailsVM.global = GlobalResponse(objects: objects)

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

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))
        wait(for: [departureTileExp, alertCardExp], timeout: 2)
    }

    func testShowsElevatorAlertOnlyOnce() throws {
        let objects = ObjectCollectionBuilder()
        let now = Date.now
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
            alert.activePeriod(start: (now - 30 * 60).toKotlinInstant(), end: nil)
        }
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "A" }
            prediction.departureTime = (now + 15).toKotlinInstant()
        })

        let nearbyVM = NearbyViewModel()
        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.setAlertSummaries([alert.id: nil])

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
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        ).environmentObject(ViewportProvider()).withFixedSettings([.stationAccessibility: true])

        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self))
        XCTAssertNotNil(try sut.inspect().find(AlertCard.self))
        XCTAssertNil(try? sut.inspect().find(text: "Elevator Closure"))
        XCTAssertNotNil(try sut.inspect().find(text: alert.header!))
        try sut.inspect().find(AlertCard.self).implicitAnyView().button().tap()
        XCTAssertEqual(
            nearbyVM.navigationStack.last,
            .alertDetails(alertId: alert.id, line: nil, routes: nil, stop: stop)
        )
    }

    func testShowsInaccessible() throws {
        let objects = ObjectCollectionBuilder()
        let now = Date.now
        let stop = objects.stop { stop in
            stop.wheelchairBoarding = .inaccessible
        }
        let route = objects.route { _ in }
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { _ in }
            prediction.departureTime = (now + 15).toKotlinInstant()
        })
        let nearbyVM = NearbyViewModel()
        let stopDetailsVM = StopDetailsViewModel()

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
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        ).environmentObject(ViewportProvider()).withFixedSettings([.stationAccessibility: true])

        XCTAssertNotNil(try sut.inspect().find(text: "This stop is not accessible"))
    }

    @MainActor
    func testShowsSubwayDelayAlertFallback() throws {
        let objects = ObjectCollectionBuilder()
        let now = Date.now
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
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip { $0.headsign = "A" }
            prediction.departureTime = (now + 15).toKotlinInstant()
        })

        let nearbyVM = NearbyViewModel()
        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.global = GlobalResponse(objects: objects)

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

        ViewHosting
            .host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([.stationAccessibility: true]))
        wait(for: [departureTileExp, alertCardExp], timeout: 2)
    }

    @MainActor
    func testShowsPredictionsAndAlertOnBranchingTrunk() async throws {
        let now = Date.now

        let objects = Shared.TestData.clone()
        let stop = objects.getStop(id: "place-kencl")
        let line = objects.getLine(id: "line-Green")
        let routeB = objects.getRoute(id: "Green-B")
        let routeC = objects.getRoute(id: "Green-C")
        let routeD = objects.getRoute(id: "Green-D")

        let alert =
            objects.alert { alert in
                alert.activePeriod(
                    start: now.addingTimeInterval(-5).toKotlinInstant(),
                    end: now.addingTimeInterval(100).toKotlinInstant()
                )
                alert.effect = .shuttle
                alert.header = "Green line shuttle on B and C branches"
                alert.informedEntity(
                    activities: [.board, .exit, .ride],
                    directionId: 0, facility: nil,
                    route: routeB.id, routeType: nil,
                    stop: "71151", trip: nil
                )
                alert.informedEntity(
                    activities: [.board, .exit, .ride],
                    directionId: 0, facility: nil,
                    route: routeC.id, routeType: nil,
                    stop: "70151", trip: nil
                )
            }
        let alertResponse = AlertsStreamDataResponse(alerts: [alert.id: alert])

        objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.departureTime = now.addingTimeInterval(300).toKotlinInstant()
            prediction.routeId = routeD.id
            prediction.stopId = stop.id
            prediction.trip = objects.trip { trip in
                trip.routeId = routeD.id
                trip.routePatternId = "Green-D-855-0"
            }
        })

        let global = GlobalResponse(objects: objects)
        let routeCardData = try await RouteCardData.companion.routeCardsForStopList(
            stopIds: [stop.id] + stop.childStopIds,
            globalData: global,
            sortByDistanceFrom: nil,
            schedules: ScheduleResponse(objects: objects),
            predictions: PredictionsStreamDataResponse(objects: objects),
            alerts: alertResponse,
            now: now.toKotlinInstant(),
            pinnedRoutes: [],
            context: .stopDetailsFiltered
        )!.first!
        let routeStopData = routeCardData.stopData.first!
        let leaf = routeStopData.data.first { $0.directionId == 0 }!

        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.global = GlobalResponse(objects: objects)

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: line.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            leaf: leaf,
            selectedDirection: .init(name: nil, destination: nil, id: 0),
            favorite: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
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
}
