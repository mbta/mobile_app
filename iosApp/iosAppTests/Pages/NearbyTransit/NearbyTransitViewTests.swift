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
import Shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

// swiftlint:disable:next type_body_length
final class NearbyTransitViewTests: XCTestCase {
    private let pinnedRoutesRepository = MockPinnedRoutesRepository()
    private let noNearbyStops = { NoNearbyStopsView(onOpenSearch: {}, onPanToDefaultCenter: {}) }
    private var cancellables = Set<AnyCancellable>()

    class FakeNearbyVM: NearbyViewModel {
        let expectation: XCTestExpectation
        let closure: (CLLocationCoordinate2D) -> Void

        init(_ expectation: XCTestExpectation, _ closure: @escaping (CLLocationCoordinate2D) -> Void = { _ in }) {
            self.expectation = expectation
            self.closure = closure
            super.init()
        }

        override func getNearbyStops(global _: GlobalResponse, location: CLLocationCoordinate2D) {
            debugPrint("ViewModel getting nearby")
            closure(location)
            expectation.fulfill()
        }
    }

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testPending() throws {
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            location: .constant(ViewportProvider.Defaults.center),
            isReturningFromBackground: .constant(false),
            nearbyVM: .init(),
            noNearbyStops: noNearbyStops
        ).withFixedSettings([.enhancedFavorites: false])
        let cards = try sut.inspect().findAll(RouteCard.self)
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
            location: .constant(ViewportProvider.Defaults.center),
            isReturningFromBackground: .constant(false),
            globalRepository: MockGlobalRepository(),
            nearbyVM: FakeNearbyVM(getNearbyExpectation),
            noNearbyStops: noNearbyStops
        )

        let hasAppeared = sut.on(\.didAppear) { view in
            let cards = view.findAll(RouteCard.self)
            XCTAssertEqual(cards.count, 5)
            for card in cards {
                XCTAssertNotNil(try card.modifier(LoadingPlaceholderModifier.self))
            }
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 5)
        wait(for: [getNearbyExpectation], timeout: 5)
    }

    func route52Objects() -> ObjectCollectionBuilder {
        let objects = ObjectCollectionBuilder()
        let route52 = objects.route { route in
            route.id = "52"
            route.type = .bus
            route.shortName = "52"
            route.directionNames = ["Outbound", "Inbound"]
            route.directionDestinations = ["Dedham Mall", "Watertown Yard"]
        }
        let stop1 = objects.stop { stop in
            stop.id = "8552"
            stop.name = "Sawmill Brook Pkwy @ Walsh Rd"
            stop.wheelchairBoarding = .accessible
        }
        let stop2 = objects.stop { stop in
            stop.id = "84791"
            stop.name = "Sawmill Brook Pkwy @ Walsh Rd - opposite side"
        }
        // In reality, 52-4-0 and 52-4-1 have typicality: .deviation,
        // but these tests are from before we started hiding deviations with no predictions,
        // and it's easier to just fudge the data than to rewrite the tests.
        objects.routePattern(route: route52) { pattern in
            pattern.id = "52-4-0"
            pattern.directionId = 0
            pattern.sortOrder = 505_200_020
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Charles River Loop"
                trip.routePatternId = pattern.id
                trip.stopIds = [stop1.id]
            }
        }
        objects.routePattern(route: route52) { pattern in
            pattern.id = "52-5-0"
            pattern.directionId = 0
            pattern.sortOrder = 505_200_000
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Dedham Mall"
                trip.routePatternId = pattern.id
                trip.stopIds = [stop1.id]
            }
        }
        objects.routePattern(route: route52) { pattern in
            pattern.id = "52-4-1"
            pattern.directionId = 1
            pattern.sortOrder = 505_201_010
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Watertown Yard"
                trip.routePatternId = pattern.id
                trip.stopIds = [stop2.id]
            }
        }
        objects.routePattern(route: route52) { pattern in
            pattern.id = "52-5-1"
            pattern.directionId = 1
            pattern.sortOrder = 505_201_000
            pattern.typicality = .typical
            pattern.representativeTrip { trip in
                trip.headsign = "Watertown Yard"
                trip.routePatternId = pattern.id
                trip.stopIds = [stop2.id]
            }
        }

        return objects
    }

    var mockLocation = CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)

    func getNearbyState(objects: ObjectCollectionBuilder) -> NearbyViewModel.NearbyTransitState {
        .init(
            loadedLocation: mockLocation,
            // swiftlint:disable:next force_cast
            stopIds: objects.stops.allKeys.map { $0 as! String }
        )
    }

    struct LoadedStops {
        let predictionStops: [String]
        let scheduleStops: [String]
    }

    func setUpSut(
        _ objects: ObjectCollectionBuilder,
        _ loadPublisher: PassthroughSubject<LoadedStops, Never>,
        now: EasternTimeInstant? = nil,
        _ pinnedRoutes: Set<String> = []
    ) -> NearbyTransitView {
        let nearbyVM = NearbyViewModel()
        nearbyVM.nearbyState = getNearbyState(objects: objects)
        nearbyVM.alerts = .init(objects: objects)

        let predictionPub = PassthroughSubject<[String], Never>()
        let schedulePub = PassthroughSubject<[String], Never>()
        let pinnedRoutesPub = PassthroughSubject<[String], Never>()

        let pinnedRoutesRepository = MockPinnedRoutesRepository(initialPinnedRoutes: pinnedRoutes)

        Publishers.Zip(predictionPub, schedulePub).sink { predictionStops, scheduleStops in
            loadPublisher.send(
                LoadedStops(predictionStops: predictionStops, scheduleStops: scheduleStops)
            )
        }.store(in: &cancellables)

        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(
                onConnectV2: { predictionStops in predictionPub.send(predictionStops) },
                connectV2Response: .init(objects: objects)
            ),
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { scheduleStops in schedulePub.send(scheduleStops) }
            ),
            location: .constant(mockLocation),
            isReturningFromBackground: .constant(false),
            globalData: .init(objects: objects),
            nearbyVM: nearbyVM,
            scheduleResponse: .init(objects: objects),
            now: now?.toNSDateLosingTimeZone() ?? Date.now,
            predictionsByStop: .init(objects: objects),
            noNearbyStops: noNearbyStops
        )
        sut.globalRepository = MockGlobalRepository(response: .init(objects: objects))

        return sut
    }

    @MainActor func testSortsPinnedRoutesToTopByDefault() throws {
        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        let objects = route52Objects()

        let testData = Shared.TestData.clone()

        objects.put(object: testData.getRoute(id: "15"))
        objects.put(object: testData.getRoutePattern(id: "15-2-0"))
        objects.put(object: testData.getTrip(id: "68166816"))
        objects.put(object: testData.getStop(id: "17863"))

        let sut = setUpSut(objects, loadPublisher, ["52"])

        let exp = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { view in
            let routes = view.findAll(RouteCard.self)

            XCTAssertEqual(routes.count, 2)
            XCTAssertNotNil(try routes[0].find(text: "52"))
            XCTAssertNotNil(try routes[1].find(text: "15"))
        }
        ViewHosting.host(view: sut.withFixedSettings([.enhancedFavorites: false]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testDoesntSortsPinnedRoutesToTopEnhancedFavorites() throws {
        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        let objects = route52Objects()

        let testData = Shared.TestData.clone()

        objects.put(object: testData.getRoute(id: "15"))
        objects.put(object: testData.getRoutePattern(id: "15-2-0"))
        objects.put(object: testData.getTrip(id: "68166816"))
        objects.put(object: testData.getStop(id: "17863"))

        let sut = setUpSut(objects, loadPublisher, ["52"])

        let exp = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { view in
            let routes = view.findAll(RouteCard.self)

            XCTAssertEqual(routes.count, 2)
            XCTAssertNotNil(try routes[0].find(text: "15"))
            XCTAssertNotNil(try routes[1].find(text: "52"))
        }
        ViewHosting.host(view: sut.withFixedSettings([.enhancedFavorites: true]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testRoutePatternsGroupedByRouteAndStop() throws {
        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        let objects = route52Objects()
        let sut = setUpSut(objects, loadPublisher)

        let exp = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { view in
            let routes = view.findAll(RouteCard.self)

            XCTAssert(!routes.isEmpty)
            guard let route = routes.first else { return }

            XCTAssertNotNil(try route.find(text: "52"))
            XCTAssertNotNil(try route.find(text: "Sawmill Brook Pkwy @ Walsh Rd"))
            XCTAssertNotNil(try route.find(text: "Outbound to")
                .find(RouteCardDepartures.self, relation: .parent).find(text: "Dedham Mall"))

            XCTAssertNotNil(try route.find(text: "Sawmill Brook Pkwy @ Walsh Rd - opposite side"))
            XCTAssertNotNil(try route.find(text: "Inbound to")
                .find(RouteCardDepartures.self, relation: .parent).find(text: "Watertown Yard"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testWithSchedules() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
        let now = EasternTimeInstant.now()

        let objects = route52Objects()

        // schedule, no prediction
        let time1 = now.plus(minutes: 45)
        let trip1 = objects.trip {
            $0.headsign = "Dedham Mall"
            $0.routePatternId = "52-5-0"
            $0.routeId = "52"
            $0.directionId = 0
        }
        objects.schedule { schedule in
            schedule.departureTime = time1
            schedule.routeId = "52"
            schedule.stopId = "8552"
            schedule.tripId = trip1.id
        }

        // schedule & prediction
        let notTime2 = now.plus(minutes: 9)
        let time2 = now.plus(minutes: 10)
        let trip2 = objects.trip {
            $0.headsign = "Charles River Loop"
            $0.routePatternId = "52-4-0"
            $0.routeId = "52"
            $0.directionId = 0
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
        let notTime3 = now.plus(minutes: 15)
        let trip3 = objects.trip {
            $0.headsign = "Watertown Yard"
            $0.routePatternId = "52-4-1"
            $0.routeId = "52"
            $0.directionId = 1
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

        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        let sut = setUpSut(objects, loadPublisher)

        let exp = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { view in
            let directions = view.findAll(RouteCardDirection.self)
            if directions.isEmpty {
                XCTFail("no departures found")
                return
            }

            XCTAssertNotNil(try directions[0].find(DirectionLabel.self).find(text: "Outbound to"))
            XCTAssertNotNil(try directions[0].find(HeadsignRowView.self) { headsignRow in
                let headsign = try? headsignRow.find(text: "Charles River Loop")
                let upcomingPrediction = try? headsignRow.find(UpcomingTripView.self)
                let prediction = try? upcomingPrediction?.find(text: "10 min")
                let realtime = try? upcomingPrediction?.find(ViewType.Image.self).actualImage().name()
                return headsign != nil &&
                    prediction != nil
                    && realtime == "live-data"
            })

            XCTAssertNotNil(try directions[0].find(HeadsignRowView.self) { headsignRow in
                let headsign = try? headsignRow.find(text: "Dedham Mall")
                let prediction = try? headsignRow.find(text: "45 min")
                return headsign != nil && prediction != nil
            })

            XCTAssertNotNil(try? directions[1].find(text: "Watertown Yard"))
            XCTAssertEqual(
                try directions[1].find(UpcomingTripView.self).actualView().prediction,
                .noTrips(UpcomingFormat.NoTripsFormatServiceEndedToday())
            )
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [exp], timeout: 1)
    }

    func testSchedulesFetchedOnAppear() throws {
        let objects = ObjectCollectionBuilder()
        objects.route { _ in }
        objects.stop { _ in }

        let schedulesFetchedExp = XCTestExpectation(description: "Schedules fetched")
        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        loadPublisher.sink { _ in schedulesFetchedExp.fulfill() }.store(in: &cancellables)

        let sut = setUpSut(objects, loadPublisher)

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [schedulesFetchedExp], timeout: 2)
    }

    @MainActor func testWithPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
        let now = EasternTimeInstant.now()
        let distantMinutes = 10
        let distantInstant = now.plus(minutes: Int32(distantMinutes))
        let objects = route52Objects()

        let rp1 = objects.routePatterns["52-5-0"] as? RoutePattern
        let rp2 = objects.routePatterns["52-4-1"] as? RoutePattern

        objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: Int32(distantMinutes))
            prediction.departureTime = now.plus(minutes: Int32(distantMinutes + 2))
            prediction.routeId = "52"
            prediction.stopId = "8552"
            prediction.tripId = objects.trip(routePattern: rp1!).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: Int32(distantMinutes + 1))
            prediction.departureTime = now.plus(minutes: Int32(distantMinutes + 5))
            prediction.status = "Overridden"
            prediction.routeId = "52"
            prediction.stopId = "8552"
            prediction.tripId = objects.trip(routePattern: rp1!).id
        }
        objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: 1).plus(seconds: 1)
            prediction.departureTime = now.plus(minutes: 2)
            prediction.routeId = "52"
            prediction.stopId = "84791"
            prediction.tripId = objects.trip(routePattern: rp2!).id
        }
        objects.prediction { prediction in
            prediction.departureTime = distantInstant
            prediction.routeId = "52"
            prediction.stopId = "84791"
            prediction.tripId = objects.trip(routePattern: rp2!).id
        }
        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        let sut = setUpSut(objects, loadPublisher, now: now)

        let exp = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { view in
            let stops = view.findAll(RouteCardDepartures.self)

            let outboundDirection = try stops[0]
                .find(text: "Dedham Mall")
                .find(RouteCardDirection.self, relation: .parent)
            XCTAssertNotNil(outboundDirection)
            XCTAssertNotNil(try outboundDirection.find(text: "10 min"))
            XCTAssertNotNil(try outboundDirection.find(text: "Overridden"))

            let inboundDirection = try stops[1]
                .find(text: "Watertown Yard")
                .find(RouteCardDirection.self, relation: .parent)
            XCTAssertNotNil(inboundDirection)
            XCTAssertNotNil(try inboundDirection.find(text: "1 min"))
            XCTAssertNotNil(try inboundDirection.find(text: "10 min"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    func testRefetchesPredictionsOnNewStops() throws {
        let sawmillAtWalshExpectation = expectation(description: "joins predictions for Sawmill @ Walsh")
        let lechmereExpectation = expectation(description: "joins predictions for Lechmere")

        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        loadPublisher.sink { loaded in
            let stopIds = loaded.predictionStops.sorted()
            if stopIds == ["84791", "8552"] {
                sawmillAtWalshExpectation.fulfill()
            } else if stopIds == ["place-lech"] {
                lechmereExpectation.fulfill()
            } else {
                XCTFail("unexpected stop IDs \(stopIds)")
            }
        }.store(in: &cancellables)

        let sut = setUpSut(route52Objects(), loadPublisher)
        ViewHosting.host(view: sut.withFixedSettings([.enhancedFavorites: false]))

        wait(for: [sawmillAtWalshExpectation], timeout: 1)

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ["place-lech"])

        wait(for: [lechmereExpectation], timeout: 1)
    }

    func testDoesntRefetchPredictionsOnStopReorder() throws {
        let sawmillAtWalshExpectation = expectation(description: "joins predictions for Sawmill @ Walsh")
        let reorderExpectation = expectation(description: "doesn't rejoin when stop order changes")
        reorderExpectation.isInverted = true

        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        loadPublisher.sink { loaded in
            let stopIds = loaded.predictionStops.sorted()
            if stopIds == ["84791", "8552"] {
                sawmillAtWalshExpectation.fulfill()
            } else if stopIds == ["8552", "84791"] {
                reorderExpectation.fulfill()
            } else {
                XCTFail("unexpected stop IDs \(stopIds)")
            }
        }.store(in: &cancellables)

        let sut = setUpSut(route52Objects(), loadPublisher)
        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [sawmillAtWalshExpectation], timeout: 1)

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ["8552", "84791"])

        wait(for: [reorderExpectation], timeout: 1)
    }

    func testFetchesPredictionsWhenNoStops() throws {
        let joinsPredictionsExpectation = expectation(description: "joins predictions")

        let objects = ObjectCollectionBuilder()

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        var sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(
                onConnectV2: { _ in joinsPredictionsExpectation.fulfill() },
                connectV2Response: .init(objects: objects)
            ),
            schedulesRepository: MockScheduleRepository(scheduleResponse: .init(objects: objects)),
            location: .constant(mockLocation),
            isReturningFromBackground: .constant(false),
            globalData: .init(objects: objects),
            nearbyVM: nearbyVM,
            scheduleResponse: .init(objects: objects),
            now: Date.now,
            predictionsByStop: .init(objects: objects),
            noNearbyStops: noNearbyStops
        )
        sut.globalRepository = MockGlobalRepository(response: .init(objects: objects))

        let hasAppeared = sut.on(\.didAppear) { view in
            let cards = view.findAll(RouteCard.self)
            XCTAssertEqual(cards.count, 5)
            for card in cards {
                XCTAssertNotNil(try card.modifier(LoadingPlaceholderModifier.self))
            }
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [hasAppeared], timeout: 1)

        nearbyVM.nearbyState = getNearbyState(objects: objects)

        wait(for: [joinsPredictionsExpectation], timeout: 1)
    }

    @MainActor func testRendersUpdatedPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
        let loadPublisher = PassthroughSubject<LoadedStops, Never>()

        let objects = route52Objects()
        let sut = setUpSut(objects, loadPublisher)

        ViewHosting.host(view: sut.withFixedSettings([:]))

        func prediction(minutesAway: Int32) -> PredictionsByStopJoinResponse {
            let rp1 = objects.routePatterns["52-5-0"] as? RoutePattern
            objects.prediction { prediction in
                prediction.id = "prediction"
                prediction.arrivalTime = EasternTimeInstant.now().plus(minutes: minutesAway)
                prediction.departureTime = EasternTimeInstant.now().plus(minutes: minutesAway)
                prediction.routeId = "52"
                prediction.stopId = "8552"
                prediction.tripId = objects.trip(routePattern: rp1!).id
            }

            return PredictionsByStopJoinResponse(objects: objects)
        }

        func changeParams(objects: ObjectCollectionBuilder) -> NearbyTransitView.RouteCardParams {
            .init(
                state: getNearbyState(objects: objects),
                global: .init(objects: objects),
                schedules: .init(objects: objects),
                predictions: .init(objects: objects),
                alerts: .init(objects: objects),
                now: Date.now,
                pinnedRoutes: pinnedRoutesRepository.pinnedRoutes
            )
        }

        let changePublisher = PassthroughSubject<Void, Never>()

        let loadExp = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { _ in
            let newPredictions = prediction(minutesAway: 2)
            sut.predictionsByStop = newPredictions
            try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: changeParams(objects: objects))
            changePublisher.send()
        }

        let changeExp1 = sut.inspection.inspect(onReceive: changePublisher, after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "2 min"))
            let newPredictions = prediction(minutesAway: 3)
            sut.predictionsByStop = newPredictions
            try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: changeParams(objects: objects))
            changePublisher.send()
        }

        let changeExp2 = sut.inspection.inspect(onReceive: changePublisher.dropFirst(), after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "3 min"))
        }

        wait(for: [loadExp, changeExp1, changeExp2], timeout: 3)
    }

    func testLeavesChannelWhenBackgrounded() throws {
        let joinExpectation = expectation(description: "joins predictions")
        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(
            onConnect: {},
            onConnectV2: { _ in joinExpectation.fulfill() },
            onDisconnect: { leaveExpectation.fulfill() }
        )
        let objects = route52Objects()
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        nearbyVM.nearbyState = getNearbyState(objects: objects)
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            isReturningFromBackground: .constant(false),
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [joinExpectation], timeout: 1)
        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)
    }

    func testLeavesChannelWhenInactive() throws {
        let joinExpectation = expectation(description: "joins predictions")
        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(
            onConnectV2: { _ in joinExpectation.fulfill() },
            onDisconnect: { leaveExpectation.fulfill() }
        )

        let objects = route52Objects()
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        nearbyVM.nearbyState = getNearbyState(objects: objects)
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            isReturningFromBackground: .constant(false),
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

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
        let objects = route52Objects()
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        nearbyVM.nearbyState = getNearbyState(objects: objects)
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            isReturningFromBackground: .constant(false),
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.active)

        wait(for: [joinExpectation], timeout: 1)
    }

    @MainActor func testScrollToTopWhenNearbyChanges() throws {
        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        let scrollPositionSetExpectation = XCTestExpectation(description: "component scrolled")
        let sut = setUpSut(route52Objects(), loadPublisher)

        let exp = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { view in
            let actualView = try view.actualView()
            actualView.scrollSubject.sink { _ in
                scrollPositionSetExpectation.fulfill()
            }.store(in: &self.cancellables)
            actualView.nearbyVM.nearbyState.stopIds = ["new-stop"]
            try actualView.inspect().implicitAnyView().vStack()
                .callOnChange(newValue: ["new-stop"])
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp, scrollPositionSetExpectation], timeout: 2)
    }

    @MainActor
    func testNearbyErrorMessage() throws {
        let repositories = MockRepositories()
        repositories.errorBanner = MockErrorBannerStateRepository(state: .DataError(messages: [], action: {}))
        loadKoinMocks(repositories: repositories)
        let sut = NearbyTransitView(
            togglePinnedUsecase: TogglePinnedRouteUsecase(repository: pinnedRoutesRepository),
            pinnedRouteRepository: pinnedRoutesRepository,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
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

    @MainActor func testNoService() throws {
        let objects = route52Objects()
        objects.alert { alert in
            alert.activePeriod(start: EasternTimeInstant.now().minus(seconds: 1), end: nil)
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

        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        let sut = setUpSut(objects, loadPublisher)

        let exp = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "Suspension")
                .find(RouteCardDepartures.self, relation: .parent).find(text: "Dedham Mall"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testElevatorClosed() throws {
        let objects = route52Objects()
        objects.alert { alert in
            alert.activePeriod(start: EasternTimeInstant.now().minus(seconds: 1), end: nil)
            alert.effect = .elevatorClosure
            alert.informedEntity(
                activities: [.usingWheelchair],
                directionId: nil,
                facility: nil,
                route: nil,
                routeType: nil,
                stop: "8552",
                trip: nil
            )
        }
        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        let sut = setUpSut(objects, loadPublisher)

        let exp = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "1 elevator closed"))
        }

        ViewHosting.host(view: sut.withFixedSettings([.stationAccessibility: true]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testDisplaysWheelchairNotAccessibile() throws {
        let objects = route52Objects()
        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        let sut = setUpSut(objects, loadPublisher)

        let exp = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { view in
            XCTAssertThrowsError(try view.find(text: "1 elevator closed"))
            XCTAssertNotNil(try view.find(viewWithTag: "wheelchair_not_accessible"))
        }
        ViewHosting.host(view: sut.withFixedSettings([.stationAccessibility: true]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testEmptyFallback() throws {
        let loadPublisher = PassthroughSubject<LoadedStops, Never>()
        let sut = setUpSut(.init(), loadPublisher)

        let hasAppeared = sut.inspection.inspect(onReceive: loadPublisher, after: 0.5) { view in
            XCTAssertNil(try? view.find(LoadingCard<Text>.self))
            XCTAssertNotNil(try view.find(text: "No nearby stops"))
            XCTAssertNotNil(try view.find(text: "You’re outside the MBTA service area."))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 2)
    }
}
