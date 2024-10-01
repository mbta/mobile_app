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
            globalRepository: MockGlobalRepository(),
            nearbyVM: .init()
        )

        let hasAppeared = sut.on(\.didAppear) { view in
            XCTAssertNotNil(try view.find(LoadingCard<Text>.self))
        }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 5)
        wait(for: [getNearbyExpectation], timeout: 5)
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
                trip.routePatternId = pattern.id
            }
        }
        let rp50 = objects.routePattern(route: route52) { pattern in
            pattern.id = "52-5-0"
            pattern.sortOrder = 505_200_000
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Dedham Mall"
                trip.routePatternId = pattern.id
            }
        }
        let rp41 = objects.routePattern(route: route52) { pattern in
            pattern.id = "52-4-1"
            pattern.sortOrder = 505_201_010
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Watertown Yard"
                trip.routePatternId = pattern.id
            }
        }
        let rp51 = objects.routePattern(route: route52) { pattern in
            pattern.id = "52-5-1"
            pattern.sortOrder = 505_201_000
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Watertown Yard"
                trip.routePatternId = pattern.id
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
        let trip1 = objects.trip {
            $0.headsign = "Dedham Mall"
            $0.routePatternId = "52-5-0"
        }
        objects.schedule { schedule in
            schedule.departureTime = time1
            schedule.routeId = "52"
            schedule.stopId = "8552"
            schedule.tripId = trip1.id
        }

        // schedule & prediction
        let notTime2 = Date.now.addingTimeInterval(9 * 60).toKotlinInstant()
        let time2 = Date.now.addingTimeInterval(10 * 60).toKotlinInstant()
        let trip2 = objects.trip {
            $0.headsign = "Charles River Loop"
            $0.routePatternId = "52-4-0"
        }
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
        let trip3 = objects.trip {
            $0.headsign = "Watertown Yard"
            $0.routePatternId = "52-4-1"
        }
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
            nearbyVM: .init(),
            scheduleResponse: .init(objects: objects)
        )

        let exp = sut.on(\.didAppear) { view in
            try view.vStack().callOnChange(newValue: PredictionsStreamDataResponse(objects: objects))
            let patterns = view.findAll(HeadsignRowView.self)

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
            nearbyVM: .init(),
            scheduleResponse: .init(objects: objects)
        )

        ViewHosting.host(view: sut)
        wait(for: [schedulesFetchedExp], timeout: 1)
    }

    @MainActor func testWithPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
        let now = Date.now
        let distantMinutes: Double = 10
        let distantInstant = now.addingTimeInterval(distantMinutes * 60).toKotlinInstant()
        let objects = ObjectCollectionBuilder()
        let route = objects.route()

        let rp1 = objects.routePattern(route: route) { routePattern in
            routePattern.id = "52-5-0"
            routePattern.representativeTrip { representativeTrip in
                representativeTrip.headsign = "Dedham Mall"
                representativeTrip.routePatternId = routePattern.id
            }
        }
        let rp2 = objects.routePattern(route: route) { routePattern in
            routePattern.id = "52-4-1"
            routePattern.representativeTrip { representativeTrip in
                representativeTrip.headsign = "Watertown Yard"
                representativeTrip.routePatternId = routePattern.id
            }
        }
        objects.prediction { prediction in
            prediction.arrivalTime = now.addingTimeInterval(distantMinutes * 60).toKotlinInstant()
            prediction.departureTime = now.addingTimeInterval((distantMinutes + 2) * 60).toKotlinInstant()
            prediction.routeId = "52"
            prediction.stopId = "8552"
            prediction.tripId = objects.trip(routePattern: rp1).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = now.addingTimeInterval((distantMinutes + 1) * 60).toKotlinInstant()
            prediction.departureTime = now.addingTimeInterval((distantMinutes + 5) * 60).toKotlinInstant()
            prediction.status = "Overridden"
            prediction.routeId = "52"
            prediction.stopId = "8552"
            prediction.tripId = objects.trip(routePattern: rp1).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = now.addingTimeInterval(1 * 60 + 1).toKotlinInstant()
            prediction.departureTime = now.addingTimeInterval(2 * 60).toKotlinInstant()
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
            nearbyVM: .init(),
            now: now
        )

        let exp = sut.on(\.didAppear) { view in
            try view.vStack().callOnChange(newValue: predictions)
            let stops = view.findAll(NearbyStopView.self)
            XCTAssertNotNil(try stops[0].find(text: "Charles River Loop")
                .parent().parent().find(text: "Predictions unavailable"))

            XCTAssertNotNil(try stops[0].find(text: "Dedham Mall")
                .parent().parent().find(text: "10 min"))
            XCTAssertNotNil(try stops[0].find(text: "Dedham Mall")
                .parent().parent().find(text: "Overridden"))

            XCTAssertNotNil(try stops[1].find(text: "Watertown Yard")
                .parent().parent().find(text: "1 min"))
            let expectedMinutes = distantMinutes
            let expectedState = UpcomingTripView.State.some(.Minutes(minutes: Int32(expectedMinutes)))
            XCTAssert(try !stops[1].find(text: "Watertown Yard").parent().parent()
                .findAll(UpcomingTripView.self, where: { sut in
                    try sut.actualView().prediction == expectedState
                }).isEmpty)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testWithPredictionsV2() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
        let now = Date.now
        let distantMinutes: Double = 10
        let distantInstant = now.addingTimeInterval(distantMinutes * 60).toKotlinInstant()
        let objects = ObjectCollectionBuilder()
        let route = objects.route()

        let rp1 = objects.routePattern(route: route) { routePattern in
            routePattern.id = "52-5-0"
            routePattern.representativeTrip { representativeTrip in
                representativeTrip.headsign = "Dedham Mall"
                representativeTrip.routePatternId = routePattern.id
            }
        }
        let rp2 = objects.routePattern(route: route) { routePattern in
            routePattern.id = "52-4-1"
            routePattern.representativeTrip { representativeTrip in
                representativeTrip.headsign = "Watertown Yard"
                representativeTrip.routePatternId = routePattern.id
            }
        }
        objects.prediction { prediction in
            prediction.arrivalTime = now.addingTimeInterval(distantMinutes * 60).toKotlinInstant()
            prediction.departureTime = now.addingTimeInterval((distantMinutes + 2) * 60).toKotlinInstant()
            prediction.routeId = "52"
            prediction.stopId = "8552"
            prediction.tripId = objects.trip(routePattern: rp1).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = now.addingTimeInterval((distantMinutes + 1) * 60).toKotlinInstant()
            prediction.departureTime = now.addingTimeInterval((distantMinutes + 5) * 60).toKotlinInstant()
            prediction.status = "Overridden"
            prediction.routeId = "52"
            prediction.stopId = "8552"
            prediction.tripId = objects.trip(routePattern: rp1).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = now.addingTimeInterval(1 * 60 + 1).toKotlinInstant()
            prediction.departureTime = now.addingTimeInterval(2 * 60).toKotlinInstant()
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
        let predictionsByStop: PredictionsByStopJoinResponse = .init(objects: objects)

        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            settingsRepository: MockSettingsRepository(settings: [.init(key: .predictionsV2Channel, isOn: true)]),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            nearbyVM: .init(),
            now: now
        )

        let exp = sut.on(\.didAppear) { view in
            try view.vStack().callOnChange(newValue: predictionsByStop)
            let stops = view.findAll(NearbyStopView.self)
            XCTAssertNotNil(try stops[0].find(text: "Charles River Loop")
                .parent().parent().find(text: "Predictions unavailable"))

            XCTAssertNotNil(try stops[0].find(text: "Dedham Mall")
                .parent().parent().find(text: "10 min"))
            XCTAssertNotNil(try stops[0].find(text: "Dedham Mall")
                .parent().parent().find(text: "Overridden"))

            XCTAssertNotNil(try stops[1].find(text: "Watertown Yard")
                .parent().parent().find(text: "1 min"))
            let expectedMinutes = distantMinutes
            let expectedState = UpcomingTripView.State.some(.Minutes(minutes: Int32(expectedMinutes)))
            XCTAssert(try !stops[1].find(text: "Watertown Yard").parent().parent()
                .findAll(UpcomingTripView.self, where: { sut in
                    try sut.actualView().prediction == expectedState
                }).isEmpty)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testLineGrouping() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        let greenLineState = NearbyViewModel.NearbyTransitState(
            loadedLocation: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyByRouteAndStop: GreenLineTestHelper.companion.nearbyData
        )

        let distantInstant = Date.now.addingTimeInterval(10 * 60)
            .toKotlinInstant()
        typealias Green = GreenLineTestHelper.Companion
        let objects = Green.shared.objects

        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(1 * 60 + 1).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(2 * 60).toKotlinInstant()
            prediction.routeId = Green.shared.routeB.id
            prediction.stopId = Green.shared.stopWestbound.id
            prediction.tripId = objects.trip(routePattern: Green.shared.rpB0).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(2 * 60 + 10).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(3 * 60).toKotlinInstant()
            prediction.routeId = Green.shared.routeB.id
            prediction.stopId = Green.shared.stopEastbound.id
            prediction.tripId = objects.trip(routePattern: Green.shared.rpB1).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(3 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(4 * 60).toKotlinInstant()
            prediction.routeId = Green.shared.routeC.id
            prediction.stopId = Green.shared.stopWestbound.id
            prediction.tripId = objects.trip(routePattern: Green.shared.rpC0).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(11 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(15 * 60).toKotlinInstant()
            prediction.status = "Overridden"
            prediction.routeId = Green.shared.routeC.id
            prediction.stopId = Green.shared.stopWestbound.id
            prediction.tripId = objects.trip(routePattern: Green.shared.rpC0).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(4 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(5 * 60).toKotlinInstant()
            prediction.routeId = Green.shared.routeC.id
            prediction.stopId = Green.shared.stopEastbound.id
            prediction.tripId = objects.trip(routePattern: Green.shared.rpC1).id
        }
        objects.prediction { prediction in
            prediction.departureTime = distantInstant
            prediction.routeId = Green.shared.routeC.id
            prediction.stopId = Green.shared.stopEastbound.id
            prediction.tripId = objects.trip(routePattern: Green.shared.rpC1).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(5 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(6 * 60).toKotlinInstant()
            prediction.routeId = Green.shared.routeE.id
            prediction.stopId = Green.shared.stopWestbound.id
            prediction.tripId = objects.trip(routePattern: Green.shared.rpE0).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = Date.now.addingTimeInterval(6 * 60).toKotlinInstant()
            prediction.departureTime = Date.now.addingTimeInterval(7 * 60).toKotlinInstant()
            prediction.routeId = Green.shared.routeE.id
            prediction.stopId = Green.shared.stopEastbound.id
            prediction.tripId = objects.trip(routePattern: Green.shared.rpE1).id
        }
        let predictions: PredictionsStreamDataResponse = .init(objects: Green.shared.objects)

        let globalLoadedPublisher = PassthroughSubject<Void, Never>()

        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(response: predictions),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(greenLineState),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            globalRepository: MockGlobalRepository(response: .init(objects: objects, patternIdsByStop: [:])) {
                globalLoadedPublisher.send()
            },
            nearbyVM: .init()
        )

        let exp = sut.inspection.inspect(onReceive: globalLoadedPublisher, after: 0.2) { view in
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
                onReceive _: @escaping (ApiResult<PredictionsStreamDataResponse>)
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

            func connectV2(stopIds _: [String],
                           onJoin _: @escaping (ApiResult<PredictionsByStopJoinResponse>) -> Void,
                           onMessage _: @escaping (ApiResult<PredictionsByStopMessageResponse>) -> Void) {
                /* no-op */
            }

            var lastUpdated: Instant?

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
                    vehicleType: nil,
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
            nearbyVM: .init()
        )

        func prediction(minutesAway: Double) -> PredictionsStreamDataResponse {
            let objects = ObjectCollectionBuilder()
            let trip = objects.trip { trip in
                trip.headsign = "Dedham Mall"
                trip.routePatternId = "52-5-0"
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

    func testRendersUpdatedPredictionsV2() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            settingsRepository: MockSettingsRepository(settings: [.init(key: .predictionsV2Channel, isOn: true)]),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            nearbyVM: .init()
        )

        func prediction(minutesAway: Double) -> PredictionsByStopJoinResponse {
            let objects = ObjectCollectionBuilder()
            let trip = objects.trip { trip in
                trip.headsign = "Dedham Mall"
                trip.routePatternId = "52-5-0"
            }
            objects.prediction { prediction in
                prediction.departureTime = Date.now.addingTimeInterval(minutesAway * 60).toKotlinInstant()
                prediction.routeId = "52"
                prediction.stopId = "8552"
                prediction.tripId = trip.id
            }
            return PredictionsByStopJoinResponse(objects: objects)
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

        let predictionsRepo = MockPredictionsRepository(
            onConnect: { joinExpectation.fulfill() },
            onConnectV2: {},
            onDisconnect: { leaveExpectation.fulfill() }
        )
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
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

        let predictionsRepo = MockPredictionsRepository(onConnect: { joinExpectation.fulfill() },
                                                        onConnectV2: {}, onDisconnect: { leaveExpectation.fulfill() })

        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
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

        let predictionsRepo = MockPredictionsRepository(
            onConnect: { joinExpectation.fulfill() },
            onConnectV2: {},
            onDisconnect: { leaveExpectation.fulfill() }
        )
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
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
            nearbyVM: .init()
        )
        XCTAssertNotNil(try sut.inspect().view(NearbyTransitView.self)
            .find(text: "Failed to load nearby transit, test error"))
    }

    @MainActor func testPredictionErrorMessage() throws {
        let predictionsErroredPublisher = PassthroughSubject<Bool, Never>()
        let state = NearbyViewModel.NearbyTransitState(
            loadedLocation: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyByRouteAndStop: NearbyStaticData(data: [])
        )

        let predictionsRepo = MockPredictionsRepository(onConnect: { predictionsErroredPublisher.send(true) },
                                                        onConnectV2: {},
                                                        onDisconnect: {},
                                                        connectOutcome:
                                                        ApiResultError(
                                                            code: nil,
                                                            message: SocketError.shared.FAILURE
                                                        ),
                                                        connectV2Outcome: nil)
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(state),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            nearbyVM: .init()
        )
        let exp = sut.inspection.inspect(onReceive: predictionsErroredPublisher, after: 1) { view in
            XCTAssertEqual(try view.actualView().predictionsError, SocketError.shared.FAILURE)
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
            nearbyVM: .init()
        )

        let exp = sut.on(\.didAppear) { view in
            try view.vStack().callOnChange(newValue: AlertsStreamDataResponse(objects: objects))
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
                    alertsHere: nil,
                    hasSchedulesToday: true
                )]

            ),
            now: Date.now.toKotlinInstant(),
            pushNavEntry: pushNavEntry,
            pinned: false
        )

        try sut.inspect().find(DestinationRowView.self).parent().parent().parent().button().tap()

        wait(for: [stopEntryPushedExp], timeout: 2)
    }

    func testEmptyFallback() throws {
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(.init(loadedLocation: .init(), nearbyByRouteAndStop: .init(data: []))),
            location: .constant(ViewportProvider.Defaults.center),
            nearbyVM: .init()
        )

        let hasAppeared = sut.on(\.didAppear) { view in
            XCTAssertNil(try? view.find(LoadingCard<Text>.self))
            XCTAssertNotNil(try view.find(text: "No nearby MBTA stops"))
            XCTAssertNotNil(try view.find(text: "Your current location is outside of our search area."))
        }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 5)
    }
}
