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
@_spi(Experimental) import MapboxMaps
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

// swiftlint:disable:next type_body_length
final class NearbyTransitViewTests: XCTestCase {
    private let pinnedRoutesRepository = MockPinnedRoutesRepository()
    private let noNearbyStops = { NoNearbyStopsView(hideMaps: false, onOpenSearch: {}, onPanToDefaultCenter: {}) }
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
            isReturningFromBackground: .constant(false),
            nearbyVM: .init(),
            noNearbyStops: noNearbyStops
        )
        let cards = try sut.inspect().findAll(NearbyRouteView.self)
        XCTAssertEqual(cards.count, 5)
        for card in cards {
            XCTAssertNotNil(try card.modifier(LoadingPlaceholderModifier.self))
        }
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
            isReturningFromBackground: .constant(false),
            globalRepository: MockGlobalRepository(),
            nearbyVM: .init(),
            noNearbyStops: noNearbyStops
        )

        let hasAppeared = sut.on(\.didAppear) { view in
            let cards = view.findAll(NearbyRouteView.self)
            XCTAssertEqual(cards.count, 5)
            for card in cards {
                XCTAssertNotNil(try card.modifier(LoadingPlaceholderModifier.self))
            }
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
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(connectV2Outcome: .companion.empty),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            isReturningFromBackground: .constant(false),
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )
        let exp = sut.on(\.didLoadData) { view in
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

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: IdleScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            isReturningFromBackground: .constant(false),
            nearbyVM: nearbyVM,
            scheduleResponse: .init(objects: objects),
            noNearbyStops: noNearbyStops
        )

        let exp = sut.on(\.didAppear) { view in
            try view.implicitAnyView().vStack().callOnChange(newValue: PredictionsByStopJoinResponse(objects: objects))
            let patterns = view.findAll(HeadsignRowView.self)

            XCTAssertEqual(try patterns[0].actualView().headsign, "Dedham Mall")
            let upcomingSchedule = try patterns[0].find(UpcomingTripView.self)
            XCTAssertEqual(
                try upcomingSchedule.actualView().prediction,
                .some(.ScheduleMinutes(minutes: 45))
            )

            XCTAssertEqual(try patterns[1].actualView().headsign, "Charles River Loop")
            let upcomingPrediction = try patterns[1].find(UpcomingTripView.self)
            XCTAssertEqual(
                try upcomingPrediction.actualView().prediction,
                .some(.Minutes(minutes: 10))
            )
            XCTAssertEqual(try upcomingPrediction.find(ViewType.Image.self).actualImage().name(), "live-data")

            XCTAssertEqual(try patterns[2].actualView().headsign, "Watertown Yard")
            XCTAssertEqual(try patterns[2].find(UpcomingTripView.self).actualView().prediction, .serviceEndedToday)
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
            isReturningFromBackground: .constant(false),
            nearbyVM: .init(),
            scheduleResponse: .init(objects: objects),
            noNearbyStops: noNearbyStops
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

        let predictionsByStop: PredictionsByStopJoinResponse = .init(objects: objects)

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            isReturningFromBackground: .constant(false),
            nearbyVM: nearbyVM,
            now: now,
            noNearbyStops: noNearbyStops
        )

        let exp = sut.on(\.didAppear) { view in
            try view.implicitAnyView().vStack().callOnChange(newValue: predictionsByStop)
            let stops = view.findAll(NearbyStopView.self)
            XCTAssertNotNil(try stops[0].find(text: "Charles River Loop")
                .parent().parent().find(ViewType.ProgressView.self))

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

        typealias Green = GreenLineTestHelper.Companion
        let greenLineState = NearbyViewModel.NearbyTransitState(
            loadedLocation: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyByRouteAndStop: Green.shared.nearbyData
        )

        let distantInstant = Date.now.addingTimeInterval(10 * 60)
            .toKotlinInstant()

        let objects = Green.shared.objects

        objects.schedule { schedule in
            schedule.arrivalTime = Date.now.addingTimeInterval(1 * 60 + 1).toKotlinInstant()
            schedule.departureTime = Date.now.addingTimeInterval(2 * 60).toKotlinInstant()
            schedule.stopId = Green.shared.stopWestbound.id
            schedule.trip = objects.trip(routePattern: Green.shared.rpB0)
        }
        objects.schedule { schedule in
            schedule.arrivalTime = Date.now.addingTimeInterval(2 * 60 + 10).toKotlinInstant()
            schedule.departureTime = Date.now.addingTimeInterval(3 * 60).toKotlinInstant()
            schedule.stopId = Green.shared.stopEastbound.id
            schedule.trip = objects.trip(routePattern: Green.shared.rpB1)
        }

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
        let predictions: PredictionsByStopJoinResponse = .init(objects: Green.shared.objects)

        let globalLoadedPublisher = PassthroughSubject<Void, Never>()
        let globalResponse = GlobalResponse(objects: objects)
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(connectV2Outcome: predictions),
            schedulesRepository: MockScheduleRepository(scheduleResponse: .init(objects: objects), callback: { _ in }),
            getNearby: { _, _ in },
            state: .constant(greenLineState),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            isReturningFromBackground: .constant(false),
            globalRepository: MockGlobalRepository(response: globalResponse) {
                globalLoadedPublisher.send()
            },
            globalData: globalResponse,
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )

        let exp = sut.inspection.inspect(onReceive: globalLoadedPublisher, after: 1) { view in
            let stops = view.findAll(NearbyStopView.self)
            XCTAssertEqual(stops[0].findAll(DestinationRowView.self).count, 3)

            let kenmoreDirection = try stops[0].find(text: "Kenmore & West")
                .find(DirectionRowView.self, relation: .parent)
            try debugPrint(kenmoreDirection.actualView().predictions)
            XCTAssertNotNil(try kenmoreDirection.find(text: "1 min"))
            XCTAssertNotNil(try kenmoreDirection.find(text: "3 min"))
            XCTAssertNotNil(try kenmoreDirection.find(text: "Overridden"))

            XCTAssertNotNil(try stops[0].find(text: "Heath Street")
                .parent().parent().find(text: "5 min"))

            let parkDirection = try stops[0].find(text: "Park St & North")
                .find(DirectionRowView.self, relation: .parent)
            try debugPrint(parkDirection.actualView().predictions)
            XCTAssertNotNil(try parkDirection.find(text: "2 min"))
            XCTAssertNotNil(try parkDirection.find(text: "4 min"))
            XCTAssertNotNil(try parkDirection.find(text: "6 min"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
    }

    func testRefetchesPredictionsOnNewStops() throws {
        let sawmillAtWalshExpectation = expectation(description: "joins predictions for Sawmill @ Walsh")
        let lechmereExpectation = expectation(description: "joins predictions for Lechmere")

        let predictionsRepo = MockPredictionsRepository(onConnect: {}, onConnectV2: { stopIds in
            if stopIds.sorted() == ["84791", "8552"] {
                sawmillAtWalshExpectation.fulfill()
            } else if stopIds == ["place-lech"] {
                lechmereExpectation.fulfill()
            } else {
                XCTFail("unexpected stop IDs \(stopIds)")
            }
        }, onDisconnect: {})

        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            isReturningFromBackground: .constant(false),
            nearbyVM: .init(),
            noNearbyStops: noNearbyStops
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
        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: newState as NearbyStaticData?)

        wait(for: [lechmereExpectation], timeout: 1)
    }

    func testRendersUpdatedPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            isReturningFromBackground: .constant(false),
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
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
            try view.implicitAnyView().vStack().callOnChange(newValue: prediction(minutesAway: 2))
            XCTAssertNotNil(try view.implicitAnyView().vStack().find(text: "2 min"))
            try view.implicitAnyView().vStack().callOnChange(newValue: prediction(minutesAway: 3))
            XCTAssertNotNil(try view.implicitAnyView().vStack().find(text: "3 min"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    func testLeavesChannelWhenBackgrounded() throws {
        let joinExpectation = expectation(description: "joins predictions")
        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(
            onConnect: {},
            onConnectV2: { _ in joinExpectation.fulfill() },
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
            isReturningFromBackground: .constant(false),
            nearbyVM: .init(),
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut)

        wait(for: [joinExpectation], timeout: 1)
        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)
    }

    func testLeavesChannelWhenInactive() throws {
        let joinExpectation = expectation(description: "joins predictions")
        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(onConnect: {},
                                                        onConnectV2: { _ in joinExpectation.fulfill() },
                                                        onDisconnect: { leaveExpectation.fulfill() })

        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            isReturningFromBackground: .constant(false),
            nearbyVM: .init(),
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut)

        wait(for: [joinExpectation], timeout: 1)
        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)
    }

    func testRejoinsChannelWhenReactivated() throws {
        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        joinExpectation.assertForOverFulfill = true

        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(
            onConnect: {},
            onConnectV2: { _ in joinExpectation.fulfill() },
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
            isReturningFromBackground: .constant(false),
            nearbyVM: .init(),
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut)

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.active)

        wait(for: [joinExpectation], timeout: 1)
    }

    func testScrollToTopWhenNearbyChanges() throws {
        let scrollPositionSetExpectation = XCTestExpectation()
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(connectV2Outcome: .companion.empty),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            isReturningFromBackground: .constant(false),
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )
        let exp = sut.on(\.didAppear) { view in
            let actualView = try view.actualView()
            actualView.scrollSubject.sink { _ in
                scrollPositionSetExpectation.fulfill()
            }.store(in: &self.cancellables)
            try actualView.inspect().implicitAnyView().vStack()
                .callOnChange(newValue: self.route52State.nearbyByRouteAndStop)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp, scrollPositionSetExpectation], timeout: 1)
    }

    @MainActor
    func testNearbyErrorMessage() throws {
        loadKoinMocks(repositories: MockRepositories.companion
            .buildWithDefaults(errorBanner: MockErrorBannerStateRepository(state: .DataError(action: {}))))
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(.init()),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            isReturningFromBackground: .constant(false),
            nearbyVM: .init(),
            noNearbyStops: noNearbyStops
        )

        sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.view(NearbyTransitView.self)
                .find(text: "Error loading data"))
        }
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
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)
        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(connectV2Outcome: .companion.empty),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(route52State),
            location: .constant(CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)),
            isReturningFromBackground: .constant(false),
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )

        let exp = sut.on(\.didLoadData) { view in
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
                    upcomingTrips: []

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
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])

        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(connectV2Outcome: .companion.empty),
            schedulesRepository: MockScheduleRepository(),
            getNearby: { _, _ in },
            state: .constant(.init(loadedLocation: .init(), nearbyByRouteAndStop: .init(data: []))),
            location: .constant(ViewportProvider.Defaults.center),
            isReturningFromBackground: .constant(false),
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )

        let hasAppeared = sut.on(\.didLoadData) { view in
            XCTAssertNil(try? view.find(LoadingCard<Text>.self))
            XCTAssertNotNil(try view.find(text: "No nearby stops"))
            XCTAssertNotNil(try view.find(text: "You’re outside the MBTA service area."))
        }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 5)
    }
}
