//
//  TripDetailsPageTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-05-08.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import shared
import ViewInspector
import XCTest

final class TripDetailsPageTests: XCTestCase {
    func testLoadsStopList() throws {
        let objects = ObjectCollectionBuilder()

        let stop1 = objects.stop { stop in
            stop.name = "Somewhere"
        }
        let stop2 = objects.stop { stop in
            stop.name = "Elsewhere"
        }

        let prediction = objects.prediction { prediction in
            prediction.stopId = stop2.id
            prediction.stopSequence = 2
            prediction.departureTime = Date.now.toKotlinInstant()
        }

        let globalFetcher = GlobalFetcher(backend: IdleBackend())
        globalFetcher.response = .init(objects: objects, patternIdsByStop: [:])

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripSchedulesRepository = FakeTripSchedulesRepository(
            response: TripSchedulesResponse.StopIds(stopIds: [stop1.id, stop2.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripPredictionsFetcher = TripPredictionsFetcher(socket: MockSocket())
        tripPredictionsFetcher.predictions = .init(objects: objects)

        let tripId = "123"
        let vehicleId = "999"
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            target: nil,
            globalFetcher: globalFetcher,
            tripPredictionsFetcher: tripPredictionsFetcher,
            tripSchedulesRepository: tripSchedulesRepository
        )

        let showsStopsExp = sut.inspection.inspect(onReceive: tripSchedulesLoaded, after: 0.1) { view in
            XCTAssertNotNil(try view.find(text: "Somewhere"))
            XCTAssertNotNil(try view.find(text: "Elsewhere"))
            XCTAssertNotNil(try view.find(text: "Elsewhere").parent().find(text: "ARR"))
        }

        ViewHosting.host(view: sut)

        wait(for: [showsStopsExp], timeout: 1)
    }

    class FakeTripSchedulesRepository: ITripSchedulesRepository {
        let response: TripSchedulesResponse
        let onGetTripSchedules: (() -> Void)?

        init(response: TripSchedulesResponse, onGetTripSchedules: (() -> Void)? = nil) {
            self.response = response
            self.onGetTripSchedules = onGetTripSchedules
        }

        func __getTripSchedules(tripId _: String) async throws -> TripSchedulesResponse {
            onGetTripSchedules?()
            return response
        }
    }
}
