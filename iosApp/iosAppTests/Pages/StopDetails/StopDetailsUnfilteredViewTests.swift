//
//  StopDetailsUnfilteredViewTests.swift
//  iosApp
//
//  Created by Melody Horn on 4/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

@MainActor final class StopDetailsUnfilteredViewTests: XCTestCase {
    // can’t initialize these in place because can’t reference other instance variables while initializing, can’t
    // override init for XCTestCase Swift interop reasons, no equivalent to Kotlin’s lateinit, must make these optional
    // variables and then force-unwrap every time they’re used
    var builder: ObjectCollectionBuilder?
    var now: EasternTimeInstant?
    var route: Route?
    var routePatternOne: RoutePattern?
    var routePatternTwo: RoutePattern?
    var stop: Stop?
    var inaccessibleStop: Stop?
    var globalResponse: GlobalResponse?

    override func setUp() {
        executionTimeAllowance = 60

        builder = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        self.now = now
        route = builder!.route { route in
            route.id = "route_1"
            route.type = .bus
            route.color = "FF0000"
            route.directionNames = ["North", "South"]
            route.directionDestinations = ["Downtown", "Uptown"]
            route.longName = "Sample Route Long Name"
            route.shortName = "Sample Route"
            route.textColor = "000000"
            route.lineId = "line_1"
            route.routePatternIds = ["pattern_1", "pattern_2"]
        }
        routePatternOne = builder!.routePattern(route: route!) { routePattern in
            routePattern.id = "pattern_1"
            routePattern.directionId = 0
            routePattern.name = "Sample Route Pattern"
            routePattern.routeId = "route_1"
            routePattern.representativeTripId = "trip_1"
        }
        routePatternTwo = builder!.routePattern(route: route!) { routePattern in
            routePattern.id = "pattern_2"
            routePattern.directionId = 1
            routePattern.name = "Sample Route Pattern Two"
            routePattern.routeId = "route_1"
            routePattern.representativeTripId = "trip_1"
        }
        stop = builder!.stop { stop in
            stop.id = "stop_1"
            stop.name = "Sample Stop"
            stop.locationType = .stop
            stop.latitude = 0.0
            stop.longitude = 0.0
            stop.wheelchairBoarding = .accessible
        }
        inaccessibleStop = builder!.stop { stop in
            stop.id = "stop_2"
            stop.name = "Inaccessible Stop"
            stop.locationType = .stop
            stop.latitude = 0.0
            stop.longitude = 0.0
            stop.wheelchairBoarding = .inaccessible
        }
        builder!.line { line in
            line.id = "line_1"
            line.color = "FF0000"
            line.textColor = "FFFFFF"
        }
        builder!.trip { trip in
            trip.id = "trip_1"
            trip.routeId = "route_1"
            trip.directionId = 0
            trip.headsign = "Sample Headsign"
            trip.routePatternId = "pattern_1"
        }
        builder!.prediction { prediction in
            prediction.id = "prediction_1"
            prediction.revenue = true
            prediction.stopId = "stop_1"
            prediction.tripId = "trip_1"
            prediction.routeId = "route_1"
            prediction.stopSequence = 1
            prediction.directionId = 0
            prediction.arrivalTime = now.plus(minutes: 1)
            prediction.departureTime = now.plus(minutes: 1).plus(seconds: 30)
        }

        globalResponse = .init(
            objects: builder!,
            patternIdsByStop: [stop!.id: [routePatternOne!.id, routePatternTwo!.id]]
        )
    }

    private let errorBannerViewModel = ErrorBannerViewModel(
        errorRepository: MockErrorBannerStateRepository(),
        initialLoadingWhenPredictionsStale: false
    )

    func testGroupsByDirection() async throws {
        let routeCardData = try await RouteCardData.companion.routeCardsForStopList(
            stopIds: [stop!.id] + stop!.childStopIds,
            globalData: globalResponse!,
            sortByDistanceFrom: nil,
            schedules: nil,
            predictions: .init(objects: builder!),
            alerts: .init(alerts: [:]),
            now: now!,
            context: .stopDetailsUnfiltered
        )

        let nearbyVM = NearbyViewModel()
        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.global = globalResponse!

        let sut = StopDetailsUnfilteredView(
            stopId: stop!.id,
            setStopFilter: { _ in },
            routeCardData: routeCardData,
            now: now!,
            errorBannerVM: errorBannerViewModel,
            nearbyVM: nearbyVM,
            stopDetailsVM: stopDetailsVM
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "Sample Route"))
        XCTAssertNotNil(try sut.inspect().find(text: "Sample Headsign"))
        XCTAssertNotNil(try sut.inspect().find(text: "1 min"))
        XCTAssertThrowsError(try sut.inspect().find(text: "This stop is not accessible"))
    }

    func testInaccessibleByDirection() async throws {
        let routeCardData = try await RouteCardData.companion.routeCardsForStopList(
            stopIds: [inaccessibleStop!.id] + inaccessibleStop!.childStopIds,
            globalData: globalResponse!,
            sortByDistanceFrom: nil,
            schedules: nil,
            predictions: .init(objects: builder!),
            alerts: .init(alerts: [:]),
            now: now!,
            context: .stopDetailsUnfiltered
        )

        let nearbyVM = NearbyViewModel()
        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.global = globalResponse!

        let sut = StopDetailsUnfilteredView(
            stopId: inaccessibleStop!.id,
            setStopFilter: { _ in },
            routeCardData: routeCardData,
            now: now!,
            errorBannerVM: errorBannerViewModel,
            nearbyVM: nearbyVM,
            stopDetailsVM: stopDetailsVM
        ).withFixedSettings([.stationAccessibility: true])

        XCTAssertNotNil(try sut.inspect().find(text: "This stop is not accessible"))
    }

    func testShowsElevatorAlertsWhenGroupedByDirection() async throws {
        let alert = builder!.clone().alert { alert in
            alert.header = "Elevator alert"
            alert.activePeriod(start: Date(timeIntervalSince1970: 0).toEasternInstant(), end: nil)
            alert.effect = .elevatorClosure
            alert.informedEntity(
                activities: [.usingWheelchair],
                directionId: nil,
                facility: nil,
                route: nil,
                routeType: nil,
                stop: self.stop!.id,
                trip: nil
            )
        }
        let routeCardData = try await RouteCardData.companion.routeCardsForStopList(
            stopIds: [stop!.id] + stop!.childStopIds,
            globalData: globalResponse!,
            sortByDistanceFrom: nil,
            schedules: nil,
            predictions: .init(objects: builder!),
            alerts: .init(alerts: [alert.id: alert]),
            now: now!,
            context: .stopDetailsUnfiltered
        )

        let nearbyVM = NearbyViewModel()
        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.global = globalResponse!

        let sut = StopDetailsUnfilteredView(
            stopId: stop!.id,
            setStopFilter: { _ in },
            routeCardData: routeCardData,
            now: now!,
            errorBannerVM: errorBannerViewModel,
            nearbyVM: nearbyVM,
            stopDetailsVM: stopDetailsVM
        ).withFixedSettings([.stationAccessibility: true])
        XCTAssertNotNil(try sut.inspect().find(text: "Elevator alert"))
    }
}
