//
//  NearbyTransitViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-01-22.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
@testable import iosApp
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest
@_spi(Experimental) import MapboxMaps

// swiftlint:disable:next type_body_length
final class NearbyTransitViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    private let pinnedRoutesRepository = MockPinnedRoutesRepository()
    private var cancellables = Set<AnyCancellable>()

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testPending() throws {
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(.init()),
            location: .constant(ViewportProvider.Defaults.center),
            alerts: nil,
            nearbyVM: .init()
        )
        XCTAssertNotNil(try sut.inspect().find(LoadingCard<Text>.self))
    }

    func testLoading() throws {
        let getNearbyExpectation = expectation(description: "getNearby")

        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in getNearbyExpectation.fulfill() },
            state: .constant(.init()),
            location: .constant(ViewportProvider.Defaults.center),
            alerts: nil,
            globalRepository: MockGlobalRepository(),
            nearbyVM: .init()
        )

        let hasAppeared = sut.on(\.didAppear) { view in
            XCTAssertNotNil(try view.find(LoadingCard<Text>.self))
        }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 5)
        wait(for: [getNearbyExpectation], timeout: 1)
    }

    var route52NearbyData: NearbyStaticData {
        let objects = ObjectCollectionBuilder()
        let route52 = objects.route { route in
            route.id = "52"
            route.type = .bus
            route.shortName = "52"
        }
        let stop1 = objects.stop { stop in
            stop.id = "8552"
            stop.name = "Sawmill Brook Pkwy @ Walsh Rd"
        }
        let stop2 = objects.stop { stop in
            stop.id = "84791"
            stop.name = "Sawmill Brook Pkwy @ Walsh Rd - opposite side"
        }
        // In reality, 52-4-0 and 52-4-1 have typicality: .deviation,
        // but these tests are from before we started hiding deviations with no predictions,
        // and it's easier to just fudge the data than to rewrite the tests.
        let rp40 = objects.routePattern(route: route52) { pattern in
            pattern.id = "52-4-0"
            pattern.sortOrder = 505_200_020
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Charles River Loop"
            }
        }
        let rp50 = objects.routePattern(route: route52) { pattern in
            pattern.id = "52-5-0"
            pattern.sortOrder = 505_200_000
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Dedham Mall"
            }
        }
        let rp41 = objects.routePattern(route: route52) { pattern in
            pattern.id = "52-4-1"
            pattern.sortOrder = 505_201_010
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Watertown Yard"
            }
        }
        let rp51 = objects.routePattern(route: route52) { pattern in
            pattern.id = "52-5-1"
            pattern.sortOrder = 505_201_000
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Watertown Yard"
            }
        }
        return NearbyStaticData.companion.build { builder in
            builder.route(route: route52) { builder in
                builder.stop(stop: stop1) { builder in
                    builder.headsign(headsign: "Charles River Loop", patterns: [rp40])
                    builder.headsign(headsign: "Dedham Mall", patterns: [rp50])
                }
                builder.stop(stop: stop2) { builder in
                    builder.headsign(headsign: "Watertown Yard", patterns: [rp41, rp51])
                }
            }
        }
    }

    var route52State: NearbyViewModel.NearbyTransitState {
        NearbyViewModel.NearbyTransitState(
            loadedLocation: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyByRouteAndStop: route52NearbyData
        )
    }

    func testRoutePatternsGroupedByRouteAndStop() throws {
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )
        let exp = sut.on(\.didAppear) { view in
            let routes = view.findAll(NearbyRouteView.self)
            XCTAssert(!routes.isEmpty)
            guard let route = routes.first else { return }

            XCTAssertNotNil(try route.find(text: "52"))
            XCTAssertNotNil(try route.find(text: "Sawmill Brook Pkwy @ Walsh Rd")
                .find(NearbyStopView.self, relation: .parent).find(text: "Charles River Loop"))
            XCTAssertNotNil(try route.find(text: "Sawmill Brook Pkwy @ Walsh Rd")
                .find(NearbyStopView.self, relation: .parent).find(text: "Dedham Mall"))

            XCTAssertNotNil(try route.find(text: "Sawmill Brook Pkwy @ Walsh Rd - opposite side")
                .find(NearbyStopView.self, relation: .parent).find(text: "Watertown Yard"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testWithSchedules() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        let objects = ObjectCollectionBuilder()

        // schedule, no prediction
        let time1 = Date.now.addingTimeInterval(45 * 60).toKotlinInstant()
        let trip1 = objects.trip { $0.headsign = "Dedham Mall" }
        objects.schedule { schedule in
            schedule.departureTime = time1
            schedule.routeId = "52"
            schedule.stopId = "8552"
            schedule.tripId = trip1.id
        }

        // schedule & prediction
        let notTime2 = Date.now.addingTimeInterval(9 * 60).toKotlinInstant()
        let time2 = Date.now.addingTimeInterval(10 * 60).toKotlinInstant()
        let trip2 = objects.trip { $0.headsign = "Charles River Loop" }
        let sched2 = objects.schedule { schedule in
            schedule.departureTime = notTime2
            schedule.routeId = "52"
            schedule.stopId = "8552"
            schedule.tripId = trip2.id
            schedule.stopSequence = 13
        }
        objects.prediction(schedule: sched2) { prediction in
            prediction.departureTime = time2
        }

        // schedule & cancellation
        let notTime3 = Date.now.addingTimeInterval(15 * 60).toKotlinInstant()
        let trip3 = objects.trip { $0.headsign = "Watertown Yard" }
        let sched3 = objects.schedule { schedule in
            schedule.departureTime = notTime3
            schedule.routeId = "52"
            schedule.stopId = "84791"
            schedule.tripId = trip3.id
            schedule.stopSequence = 13
        }
        objects.prediction(schedule: sched3) { prediction in
            prediction.departureTime = nil
            prediction.scheduleRelationship = .cancelled
        }

        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: IdleScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init(),
            scheduleResponse: .init(objects: objects)
        )

        let exp = sut.on(\.didAppear) { view in
            try view.vStack().callOnChange(newValue: PredictionsStreamDataResponse(objects: objects))
            let patterns = try view.findAll(HeadsignRowView.self)

            XCTAssertEqual(try patterns[0].actualView().headsign, "Dedham Mall")
            let upcomingSchedule = try patterns[0].find(UpcomingTripView.self)
            XCTAssertEqual(
                try upcomingSchedule.actualView().prediction,
                .some(.Schedule(scheduleTime: time1))
            )
            XCTAssertEqual(try upcomingSchedule.find(ViewType.Image.self).actualImage().name(), "fa-clock")

            XCTAssertEqual(try patterns[1].actualView().headsign, "Charles River Loop")
            XCTAssertEqual(
                try patterns[1].find(UpcomingTripView.self).actualView().prediction,
                .some(.Minutes(minutes: 10))
            )

            XCTAssertEqual(try patterns[2].actualView().headsign, "Watertown Yard")
            XCTAssertEqual(try patterns[2].find(UpcomingTripView.self).actualView().prediction, .none)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    func testSchedulesFetchedOnAppear() throws {
        let objects = ObjectCollectionBuilder()
        objects.route { _ in }
        objects.stop { _ in }

        let schedulesFetchedExp = XCTestExpectation(description: "Schedules fetched")

        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(scheduleResponse: .init(objects: objects),
                                                        callback: { _ in schedulesFetchedExp.fulfill() }),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init(),
            scheduleResponse: .init(objects: objects)
        )

        ViewHosting.host(view: sut)
        wait(for: [schedulesFetchedExp], timeout: 1)
    }

    @MainActor func testWithPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        let distantInstant = Date.now.addingTimeInterval(5 * 60)
            .toKotlinInstant().plus(duration: DISTANT_FUTURE_CUTOFF)
        let objects = ObjectCollectionBuilder()
        let route = objects.route()

        let rp1 = objects.routePattern(route: route) { routePattern in
            routePattern.representativeTrip { representativeTrip in
                representativeTrip.headsign = "Dedham Mall"
            }
        }
        let rp2 = objects.routePattern(route: route) { routePattern in
            routePattern.representativeTrip { representativeTrip in
                representativeTrip.headsign = "Watertown Yard"
            }
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(10 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(12 * 60).toKotlinInstant()
            prediction.routeId = "52"
            prediction.stopId = "8552"
            prediction.tripId = objects.trip(routePattern: rp1).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(11 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(15 * 60).toKotlinInstant()
            prediction.status = "Overridden"
            prediction.routeId = "52"
            prediction.stopId = "8552"
            prediction.tripId = objects.trip(routePattern: rp1).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(1 * 60 + 1).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(2 * 60).toKotlinInstant()
            prediction.routeId = "52"
            prediction.stopId = "84791"
            prediction.tripId = objects.trip(routePattern: rp2).id
        }
        objects.prediction { prediction in
            prediction.departureTime = distantInstant
            prediction.routeId = "52"
            prediction.stopId = "84791"
            prediction.tripId = objects.trip(routePattern: rp2).id
        }
        let predictions: PredictionsStreamDataResponse = .init(objects: objects)

        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )

        let exp = sut.on(\.didAppear) { view in
            try view.vStack().callOnChange(newValue: predictions)
            let stops = view.findAll(NearbyStopView.self)
            XCTAssertNotNil(try stops[0].find(text: "Charles River Loop")
                .parent().parent().find(text: "No real-time data"))

            XCTAssertNotNil(try stops[0].find(text: "Dedham Mall")
                .parent().parent().find(text: "10 min"))
            XCTAssertNotNil(try stops[0].find(text: "Dedham Mall")
                .parent().parent().find(text: "Overridden"))

            XCTAssertNotNil(try stops[1].find(text: "Watertown Yard")
                .parent().parent().find(text: "1 min"))

            let expectedState = UpcomingTripView.State
                .some(.DistantFuture(predictionTime: distantInstant))
            XCTAssert(try !stops[1].find(text: "Watertown Yard").parent().parent()
                .findAll(UpcomingTripView.self, where: { sut in
                    try debugPrint(sut.actualView())
                    return try sut.actualView().prediction == expectedState
                }).isEmpty)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testLineGrouping() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        let distantInstant = Date.now.addingTimeInterval(5 * 60)
            .toKotlinInstant().plus(duration: DISTANT_FUTURE_CUTOFF)
        typealias Green = GreenLineHelper
        let objects = Green.objects

        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(1 * 60 + 1).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(2 * 60).toKotlinInstant()
            prediction.routeId = Green.routeB.id
            prediction.stopId = Green.stopWestbound.id
            prediction.tripId = objects.trip(routePattern: Green.rpB0).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(2 * 60 + 10).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(3 * 60).toKotlinInstant()
            prediction.routeId = Green.routeB.id
            prediction.stopId = Green.stopEastbound.id
            prediction.tripId = objects.trip(routePattern: Green.rpB1).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(3 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(4 * 60).toKotlinInstant()
            prediction.routeId = Green.routeC.id
            prediction.stopId = Green.stopWestbound.id
            prediction.tripId = objects.trip(routePattern: Green.rpC0).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(11 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(15 * 60).toKotlinInstant()
            prediction.status = "Overridden"
            prediction.routeId = Green.routeC.id
            prediction.stopId = Green.stopWestbound.id
            prediction.tripId = objects.trip(routePattern: Green.rpC0).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(4 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(5 * 60).toKotlinInstant()
            prediction.routeId = Green.routeC.id
            prediction.stopId = Green.stopEastbound.id
            prediction.tripId = objects.trip(routePattern: Green.rpC1).id
        }
        objects.prediction { prediction in
            prediction.departureTime = distantInstant
            prediction.routeId = Green.routeC.id
            prediction.stopId = Green.stopEastbound.id
            prediction.tripId = objects.trip(routePattern: Green.rpC1).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(5 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(6 * 60).toKotlinInstant()
            prediction.routeId = Green.routeE.id
            prediction.stopId = Green.stopWestbound.id
            prediction.tripId = objects.trip(routePattern: Green.rpE0).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(6 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(7 * 60).toKotlinInstant()
            prediction.routeId = Green.routeE.id
            prediction.stopId = Green.stopEastbound.id
            prediction.tripId = objects.trip(routePattern: Green.rpE1).id
        }
        let predictions: PredictionsStreamDataResponse = .init(objects: Green.objects)

        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(Green.state),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )

        let exp = sut.on(\.didAppear) { view in
            try view.vStack().callOnChange(newValue: predictions)
            let stops = view.findAll(NearbyStopView.self)
            XCTAssertEqual(stops[0].findAll(DestinationRowView.self).count, 3)

            let kenmoreDirection = try stops[0].find(text: "Kenmore & West")
                .parent().parent().parent().parent()
            XCTAssertNotNil(try kenmoreDirection.find(text: "1 min"))
            XCTAssertNotNil(try kenmoreDirection.find(text: "3 min"))
            XCTAssertNotNil(try kenmoreDirection.find(text: "Overridden"))

            XCTAssertNotNil(try stops[0].find(text: "Heath Street")
                .parent().parent().find(text: "5 min"))

            let parkDirection = try stops[0].find(text: "Park St & North")
                .parent().parent().parent().parent()
            XCTAssertNotNil(try parkDirection.find(text: "2 min"))
            XCTAssertNotNil(try parkDirection.find(text: "4 min"))
            XCTAssertNotNil(try parkDirection.find(text: "6 min"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    func testRefetchesPredictionsOnNewStops() throws {
        let sawmillAtWalshExpectation = expectation(description: "joins predictions for Sawmill @ Walsh")
        let lechmereExpectation = expectation(description: "joins predictions for Lechmere")

        class FakePredictionsRepository: IPredictionsRepository {
            let sawmillAtWalshExpectation: XCTestExpectation
            let lechmereExpectation: XCTestExpectation

            init(sawmillAtWalshExpectation: XCTestExpectation, lechmereExpectation: XCTestExpectation) {
                self.sawmillAtWalshExpectation = sawmillAtWalshExpectation
                self.lechmereExpectation = lechmereExpectation
            }

            func connect(
                stopIds: [String],
                onReceive _: @escaping (Outcome<PredictionsStreamDataResponse, shared.SocketError._ObjectiveCType>)
                    -> Void
            ) {
                if stopIds.sorted() == ["84791", "8552"] {
                    sawmillAtWalshExpectation.fulfill()
                } else if stopIds == ["place-lech"] {
                    lechmereExpectation.fulfill()
                } else {
                    XCTFail("unexpected stop IDs \(stopIds)")
                }
            }

            func disconnect() { /* no-op */ }
        }

        let predictionsRepo = FakePredictionsRepository(
            sawmillAtWalshExpectation: sawmillAtWalshExpectation,
            lechmereExpectation: lechmereExpectation
        )
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )

        ViewHosting.host(view: sut)

        wait(for: [sawmillAtWalshExpectation], timeout: 1)

        let newState = NearbyStaticData.companion.build { builder in
            builder.route(route: sut.state.nearbyByRouteAndStop!.data[0].sortRoute()) { builder in
                let lechmere = Stop(
                    id: "place-lech",
                    latitude: 90.12,
                    longitude: 34.56,
                    name: "Lechmere",
                    locationType: .station,
                    description: nil,
                    platformName: nil,
                    childStopIds: [],
                    connectingStopIds: [],
                    parentStationId: nil
                )
                builder.stop(stop: lechmere) { _ in
                }
            }
        }
        try sut.inspect().vStack().callOnChange(newValue: newState as NearbyStaticData?)

        wait(for: [lechmereExpectation], timeout: 1)
    }

    func testRendersUpdatedPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )

        func prediction(minutesAway: Double) -> PredictionsStreamDataResponse {
            let objects = ObjectCollectionBuilder()
            let trip = objects.trip { trip in
                trip.headsign = "Dedham Mall"
            }
            objects.prediction { prediction in
                prediction.departureTime = Date.now.addingTimeInterval(minutesAway * 60).toKotlinInstant()
                prediction.routeId = "52"
                prediction.stopId = "8552"
                prediction.tripId = trip.id
            }
            return PredictionsStreamDataResponse(objects: objects)
        }

        let exp = sut.on(\.didAppear) { view in
            try view.vStack().callOnChange(newValue: prediction(minutesAway: 2))
            XCTAssertNotNil(try view.vStack().find(text: "2 min"))
            try view.vStack().callOnChange(newValue: prediction(minutesAway: 3))
            XCTAssertNotNil(try view.vStack().find(text: "3 min"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    func testLeavesChannelWhenBackgrounded() throws {
        let joinExpectation = expectation(description: "joins predictions")
        let leaveExpectation = expectation(description: "leaves predictions")

        class FakePredictionsRepository: IPredictionsRepository {
            let joinExpectation: XCTestExpectation
            let leaveExpectation: XCTestExpectation

            init(joinExpectation: XCTestExpectation, leaveExpectation: XCTestExpectation) {
                self.joinExpectation = joinExpectation
                self.leaveExpectation = leaveExpectation
            }

            func connect(
                stopIds _: [String],
                onReceive _: @escaping (Outcome<PredictionsStreamDataResponse, shared.SocketError._ObjectiveCType>)
                    -> Void
            ) {
                joinExpectation.fulfill()
            }

            func disconnect() {
                leaveExpectation.fulfill()
            }
        }

        let predictionsRepo = FakePredictionsRepository(
            joinExpectation: joinExpectation,
            leaveExpectation: leaveExpectation
        )
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )

        ViewHosting.host(view: sut)

        wait(for: [joinExpectation], timeout: 1)
        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)
    }

    func testLeavesChannelWhenInactive() throws {
        let joinExpectation = expectation(description: "joins predictions")
        let leaveExpectation = expectation(description: "leaves predictions")

        class FakePredictionsRepository: IPredictionsRepository {
            let joinExpectation: XCTestExpectation
            let leaveExpectation: XCTestExpectation

            init(joinExpectation: XCTestExpectation, leaveExpectation: XCTestExpectation) {
                self.joinExpectation = joinExpectation
                self.leaveExpectation = leaveExpectation
            }

            func connect(
                stopIds _: [String],
                onReceive _: @escaping (Outcome<PredictionsStreamDataResponse, shared.SocketError._ObjectiveCType>)
                    -> Void
            ) {
                joinExpectation.fulfill()
            }

            func disconnect() {
                leaveExpectation.fulfill()
            }
        }

        let predictionsRepo = FakePredictionsRepository(
            joinExpectation: joinExpectation,
            leaveExpectation: leaveExpectation
        )
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )

        ViewHosting.host(view: sut)

        wait(for: [joinExpectation], timeout: 1)
        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)
    }

    func testRejoinsChannelWhenReactivated() throws {
        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        joinExpectation.assertForOverFulfill = true

        let leaveExpectation = expectation(description: "leaves predictions")

        class FakePredictionsRepository: IPredictionsRepository {
            let joinExpectation: XCTestExpectation
            let leaveExpectation: XCTestExpectation

            init(joinExpectation: XCTestExpectation, leaveExpectation: XCTestExpectation) {
                self.joinExpectation = joinExpectation
                self.leaveExpectation = leaveExpectation
            }

            func connect(
                stopIds _: [String],
                onReceive _: @escaping (Outcome<PredictionsStreamDataResponse, shared.SocketError._ObjectiveCType>)
                    -> Void
            ) {
                joinExpectation.fulfill()
            }

            func disconnect() {
                leaveExpectation.fulfill()
            }
        }

        let predictionsRepo = FakePredictionsRepository(
            joinExpectation: joinExpectation,
            leaveExpectation: leaveExpectation
        )
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )

        ViewHosting.host(view: sut)

        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.active)

        wait(for: [joinExpectation], timeout: 1)
    }

    func testScrollToTopWhenNearbyChanges() throws {
        let scrollPositionSetExpectation = XCTestExpectation()
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )
        let exp = sut.on(\.didAppear) { view in
            let actualView = try view.actualView()
            actualView.scrollSubject.sink { _ in
                scrollPositionSetExpectation.fulfill()
            }.store(in: &self.cancellables)
            try actualView.inspect().vStack().callOnChange(newValue: self.route52State.nearbyByRouteAndStop)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp, scrollPositionSetExpectation], timeout: 1)
    }

    func testNearbyErrorMessage() throws {
        let state = NearbyViewModel.NearbyTransitState(error: "Failed to load nearby transit, test error")
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(state),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )
        XCTAssertNotNil(try sut.inspect().view(NearbyTransitView.self)
            .find(text: "Failed to load nearby transit, test error"))
    }

    func testPredictionErrorMessage() throws {
        let predictionsErroredPublisher = PassthroughSubject<Bool, Never>()
        let state = NearbyViewModel.NearbyTransitState(
            loadedLocation: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyByRouteAndStop: NearbyStaticData(data: [])
        )
        class FakePredictionsRepository: IPredictionsRepository {
            let callback: (() -> Void)?

            init(callback: @escaping (() -> Void)) {
                self.callback = callback
            }

            func connect(
                stopIds _: [String],
                onReceive: @escaping (Outcome<PredictionsStreamDataResponse, shared.SocketError._ObjectiveCType>)
                    -> Void
            ) {
                callback?()
                onReceive(Outcome(data: nil, error: SocketError.unknown.toKotlinEnum()))
            }

            func disconnect() { /* no-op */ }
        }
        let predictionsRepo = FakePredictionsRepository {
            predictionsErroredPublisher.send(true)
        }
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(state),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: nil,
            nearbyVM: .init()
        )
        let exp = sut.inspection.inspect(onReceive: predictionsErroredPublisher, after: 0.2) { view in
            XCTAssertEqual(try view.actualView().predictionsError, shared.SocketError.unknown)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
    }

    func testNoService() throws {
        let objects = ObjectCollectionBuilder()
        objects.alert { alert in
            alert.activePeriod(start: Date.now.addingTimeInterval(-1).toKotlinInstant(), end: nil)
            alert.effect = .suspension
            alert.informedEntity(
                activities: [.board],
                directionId: nil,
                facility: nil,
                route: "52",
                routeType: .bus,
                stop: "8552",
                trip: nil
            )
        }
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            alerts: AlertsStreamDataResponse(objects: objects),
            nearbyVM: .init()
        )

        let exp = sut.on(\.didAppear) { view in
            XCTAssertNotNil(try view.find(text: "Suspension"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    func testStopPageLink() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { pattern in
            pattern.directionId = 1
        }
        let stop = objects.stop { $0.name = "This Stop" }

        let stopEntryPushedExp = XCTestExpectation(description: "pushNavEntry called with stop details")

        func pushNavEntry(navEntry: SheetNavigationStackEntry) {
            if case let .stopDetails(matchedStop, matchedFilter) = navEntry {
                if stop.id == matchedStop.id, matchedFilter?.routeId == route.id,
                   matchedFilter?.directionId == pattern.directionId {
                    stopEntryPushedExp.fulfill()
                }
            }
        }

        let sut = NearbyStopView(
            patternsAtStop: PatternsByStop(
                route: route, stop: stop,
                patterns: [RealtimePatterns.ByHeadsign(
                    route: route,
                    headsign: "Place",
                    line: nil,
                    patterns: [pattern],
                    upcomingTrips: nil,
                    alertsHere: nil
                )]

            ),
            now: Date.now.toKotlinInstant(),
            pushNavEntry: pushNavEntry,
            pinned: false
        )

        try sut.inspect().find(DestinationRowView.self).parent().parent().parent().button().tap()

        wait(for: [stopEntryPushedExp], timeout: 2)
    }
}
